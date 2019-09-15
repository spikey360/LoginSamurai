package com.deltasoft.quickeats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

public class SamuraiSentinel extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        Start the vigil
        //Get the saved settings on Vigil frequency and URL
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String loginUrl = pref.getString(SettingsActivity.urlKey, null);
        String prefUser = pref.getString(SettingsActivity.usernameKey, null);
        String prefPassword = pref.getString(SettingsActivity.passwordKey, null);
        boolean loginMethod = pref.getBoolean(SettingsActivity.httpMethodKey, true); //true for default POST
        String pingServer = pref.getString(SettingsActivity.pingServerKey, SamuraiService.DEFAULT_PING_SRV);
        int millis = Integer.parseInt(pref.getString(SettingsActivity.freqKey, SamuraiService.DEFAULT_VIGIL));

        //If service is already not running, start a service
        SamuraiService.startActionVigil(context,

                prefUser,prefPassword,
                prefUser,
                prefPassword,
                loginUrl,
                millis,
                pingServer,
                loginMethod);

    }
}
