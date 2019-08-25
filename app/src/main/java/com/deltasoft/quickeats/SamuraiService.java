package com.deltasoft.quickeats;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;


/**
 * An {@link SamuraiService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class SamuraiService extends IntentService {

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_VIGIL = "com.deltasoft.quickeats.action.VIGIL"; //Monitor state
//    Defaults
    public static final String DEFAULT_VIGIL = "2000"; //2s vigil is the deafult time
    public static final int DEFAULT_VIGIL_INT = 120000;
    public static final String DEFAULT_PING_SRV = "8.8.8.8"; //Google name server
    private static final boolean DEFAULT_METHOD_POST = true;
    private static final int PING_TIMEOUT = 2000; //Acceptable RTT
    private static final String HTTPS_PROTOCOL = "https";

//    Related to parameters
    private static final String EXTRA_USER = "com.deltasoft.quickeats.extra.USER";
    private static final String EXTRA_PUSER = "com.deltasoft.quickeats.extra.PUSER";
    private static final String EXTRA_PASS = "com.deltasoft.quickeats.extra.PASS";
    private static final String EXTRA_PPASS = "com.deltasoft.quickeats.extra.PPASS";
    private static final String EXTRA_URL  = "com.deltasoft.quickeats.extra.URL";
    private static final String EXTRA_WAIT = "com.deltasoft.quickeats.extra.WAIT";
    private static final String EXTRA_SERVER = "com.deltasoft.quickeats.extra.SERVER";
    private static final String EXTRA_METHOD = "com.deltasoft.quickeats.extra.METHOD";

//    Service related
    private static final String samuraiNotificationChannel = "com.deltasoft.quickeats.channel.samurai";
    private static final String samuraiNotification = "Login Samurai";

    //Attribute for the last used notification
    /**
     * Attribute for the last used notification
     */
    private static int lastNotifId;
    private static boolean stopFlag;
    private static int spawn;
    private static boolean connectionState;

    public SamuraiService() {

        super("SamuraiService");

    }

    public static boolean isConnectionUp(){
        return connectionState;
    }

    public static boolean isServiceRunning(){
        if(spawn!=0){
            return true;
        }
        return false;
    }

    private void setConnectionState(boolean up){
        connectionState = up;
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionVigil(Context context, String username, String password,
                                        String prefUsername, String prefPassword,
                                        String url, int millis, String pingServer, boolean postMethod) {
//        Pack the parameters into intent
        Intent intent = new Intent(context, SamuraiService.class);
        intent.setAction(ACTION_VIGIL);
        intent.putExtra(EXTRA_WAIT, millis);
        intent.putExtra(EXTRA_URL,url);
        intent.putExtra(EXTRA_USER,username);
        intent.putExtra(EXTRA_PUSER,prefUsername);
        intent.putExtra(EXTRA_PPASS,prefPassword);
        intent.putExtra(EXTRA_PASS,password);
        intent.putExtra(EXTRA_SERVER, pingServer);
        intent.putExtra(EXTRA_METHOD, postMethod);
        //Start the service
        stopFlag = false; //Enable the service
        //Update the spawn number
        spawn++;
        context.startService(intent);
    }

    public static void stopVigil(Context context){
        Intent intent = new Intent(context, SamuraiService.class);
        intent.setAction(ACTION_VIGIL);
        //Stop the service
        stopFlag = true;
        context.stopService(intent);
    }

    public int onStartCommand(Intent intent, int flags, int startID){
        //Notification builder
        createNotificationChannel();
        super.onStartCommand(intent,flags, startID);
        return START_REDELIVER_INTENT;
    }

    public void onDestroy(){
        super.onDestroy();
        pushNotification(getString(R.string.msg_srv_stp)); //"Service has been stopped"
    }

//    Store all runtime parameters here
    static int millis;
    static String url;
    static String server;
    static String username;
    static String password;
    static boolean postMethod;

    @Override
    protected void onHandleIntent(Intent intent) {
//            Determine Intent parameters
        if(intent!=null){
        final String action = intent.getAction();
            if (ACTION_VIGIL.equals(action)) {
                //Runtime variables are loaded the first time through Activity
                millis = intent.getIntExtra(EXTRA_WAIT, DEFAULT_VIGIL_INT);
                url = intent.getStringExtra(EXTRA_URL);
                server = intent.getStringExtra(EXTRA_SERVER);
                username = intent.getStringExtra(EXTRA_USER);
                password = intent.getStringExtra(EXTRA_PASS);
                postMethod = intent.getBooleanExtra(EXTRA_METHOD, DEFAULT_METHOD_POST);
            }
//                Start the vigil
                handleActionVigil(server, millis, url, username, password, postMethod);
//                Let the world know
                pushNotification(getString(R.string.msg_srv_start)); //Service has been started
            }
//        }
    }

    /**
     * Handle action Vigil in the provided background thread with the provided
     * parameters.
     * @param server
     * @param frequency
     */
    private void handleActionVigil(String server, int frequency, String loginUrl, String username, String password, boolean method) {
        int spawnAtStart = spawn;
        boolean isReachable = false;
        do {
            //Establish a connection with the URL
            isReachable = pingServer(server);
            if (isReachable){
                //Connection is reachable, sleep for some time
                setConnectionState(true);
                Log.i(this.toString(),"Connection is UP, sleeping "+frequency+" ms");
            }else{
                //Connection is not reachable
                setConnectionState(false);
                Log.i(this.toString(),"Connection is DOWN, trying to login with "+loginUrl);
                //Start actions for login
                if(login(loginUrl, username, password, method))
                    Log.i(this.toString(),"Connection attempted with "+loginUrl);
                pushNotification(getString(R.string.msg_conn_att)+loginUrl); //Connection attempted with
            }
            trySleeping(frequency); //important to sleep after trying to login
            int spawnAtEnd = spawn;
            //Check if the starting spawn number and current number are same
            if(spawnAtEnd!=spawnAtStart)
                stopSelf(); //If they are different, stop this spawn
        }while(!stopFlag);

        //Stop background service after everything done
        stopSelf();
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence channelName = getString(R.string.channelName);
            NotificationChannel notifChannel
                    = new NotificationChannel(samuraiNotificationChannel, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notifManager = getSystemService(NotificationManager.class);
            notifManager.createNotificationChannel(notifChannel);
        }
    }

    private boolean pingServer(String serverAddress){
            boolean reached = isReachable(serverAddress,PING_TIMEOUT);
            Log.i(toString(),"Server "+serverAddress+" reached: "+reached);
            return reached;
    }

    private boolean login(String loginUrl, String username, String password, boolean method){
        if(loginUrl != null){
            if(method) {
                Log.d(toString(), "Login using POST");
                return loginUsingPost(loginUrl, username, password);
            }else{
                Log.d(toString(), "Login using POST");
                return loginUsingGet(loginUrl, username, password);
            }
        }
        return false;
    }

    private boolean isReachable(String nameServer, int timeout){

        URL url;
        try{
            url = new URL(HTTPS_PROTOCOL,nameServer,"/");
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.getContent();
        }catch(UnknownHostException e){
            Log.e(toString(),"Unknown Host "+nameServer);
            return false;
        }catch(IOException e){
            Log.e(toString(),"IO Exception "+nameServer);
            return false;
        }
        return true;
    }

    private void trySleeping(int millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean loginUsingPost(String loginUrl, String username, String password){
        URL login;
        try{
            login = new URL(loginUrl);
            HttpURLConnection loginConnection = (HttpURLConnection)login.openConnection();
            loginConnection.setRequestMethod("POST");
            loginConnection.setDoOutput(true);
            String outString = URLEncoder.encode("username","UTF-8")+"="+
                    URLEncoder.encode(username,"UTF-8")+"&"+
                    URLEncoder.encode("password","UTF-8")+"="+
                    URLEncoder.encode(password,"UTF-8");
            byte[] outBytes = outString.getBytes(StandardCharsets.UTF_8);
            Log.d(toString(),"POST parameters: "+outBytes.toString());
            loginConnection.setFixedLengthStreamingMode(outBytes.length);
            loginConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            loginConnection.connect();
            try(OutputStream os = loginConnection.getOutputStream()) {
                Log.d(toString(),"Writing "+outBytes.length+"B to output");
                os.write(outBytes);
                os.flush();
                os.close();
            }
            return true;
        }catch(UnknownHostException e){
            Log.e(toString(),"Unknown Host "+loginUrl);
            return false;
        }catch(IOException e){
            Log.e(toString(),"IO Exception "+loginUrl);
            return false;
        }
    }

    private boolean loginUsingGet(String url, String username, String password){
        if(url.contains("<u>") && url.contains("<p>")) {
            //replace <u>   //replace <p>
            String builtUrl = url.replace("<u>", username)
                    .replace("<p>", password);
            URL login;
            try{
                login = new URL(builtUrl);
                login.openConnection();
                return true;
            }catch(IOException e){
                Log.e(toString(),"IO Exception "+builtUrl);
                return false;
            }
        }
        return false;
    }

    private void pushNotification(String msgString){
            //Push a notification
            NotificationManagerCompat manager = NotificationManagerCompat.from(this.getApplicationContext());
            //Build the notification
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this.getApplicationContext(), samuraiNotificationChannel)
                    .setSmallIcon(R.drawable.ic_notif_samurai)
                    .setContentTitle(samuraiNotification)
                    .setContentText(msgString)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);
            //Show the notification
            manager.notify(lastNotifId++, notifBuilder.build());

    }
}