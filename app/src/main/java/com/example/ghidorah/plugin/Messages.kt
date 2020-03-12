package com.example.ghidorah.plugin

import java.io.File
import java.util.Arrays
import java.util.HashMap

import com.google.gson.Gson

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

import com.example.ghidorah.Constants
import org.json.JSONObject
import java.lang.reflect.Method

class Messages : IPlugin {
    override fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        modelMultiClass = XposedHelpers.findClassIfExists(Constants.ModelMultiClassName, lpparam.classLoader)
        mmKernelClass = XposedHelpers.findClassIfExists(Constants.MMKernelClassName, lpparam.classLoader)
        mmKernelStaticMethod = XposedHelpers.callStaticMethod(mmKernelClass, Constants.MMKernelMethodName)
        mmKernelField = XposedHelpers.getObjectField(mmKernelStaticMethod, Constants.MMKernelFieldName)
        netSceneQueueClass = XposedHelpers.findClassIfExists(Constants.NetSceneQueueClassName, lpparam.classLoader)
        netSceneBaseClass = XposedHelpers.findClassIfExists(Constants.NetSceneBaseClassName, lpparam.classLoader)
        netSceneQueueStaticMethod =
            XposedHelpers.callStaticMethod(netSceneQueueClass, Constants.NetSceneQueueMethodName, mmKernelField)
        netSceneQueueMethod = XposedHelpers.findMethodExactIfExists(
            netSceneQueueClass, Constants.NetSceneQueueMethodName, netSceneBaseClass, Int::class.javaPrimitiveType!!
        )

        Constants.socket?.on("postMessage") { args ->
            try {
                val data = JSONObject(args[0].toString())
                val talker = data.getString("field_talker")
                val text = data.getString("field_content")
                insertMessage(talker, text)
            } catch (e: Error) {
            } catch (e: Exception) {
            }
        }

        XposedHelpers.findAndHookMethod(Constants.SQLiteDatabaseClassName,
            lpparam.classLoader,
            Constants.SQLiteDatabaseDeleteMethod,
            String::class.java,
            String::class.java,
            Array<String>::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                    try {
                        val media = arrayOf("ImgInfo2", "voiceinfo", "videoinfo2", "WxFileIndex2")
                        if (Arrays.asList(*media).contains(param!!.args[0])) {
                            param.result = 1
                        }
                    } catch (e: Error) {
                    } catch (e: Exception) {
                    }
                }
            })

        XposedHelpers.findAndHookMethod(File::class.java, "delete", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                try {
                    val path = (param!!.thisObject as File).absolutePath
                    if (path.contains("/image2/") || path.contains("/voice2/") || path.contains("/video/")) param.result =
                        true
                } catch (e: Error) {
                } catch (e: Exception) {
                }
            }
        })

        val msgInfoClass = XposedHelpers.findClass(Constants.MsgInfoClassName, lpparam.classLoader)
        val msgInsertClass = XposedHelpers.findClass("com.tencent.mm.storage.bj", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(Constants.MsgInfoStorageClassName,
            lpparam.classLoader,
            Constants.MsgInfoStorageInsertMethodName,
            msgInfoClass,
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                    try {
                        val msg = param!!.args[0]
                        val talker = XposedHelpers.getObjectField(msg, "field_talker").toString()
                        val content = XposedHelpers.getObjectField(msg, "field_content").toString()
                        Constants.socket?.emit("message", objectToJson(msg))
                    } catch (e: Error) {
                    } catch (e: Exception) {
                    }
                }
            })
    }

    private fun insertMessage(talker: String?, content: String?) {
        try {
            if (content == null || talker == null || content.isEmpty() || talker.isEmpty()) {
                return
            }
            val objectiVar = XposedHelpers.newInstance(modelMultiClass, arrayOf<Class<*>>(
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Any::class.java
            ), talker, content, 1, 1, object : HashMap<String, String>() {
                init {
                    put(talker, talker)
                }
            })
            val objectParamiVar = arrayOf(objectiVar, 0)
            netSceneQueueMethod.invoke(netSceneQueueStaticMethod, *objectParamiVar)
        } catch (e: Error) {
        } catch (e: Exception) {
        }
    }

    companion object {
        private lateinit var modelMultiClass: Class<*>
        private lateinit var mmKernelClass: Class<*>
        private lateinit var mmKernelStaticMethod: Any
        private lateinit var mmKernelField: Any
        private lateinit var netSceneQueueClass: Class<*>
        private lateinit var netSceneBaseClass: Class<*>
        private lateinit var netSceneQueueStaticMethod: Any
        private lateinit var netSceneQueueMethod: Method
        private fun objectToJson(obj: Any?): String = Gson().toJson(obj)
    }
}