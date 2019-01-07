package com.example.sooraj.fitnessapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.example.sooraj.fitnessapp.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BoundService extends Service implements SensorEventListener {

    //Fields
    private static DatabaseReference users;
    private static User user;
    private static String username;
    private FirebaseDatabase database;
    private IBinder mBinder = new MyBinder();
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        //Get reference to database
        database = FirebaseDatabase.getInstance();
        users = database.getReference("Users");

        //Register sensor for step detection
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        isRunning = true;
    }


    @Override
    public IBinder onBind(Intent intent) {

        //Get reference to current user
        username = intent.getExtras().getString("username");
        users.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                user = dataSnapshot.child(username).getValue(User.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        setAlarm(getApplicationContext());
        return mBinder;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        //Check to make sure user reference exists
        //Check to make sure the sensor that triggered event is the step detector sensor
        if (user != null) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                user.setSteps(user.getSteps() + 1);
                users.child(username).child("steps").setValue(user.getSteps());
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Unregisters sensor when BoundService is deleted
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
        isRunning = false;
    }

    //Sets up custom BroadcastReceiver
    //Configured to trigger receiver at midnight everyday
    public void setAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, MyAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    public static class MyAlarm extends BroadcastReceiver {


        //Required zero argument constructor
        public MyAlarm() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {


            users.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    user = dataSnapshot.child(username).getValue(User.class);
                    Calendar c = Calendar.getInstance();

                    //12 hours subtracted to ensure correct date is stored
                    //Receiver was triggered past midnight due to its low priority among other system tasks
                    Date date = new Date(System.currentTimeMillis() - 12 * 60 * 60 * 1000);
                    SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                    String dateString = df.format(date);

                    //Stores amount of steps taken, calories eaten, and weight that day
                    user.putStepsStorage(dateString, user.getSteps());
                    user.putCalorieStorage(dateString, user.getCalories());
                    user.putWeightStorage(dateString, user.getWeight());

                    //Resets steps, calories, and macro-nutrient counts
                    user.setSteps(0);
                    user.resetFood();

                    //Updates user in Firebase
                    users.child(username).setValue(user);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    //TODO fix documentation
    //Custom binder which simplifies code
    public class MyBinder extends Binder {

        //Custom zero argument getService method
        public BoundService getService() {
            return BoundService.this;
        }
    }
}
