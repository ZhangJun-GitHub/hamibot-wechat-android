package com.example.ghidorah

import io.socket.client.Socket

object Constants {
    const val PACKAGE_NAME = "com.example.ghidorah"
    lateinit var uid: String
    lateinit var sdk: String
    lateinit var serial: String
    lateinit var device: String
    lateinit var provider: String
    lateinit var version: String
    lateinit var appVersion: String
    var wechatVersion = ""
    lateinit var username: String
    lateinit var nickname: String
    var hooked = false
    const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
    lateinit var SQLiteDatabaseClassName: String
    lateinit var SQLiteDatabaseUpdateMethod: String
    lateinit var SQLiteDatabaseInsertMethod: String
    lateinit var SQLiteDatabaseDeleteMethod: String
    lateinit var ContactInfoUIClassName: String
    lateinit var ChatroomInfoUIClassName: String
    lateinit var LuckyMoneyReceiveUIClassName: String
    lateinit var MsgInfoClassName: String
    lateinit var MsgInfoStorageClassName: String
    lateinit var MsgInfoStorageInsertMethodName: String
    lateinit var ModelMultiClassName: String
    lateinit var NetSceneBaseClassName: String
    lateinit var NetSceneQueueClassName: String
    lateinit var NetSceneQueueMethodName: String
    lateinit var MMKernelClassName: String
    lateinit var MMKernelMethodName: String
    lateinit var MMKernelFieldName: String
    var socket: Socket? = null
    const val DEVELOPMENT_SERVER = "https://hamibot.com"
    var token: String? =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI1Y2NmZGY5MzBiNjhiZjIyNjljMDI0NmEiLCJwbGF0Zm9ybSI6IldlY2hhdCIsInBsYXRmb3JtSWQiOiI1Y2VkMzVmYTNlMzcxOTA4MDg4Y2M0Y2MiLCJwYWRkaW5nIjoiYTJmNGFjODU4MGYwNTdlMTJkYjMwYTBhYjQ5ODMyOGY0MmVkZjI3ODAwMmYxIiwiaWF0IjoxNTYxMDgzOTAwLCJleHAiOjE1OTI2MTk5MDB9.jP_L9lFqZlGFi7hyu87w2ke9m6D0DMQDTSXo7r_P2Ew"
}