package com.deltasoft.quickeats;


import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;


public class MainActivity extends AppCompatActivity {

    Switch serviceSwitch;
    EditText userField;
    EditText passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set the preference manager
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //Find and set the texts
        userField = (EditText) findViewById(R.id.username);
        passwordField = (EditText) findViewById(R.id.password);

        /*Activity for the switch*/
        //find the switch
        serviceSwitch = (Switch) findViewById(R.id.service);
        //set its handler
        serviceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceSwitch.isChecked()) {
                    //Get the username and password from the screen
                    userField.setEnabled(false);
                    passwordField.setEnabled(false);

                    //Get the saved settings on Vigil frequency and URL
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    String loginUrl = pref.getString(SettingsActivity.urlKey, null);
                    String prefUser = pref.getString(SettingsActivity.usernameKey, null);
                    String prefPassword = pref.getString(SettingsActivity.passwordKey, null);
                    boolean loginMethod = pref.getBoolean(SettingsActivity.httpMethodKey, true); //true for default POST
                    String pingServer = pref.getString(SettingsActivity.pingServerKey, SamuraiService.DEFAULT_PING_SRV);
                    int millis = Integer.parseInt(pref.getString(SettingsActivity.freqKey, SamuraiService.DEFAULT_VIGIL));

                    if (userField.getText().toString().length() == 0 || passwordField.getText().toString().length() == 0) {
                        userField.setText(prefUser);
                        passwordField.setText(prefPassword);
                    }
                    //If service is already not running, start a service
                    SamuraiService.startActionVigil(MainActivity.this,
                            userField.getText().toString(),
                            passwordField.getText().toString(),
                            prefUser,
                            prefPassword,
                            loginUrl,
                            millis,
                            pingServer,
                            loginMethod);
                } else {
                    userField.setEnabled(true);
                    passwordField.setEnabled(true);
                    //If a service is running, stop it
                    SamuraiService.stopVigil(MainActivity.this);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_settings){
            Intent settingsIntent = new Intent(this,SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}
