package com.example.ghidorah

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings.Secure
import android.telephony.TelephonyManager
import android.util.Log

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

import com.example.ghidorah.plugin.HideModule
import com.example.ghidorah.plugin.Messages
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject
import java.net.URL
import android.os.StrictMode


class Ghidorah : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == Constants.WECHAT_PACKAGE_NAME) {
            try {
                XposedHelpers.findAndHookMethod(ContextWrapper::class.java,
                    "attachBaseContext",
                    Context::class.java,
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                            super.afterHookedMethod(param)
                            val processName = lpparam.processName
                            if (processName != Constants.WECHAT_PACKAGE_NAME) {
                                return
                            }
                            if (Constants.wechatVersion != "") return

                            val context = param!!.args[0] as Context
                            loadPackParam = lpparam

                            val wechatSharedPref = context.getSharedPreferences(
                                "${Constants.WECHAT_PACKAGE_NAME}_preferences", Context.MODE_PRIVATE
                            )
                            Constants.username = wechatSharedPref.getString("login_weixin_username", "")!!
                            Constants.nickname = wechatSharedPref.getString("last_login_nick_name", "")!!
                            if (Constants.username == "") return

                            getDeviceInfo(context)

                            sharedPref = context.getSharedPreferences(
                                "${Constants.PACKAGE_NAME}_prefs", Context.MODE_PRIVATE
                            )
                            val expiredAt = sharedPref.getLong("expiredAt", 0)
                            val now = getTimestamp()
                            if (now < expiredAt) {
                                Constants.token = sharedPref.getString("token", Constants.token)
                            }

                            if (null == Constants.socket) {
                                createSocket()
                            }
                            if (null != Constants.socket) {
                                Constants.socket?.connect()
                            }
                        }
                    })
            } catch (e: Error) {
            } catch (e: Exception) {
            }
        }
    }

    private fun createSocket() {
        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)
        opts.forceNew = true
        opts.query = "token=${Constants.token}"
        val socket = IO.socket(Constants.DEVELOPMENT_SERVER, opts)
        Constants.socket = socket

        socket?.on(Socket.EVENT_CONNECT) {
            try {
                val deviceInfo = JSONObject()
                deviceInfo.put("uid", Constants.uid)
                deviceInfo.put("username", Constants.username)
                deviceInfo.put("nickname", Constants.nickname)
                deviceInfo.put("platform", "Wechat")
                deviceInfo.put("version", Constants.wechatVersion)
                deviceInfo.put("serial", Constants.serial)
                deviceInfo.put("provider", Constants.provider)
                deviceInfo.put("device", Constants.device)
                deviceInfo.put("sdk", Constants.sdk)
                socket.emit("login", deviceInfo)
            } catch (e: Error) {
            } catch (e: Exception) {
            }
        }

        socket?.on("message") { args ->
        }

        socket?.on("updateConfig") { args ->
            if (!Constants.hooked) {
                val data = JSONObject(args[0].toString())
                Constants.SQLiteDatabaseClassName = data.getString("SQLiteDatabaseClassName")
                Constants.SQLiteDatabaseUpdateMethod = data.getString("SQLiteDatabaseUpdateMethod")
                Constants.SQLiteDatabaseInsertMethod = data.getString("SQLiteDatabaseInsertMethod")
                Constants.SQLiteDatabaseDeleteMethod = data.getString("SQLiteDatabaseDeleteMethod")
                Constants.ContactInfoUIClassName = data.getString("ContactInfoUIClassName")
                Constants.ChatroomInfoUIClassName = data.getString("ChatroomInfoUIClassName")
                Constants.LuckyMoneyReceiveUIClassName = data.getString("LuckyMoneyReceiveUIClassName")
                Constants.MsgInfoClassName = data.getString("MsgInfoClassName")
                Constants.MsgInfoStorageClassName = data.getString("MsgInfoStorageClassName")
                Constants.MsgInfoStorageInsertMethodName = data.getString("MsgInfoStorageInsertMethodName")
                Constants.ModelMultiClassName = data.getString("ModelMultiClassName")
                Constants.NetSceneBaseClassName = data.getString("NetSceneBaseClassName")
                Constants.NetSceneQueueClassName = data.getString("NetSceneQueueClassName")
                Constants.NetSceneQueueMethodName = data.getString("NetSceneQueueMethodName")
                Constants.MMKernelClassName = data.getString("MMKernelClassName")
                Constants.MMKernelMethodName = data.getString("MMKernelMethodName")
                Constants.MMKernelFieldName = data.getString("MMKernelFieldName")
                loadPlugins(loadPackParam)
            }
        }

        socket?.on("updateToken") { args ->
            val data = JSONObject(args[0].toString())
            val token = data.getString("token")
            val expiredAt = data.getLong("expiredAt")
            val editor = sharedPref.edit()
            editor.putString("token", token)
            editor.putLong("expiredAt", expiredAt)
            editor.apply()
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
        }
        socket?.on(Socket.EVENT_RECONNECTING) {
        }
    }

    private fun loadPlugins(lpparam: LoadPackageParam) {
        if (!Constants.hooked) {
            Constants.hooked = true
            for (plugin in plugins) {
                try {
                    plugin.hook(lpparam)
                } catch (e: Error) {
                } catch (e: Exception) {
                }
            }
        }
    }

    companion object {
        private val plugins = arrayOf(
            HideModule(),
            Messages()
        )
        private lateinit var loadPackParam: LoadPackageParam
        private lateinit var sharedPref: SharedPreferences

        private fun objectToJson(obj: Any?): String = Gson().toJson(obj)

        private fun getTimestamp(): Long {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            try {
                val jsonStr = URL("https://api.m.taobao.com/rest/api3.do?api=mtop.common.getTimestamp").readText()
                val jsonObj = JSONObject(jsonStr)
                val data = jsonObj.getJSONObject("data")
                return data.getLong("t")
            } catch (e: Error) {
            } catch (e: Exception) {
            }
            return System.currentTimeMillis()
        }

        private fun getVersionName(context: Context, packageName: String): String {
            try {
                val packageManager = context.packageManager
                val packInfo = packageManager.getPackageInfo(packageName, 0)
                return packInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
            }
            return ""
        }

        @SuppressLint("HardwareIds")
        private fun getDeviceInfo(context: Context) {
            Constants.uid = Secure.getString(context.applicationContext.contentResolver, Secure.ANDROID_ID)
            Constants.sdk = Integer.valueOf(Build.VERSION.SDK_INT).toString()
            Constants.version = Build.VERSION.RELEASE
            Constants.serial = Build::class.java.getField("SERIAL").get(null) as String
            Constants.device = Build::class.java.getField("MODEL").get(null) as String
            Constants.appVersion = this.getVersionName(context, Constants.PACKAGE_NAME)
            Constants.wechatVersion = this.getVersionName(context, Constants.WECHAT_PACKAGE_NAME)

            val telephonyManager =
                context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            Constants.provider = telephonyManager.networkOperatorName
        }
    }
}
