package com.micinjector.hook;

import android.content.Context;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    
    private static final String TAG = "MicInjector";
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.micinjector")) {
            Log.i(TAG, "MicInjector module loaded");
            
            XposedHelpers.findAndHookMethod(
                "com.micinjector.hook.MainHook",
                lpparam.classLoader,
                "init",
                XC_LoadPackage.LoadPackageParam.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.i(TAG, "MainHook initialized");
                    }
                }
            );
        }
        
        try {
            MainHook mainHook = MainHook.getInstance();
            mainHook.init(lpparam);
            
            Log.i(TAG, "Hooked package: " + lpparam.packageName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error hooking package: " + lpparam.packageName);
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }
}
