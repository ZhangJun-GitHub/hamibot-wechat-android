package com.example.ghidorah.plugin

import de.robv.android.xposed.callbacks.XC_LoadPackage

interface IPlugin {
    fun hook(lpparam: XC_LoadPackage.LoadPackageParam)
}