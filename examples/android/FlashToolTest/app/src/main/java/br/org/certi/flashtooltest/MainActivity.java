/*
 * Copyright 2018 Fundação CERTI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package br.org.certi.flashtooltest;

import android.Manifest;
import android.Manifest.permission;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import br.org.certi.jocd.board.MbedBoard;
import br.org.certi.jocd.dapaccess.connectioninterface.android.AndroidApplicationContext;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.tools.AsyncResponse;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
    AsyncResponse.ListBoards, AsyncResponse.FlashBoard {

  // Logging
  private static final String TAG = "MainActivity";

  TextView textViewConnectedBoards;
  TextView textViewResult;
  EditText editTextSelectedFile;

  private String ACTION_USB_PERMISSION;
  private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
  private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
  private PendingIntent permissionIntent;
  IntentFilter filter;
  private String flashFilePath = "Download/microbit.hex";

  private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1;
  private static final int REQUEST_CODE_SELECT_FILE = 2;


  private static enum Fsm {
    INIT,
    CLICKED_LIST_DEVICES,
    STARTED_LISTING_DEVICES,
    CLICKED_FLASH_DEVICE,
    STARTED_FLASHING_DEVICE,
    TESTS_ACTIVITY
  }

  Fsm fsm = Fsm.INIT;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    textViewConnectedBoards = (TextView) findViewById(R.id.textViewConnectedBoards);
    textViewResult = (TextView) findViewById(R.id.textViewResult);
    textViewConnectedBoards.setText("Devices....");
    editTextSelectedFile = (EditText) findViewById(R.id.editTextSelectedFile);

    AndroidApplicationContext.getInstance().init(getApplicationContext());

    // Register the broadcast receiver to request permission for USB devices.
    ACTION_USB_PERMISSION = this.getPackageName() + ".USB_PERMISSION";
    permissionIntent = PendingIntent.getBroadcast(this, 0,
        new Intent(ACTION_USB_PERMISSION), 0);
    filter = new IntentFilter();
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_USB_ATTACHED);
    filter.addAction(ACTION_USB_DETACHED);
    registerReceiver(broadcastReceiver, filter);

    // Update the list of devices.
    listDevices();

    checkFileReadPermission();
  }

  public void onClickListDevices(View view) {
    Log.d("CLICK", "Button list devices clicked.");
    this.fsm = Fsm.CLICKED_LIST_DEVICES;
    listDevices();
  }

  private void listDevices() {
    this.fsm = Fsm.STARTED_LISTING_DEVICES;
    // Create a new async task to list all connected devices.
    AsyncFlashToolListDevices asyncListDevices = new AsyncFlashToolListDevices(this, this);
    asyncListDevices.execute();
  }

  public void onClickFlashDevice(View view) {
    this.fsm = Fsm.CLICKED_FLASH_DEVICE;
    Log.d("CLICK", "Button flash device clicked.");

    // We will need to access the file system. Check if we have permission.
    if (!checkFileReadPermission()) {
      Log.d("CLICK", "We don't have enough permission to access the file.");
      return;
    }

    flashDevice();
  }

  public void onClickSelectFile(View view) {
    Log.d("CLICK", "Button select file clicked.");

    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("*/*");
    startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
  }

  private void flashDevice() {
    this.fsm = Fsm.STARTED_FLASHING_DEVICE;

    String fullPath = Environment.getExternalStorageDirectory() + "/" + this.flashFilePath;

    if (fullPath == null) {
      Toast.makeText(this, "Can't flash without a file. Please select one.", Toast.LENGTH_LONG)
          .show();
      this.fsm = Fsm.INIT;
      return;
    } else {
      // Check if the file exists.
      File file = new File(fullPath);
      if (!file.exists()) {
        Toast.makeText(this, "Can't find the file " + editTextSelectedFile.getText(),
            Toast.LENGTH_LONG).show();
        this.fsm = Fsm.INIT;
        return;
      }
    }

    // Create a new async task to flash the first devices.
    AsyncFlashToolFlashBoard asyncFlashDevice = new AsyncFlashToolFlashBoard(this, this,
        fullPath);
    asyncFlashDevice.execute();
  }

  public void onClickTestsButon(View view) {
    Log.d("CLICK", "Button open tests clicked.");

    this.fsm = Fsm.TESTS_ACTIVITY;

    Intent intent = new Intent(this, TestsActivity.class);
    startActivity(intent);
  }

  /*
   * Callback to onProgressUpdate (from AsyncTask).
   */
  public void processAsyncTaskUpdate(String status) {
    textViewConnectedBoards.setText(status);
  }

  /*
   * Callback to onPostExecute (from AsyncTask.FlashBoard).
   */
  public void processAsyncTaskFinish(String result) {
    textViewResult.setText(result);
    this.fsm = Fsm.INIT;
  }

  /*
   * Callback to onPostExecute (from AsyncTask.ListBoards).
   */
  public void processAsyncTaskFinish(List<MbedBoard> boards) {

    if (boards == null || boards.size() == 0) {
      textViewConnectedBoards.setText("No boards connected.");
      this.fsm = Fsm.INIT;
      return;
    }

    String boardsText = "";
    for (int i = 0; i < boards.size(); i++) {
      MbedBoard board = boards.get(i);
      boardsText =
          boardsText + "Board " + (i + 1) + ": " + board.name + " (Board ID: " + board.boardId
              + ")\n";
    }

    textViewConnectedBoards.setText(boardsText);
    this.fsm = Fsm.INIT;
  }

  /*
   * Callback to onException (from AsyncTask.ListBoards/FlashBoard).
   */
  public void processAsyncException(Exception exception) {
    if (exception instanceof InsufficientPermissions) {
      // We don't have access to access the USB device.
      // Request permission.

      UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
      UsbDevice usbDevice = (UsbDevice) ((InsufficientPermissions) exception).usbDevice;
      usbManager.requestPermission(usbDevice, permissionIntent);

      Log.w(TAG, "Insufficient permissions to access USB Device");
      return;
    }
  }

  private void resumeOperation() {

    if (fsm == Fsm.CLICKED_LIST_DEVICES || fsm == Fsm.STARTED_LISTING_DEVICES) {
      // Restart.
      // Create a new async task to list all connected devices.
      listDevices();
    }

    if (fsm == Fsm.CLICKED_FLASH_DEVICE || fsm == Fsm.STARTED_FLASHING_DEVICE) {
      // Restart.
      // Create a new async task to flash the first connected device.
      flashDevice();
    }
  }

  private void setFileFlashPath(String path) {
    // The Intent will return a path will result in a string with the following pattern:
    // "/document/primary:/Download/microbit.hex"
    // As we don't want this information, we need to get only the string after the colon.
    String[] parts = path.split(":");
    path = parts.length >= 2 ? parts[1] : "";
    this.flashFilePath = path;
    editTextSelectedFile.setText(path);
  }

  /*
   * Check if we have the necessary permissions to access the file system.
   */
  private boolean checkFileReadPermission() {
    // We need to ask for permission on Android 6.0 (Marshmallow) and later. Before that, only the
    // manifest was required.
    if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.LOLLIPOP) {
      return true;
    }

    // Since we got here, means that this is has LOLLIPOP or newer Android API. We need to
    // explicitly request permission.
    // - First check if we already have.
    int result = ContextCompat
        .checkSelfPermission(getApplicationContext(), permission.READ_EXTERNAL_STORAGE);
    if (result == PackageManager.PERMISSION_GRANTED) {
      // Yes, we already have permission. There is nothing else to do.
      return true;
    }

    // No, We do not have permission. Ask for...
    // If the user give permission, next time we won't get here again.
    ActivityCompat
        .requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
            REQUEST_CODE_READ_EXTERNAL_STORAGE);
    return false;
  }

  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (device != null) {
              resumeOperation();
            }
          } else {
            Log.d(TAG, "Permission denied for device.");
            textViewConnectedBoards.setText("Permission denied for device.");
            fsm = Fsm.INIT;
          }
        }
      }

      if (ACTION_USB_ATTACHED.equals(action) || ACTION_USB_DETACHED.equals(action)) {
        listDevices();
      }
    }
  };

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Check which request we're responding to.
    if (requestCode == REQUEST_CODE_SELECT_FILE) {
      // Make sure the request was successful.
      if (resultCode == RESULT_OK) {
        // The user picked a file.
        setFileFlashPath(data.getData().getPath());
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
      for (int i = 0; i < permissions.length; i++) {
        String permission = permissions[i];
        int grantResult = grantResults[i];

        if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          if (grantResult == PackageManager.PERMISSION_GRANTED) {
            resumeOperation();
          } else {
            Log.w(TAG, "Permission denied to access the external storage.");
            fsm = Fsm.INIT;
          }
        }
      }
    }
  }

}
