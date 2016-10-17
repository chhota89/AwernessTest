package com.bridgelabz.awernesstest;

import android.Manifest;
import android.app.PendingIntent;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.nio.charset.Charset;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String API_KEY = "AIzaSyCOylTOxFNs-AI0lB8r65FUE1Th1eaFbSo";

    private static final String NEAR_BY_API_KEY = "AIzaSyD-3kIIyANEWDbpPKFkWa8LaD0buqEGGDw";

    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVE";

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int LOCATION=233,NEAR_BY_PLACE=234,WEATHER=235;

    private static final int TTL_IN_SECONDS = 3 * 60; // Three minutes.


    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to three
     * minutes in this sample.
     */
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();


    private GoogleApiClient googleApiClient,nearByClient;
    TextView textView;
    FenceReciver fenceReciver;
    PendingIntent pendingIntent;
    MessageListener messageListener;
    Message message;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.text);

        fenceReciver=new FenceReciver();
        Intent intent=new Intent(FENCE_RECEIVER_ACTION);
        pendingIntent=PendingIntent.getBroadcast(MainActivity.this,10001,intent,0);

        googleApiClient = new GoogleApiClient.Builder(MainActivity.this).addApi(Awareness.API).build();
        googleApiClient.connect();

        nearByClient=new GoogleApiClient.Builder(MainActivity.this).addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this,this)
                .build();

        message=new Message("I am Laxman".getBytes(Charset.forName("UTF-8")));

        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.sample_audio);

        messageListener=new MessageListener() {
            @Override
            public void onFound(Message message) {
                String nearbyMessageString = new String(message.getContent()).trim();
                showToast("Message Found "+new String(nearbyMessageString.getBytes(Charset.forName("UTF-8"))));
            }

            @Override
            public void onLost(Message message) {
                String nearbyMessageString = new String(message.getContent()).trim();
                showToast("Message Lost "+new String(nearbyMessageString.getBytes(Charset.forName("UTF-8"))));
            }
        };


        //Find user Activity
        getUserActivity();

        //Find head phone is plugin or not
        getHeadPhoneState();

        //Find Location result
        getLocation();

        //Find Place
        getNearByPlace();

        //get weather condition
        getWeatherCondition();
    }

    private void getUserActivity(){
        Awareness.SnapshotApi.getDetectedActivity(googleApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                        if (!detectedActivityResult.getStatus().isSuccess()) {
                            Log.i(TAG, "onResult: Fail to get activity api");
                            return;
                        }

                        ActivityRecognitionResult activityRecognitionResult = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity detectedActivity = activityRecognitionResult.getMostProbableActivity();
                        Log.i(TAG, "onResult: " + detectedActivity.toString());
                        String previousText = textView.getText().toString();
                        textView.setText(previousText + "\n" + detectedActivity.toString());
                    }
                });
    }

    private void getHeadPhoneState(){
        Awareness.SnapshotApi.getHeadphoneState(googleApiClient)
                .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
                    @Override
                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
                        if (!headphoneStateResult.getStatus().isSuccess()) {
                            Log.i(TAG, "onResult: Fail to get activity api");
                            return;
                        }
                        String previousText = textView.getText().toString();
                        HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                        if (headphoneState.getState() == HeadphoneState.PLUGGED_IN) {
                            textView.setText(previousText + "\n" + "Head phone is plugin");
                        } else
                            textView.setText(previousText + "\n" + "Head phone is NOT plugin");
                    }
                });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            }
        }
        else if(requestCode==NEAR_BY_PLACE){
            getNearByPlace();
        }
        else if(requestCode==WEATHER)
            getWeatherCondition();
    }

    private void getNearByPlace(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Awareness.SnapshotApi.getPlaces(googleApiClient)
                    .setResultCallback(new ResultCallback<PlacesResult>() {
                        @Override
                        public void onResult(@NonNull PlacesResult placesResult) {
                            if (!placesResult.getStatus().isSuccess()) {
                                Log.i(TAG, "onResult: Fail to get place api");
                                return;
                            }
                            List<PlaceLikelihood> placeLikelihoodList = placesResult.getPlaceLikelihoods();
                            // Show the top 5 possible location results.
                            if (placeLikelihoodList != null) {
                                for (int i = 0; i < 5 && i < placeLikelihoodList.size(); i++) {
                                    String previousText = textView.getText().toString();
                                    PlaceLikelihood p = placeLikelihoodList.get(i);
                                    textView.setText(previousText+"\n"+p.getPlace().getName().toString() + ", likelihood: " + p.getLikelihood());
                                    //Log.i(TAG, p.getPlace().getName().toString() + ", likelihood: " + p.getLikelihood());
                                }
                            } else {
                                Log.e(TAG, "Place is null.");
                            }
                        }
                    });
        }else{
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},NEAR_BY_PLACE);
        }
    }

    private void getLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Awareness.SnapshotApi.getLocation(googleApiClient)
                    .setResultCallback(new ResultCallback<LocationResult>() {
                        @Override
                        public void onResult(@NonNull LocationResult LoaLocationResult) {
                            if (!LoaLocationResult.getStatus().isSuccess()) {
                                Log.i(TAG, "onResult: Fail to get activity api");
                                return;
                            }
                            String previousText = textView.getText().toString();
                            Location location = LoaLocationResult.getLocation();
                            textView.setText(previousText + "\n" + "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                        }
                    });
        }else{
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION);
        }
    }

    public void getWeatherCondition() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Awareness.SnapshotApi.getWeather(googleApiClient)
                    .setResultCallback(new ResultCallback<WeatherResult>() {
                        @Override
                        public void onResult(@NonNull WeatherResult weatherResult) {
                            if (!weatherResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Could not get weather.");
                                return;
                            }
                            Weather weather = weatherResult.getWeather();
                            Log.i(TAG, "Weather: " + weather);
                            String previousText = textView.getText().toString();
                            textView.setText(previousText+"\n"+weather);
                        }
                    });
        }else
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},WEATHER);

    }

    private void registerFences() {
        // Create a fence.
        AwarenessFence headphoneFence= HeadphoneFence.during(HeadphoneState.PLUGGED_IN);

        Awareness.FenceApi.updateFences(googleApiClient, new FenceUpdateRequest.Builder()
                        .addFence("headphoneFenceKey", headphoneFence, pendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Fence was successfully registered.");
                        } else {
                            Log.e(TAG, "Fence could not be registered: " + status);
                        }
                    }
                });
    }

    private void unregisterFence() {
        Awareness.FenceApi.updateFences(googleApiClient,new FenceUpdateRequest.Builder()
                .removeFence("headphoneFenceKey")
                .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Fence was successfully UNRegistered.");
                        } else {
                            Log.e(TAG, "Fence could not be UNregistered: " + status);
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerFences();
        registerReceiver(fenceReciver, new IntentFilter(FENCE_RECEIVER_ACTION));
    }

    @Override
    protected void onStop() {
        unregisterFence();
        unPublishMessage();
        unSubcribeMessage();
        unregisterReceiver(fenceReciver);
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        showToast("Connection is establish.");
        publishMessage();
        subscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {
        showToast("Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showToast("Connection Fail "+connectionResult.getErrorMessage());
    }

    private void publishMessage(){
        Log.i(TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        showToast("No longer publishing");

                    }
                }).build();

        Nearby.Messages.publish(nearByClient, message, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            showToast("Published successfully.");
                        } else {
                            showToast("Could not publish, status = " + status);
                        }
                    }
                });
    }

    private void unPublishMessage(){
        Nearby.Messages.unpublish(nearByClient, message);
    }

    private void subscribe(){

        SubscribeOptions options=new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                    }
                }).build();

        Nearby.Messages.subscribe(nearByClient,messageListener,options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {

                        if (status.isSuccess()) {
                            showToast("Subscribed successfully.");
                        } else {
                            showToast("Unable to subscribe to topic");
                        }
                    }
                });
    }

    private void unSubcribeMessage(){
        Nearby.Messages.unsubscribe(nearByClient, messageListener);
    }

    private void showToast(String message){
        Log.i(TAG, "showToast: ... "+message);
        Toast.makeText(MainActivity.this,message,Toast.LENGTH_LONG).show();
    }

    public class FenceReciver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            FenceState fenceState = FenceState.extract(intent);

            Log.d(TAG, "Fence Receiver Received");

            if (TextUtils.equals(fenceState.getFenceKey(), "headphoneFenceKey")) {
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "Fence > Headphones are plugged in.");
                        mediaPlayer.start();
                        break;
                    case FenceState.FALSE:
                        Log.i(TAG, "Fence > Headphones are NOT plugged in.");
                        mediaPlayer.pause();
                        break;
                    case FenceState.UNKNOWN:
                        Log.i(TAG, "Fence > The headphone fence is in an unknown state.");
                        break;
                }
            }
        }
    }
}
