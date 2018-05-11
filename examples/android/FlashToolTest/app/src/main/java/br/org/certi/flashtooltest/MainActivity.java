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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import br.org.certi.jocd.board.MbedBoard;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.tools.AsyncResponse;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
    AsyncResponse.ListBoards, AsyncResponse.FlashBoard {

  // Logging
  private static final String TAG = "MainActivity";

  TextView textViewConnectedBoards;
  TextView textViewResult;

  private String ACTION_USB_PERMISSION;
  private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
  private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
  private PendingIntent permissionIntent;
  IntentFilter filter;

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
    Log.d("CLICK", "Button flash device clicked.");
    this.fsm = Fsm.CLICKED_FLASH_DEVICE;
    flashDevice();
  }

  private void flashDevice() {
    this.fsm = Fsm.STARTED_FLASHING_DEVICE;
    // Create a new async task to flash the first devices.
    AsyncFlashToolFlashBoard asyncFlashDevice = new AsyncFlashToolFlashBoard(this, this);
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
          }
        }
      }

      if (ACTION_USB_ATTACHED.equals(action) || ACTION_USB_DETACHED.equals(action)) {
        listDevices();
      }
    }
  };

  private void resumeOperation() {

    if (fsm == Fsm.CLICKED_LIST_DEVICES || fsm == Fsm.STARTED_LISTING_DEVICES) {
      // Restart.
      // Create a new async task to list all connected devices.
      listDevices();
    }

    if (fsm == Fsm.CLICKED_LIST_DEVICES || fsm == Fsm.STARTED_FLASHING_DEVICE) {
      // Restart.
      // Create a new async task to list all connected devices.
      flashDevice();
    }
  }
}
