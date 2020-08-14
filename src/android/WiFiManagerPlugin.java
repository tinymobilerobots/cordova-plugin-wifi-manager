package net.sushichop.cordova.wifimanager;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class WiFiManagerPlugin extends CordovaPlugin {

    private static final int ERROR_CODE_OFFSET      = 1000;

    private static final int ADD_NETWORK_ERROR      = 1 + ERROR_CODE_OFFSET;
    private static final int ENABLE_NETWORK_ERROR   = 2 + ERROR_CODE_OFFSET;
    private static final int RECONNECT_ERROR        = 3 + ERROR_CODE_OFFSET;
    private static final int DISCONNECT_ERROR       = 4 + ERROR_CODE_OFFSET;
    private static final int DISABLE_NETWORK_ERROR  = 5 + ERROR_CODE_OFFSET;
    private static final int UNKNOWN_ACTION_ERROR   = 6 + ERROR_CODE_OFFSET;

    private static final String ADD_NETWORK_ERROR_MESSAGE       = "addNetwork failed.";
    private static final String ENABLE_NETWORK_ERROR_MESSAGE    = "enableNetwork failed.";
    private static final String RECONNECT_ERROR_MESSAGE         = "reconnect failed.";
    private static final String DISCONNECT_ERROR_MESSAGE        = "disconnect failed.";
    private static final String DISABLE_NETWORK_ERROR_MESSAGE   = "disableNetwork failed.";
    private static final String UNKNOWN_ACTION_ERROR_MESSAGE    = "unknownAction occurred.";

    private WifiManager manager;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Context context = cordova.getActivity().getApplicationContext();
        manager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("connect"))
            connect(args, callbackContext);
        else if (action.equals("disconnect"))
            disconnect(args, callbackContext);
        else
            executeUnknownAction(callbackContext);

        return true;
    }

    private void connect(JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (!manager.isWifiEnabled()) {
            manager.setWifiEnabled(true);
        }

        String ssid = args.optString(0);
        String passphrase = args.optString(1);
        JSONObject json = new JSONObject();

        WifiConfiguration config = setWPAConfiguration(ssid, passphrase);

        int networkId = manager.addNetwork(config);
        manager.updateNetwork(config);

        if (networkId == -1) {
            json.put("code", ADD_NETWORK_ERROR);
            json.put("message", ADD_NETWORK_ERROR_MESSAGE);
            callbackContext.error(json);
            return;
        }

        if (!manager.enableNetwork(networkId, true))  {
            json.put("code", ENABLE_NETWORK_ERROR);
            json.put("message", ENABLE_NETWORK_ERROR_MESSAGE);
            callbackContext.error(json);
            return;
        }

        if (!manager.reconnect()) {
            json.put("code", RECONNECT_ERROR);
            json.put("message", RECONNECT_ERROR_MESSAGE);
            callbackContext.error(json);
            return;
        }

        json.put("ssid", ssid);
        json.put("passphrase", passphrase);
        callbackContext.success(json);
    }


    private void disconnect(JSONArray args, CallbackContext callbackContext) throws JSONException {

        String ssid = args.optString(0);

        JSONObject json = new JSONObject();

        WifiConfiguration config = setWPAConfiguration(ssid);

        int networkId = manager.addNetwork(config);
        manager.updateNetwork(config);

        if (networkId == -1) {
            json.put("code", ADD_NETWORK_ERROR);
            json.put("message", ADD_NETWORK_ERROR_MESSAGE);
            callbackContext.error(json);
            return;
        }

        if (!manager.disconnect()) {
            json.put("code", DISCONNECT_ERROR);
            json.put("message", DISCONNECT_ERROR_MESSAGE);
            callbackContext.error(json);
            return;
        }

        if (!manager.disableNetwork(networkId)) {
            json.put("code", DISABLE_NETWORK_ERROR);
            json.put("message", DISABLE_NETWORK_ERROR_MESSAGE);
            callbackContext.error(json);
            return;
        }

        json.put("ssid", ssid);

        callbackContext.success(json);
    }

    private void executeUnknownAction(CallbackContext callbackContext) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("code", UNKNOWN_ACTION_ERROR);
        json.put("message", UNKNOWN_ACTION_ERROR_MESSAGE);
        callbackContext.error(json);
    }

    private WifiConfiguration setWPAConfiguration(String ssid, String passphrase) {

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        if (passphrase != null) {
            config.preSharedKey = "\"" + passphrase + "\"";
        }

        //config.status = WifiConfiguration.Status.ENABLED;

        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

        return config;
    }

    private WifiConfiguration setWPAConfiguration(String ssid) {
        return setWPAConfiguration(ssid, null);
    }
}
