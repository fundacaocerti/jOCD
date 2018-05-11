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
import android.content.pm.ApplicationInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import br.org.certi.jocd.board.MbedBoard;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.tools.AsyncResponse;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class TestsActivity extends AppCompatActivity implements AsyncResponse.ListBoards {

  // Logging
  private static final String TAG = "TestsActivity";

  List<MbedBoard> boards = null;
  MbedBoard selectedBoard = null;

  private String ACTION_USB_PERMISSION;
  private PendingIntent permissionIntent;

  private static enum Fsm {
    STARTED,
    CLICKED_CONNECT_DISCONNECT,
    STARTING_CONNECT_DISCONNECT
  }

  ;
  Fsm fsm = Fsm.STARTED;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tests);

    ApplicationInfo applicationInfo = this.getApplicationInfo();
    int stringId = applicationInfo.labelRes;
    String appName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() :
        this.getString(stringId);

    // Register the broadcast receiver to request permission for USB devices.
    ACTION_USB_PERMISSION = this.getPackageName() + ".USB_PERMISSION";
    permissionIntent = PendingIntent.getBroadcast(this, 0,
        new Intent(ACTION_USB_PERMISSION), 0);
    IntentFilter filter = new IntentFilter(this.ACTION_USB_PERMISSION);
    registerReceiver(mUsbReceiver, filter);
  }

  public void onClickConnect(View view) {
    Toast.makeText(this, "Button clicked...", Toast.LENGTH_LONG).show();

    this.fsm = Fsm.CLICKED_CONNECT_DISCONNECT;

    // Create a new async task to list all connected devices.
    AsyncFlashToolListDevices asyncListDevices = new AsyncFlashToolListDevices(this, this);
    asyncListDevices.execute();
  }

  private void doConnectDisconnect() {
    // Update the state.
    this.fsm = Fsm.STARTING_CONNECT_DISCONNECT;

    // Check if we have boards to connect/disconnect.
    if (boards.size() < 1) {
      Toast.makeText(this, "No boards connected", Toast.LENGTH_LONG).show();

      // Reset to initial state and exit.
      this.fsm = Fsm.STARTED;
      return;
    }

    // Get the first board.
    this.selectedBoard = boards.get(0);

    try {
      this.selectedBoard.dapAccessLink.connect();
      this.selectedBoard.dapAccessLink.swjSequence();
      this.selectedBoard.dapAccessLink.disconnect();
    } catch (DeviceError e) {
      Toast.makeText(this, "ERROR: DeviceError exception", Toast.LENGTH_LONG).show();
    } catch (TimeoutException e) {
      Toast.makeText(this, "ERROR: Timeout exception", Toast.LENGTH_LONG).show();
    }
    Toast.makeText(this, "Passed!", Toast.LENGTH_LONG).show();

    // Reset to the first state.
    this.fsm = Fsm.STARTED;
  }

  /*
   * Callback to onProgressUpdate (from AsyncTask).
   */
  public void processAsyncTaskUpdate(String status) {
  }

  /*
   * Callback to onPostExecute (from AsyncTask.ListBoards).
   */
  public void processAsyncTaskFinish(List<MbedBoard> boards) {
    this.boards = boards;

    if (this.fsm == Fsm.CLICKED_CONNECT_DISCONNECT) {
      doConnectDisconnect();
    }
  }

  /*
   * Callback to onException (from AsyncTask.ListBoards/FlashBoard).
   */
  public void processAsyncException(Exception exception) {
    if (exception instanceof InsufficientPermissions) {
      // We don't have access to access the USB device.
      // Request permission.

      UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
      UsbDevice usbDevice = (UsbDevice)((InsufficientPermissions) exception).usbDevice;
      usbManager.requestPermission(usbDevice, permissionIntent);

      Log.w(TAG, "Insufficient permissions to access USB Device");
      return;
    }
  }

  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
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
          }
        }
      }
    }
  };

  private void resumeOperation() {

    if (fsm == Fsm.CLICKED_CONNECT_DISCONNECT) {
      // Restart.
      AsyncFlashToolListDevices asyncListDevices = new AsyncFlashToolListDevices(this, this);
      asyncListDevices.execute();
    }

    if (fsm == Fsm.STARTING_CONNECT_DISCONNECT) {
      // Restart.
      doConnectDisconnect();
    }
  }

}
