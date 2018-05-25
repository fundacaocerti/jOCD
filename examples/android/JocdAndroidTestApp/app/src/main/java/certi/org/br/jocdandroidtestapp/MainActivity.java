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
package certi.org.br.jocdandroidtestapp;

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
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import br.org.certi.jocd.Jocd;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
    AsyncResponse.ListBoards, AsyncResponse.FlashBoard {

  EditText fileSelectedEditView;
  TextView noDeviceConnectedTextView;
  ListView boardsListView;
  TextView listDevicesConnectedTextView;
  TextView boardFlashedTextView;

  ArrayAdapter<String> adapter;
  ArrayList<String> listItems = new ArrayList<String>();

  private String flashFilePath = "Download/microbit.hex";

  private String ACTION_USB_PERMISSION;
  private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
  private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
  private PendingIntent permissionIntent;
  IntentFilter filter;

  private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1;
  private static final int REQUEST_CODE_SELECT_FILE = 2;

  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (device != null) {
//              resumeOperation();
            }
          } else {
            Log.d("fefe", "Permission denied for device.");
//            textViewConnectedBoards.setText("Permission denied for device.");
//            fsm = Fsm.INIT;
          }
        }
      }

      if (ACTION_USB_ATTACHED.equals(action) || ACTION_USB_DETACHED.equals(action)) {
        listDevices();
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    noDeviceConnectedTextView = findViewById(R.id.noDeviceConnectedTextView);
    fileSelectedEditView = findViewById(R.id.fileSelectedEditView);
    boardsListView = findViewById(R.id.boardsListView);
    listDevicesConnectedTextView = findViewById(R.id.listDevicesConnectedTextView);
    boardFlashedTextView = findViewById(R.id.boardFlashedTextView);
    fileSelectedEditView.setText(flashFilePath);
    adapter = new ArrayAdapter<String>(getApplicationContext(),
        android.R.layout.simple_spinner_item, listItems) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        // Get the current item from ListView
        View view = super.getView(position, convertView, parent);

        // Get the Layout Parameters for ListView Current Item View
        LayoutParams params = view.getLayoutParams();

        // Set the height of the Item View
        params.height = 50;
        view.setLayoutParams(params);

        return view;
      }
    };
    boardsListView.setAdapter(adapter);

    boardsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
          int pos, long id) {
        if (boardsListView.isItemChecked(pos)) {
          boardsListView.setItemChecked(pos, false);
        } else {
          boardsListView.setItemChecked(pos, true);
        }

        return true;
      }
    });

    boardsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
        String boardText = ((TextView) view).getText().toString();
        boardFlashedTextView.setText("Flashing board " + boardText);
        flashDevice(getUniqueIdFromItem(boardText));
      }
    });

    // Register the broadcast receiver to request permission for USB devices.
    ACTION_USB_PERMISSION = this.getPackageName() + ".USB_PERMISSION";
    permissionIntent = PendingIntent.getBroadcast(this, 0,
        new Intent(ACTION_USB_PERMISSION), 0);
    filter = new IntentFilter();
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_USB_ATTACHED);
    filter.addAction(ACTION_USB_DETACHED);
    registerReceiver(broadcastReceiver, filter);

    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
    for (String key : devices.keySet()) {
      UsbDevice usbDevice = devices.get(key);
      usbManager.requestPermission(usbDevice, permissionIntent);
    }
    checkFileReadPermission();

    Jocd.init(this);
  }

  public void onClickSelectFile(View view) {
    Log.d("CLICK", "Button select file clicked.");

    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("*/*");
    startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
  }

  public void onClickListDevices(View view) {
    Log.d("CLICK", "Button list devices clicked.");
    listDevices();
  }

  private void listDevices() {
    boardFlashedTextView.setText("");
    listItems.clear();
    adapter.notifyDataSetChanged();
    // Create a new async task to list all connected devices.
    AsyncFlashToolListDevices asyncListDevices = new AsyncFlashToolListDevices(this);
    asyncListDevices.execute();
  }

  private void flashDevice(String uniqueId) {
    // We will need to access the file system. Check if we have permission.
    if (!checkFileReadPermission()) {
      Log.d("CLICK", "We don't have enough permission to access the file.");
      return;
    }

    String fullPath = Environment.getExternalStorageDirectory() + "/" + this.fileSelectedEditView.getText();
    boardFlashedTextView.setVisibility(View.GONE);

    if (fullPath == null) {
      Toast.makeText(this, "Can't flash without a file. Please select one.", Toast.LENGTH_LONG)
          .show();
      return;
    } else {
      // Check if the file exists.
      File file = new File(fullPath);
      if (!file.exists()) {
        Toast.makeText(this, "Can't find the file " + fileSelectedEditView.getText(),
            Toast.LENGTH_LONG).show();
        return;
      }
    }

    boardFlashedTextView.setVisibility(View.VISIBLE);

    // Create a new async task to flash the first devices.
    AsyncFlashToolFlashBoard asyncFlashDevice = new AsyncFlashToolFlashBoard(this, this,
        fullPath, uniqueId);
    asyncFlashDevice.execute();
  }

  private String setItemText(String name, String uniqueId) {
    return name + " - " + uniqueId;
  }

  private String getUniqueIdFromItem(String itemText) {
    return itemText.split(" - ")[1];
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

  private void setFileFlashPath(String path) {
    // The Intent will return a path will result in a string with the following pattern:
    // "/document/primary:/Download/microbit.hex"
    // As we don't want this information, we need to get only the string after the colon.
    String[] parts = path.split(":");
    path = parts.length >= 2 ? parts[1] : "";
    this.flashFilePath = path;
    fileSelectedEditView.setText(path);
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
//            resumeOperation();
          } else {
//            Log.w(TAG, "Permission denied to access the external storage.");
          }
        }
      }
    }
  }

  /*
   * Callback to onPostExecute (from AsyncTask.ListBoards).
   */
  @Override
  public void processAsyncTaskFinish(HashMap<String, String> boards) {
    if (boards == null || boards.size() == 0) {
      this.noDeviceConnectedTextView.setVisibility(View.VISIBLE);
      this.listDevicesConnectedTextView.setVisibility(View.GONE);
      return;
    }
    this.noDeviceConnectedTextView.setVisibility(View.GONE);
    this.listDevicesConnectedTextView.setVisibility(View.VISIBLE);

    String boardsText = "";
    for (String uniqueId : boards.keySet()) {
      String name = boards.get(uniqueId);
      listItems.add(setItemText(name, uniqueId));
    }
    adapter.notifyDataSetChanged();
  }

  @Override
  public void processAsyncTaskFinish(String result) {
    this.boardFlashedTextView.setVisibility(View.VISIBLE);
    this.boardFlashedTextView.setText(result);
  }

  @Override
  public void processAsyncTaskUpdate(String status) {

  }
}
