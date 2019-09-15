package com.deltasoft.quickeats;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
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
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    Switch serviceSwitch;
    EditText userField;
    EditText passwordField;
    TextView connectionState;
    AlarmManager alarmManager;
    PendingIntent pendingSentinel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set the preference manager
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //Find and set the texts
        userField = findViewById(R.id.username);
        passwordField = findViewById(R.id.password);
        connectionState = findViewById(R.id.connectionState);
        //Find if service is Running
        if(SamuraiService.isServiceRunning()){
            if(SamuraiService.isConnectionUp()) {
                connectionState.setText(getString(R.string.connection_state_up));
                userField.setEnabled(false);
                passwordField.setEnabled(false);
            }else{
                connectionState.setText(getString(R.string.connection_state_down));
            }
        }else{
            connectionState.setText(getString(R.string.service_state_down));
        }
        //TODO: Add adaptability for screen changes using ViewModel/savedInstanceState
        /*Activity for the switch*/
        //find the switch
        serviceSwitch = (Switch) findViewById(R.id.service);
        //set its handler
        serviceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Clear connection state
                ((TextView)findViewById(R.id.connectionState)).setText(null);
                if (serviceSwitch.isChecked()) {
                    //Get the username and password from the screen
                    userField.setEnabled(false);
                    passwordField.setEnabled(false);

                    //Get the saved settings on Vigil frequency and URL
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
//                    String loginUrl = pref.getString(SettingsActivity.urlKey, null);
                    String prefUser = pref.getString(SettingsActivity.usernameKey, null);
                    String prefPassword = pref.getString(SettingsActivity.passwordKey, null);
//                    boolean loginMethod = pref.getBoolean(SettingsActivity.httpMethodKey, true); //true for default POST
//                    String pingServer = pref.getString(SettingsActivity.pingServerKey, SamuraiService.DEFAULT_PING_SRV);
                    int millis = Integer.parseInt(pref.getString(SettingsActivity.freqKey, SamuraiService.DEFAULT_VIGIL));

                    if (userField.getText().toString().length() == 0 || passwordField.getText().toString().length() == 0) {
                        userField.setText(prefUser);
                        passwordField.setText(prefPassword);
                    }
                    //If service is already not running, start a service
//                    SamuraiService.startActionVigil(MainActivity.this,
//                            userField.getText().toString(),
//                            passwordField.getText().toString(),
//                            prefUser,
//                            prefPassword,
//                            loginUrl,
//                            millis,
//                            pingServer,
//                            loginMethod);
//                    Figure out the intent
                    Intent sentinelIntent = new Intent(MainActivity.this,SamuraiSentinel.class);
                    pendingSentinel = PendingIntent.getBroadcast(MainActivity.this,0,sentinelIntent,0);
//                    Invoke a repeating alarm after millis
                    alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,0,millis,pendingSentinel);

                } else {
                    userField.setEnabled(true);
                    passwordField.setEnabled(true);
                    //If a service is running, stop it
//                    SamuraiService.stopVigil(MainActivity.this);
                    alarmManager.cancel(pendingSentinel);
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

    EditText getUserField(){
        return userField;
    }

    EditText getPasswordField(){
        return passwordField;
    }
}
