package com.example.mrtin.hackinghealth;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by mrtin on 2016-11-19.
 */
public class liveStreamActivity extends MainActivity implements SensorEventListener{
    private BandClient client= null;
    private ImageButton startButton;
    long steps;
    private TextView txtStatus, txtStatus2;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final WeakReference<Activity> reference= new WeakReference<Activity>(this);

        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.live_stream_main, null, false);
        mDrawer.addView(contentView, 0);

        getSupportActionBar().setTitle("Live Stream");
        //startButton= (ImageButton) findViewById(R.id.btnStart);
        txtStatus= (TextView) findViewById(R.id.txtStat);
        txtStatus2= (TextView) findViewById(R.id.txtStat2);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

        new HeartRateConsentTask().execute(reference);
        Calendar rightNow= Calendar.getInstance();
        new HeartRateSubscriptionTask().execute();
        new BandGSRSubscriptionTask().execute();
    }
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            //Find paired bands
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                //display message to user
                return false;
            }
            client = BandClientManager.getInstance().create(this, devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        return ConnectionState.CONNECTED == client.connect().await();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
        }
        if(mySensor.getType() == Sensor.TYPE_GYROSCOPE){
            float x= sensorEvent.values[0];
            float y= sensorEvent.values[1];
            float z= sensorEvent.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class BandPedometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    client.getSensorManager().registerPedometerEventListener(mPedometerEventListener);
                }
                else{
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the bnad is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }
    private class BandGSRSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    client.getSensorManager().registerGsrEventListener(mGSREventListener);
                }
                else{
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }
    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    } else {
                        appendToUI("You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }
    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        protected Void doInBackground(WeakReference<Activity>... params){
            try{
                if(getConnectedBandClient()){
                    if(params[0].get()!=null){
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean b){}
                        });
                    }
                }
                else{
                    appendToUI("Band not connected");
                }
            }
            catch(Exception e)
            {}
            return null;
        }
    }
    private BandPedometerEventListener mPedometerEventListener= new BandPedometerEventListener() {
        @Override
        public void onBandPedometerChanged(BandPedometerEvent bandPedometerEvent) {
            try {
                steps=bandPedometerEvent.getStepsToday();
            } catch (InvalidBandVersionException e) {
                e.printStackTrace();
            }
        }
    };
    private BandHeartRateEventListener mHeartRateEventListener= new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if(event!=null){
                Calendar rightNow= Calendar.getInstance();
                String date= getDate(rightNow);
                //message to cloud
                new BandPedometerSubscriptionTask().execute();
                appendToUI(String.format("Heart Rate= %d beats per minute\n" + "Quality=%s\n", event.getHeartRate(), event.getQuality()));
            }
        }
    };
    private BandGsrEventListener mGSREventListener= new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent bandGsrEvent) {
            if(bandGsrEvent!=null){
                Calendar rightNow= Calendar.getInstance();
                String date=getDate(rightNow);
                new BandPedometerSubscriptionTask().execute();
                GSRToUI(String.format("Your resistance is:%d", bandGsrEvent.getResistance()));
            }
        }
    };

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(string);
            }
        });
    }
    private void GSRToUI(final String string){
        this.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                txtStatus2.setText(string);
            }
        });
    }
    private String getDate(Calendar rightNow){
        String date;
        Date now= rightNow.getTime();
        date= (now.getMonth()+1)+ "/" +  now.getDate()+ "/" + (now.getYear()-100)+ " " + now.getHours()+":"+now.getMinutes()+":"+now.getSeconds();
        return date;
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.getMenu().getItem(0).setChecked(true);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
