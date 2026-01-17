/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2022 LSPosed Contributors
 */

#include "patch_loader.h"

#include <sys/mman.h>
#include <cstring>

#include "art/runtime/jit/profile_saver.h"
#include "art/runtime/oat_file_manager.h"
#include "elf_util.h"
#include "jni/bypass_sig.h"
#include "native_util.h"
#include "symbol_cache.h"
#include "utils/jni_helper.hpp"

using namespace lsplant;

namespace lspd {

    void PatchLoader::LoadDex(JNIEnv *env, Context::PreloadedDex &&dex) {
        auto class_activity_thread = JNI_FindClass(env, "android/app/ActivityThread");
        if (!class_activity_thread) {
            LOGE("FindClass failed: ActivityThread");
            return;
        }

        auto class_activity_thread_app_bind_data =
                JNI_FindClass(env, "android/app/ActivityThread$AppBindData");
        if (!class_activity_thread_app_bind_data) {
            LOGE("FindClass failed: AppBindData");
            return;
        }

        auto class_loaded_apk = JNI_FindClass(env, "android/app/LoadedApk");
        if (!class_loaded_apk) {
            LOGE("FindClass failed: LoadedApk");
            return;
        }

        auto mid_current_activity_thread = JNI_GetStaticMethodID(
                env, class_activity_thread, "currentActivityThread", "()Landroid/app/ActivityThread;");
        if (!mid_current_activity_thread) {
            LOGE("GetStaticMethodID failed: currentActivityThread");
            return;
        }

        auto mid_get_classloader =
                JNI_GetMethodID(env, class_loaded_apk, "getClassLoader", "()Ljava/lang/ClassLoader;");
        if (!mid_get_classloader) {
            LOGE("GetMethodID failed: getClassLoader");
            return;
        }

        auto fid_m_bound_application = JNI_GetFieldID(env, class_activity_thread, "mBoundApplication",
                "Landroid/app/ActivityThread$AppBindData;");
        if (!fid_m_bound_application) {
            LOGE("GetFieldID failed: mBoundApplication");
            return;
        }

        auto fid_info =
                JNI_GetFieldID(env, class_activity_thread_app_bind_data, "info", "Landroid/app/LoadedApk;");
        if (!fid_info) {
            LOGE("GetFieldID failed: info");
            return;
        }

        auto activity_thread =
                JNI_CallStaticObjectMethod(env, class_activity_thread, mid_current_activity_thread);
        if (!activity_thread) {
            LOGE("CallStaticObjectMethod failed: currentActivityThread");
            return;
        }

        auto m_bound_application = JNI_GetObjectField(env, activity_thread, fid_m_bound_application);
        if (!m_bound_application) {
            LOGE("GetObjectField failed: mBoundApplication");
            return;
        }

        auto info = JNI_GetObjectField(env, m_bound_application, fid_info);
        if (!info) {
            LOGE("GetObjectField failed: info");
            return;
        }

        auto stub_classloader = JNI_CallObjectMethod(env, info, mid_get_classloader);
        if (!stub_classloader) [[unlikely]] {
            LOGE("getStubClassLoader failed!!!");
            return;
        }

        auto in_memory_classloader = JNI_FindClass(env, "dalvik/system/InMemoryDexClassLoader");
        if (!in_memory_classloader) {
            LOGE("FindClass failed: InMemoryDexClassLoader");
            return;
        }

        auto mid_init = JNI_GetMethodID(env, in_memory_classloader, "<init>",
                "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        if (!mid_init) {
            LOGE("GetMethodID failed: InMemoryDexClassLoader.<init>");
            return;
        }

        auto dex_buffer = env->NewDirectByteBuffer(dex.data(), dex.size());
        if (!dex_buffer) {
            LOGE("NewDirectByteBuffer failed");
            return;
        }

        if (auto my_cl =
                JNI_NewObject(env, in_memory_classloader, mid_init, dex_buffer, stub_classloader)) {
            inject_class_loader_ = JNI_NewGlobalRef(env, my_cl);
        } else {
            LOGE("InMemoryDexClassLoader creation failed!!!");
            env->DeleteLocalRef(dex_buffer);
            return;
        }

        env->DeleteLocalRef(dex_buffer);
    }

    void PatchLoader::InitArtHooker(JNIEnv *env, const InitInfo &initInfo) {
        Context::InitArtHooker(env, initInfo);
        handler = initInfo;
        art::ProfileSaver::DisableInline(initInfo);
        art::FileManager::DisableBackgroundVerification(initInfo);
    }

    void PatchLoader::InitHooks(JNIEnv *env) {
        Context::InitHooks(env);
        RegisterBypass(env);
    }

    void PatchLoader::SetupEntryClass(JNIEnv *env) {
        if (auto entry_class = FindClassFromLoader(env, GetCurrentClassLoader(),
                "com.navi.phantom.loader.LSPApplication")) {
            entry_class_ = JNI_NewGlobalRef(env, entry_class);
        }
    }

    void PatchLoader::Load(JNIEnv *env) {
        /* InitSymbolCache(nullptr); */
        lsplant::InitInfo initInfo{
                .inline_hooker =
                [](auto t, auto r) {
                    void *bk = nullptr;
                    return HookInline(t, r, &bk) == 0 ? bk : nullptr;
                },
                .inline_unhooker = [](auto t) {
                    return UnhookInline(t) == 0;
                },
                .art_symbol_resolver = [](auto symbol) {
                    return GetArt()->getSymbAddress(symbol);
                },
                .art_symbol_prefix_resolver =
                [](auto symbol) {
                    return GetArt()->getSymbPrefixFirstAddress(symbol);
                },
        };

        auto stub = JNI_FindClass(env, "com/navi/phantom/metaloader/LSPAppComponentFactoryStub");
        if (!stub) {
            LOGE("Failed to find LSPAppComponentFactoryStub class");
            return;
        }

        auto dex_field = JNI_GetStaticFieldID(env, stub, "dex", "[B");
        if (!dex_field) {
            LOGE("Failed to find dex field");
            return;
        }

        ScopedLocalRef<jbyteArray> array = JNI_GetStaticObjectField(env, stub, dex_field);
        if (!array) {
            LOGE("Failed to get dex byte array");
            return;
        }

        jsize array_len = env->GetArrayLength(array.get());
        if (array_len <= 0) {
            LOGE("Invalid dex array length: %d", array_len);
            return;
        }

        // Allocate memory that PreloadedDex can safely munmap
        void* dex_data = mmap(nullptr, array_len, PROT_READ | PROT_WRITE,
                              MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (dex_data == MAP_FAILED) {
            PLOGE("Failed to allocate memory for dex");
            return;
        }

        // Copy JNI array data to mmap'd buffer, then release JNI elements
        jbyte* elements = env->GetByteArrayElements(array.get(), nullptr);
        if (!elements) {
            LOGE("Failed to get byte array elements");
            munmap(dex_data, array_len);
            return;
        }
        memcpy(dex_data, elements, array_len);
        env->ReleaseByteArrayElements(array.get(), elements, JNI_ABORT);

        // Make read-only for safety
        mprotect(dex_data, array_len, PROT_READ);

        auto dex = PreloadedDex{dex_data, static_cast<size_t>(array_len)};

        InitArtHooker(env, initInfo);
        LoadDex(env, std::move(dex));
        InitHooks(env);

        GetArt(true);

        SetupEntryClass(env);
        FindAndCall(env, "onLoad", "()V");
    }
}  // namespace lspd