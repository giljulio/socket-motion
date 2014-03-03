package com.socketmotion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.authorwjf.R;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.ConnectCallback;
import com.koushikdutta.async.http.socketio.DisconnectCallback;
import com.koushikdutta.async.http.socketio.ErrorCallback;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;

public class MainActivity extends Activity implements 
		SensorEventListener, DisconnectCallback, ErrorCallback, EventCallback, ConnectCallback {

	private static final String TAG = MainActivity.class.getSimpleName();

	private static final String END_POINT = "http://boiling-sands-2684.herokuapp.com:80";

	private static final String KEY_AZIMUTH = "azimuth", KEY_PITCH = "pitch", KEY_ROLL = "roll";

	private SensorManager mSensorManager;
	private Sensor mOrientation;
	private SocketIOClient mClient;


	private TextView mXAxisLabel = (TextView)findViewById(R.id.x_axis);
	private TextView mYAxisLabel = (TextView)findViewById(R.id.y_axis);
	private TextView mZAxisLabel = (TextView)findViewById(R.id.z_axis);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		Log.d(TAG, "Attempting to connect to server...");
		SocketIOClient.connect(END_POINT, this, new Handler());
	}


	@Override
	public void onConnectCompleted(Exception ex, SocketIOClient client) {
		if (ex != null) {
			ex.printStackTrace();
			return;
		}

		Log.d(TAG, "Connection to server.");

		client.setDisconnectCallback(MainActivity.this);
		client.setErrorCallback(MainActivity.this);
		client.addListener("createCallback", new EventCallback() {

			@Override
			public void onEvent(String event, JSONArray argument,
					Acknowledge acknowledge) {

				try {
					TextView tv = (TextView) findViewById(R.id.main_heading);
					JSONObject obj = argument.getJSONObject(0);

					tv.setText(obj.getString("code"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});

		mClient = client;
		mClient.emit("create", new JSONArray());
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
		// You must implement this callback in your code.
	}

	@Override
	protected void onResume() {
		super.onResume();        
		mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float azimuth_angle = event.values[0];
		float pitch_angle = event.values[1];
		float roll_angle = event.values[2];

		mXAxisLabel.setText(""+azimuth_angle);
		mYAxisLabel.setText(""+pitch_angle);
		mZAxisLabel.setText(""+roll_angle);

		if(mClient != null){
			try {
				JSONObject motionObj = new JSONObject();
				motionObj.put(KEY_AZIMUTH, azimuth_angle);
				motionObj.put(KEY_PITCH, pitch_angle);
				motionObj.put(KEY_ROLL, roll_angle);
				JSONArray motionArr = new JSONArray();
				motionArr.put(motionObj);
				mClient.emit("update", motionArr);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onError(String error) {
		Log.d(TAG, error);

	}

	@Override
	public void onDisconnect(Exception e) {
		Log.d(TAG, "Disconnected:" + e.getMessage());
		mClient = null;
	}

	@Override
	public void onEvent(String event, JSONArray argument, Acknowledge acknowledge) {
		Log.d(TAG, "event: " + event + " arguments: " + argument.toString());
		try {
			if(event.equals("createCallback")){
				TextView tv = (TextView) findViewById(R.id.main_heading);
				JSONObject obj = argument.getJSONObject(0);
				tv.setText(obj.getString("code"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}