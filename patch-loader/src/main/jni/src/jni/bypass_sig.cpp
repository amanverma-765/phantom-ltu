#include "bypass_sig.h"

#include <dlfcn.h>
#include <link.h>
#include <sys/syscall.h>

#include <cstring>
#include <mutex>
#include <set>
#include <string>
#include <string_view>

#include <dobby.h>

#include "native_api.h"
#include "elf_util.h"
#include "logging.h"
#include "native_util.h"
#include "../patch_loader.h"
#include "utils/hook_helper.hpp"
#include "utils/jni_helper.hpp"

using lsplant::operator""_sym;

namespace lspd {

std::string apkPath;
std::string redirectPath;

inline static constexpr auto kLibCName = "libc.so";

std::unique_ptr<const SandHook::ElfImg> &GetC(bool release = false) {
    static std::unique_ptr<const SandHook::ElfImg> kImg = nullptr;
    if (release) {
        kImg.reset();
    } else if (!kImg) {
        kImg = std::make_unique<SandHook::ElfImg>(kLibCName);
    }
    return kImg;
}

// Level 2: redirect the app reading its own installed apk to the stored original, so a signature
// check recovers the original signer. Covers every libc file entry point, since open/openat/creat/
// fopen/... all funnel to __openat, which we hook by its prologue.
inline static auto __openat_ =
    "__openat"_sym.hook->*[]<lsplant::Backup auto backup>(int fd, const char *pathname, int flag,
                                                          int mode) static -> int {
    if (pathname == nullptr) return backup(fd, pathname, flag, mode);
    if (pathname == apkPath) {
        LOGD("Redirect openat from %s to %s", pathname, redirectPath.c_str());
        return backup(fd, redirectPath.c_str(), flag, mode);
    }
    return backup(fd, pathname, flag, mode);
};

bool HookOpenat(const lsplant::HookHandler &handler) { return handler(__openat_); }

// ---- Level 3: raw-`svc` apk-read redirect --------------------------------------------------------
//
// A packer that reads its own apk with an inline `svc` never touches libc, so level 2's __openat
// hook does not see it. Here every `svc` in the app's OWN native libraries is instrumented; the
// handler rewrites the path argument of a file syscall that targets base.apk to the stored original,
// exactly as __openat does for the libc path. On arm64 there is no `open`/`stat`/`access` syscall --
// only the `*at` forms, all of which carry the path in x1 -- so one register covers every case.
#if defined(__aarch64__)

namespace {

std::string g_appDirPrefix;             // the /data/app install dir, e.g. "/data/app/~~hash/pkg-hash"
std::string g_originPrefix;             // the origin-apk cache dir the app actually runs its code from
std::mutex g_svcMutex;                  // guards g_instrumented and one-time dlopen hooking
std::set<uintptr_t> g_instrumented;     // svc addresses already handed to Dobby, to dedup rescans

constexpr uint32_t kSvc0 = 0xd4000001;  // `svc #0` on arm64

#ifndef __NR_openat2
#define __NR_openat2 437
#endif

bool isPathSyscall(long nr) {
    switch (nr) {
        case __NR_openat:
#ifdef __NR_openat2
        case __NR_openat2:
#endif
            return true;
        default:
            return false;
    }
}

void svcHandler(void *, DobbyRegisterContext *ctx) {
    long nr = static_cast<long>(ctx->general.regs.x8);
    if (!isPathSyscall(nr)) return;
    auto path = reinterpret_cast<const char *>(ctx->general.regs.x1);
    if (path == nullptr || apkPath.empty()) return;
    if (std::strncmp(path, apkPath.c_str(), apkPath.size() + 1) != 0) return;
    ctx->general.regs.x1 = reinterpret_cast<uint64_t>(redirectPath.c_str());
    LOGD("Redirect svc %ld apk read to %s", nr, redirectPath.c_str());
}

int instrumentRange(uintptr_t base, size_t len) {
    auto *words = reinterpret_cast<const uint32_t *>(base);
    size_t count = len / sizeof(uint32_t);
    int done = 0;
    for (size_t i = 0; i < count; ++i) {
        if (words[i] != kSvc0) continue;
        auto addr = base + i * sizeof(uint32_t);
        if (!g_instrumented.insert(addr).second) continue;
        if (DobbyInstrument(reinterpret_cast<void *>(addr), &svcHandler) != 0) {
            LOGW("DobbyInstrument failed at %p", reinterpret_cast<void *>(addr));
            g_instrumented.erase(addr);
        } else {
            ++done;
        }
    }
    return done;
}

int phdrCallback(struct dl_phdr_info *info, size_t, void *) {
    const char *name = info->dlpi_name;
    if (name == nullptr || name[0] == '\0') return 0;
    std::string_view n{name};
    bool appOwned = (!g_appDirPrefix.empty() && n.compare(0, g_appDirPrefix.size(), g_appDirPrefix) == 0) ||
                    n.find("/cache/phantom/origin/") != std::string_view::npos ||
                    n.find("/cache/lspatch/origin/") != std::string_view::npos;
    if (!appOwned) return 0;
    if (n.find("/assets/phantom/so/") != std::string_view::npos ||
        n.find("/assets/lspatch/so/") != std::string_view::npos) return 0;

    int done = 0;
    for (int i = 0; i < info->dlpi_phnum; ++i) {
        const auto &ph = info->dlpi_phdr[i];
        if (ph.p_type != PT_LOAD || !(ph.p_flags & PF_X) || ph.p_filesz == 0) continue;
        done += instrumentRange(info->dlpi_addr + ph.p_vaddr, ph.p_filesz);
    }
    if (done > 0) LOGD("svc redirect: instrumented %d site(s) in %s", done, name);
    return 0;
}

void scanAppLibs() {
    std::lock_guard<std::mutex> lock(g_svcMutex);
    if (g_appDirPrefix.empty()) return;
    dl_iterate_phdr(&phdrCallback, nullptr);
}

void *(*g_orig_loader_android_dlopen_ext)(const char *, int, const void *, const void *) = nullptr;
void *my_loader_android_dlopen_ext(const char *filename, int flags, const void *extinfo,
                                   const void *caller_addr) {
    void *h = g_orig_loader_android_dlopen_ext(filename, flags, extinfo, caller_addr);
    if (h != nullptr) scanAppLibs();
    return h;
}

void *(*g_orig_loader_dlopen)(const char *, int, const void *) = nullptr;
void *my_loader_dlopen(const char *filename, int flags, const void *caller_addr) {
    void *h = g_orig_loader_dlopen(filename, flags, caller_addr);
    if (h != nullptr) scanAppLibs();
    return h;
}

void hookDlopen() {
    SandHook::ElfImg linker("linker64");
    if (auto ext = linker.getSymbAddress<void *>("__loader_android_dlopen_ext")) {
        DobbyHook(ext, reinterpret_cast<dobby_dummy_func_t>(my_loader_android_dlopen_ext),
                  reinterpret_cast<dobby_dummy_func_t *>(&g_orig_loader_android_dlopen_ext));
    } else {
        LOGW("could not resolve __loader_android_dlopen_ext; dlopen rescan skipped for it");
    }
    if (auto plain = linker.getSymbAddress<void *>("__loader_dlopen")) {
        DobbyHook(plain, reinterpret_cast<dobby_dummy_func_t>(my_loader_dlopen),
                  reinterpret_cast<dobby_dummy_func_t *>(&g_orig_loader_dlopen));
    } else {
        LOGW("could not resolve __loader_dlopen; dlopen rescan skipped for it");
    }
}

}  // namespace

#endif  // defined(__aarch64__)

LSP_DEF_NATIVE_METHOD(void, SigBypass, enableSvcRedirect) {
#if defined(__aarch64__)
    if (apkPath.empty()) {
        LOGE("enableSvcRedirect called before enableOpenatHook");
        return;
    }
    {
        std::lock_guard<std::mutex> lock(g_svcMutex);
        auto slash = apkPath.find_last_of('/');
        g_appDirPrefix = slash == std::string::npos ? apkPath : apkPath.substr(0, slash);
        auto oslash = redirectPath.find_last_of('/');
        g_originPrefix = oslash == std::string::npos ? redirectPath : redirectPath.substr(0, oslash);
        LOGD("svc redirect scope %s | %s", g_appDirPrefix.c_str(), g_originPrefix.c_str());
    }
    static std::once_flag nearBranchOnce;
    std::call_once(nearBranchOnce, dobby_enable_near_branch_trampoline);
    hookDlopen();
    scanAppLibs();
    LOGD("enableSvcRedirect armed; %zu svc site(s) instrumented so far", g_instrumented.size());
#else
    LOGW("enableSvcRedirect: raw-svc apk-read redirect is implemented on arm64 only");
#endif
}

LSP_DEF_NATIVE_METHOD(void, SigBypass, enableOpenatHook, jstring origApkPath,
                      jstring cacheApkPath) {
    // Populate the globals before the hook goes live: the hook reads apkPath/redirectPath, and they
    // are written once here and never reassigned, so it only ever observes populated, immutable values
    // -- closing both the empty-match window and the write/read data race.
    lsplant::JUTFString str1(env, origApkPath);
    lsplant::JUTFString str2(env, cacheApkPath);
    apkPath = str1.get();
    redirectPath = str2.get();
    LOGD("apkPath %s", apkPath.c_str());
    LOGD("redirectPath %s", redirectPath.c_str());
    auto r = HookOpenat(lsplant::InitInfo{
        .inline_hooker =
            [](auto t, auto r) {
                void *bk = nullptr;
                return HookInline(t, r, &bk) == 0 ? bk : nullptr;
            },
        .art_symbol_resolver = [](auto symbol) { return GetC()->getSymbAddress(symbol); },
    });
    if (!r) {
        LOGE("Hook __openat fail");
        return;
    }
    GetC(true);
}

static JNINativeMethod gMethods[] = {
    LSP_NATIVE_METHOD(SigBypass, enableOpenatHook, "(Ljava/lang/String;Ljava/lang/String;)V"),
    LSP_NATIVE_METHOD(SigBypass, enableSvcRedirect, "()V")};

void RegisterBypass(JNIEnv *env) { REGISTER_LSP_NATIVE_METHODS(SigBypass); }

}  // namespace lspd