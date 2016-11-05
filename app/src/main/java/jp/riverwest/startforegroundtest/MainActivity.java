package jp.riverwest.startforegroundtest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MyService mService;
    private boolean mBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            MyService.MyBinder binder = (MyService.MyBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate() >>>");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        if (!MyService.isStarted()) {
//            startService(new Intent(this, MyService.class));
//        }

        Log.i(TAG, "onCreate() <<<");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy() >>>");
        super.onDestroy();
        Log.i(TAG, "onDestroy() <<<");
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart() >>>");
        super.onStart();

        if (!MyService.isStarted()) {

            startService(new Intent(this, MyService.class));
        }
        else {

            Intent intent = new Intent(this, MyService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        Log.i(TAG, "onStart() <<<");
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop() >>>");
        super.onStop();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        Log.i(TAG, "onStop() <<<");
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyUp() >>>");

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("サービスを終了しますか？");
            builder.setPositiveButton("はい", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    stopService(new Intent(MainActivity.this, MyService.class));
                    finish();
                }
            });
            builder.setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });

            builder.setCancelable(true);
            AlertDialog dialog = builder.create();
            dialog.show();

            return false;
        }

        return super.onKeyUp(keyCode, event);
    }
}
