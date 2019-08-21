package com.deltasoft.quickeats;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    public static final String urlKey = "prefLoginURL";
    public static final String freqKey = "vigilFrequency";
    public static String pingServerKey = "pingServer";
    public static String httpMethodKey = "prefHttpMethod";
    public static String usernameKey = "prefUsername";
    public static String passwordKey = "prefPassword";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set up the preference screen
        getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
