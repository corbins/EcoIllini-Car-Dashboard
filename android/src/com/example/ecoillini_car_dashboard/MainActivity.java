package com.example.ecoillini_car_dashboard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity implements Runnable {

	//Developed by Corbin Souffrant
	
	
	//Important files:
	//res/xml/accessory_filter.xml -- make sure it matches the arduino descriptions if any of it changes
	//res/values/strings.xml -- strings for the app
	//res/layout/activity_main.xml -- the activity GUI
	//AndroidManifest.xml -- set up any app required things in here
	//This file is the source code, duh!
	
	//Message constants for handler swtich
	private static final int MESSAGE_SPEED = 1;
	private static final int MESSAGE_DISTANCE = 2;
	private static final int MESSAGE_TIME = 3;
	
	//Provide the TAG and Permission
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private static final String TAG = "EcoIllini Car Dashboard";
	
	//Textviews, usb permissions
	private TextView distance_text;
	private TextView speed_text;
	private TextView time_text;
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	
	//File I/O setup
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	//Custom Message Object classes for handler
	protected class SpeedMsg {
		private int speed;
		
		public SpeedMsg(int speed) {
			this.speed = speed;
		}
		
		public int getSpeed() {
			return speed;
		}
	}
	
	protected class DistanceMsg {
		private int distance;
		
		public DistanceMsg(int distance) {
			this.distance = distance;
		}
		
		public int getDistance() {
			return distance;
		}
	}
	
	protected class TimeMsg {
		private int time;
		
		public TimeMsg(int time) {
			this.time = time;
		}
		
		public int getTime() {
			return time;
		}
	}
	
	//Set up the accessories permissions
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Startup the broadcast receiver, get permissions, register the receiver
        mUsbManager = UsbManager.getInstance(this);
        mPermissionIntent = PendingIntent.getBroadcast(this,  0,  new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        
        if(getLastNonConfigurationInstance() != null) {
        	mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
        	openAccessory(mAccessory);
        }
        
        //define widgets and layout
        setContentView(R.layout.activity_main);   
        speed_text = (TextView) findViewById(R.id.speed_text);
        distance_text = (TextView) findViewById(R.id.distance_text);
        time_text = (TextView) findViewById(R.id.time_text);
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	if(mAccessory != null) {
    		return mAccessory;
    	} else {
    		return super.onRetainNonConfigurationInstance();
    	}
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	if(mInputStream != null && mOutputStream != null) {
    		return;
    	}
    	
    	//See if accessories are connected, connect if so.
    	UsbAccessory[] accessories = mUsbManager.getAccessoryList();
    	UsbAccessory accessory = (accessories == null ? null : accessories[0]);

    	//Set up permissions if necessary
    	if(accessory != null) {
    		if(mUsbManager.hasPermission(accessory)) {
    			openAccessory(accessory);
    		} else {
    			synchronized(mUsbReceiver) {
    				if(!mPermissionRequestPending) {
    					mUsbManager.requestPermission(accessory,  mPermissionIntent);
    					mPermissionRequestPending = true;
    				}
    			}
    		}
    	} else {
    		Log.d(TAG, "mAccessory is null");
    	}
    }
    
    //I hope the driver doesn't need to use this
    @Override
    public void onPause() {
    	super.onPause();
    	closeAccessory();
    }
    
    //Bye bye, unregister the USB device
    public void onDestroy() {
    	unregisterReceiver(mUsbReceiver);
    	super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void openAccessory(UsbAccessory accessory) {
    	//Begin accessory communication, set up the channels
    	mFileDescriptor = mUsbManager.openAccessory(accessory);
    	if(mFileDescriptor != null) {
    		//Reconnection, mark it in the csv and set up the file streams
    		appendLog("App started");
    		mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "EcoIllini Car Dashboard");
			thread.start();
			Log.d(TAG, "accessory opened");
    	} else {
    		Log.d(TAG, "accessory open fail");
    	}
    }
    
    //Close the IO
    private void closeAccessory() {
    	try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
    }
    
    //Write the command to the arduino, we don't use this function...
    public void sendCommand(byte command, byte target, int value) {
    	byte[] buffer = new byte[3];
		if (value > 255)
			value = 255;

		buffer[0] = command;
		buffer[1] = target;
		buffer[2] = (byte) value;
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
    }
    
    //convert the message packets to ints
    private int composeInt(byte hi, byte lo) {
    	int val = (int) hi & 0xff;
    	val*=256;
    	val+=(int) lo & 0xff;
    	return val;
    }
    
    //Runnable routine
    public void run() {
    	int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			
			//Anything stored on the communication stream?
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				switch (buffer[i]) {
				case 0x1:
						//handle the speed
					if(len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_SPEED);
						m.obj = new SpeedMsg(composeInt(buffer[i+1], buffer[i+2]));
						mHandler.sendMessage(m);
					}
					i+=3;
					break;
				case 0x2:
						//handle the distance
					if(len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_DISTANCE);
						m.obj = new DistanceMsg(composeInt(buffer[i+1], buffer[i+2]));
						mHandler.sendMessage(m);
					}
					i+=3;
					break;
				case 0x3:
						//handle the time
					if(len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_TIME);
						m.obj = new TimeMsg(composeInt(buffer[i+1], buffer[i+2]));
						mHandler.sendMessage(m);
					}
					i+=3;
					break;
				default:
					//unknown, quit.
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}

		}
    }
    
    //This handles the packets, append to log if needed and then call the appropriate routines
    Handler mHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what){
    		case MESSAGE_SPEED:
    			SpeedMsg s = (SpeedMsg) msg.obj;
    			appendLog(s.getSpeed()+",");
    			handleSpeedMessage(s);
    			break;
    		case MESSAGE_DISTANCE:
    			DistanceMsg d = (DistanceMsg) msg.obj;
    			handleDistanceMessage(d);
    			break;
    		case MESSAGE_TIME:
    			TimeMsg t = (TimeMsg) msg.obj;
    			handleTimeMessage(t);
    			break;
    		}
    	}
    };
    
    //message handling routines
    protected void handleSpeedMessage(SpeedMsg s){
    	speed_text.setText(""+s.getSpeed());
    }
    //The distance was converted to an int by * 10000, so I fix that here.
    protected void handleDistanceMessage(DistanceMsg d){
    	DecimalFormat df = new DecimalFormat("#.##");
    	distance_text.setText(df.format((double)d.getDistance()/10000));
    }
    protected void handleTimeMessage(TimeMsg t){
    	String time = String.format("%02d:%02d", t.getTime() / 60, t.getTime() % 60);
    	time_text.setText(time);
    }
    
    //Appends the log with the speed every update.
    public void appendLog(String text)
    {       
       File logFile = new File("sdcard/log.csv");
       if (!logFile.exists())
       {
          try
          {
             logFile.createNewFile();
          } 
          catch (IOException e)
          {
             // TODO Auto-generated catch block
             e.printStackTrace();
          }
       }
       try
       {
          //BufferedWriter for performance, true to set append to file flag
          BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
          buf.append(text);
          buf.newLine();
          buf.close();
       }
       catch (IOException e)
       {
          // TODO Auto-generated catch block
          e.printStackTrace();
       }
    }
}
