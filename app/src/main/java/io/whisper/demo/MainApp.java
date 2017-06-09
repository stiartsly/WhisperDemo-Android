package io.whisper.demo;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * Main Application of the project
 * 
 * Contains methods to build the "static" strings. These strings were before constants in different
 * classes
 */
public class MainApp extends Application {

    private static Context mContext;
    private static ContentResolver mContentResolver;

    private static Activity mCurrentActivity = null;
    private static float mScreenBrightness = (float) 0.5;

    public void onCreate() {
        super.onCreate();

        MainApp.mContext = getApplicationContext();
        MainApp.mContentResolver = getContentResolver();

        this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                mCurrentActivity = activity;
                setCurrentActivityBrightness();
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (mCurrentActivity == activity) {
                    mCurrentActivity = null;
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (mCurrentActivity == activity) {
                    mCurrentActivity = null;
                }
            }
        });
    }

    public static Context getAppContext() {
        return MainApp.mContext;
    }

    public static ContentResolver getAppContentResolver() {
        return MainApp.mContentResolver;
    }

    public static float getScreenBrightness() {
        return mScreenBrightness;
    }

    public static void setScreenBrightness(float brightness) {
        MainApp.mScreenBrightness = brightness;
        setCurrentActivityBrightness();
    }

    private static void setCurrentActivityBrightness() {
        if (mCurrentActivity != null) {
            Window window = mCurrentActivity.getWindow();
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.screenBrightness = mScreenBrightness;
            window.setAttributes(layoutParams);
        }
    }
}
