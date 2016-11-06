package jp.riverwest.startforegroundtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by kawa on 2016/11/06.
 */

public class WifiHelper {

    private static final String TAG = WifiHelper.class.getSimpleName();

    private final int STATE_WIFI_ON = 1;
    private final int STATE_SCAN_AP = 2;
    private final int STATE_CONNECT_AP = 3;
    private final int STATE_SEARCH_DEVICES = 4;

    private Context mContext;
    private WifiManager mWifiManager;

    private String mInitialSsid;    // 今まで接続していたSSID
    private String mAccessPointName;    // アクセスポイント名

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);

            switch (msg.what) {
                case STATE_WIFI_ON:
                    checkAccessPoint();
                    break;
                case STATE_SCAN_AP:
                    scanAccessPoint();
                    break;
                case STATE_CONNECT_AP:
                    connectAccessPoint();
                    break;
            }
        }
    };

    /**
     * WIFI状態を受信するクラス
     */
    private BroadcastReceiver mWifiStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                // 変化前の状態を取得
                int previousState = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                // 変化後の状態を取得
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

                switch (state) {
                    case WifiManager.WIFI_STATE_ENABLING:
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        mHandler.sendEmptyMessage(STATE_WIFI_ON);
                        mContext.unregisterReceiver(mWifiStateChangedReceiver);
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                    case WifiManager.WIFI_STATE_DISABLED:
                    case WifiManager.WIFI_STATE_UNKNOWN:
                        break;
                }
            }
        }
    };

    /**
     * アクセスポイント検索結果のレシーバー
     */
    private BroadcastReceiver mScanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            mContext.unregisterReceiver(mScanResultsReceiver);
            mHandler.sendEmptyMessage(STATE_CONNECT_AP);
        }
    };

    /**
     * APに接続したときのレシーバー
     */
    BroadcastReceiver mReconnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

                NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                switch (netInfo.getState()) {
                    case CONNECTING:
                        break;
                    case CONNECTED:
                        // SSIDがカメラなら次へ
                        WifiInfo info = mWifiManager.getConnectionInfo();
                        String ssid = info.getSSID();

                        if (ssid.matches(".*DSC-QX10.*")) {

                            mContext.unregisterReceiver(mReconnectReceiver);
                            mHandler.sendEmptyMessage(STATE_SEARCH_DEVICES);
                        }
                        break;
                    case DISCONNECTING:
                        break;
                    case DISCONNECTED:
                        break;
                    default:
                        break;
                }
            }
        }
    };

    /**
     * コンストラクタ
     * @param context
     */
    public WifiHelper(Context context) {

        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
    }

    /**
     * 破棄する
     * @param context
     */
    public void destroy(Context context) {

    }

    /**
     * 指定のアクセスポイントに接続する
     * @param accessPointName
     */
    public void connect(String accessPointName) {

        mAccessPointName = accessPointName;

        // wifiはONになっているか？
        if (!mWifiManager.isWifiEnabled()) {

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            mContext.registerReceiver(mWifiStateChangedReceiver, intentFilter);

            mWifiManager.setWifiEnabled(true);
            return;
        }
        else {
            // アクセスポイントを探す
            mHandler.sendEmptyMessage(STATE_WIFI_ON);
        }
    }

    /**
     * アクセスポイントを探す
     */
    private void checkAccessPoint() {

        // 現在接続しているSSIDを確認する
        WifiInfo info = mWifiManager.getConnectionInfo();
        String ssid = info.getSSID();

        if (!ssid.matches(".*" + mAccessPointName + ".*")) {

            mInitialSsid = ssid;
            mHandler.sendEmptyMessage(STATE_SCAN_AP);
        }
        else {

        }
    }

    /**
     * アクセスポイントを検索
     */
    private void scanAccessPoint() {

        // スキャン結果の受信を準備
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mContext.registerReceiver(mScanResultsReceiver, filter);

        // APスキャンを開始する
        mWifiManager.startScan();
    }

    /**
     * アクセスポイントに接続
     */
    private void connectAccessPoint() {

        // スキャン結果を取得
        List<ScanResult> apLists = mWifiManager.getScanResults();
        boolean foundCamera = false;

        for (ScanResult result : apLists) {

            // スキャン結果に指定APを見つけたら接続する
            if (result.SSID.matches(".*" + mAccessPointName + ".*")) {

                // 設定済みのネットワークを取得する
                List<WifiConfiguration> configLists = mWifiManager.getConfiguredNetworks();

                // 設定済みのネットワークの中にカメラがあれば接続する
                for (WifiConfiguration config : configLists) {

                    if (config.SSID != null && config.SSID.matches(".*" + mAccessPointName + ".*")) {

                        IntentFilter filter = new IntentFilter();
                        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                        mContext.registerReceiver(mReconnectReceiver, filter);

                        for (WifiConfiguration tmp : configLists) {
                            mWifiManager.enableNetwork(tmp.networkId, false);
                        }

                        foundCamera = true;
                        mWifiManager.enableNetwork(config.networkId, true);
                        break;
                    }
                }

                break;
            }
        }

        if (!foundCamera) {

//            Log.d(Consts.TAG, "camera failed.");
//            setText("カメラが見つかりませんでした\n");
        }
    }

    /**
     * アクセスポイントを戻す
     */
    private void restoreAccessPoint() {

        if (mInitialSsid != null && !mInitialSsid.isEmpty()) {

            List<WifiConfiguration> configLists = mWifiManager.getConfiguredNetworks();

            for (WifiConfiguration config : configLists) {

                if (config.SSID != null && config.SSID.matches(".*" + mInitialSsid + ".*")) {

                    mWifiManager.enableNetwork(config.networkId, true);
                    break;
                }
            }
        }
    }
}
