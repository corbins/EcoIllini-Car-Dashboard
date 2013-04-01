package com.example.ecoillini_car_dashboard;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements Runnable {

	//Don't know if I need these, I'll remove them later probably
	private static final byte COMMAND_BUTTON = 0x1;
	private static final byte TARGET_BUTTON = 0x1;
	private static final byte VALUE_ON = 0x1;
	private static final byte VALUE_OFF = 0x0;
	
	//Provide the TAG and Permission, and button tests
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private static final String TAG = "EcoIllini Car Dashboard";
	private static final String BUTTON_PRESSED_TEXT = "The Button is pressed!";
	private static final String BUTTON_NOT_PRESSED_TEXT = "The Button is not pressed";
	
	//Button text, permission and usb management
	private TextView buttonStateTextView;
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	
	//File I/O setup
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	//Set up the accessories permissions
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					//UsbAccessory accessory = UsbManager.getAccessory(intent);
					UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
						buttonStateTextView.setText("Permission denied for accessory.");
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				//UsbAccessory accessory = UsbManager.getAccessory(intent);
				UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
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
        //mUsbManager = UsbManager.getInstance(this);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this,  0,  new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        
        //TODO: Deprecated.  I hate you android.
        if(getLastNonConfigurationInstance() != null) {
        	mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
        	openAccessory(mAccessory);
        }
        
        setContentView(R.layout.activity_main);
        buttonStateTextView = (TextView) findViewById(R.id.toggleButtonLED);
        
    }
    
    //TODO: Deprecated.  I hate you android.
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
    	
    	//I don't know why this intent is here D:
    	Intent intent = getIntent();
    	if(mInputStream != null && mOutputStream != null) {
    		return;
    	}
    	
    	//Connect to the accessories
    	UsbAccessory[] accessories = mUsbManager.getAccessoryList();
    	UsbAccessory accessory = (accessories == null ? null : accessories[0]);
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
			buttonStateTextView.setText("mAccessory is null.");

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
    		mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "EcoIllini Car Dashboard");
			thread.start();
			Log.d(TAG, "accessory opened");
			buttonStateTextView.setText("Accessory opened.");
    	} else {
    		Log.d(TAG, "accessory open fail");
			buttonStateTextView.setText("Accessory open fail");

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
    
    //Write the command to the arduino
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
				buttonStateTextView.setText("Write failed.");

			}
		}
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

				//ALL COMMANDS ARE UNKNOWN. ZOMG!!!
				switch (buffer[i]) {
				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					buttonStateTextView.setText("Unknown msg: " + buffer[i]);

					i = len;
					break;
				}
			}

		}
    }
}
