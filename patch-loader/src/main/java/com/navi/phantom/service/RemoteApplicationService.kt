package com.navi.phantom.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.os.UserHandle
import android.widget.Toast
import co.touchlab.kermit.Logger
import com.navi.phantom.shared.Constants as PhantomConstants
import org.lsposed.lspd.models.Module
import org.lsposed.lspd.service.ILSPApplicationService
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RemoteApplicationService @SuppressLint("DiscouragedPrivateApi") constructor(
    context: Context
) : ILSPApplicationService {

    private val log = Logger.withTag("Phantom")

    @Volatile
    private var service: ILSPApplicationService? = null

    init {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    PhantomConstants.MANAGER_PACKAGE_NAME,
                    "${PhantomConstants.MANAGER_PACKAGE_NAME}.manager.ModuleService"
                )
                putExtra("packageName", context.packageName)
            }
            val latch = CountDownLatch(1)
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    log.i { "Manager binder received" }
                    service = ILSPApplicationService.Stub.asInterface(binder)
                    latch.countDown()
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    log.e { "Manager service died" }
                    service = null
                }
            }

            log.i { "Request manager binder" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.bindService(intent, Context.BIND_AUTO_CREATE, Executors.newSingleThreadExecutor(), conn)
            } else {
                val handlerThread = HandlerThread("RemoteApplicationService")
                handlerThread.start()
                val handler = Handler(handlerThread.looper)
                try {
                    val contextImplClass = context.javaClass
                    val getUserMethod = contextImplClass.getMethod("getUser")
                    val bindServiceAsUserMethod = contextImplClass.getDeclaredMethod(
                        "bindServiceAsUser",
                        Intent::class.java,
                        ServiceConnection::class.java,
                        Int::class.javaPrimitiveType,
                        Handler::class.java,
                        UserHandle::class.java
                    )
                    val userHandle = getUserMethod.invoke(context) as UserHandle
                    bindServiceAsUserMethod.invoke(context, intent, conn, Context.BIND_AUTO_CREATE, handler, userHandle)
                } catch (e: ReflectiveOperationException) {
                    log.e(e) { "Failed to bind service using reflection, API may be unsupported" }
                    throw RemoteException("Unsupported Android version for manager binding").apply { initCause(e) }
                }
            }

            val success = latch.await(3500, TimeUnit.MILLISECONDS)
            if (!success) {
                log.w { "Bind manager service timed out (manager may still be cold-starting). Will connect asynchronously." }
            }
        } catch (e: Exception) {
            log.w(e) { "Failed to bind manager service synchronously: ${e.message}" }
        }
    }

    override fun isLogMuted(): Boolean = false

    override fun getLegacyModulesList(): List<Module> = service?.legacyModulesList ?: emptyList()

    override fun getModulesList(): List<Module> = service?.modulesList ?: emptyList()

    override fun getPrefsPath(packageName: String): String =
        File(Environment.getDataDirectory(), "data/$packageName/shared_prefs/").absolutePath

    override fun asBinder(): IBinder? = service?.asBinder()

    override fun requestInjectedManagerBinder(binder: MutableList<IBinder>?): ParcelFileDescriptor? = null

    override fun getConfigBundle(): android.os.Bundle = service?.configBundle ?: android.os.Bundle.EMPTY
}