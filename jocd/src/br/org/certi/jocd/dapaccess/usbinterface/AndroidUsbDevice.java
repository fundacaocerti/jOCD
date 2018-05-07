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
package br.org.certi.jocd.dapaccess.usbinterface;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidUsbDevice implements UsbInterface {

    // Logging
    private static final String TAG = "AndroidUsbDevice";

    protected int vendorId;
    protected int  productId;
    protected String deviceName;
    protected String productName;
    protected String manufacturerName;
    protected String serialNumber;

    // Android USB Manager.
    private final UsbManager usbManager;
    private final Context context;

    /*
     * Constructor
     */
    public AndroidUsbDevice(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    /*
     * Returns all the connected CMSIS-DAP devices.
     */
    public List<UsbInterface> getAllConnectedDevices() {

        HashMap<String, UsbDevice> usbDevList = usbManager.getDeviceList();

        List<UsbInterface> deviceList = new ArrayList<UsbInterface>();

        Log.d(TAG, "Listing connected devices...");
        for(Map.Entry<String, UsbDevice> entry : usbDevList.entrySet()) {
            UsbDevice device = entry.getValue();
            Log.d(TAG, "key: " + entry.getKey());
            Log.d(TAG, "value: " + device);

            AndroidUsbDevice board = new AndroidUsbDevice(context);
            board.vendorId = device.getVendorId();
            board.productId = device.getProductId();
            board.deviceName = device.getDeviceName();
            board.productName = device.getProductName();
            board.manufacturerName = device.getManufacturerName();
            board.serialNumber = device.getSerialNumber();

            // Add this board to the list of devices.
            deviceList.add(board);
            Log.d(TAG, "Vendor ID: " + board.vendorId +
                    "\nProduct ID: " + board.productId +
                    "\nDevice Name: " + board.deviceName +
                    "\nProduct Name: " + board.productName +
                    "\nManufacturer Name: " + board.manufacturerName +
                    "\nSerial Number: " + board.serialNumber);
        }


        // TODO
        return deviceList;
    }

    public int getVendorId() {
        return this.vendorId;
    }

    public int getProductId() {
        return this.productId;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getManufacturerName() {
        return this.manufacturerName;
    }

    public String getSerialNumber() {
        return this.serialNumber;
    }

    public void rxHandler() {
        // TODO
    }
    public void read() {
        // TODO
    }

    public void write() {
        // TODO
    }


    /*
     * Open the device.
     */
    public void open() {
        // TODO
    }

    /*
     * Close the device.
     */
    public void close() {
        // TODO
    }
}
