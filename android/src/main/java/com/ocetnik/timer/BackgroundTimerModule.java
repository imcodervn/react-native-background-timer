package com.ocetnik.timer;

import android.app.Activity;
import android.os.Handler;
import android.os.PowerManager;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.lang.Runnable;
import java.lang.ref.WeakReference;

public class BackgroundTimerModule extends ReactContextBaseJavaModule {

    private MyHandler handler;
    private ReactContext reactContext;
    private Runnable runnable;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private final LifecycleEventListener listener = new LifecycleEventListener() {
        @Override
        public void onHostResume() {
            wakeLock.acquire();
        }

        @Override
        public void onHostPause() {
            //wakeLock.release();
        }

        @Override
        public void onHostDestroy() {
            if (wakeLock.isHeld()) wakeLock.release();
        }
    };

    public BackgroundTimerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.powerManager = (PowerManager) getReactApplicationContext().getSystemService(reactContext.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rohit_bg_wakelock");
        reactContext.addLifecycleEventListener(listener);
    }

    @Override
    public String getName() {
        return "RNBackgroundTimer";
    }

    @ReactMethod
    public void start(final int delay) {
        handler = new MyHandler(getCurrentActivity());
        runnable = new Runnable() {
            @Override
            public void run() {
                if (handler.isValid()) {
                    sendEvent(reactContext, "backgroundTimer");
                }
            }
        };

        handler.post(runnable);
    }

    @ReactMethod
    public void stop() {
        // avoid null pointer exceptio when stop is called without start
        if (handler != null) handler.removeCallbacks(runnable);
    }

    private void sendEvent(ReactContext reactContext, String eventName) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, null);
    }

    @ReactMethod
    public void setTimeout(final int id, final int timeout) {
        final MyHandler handler = new MyHandler(getCurrentActivity());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (handler.isValid()) {
                    if (getReactApplicationContext().hasActiveCatalystInstance()) {
                        getReactApplicationContext()
                                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("backgroundTimer.timeout", id);
                    }
                }
            }
        }, timeout);
    }

    /*@ReactMethod
    public void clearTimeout(final int id) {
        // todo one day..
        // not really neccessary to have
    }*/

    private static class MyHandler extends Handler {
        private final WeakReference<Activity> weakReference;

        MyHandler(Activity activity) {
            weakReference = new WeakReference<>(activity);
        }

        public boolean isValid() {
            Activity activity = weakReference.get();
            return activity != null && !activity.isFinishing();
        }
    }
}
