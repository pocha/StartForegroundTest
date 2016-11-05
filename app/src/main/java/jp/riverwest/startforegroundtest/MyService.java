package jp.riverwest.startforegroundtest;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

// http://www.hiroom2.com/2016/02/27/android%E3%81%AEservice%E3%81%AB%E3%81%A4%E3%81%84%E3%81%A6/

public class MyService extends Service {

    private static final String TAG = MyService.class.getSimpleName();
    private static boolean mStarted = false;

    private final IBinder mBinder = new MyBinder();

    public static boolean isStarted() {
        return mStarted;
    }

    public class MyBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }

    public MyService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "onStartCommand() >>>");

        mStarted = true;

        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Title")
                .setContentText("Text")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(startId, notification);

        Log.i(TAG, "onStartCommand() <<<");

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() >>>");
        super.onCreate();
        Log.i(TAG, "onCreate() <<<");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy() >>>");
        super.onDestroy();
        mStarted = false;
        Log.i(TAG, "onDestroy() <<<");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        Log.i(TAG, "onBind() >>>");
        Log.i(TAG, "onBind() <<<");
        return mBinder;
    }
}
