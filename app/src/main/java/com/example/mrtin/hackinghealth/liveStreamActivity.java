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
import com.microsoft.band.sensors.HeartRateQuality;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.abs;

/**
 * Created by mrtin on 2016-11-19.
 */
public class liveStreamActivity extends MainActivity implements SensorEventListener{
    private BandClient client= null;
    double rightNow, previous= System.currentTimeMillis();
    private ImageButton startButton;
    long steps;
    private TextView txtStatus, txtStatus2, txtStatus3, txtStatus4, txtStatus5, txtStatus6, txtStatus7, txtStatus8, txtStatus9, txtStatus10, txtStatus11;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope, senSteps;
    int stepping=0;
    double averageCadence=0, sumCadence=0;
    int average=1;
    int heartRate;
    int normHR=80;
    int xAdjG, yAdjG, zAdjG;
    int xAdjA, yAdjA, zAdjA;
    int compensation=0;
    HeartRateQuality quality;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final WeakReference<Activity> reference= new WeakReference<Activity>(this);

        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.live_stream_main, null, false);
        mDrawer.addView(contentView, 0);

        getSupportActionBar().setTitle("Live Stream");
        startButton= (ImageButton) findViewById(R.id.btnStart);
        txtStatus= (TextView) findViewById(R.id.txtStat);
        txtStatus2= (TextView) findViewById(R.id.txtStat2);
        txtStatus3= (TextView) findViewById(R.id.txtStat3);
        txtStatus4= (TextView) findViewById(R.id.txtStat4);
        txtStatus5= (TextView) findViewById(R.id.txtStat5);
        txtStatus6= (TextView) findViewById(R.id.txtStat6);
        txtStatus7= (TextView) findViewById(R.id.txtStat7);
        txtStatus8= (TextView) findViewById(R.id.txtStat8);
        txtStatus9= (TextView) findViewById(R.id.txtStat9);
        txtStatus10=(TextView) findViewById(R.id.txtStat10);
        txtStatus11=(TextView) findViewById(R.id.txtStat11);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        senGyroscope= senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        senSteps= senSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        senSensorManager.registerListener(this, senSteps, SensorManager.SENSOR_DELAY_NORMAL);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new HeartRateConsentTask().execute(reference);
                Calendar rightNow= Calendar.getInstance();
                new HeartRateSubscriptionTask().execute();
                new BandGSRSubscriptionTask().execute();
            }
        });
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
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float x= sensorEvent.values[0];
            float y= sensorEvent.values[1];
            float z= sensorEvent.values[2];
            xAdjA = (int)(sensorEvent.values[0]*1.15);
            yAdjA = (int)(sensorEvent.values[1]*1.15);
            zAdjA = (int)(sensorEvent.values[2]*1.15);
            runOnNick("X:"+ x + "Y:" + y + "Z:" + z);
            runAdj("X:" + xAdjA + "Y:" + yAdjA + "Z:" + zAdjA);
        }
        if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            float x= sensorEvent.values[0];
            float y= sensorEvent.values[1];
            float z= sensorEvent.values[2];
            xAdjG= (int)(sensorEvent.values[0]*1.15);
            yAdjG= (int)(sensorEvent.values[1]*1.15);
            zAdjG= (int)(sensorEvent.values[2]*1.15);
            runOnBrandon("X:"+ x + "Y:" + y + "Z:" + z);
            runAdj2("X:" + xAdjG + "Y:" + yAdjG + "Z:" + zAdjG);
        }
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR){
            stepping++;
            rightNow= System.currentTimeMillis();
            runOnMarc("Steps:" + stepping);
            if(rightNow-previous<3000 && rightNow-previous>0){
                double cadence= 60.0/(rightNow-previous)*1000;
                sumCadence+=cadence;
                averageCadence=sumCadence/average;
                average++;
                printCadence("Cadence:" + cadence);
                printCadenceAvg("Avg Cadence:" + averageCadence);
                if(abs(averageCadence-cadence)<15 && (heartRate-normHR)<10){
                    printStatus("Walking Speed is Below Average");
                }
                else if(abs(averageCadence-cadence)>15 && (heartRate-normHR)>10){
                    printStatus("Walking Speed is Above Average");
                }
                else if(abs(averageCadence-cadence)>15 && (heartRate-normHR)<10){
                    printStatus("Walking Speed is Above Average, but Heart Rate is Below");
                }
                else if(abs(averageCadence-cadence)<15 && (heartRate-normHR)>10){
                    printStatus("Walking Speed is Below Average and Heart Rate is Above Average");
                }
                if(xAdjA>5 || xAdjG >5 || yAdjA>5 || zAdjA>5 || yAdjG>5 || zAdjG>5){
                    compensation++;
                    printStatus2("Compensation" + compensation);
                }
                else{
                    printStatus("Walking Speed and Heart Rate is Average");
                }
            }
            previous= rightNow;
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
                heartRate= event.getHeartRate();
                quality=event.getQuality();
                appendToUI(String.format("Heart Rate= %d beats per minute\n" + "Quality=%s\n", heartRate, quality));
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
    private void runOnNick(final String string){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus3.setText(string);
            }
        });
    }
    private void runOnBrandon(final String string){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus4.setText(string);
            }
        });
    }
    private void printCadenceAvg(final String string){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus8.setText(string);
            }
        });
    }
    private void runOnMarc(final String string){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus5.setText(string);
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
    private void printCadence(final String string){
        this.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                txtStatus6.setText(string);
            }
        });
    }
    private void printStatus(final String string){
        this.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                txtStatus7.setText(string);
            }
        });
    }
    private void printStatus2(final String string){
        this.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                txtStatus11.setText(string);
            }
        });
    }
    private void runAdj(final String string){
        this.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                txtStatus9.setText(string);
            }
        });
    }
    private void runAdj2(final String string){
        this.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                txtStatus10.setText(string);
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
