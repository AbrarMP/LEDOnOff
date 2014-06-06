package com.example.ledonoff;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import com.example.ledonoff.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class LEDOnOff extends Activity {
  private static final String TAG = "LEDOnOff";
  
  Button btnOne, btnTwo;
  SeekBar seek;
  private String motorSelect = "0";
  private static final int REQUEST_ENABLE_BT = 1;
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private OutputStream outStream = null;
  
  // Well known SPP UUID
  private static final UUID MY_UUID =
      UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  // Insert your bluetooth devices MAC address
  private static String address = "20:13:06:19:17:46";
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "In onCreate()");

    setContentView(R.layout.main);

    btnOne = (Button) findViewById(R.id.btn1);
    btnTwo = (Button) findViewById(R.id.btn2);
    seek = (SeekBar) findViewById(R.id.seekBar1);
    
    btAdapter = BluetoothAdapter.getDefaultAdapter();
    checkBTState();

    btnOne.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        //sendData("255\r\n");
    	motorSelect = "1\n";
        Toast msg = Toast.makeText(getBaseContext(),
            "You have selected motor 1", Toast.LENGTH_SHORT);
        msg.show();
      }
    });

    btnTwo.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        //sendData("0\r\n");
    	motorSelect = "2\r\n";
        Toast msg = Toast.makeText(getBaseContext(),
            "You have selected motor 2", Toast.LENGTH_SHORT);
        msg.show();
      }
    });
    seek.setMax(255);
    seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    	 int progressChanged = 0;
    	 
         public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
             progressChanged = progress;
             sendData(motorSelect + Integer.toString(progressChanged)+"\r\n");
             Log.d("Data Sent", motorSelect + Integer.toString(progressChanged)+"\r\n");
         }

         public void onStartTrackingTouch(SeekBar seekBar) {
             // TODO Auto-generated method stub
         }

         public void onStopTrackingTouch(SeekBar seekBar) {
             Toast.makeText(LEDOnOff.this,"seek bar progress:"+progressChanged, 
                     Toast.LENGTH_SHORT).show();
         }
    });
  }
  
  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "...In onResume - Attempting client connect...");
  
    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = btAdapter.getRemoteDevice(address);
  
    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
    try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }
  
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
  
    // Establish the connection.  This will block until it connects.
    Log.d(TAG, "...Connecting to Remote...");
    try {
      btSocket.connect();
      Log.d(TAG, "...Connection established and data link opened...");
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
    
    // Create a data stream so we can talk to server.
    Log.d(TAG, "...Creating Socket...");

    try {
      outStream = btSocket.getOutputStream();
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    Log.d(TAG, "...In onPause()...");

    if (outStream != null) {
      try {
        outStream.flush();
      } catch (IOException e) {
        errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
      }
    }

    try     {
      btSocket.close();
    } catch (IOException e2) {
      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
  }
  
  private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on

    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth is enabled...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
      }
    }
  }

  private void errorExit(String title, String message){
    Toast msg = Toast.makeText(getBaseContext(),
        title + " - " + message, Toast.LENGTH_SHORT);
    msg.show();
    finish();
  }

  private void sendData(String message) {
    byte[] msgBuffer = message.getBytes();

    Log.d(TAG, "...Sending data: " + message + "...");

    try {
      outStream.write(msgBuffer);
    } catch (IOException e) {
      String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
      if (address.equals("00:00:00:00:00:00")) 
        msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
      msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
      
      errorExit("Fatal Error", msg);       
    }
  }
}