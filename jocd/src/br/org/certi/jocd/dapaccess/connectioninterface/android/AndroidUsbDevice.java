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
package br.org.certi.jocd.dapaccess.connectioninterface.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.util.Util;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AndroidUsbDevice implements ConnectionInterface {

  // Logging
  private final static String CLASS_NAME = AndroidUsbDevice.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  protected int vendorId;
  protected int productId;
  protected String deviceName;
  protected String productName;
  protected String manufacturerName;
  protected String serialNumber;

  // Android USB Manager.
  private final UsbManager usbManager;
  private final Context context;
  private UsbDevice device;
  private AtomicBoolean atomicOpen = new AtomicBoolean(false);
  private final String appName;

  private int packetCount = 1;
  private int packetSize = 64;

  private UsbInterface usbInterface;

  // Interface number for HID.
  private int interfaceNumber;
  private UsbDeviceConnection deviceConnection;
  private UsbEndpoint inputEndpoint;
  private UsbEndpoint outputEndpoint;

  // Locker object for read/write thread.
  private Object locker = new Object();
  private Thread rxThread = null;

  // A queue to hold the received data while it isn't read.
  private ArrayDeque<byte[]> rxQueue = new ArrayDeque<byte[]>();

  public static final byte USB_CLASS_HID = (byte) 0x03;
  public static final byte USB_INPUT_ENDPOINT_ADDRESS = (byte) 0x80;

  /*
   * Constructor
   */
  public AndroidUsbDevice() {
    this.context = AndroidApplicationContext.get();
    this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);

    ApplicationInfo applicationInfo = this.context.getApplicationInfo();
    int stringId = applicationInfo.labelRes;
    this.appName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() :
        this.context.getString(stringId);
  }

  /*
   * Returns all connected devices.
   */
  public List<ConnectionInterface> getAllConnectedDevices() {

    HashMap<String, UsbDevice> usbDevList = usbManager.getDeviceList();

    List<ConnectionInterface> deviceList = new ArrayList<ConnectionInterface>();

    LOGGER.log(Level.FINE, "Listing connected devices...");
    for (Map.Entry<String, UsbDevice> entry : usbDevList.entrySet()) {
      UsbDevice device = entry.getValue();
      LOGGER.log(Level.FINE, "key: " + entry.getKey());
      LOGGER.log(Level.FINE, "value: " + device);

      AndroidUsbDevice board = new AndroidUsbDevice();
      board.device = device;
      board.vendorId = device.getVendorId();
      board.productId = device.getProductId();
      board.deviceName = device.getDeviceName();
      board.productName = device.getProductName();
      board.manufacturerName = device.getManufacturerName();
      board.serialNumber = device.getSerialNumber();

      // Add this board to the list of devices.
      deviceList.add(board);
      LOGGER.log(Level.FINE, "Vendor ID: " + board.vendorId +
          "\nProduct ID: " + board.productId +
          "\nDevice Name: " + board.deviceName +
          "\nProduct Name: " + board.productName +
          "\nManufacturer Name: " + board.manufacturerName +
          "\nSerial Number: " + board.serialNumber);
    }

    return deviceList;
  }

  public void rxHandler() {
    // TODO
  }

  /*
   * Overload to read(timeout).
   * Use 20ms as the default timeout.
   */
  public byte[] read() throws TimeoutException {
    return this.read(20);
  }

  /*
   * Read data on the IN endpoint associated to the HID interface.
   */
  public byte[] read(int timeout) throws TimeoutException {
    if (device == null) {
      LOGGER.log(Level.SEVERE, "Internal Error. Trying to read from null device");
      return null;
    }

    long startTime = System.currentTimeMillis();

    while (rxQueue.isEmpty()) {
      if (System.currentTimeMillis() - startTime > timeout) {
        // Timeout.
        // Read operations should typically take ~1-2ms.
        // If this exception occurs, then it could indicate
        // a problem in one of the following areas:
        // 1. Bad usb driver causing either a dropped read or write
        // 2. CMSIS-DAP firmware problem cause a dropped read or write
        // 3. CMSIS-DAP is performing a long operation or is being
        //    halted in a debugger
        LOGGER.log(Level.SEVERE, "Read timed out.");
        throw new TimeoutException();
      }
    }

    return rxQueue.poll();
  }

  /*
   * Overload to write(data, timeout).
   * Use 20ms as the default timeout.
   */
  public void write(byte[] data) throws Error {
    write(data, 20);
  }

  /*
   * Write data on the OUT endpoint associated to the HID interface.
   */
  public void write(byte[] data, int timeout) throws Error {
    if (device == null || usbInterface == null) {
      LOGGER.log(Level.SEVERE, "Internal Error on write. The device/usbInterface is null");
      return;
    }

    // If we have an output endpoint, get its packet size.
    int reportSize = this.packetSize;
    if (outputEndpoint != null) {
      reportSize = outputEndpoint.getMaxPacketSize();
    }

    // Fill the packet the left space, appending 0 on the end of data.
    byte[] packet = Util.fillArray(data, reportSize, (byte)0x00);

    synchronized (locker) {

      // If we don't have an output endpoint, than send it as a control transfer
      // using endpoint 0.
      if (outputEndpoint == null) {
        // Host to device request of type Class of Recipient Interface.
        // Request type bitmask:
        //    Direction   | Type  | Recipient |
        // Host to device | Class | Interface |
        //       0        |  01   |   00001   |
        // requestType = 0 01 00000 = 0010 0001 = 0x21
        int requestType = 0x21;

        // Set_REPORT (HID class-specific request for transferring data over EP0)
        // Request:
        // SET_CONFIGURATION = 0x09
        int request = 0x09;

        // Issuing an OUT report.
        int value = 0x200;

        // mBed Board interface number for HID.
        int index = this.interfaceNumber;
        deviceConnection
            .controlTransfer(requestType, request, value, index, packet, data.length, timeout);
        return;
      }

      // If we got here, means that we have an output endpoint.
      int written = deviceConnection.bulkTransfer(outputEndpoint, packet, packet.length, timeout);
    }
  }

  /*
   * Open the device.
   */
  public void open() throws InsufficientPermissions {
    // From now, no one else can open this device until we do not set it to false again.
    if (!atomicOpen.compareAndSet(false, true)) {
      LOGGER.log(Level.WARNING, "Trying to open USB device while is already opened.");
      return;
    }

    // Do it once, and break to clean if anything goes wrong.
    do {
      if (this.deviceConnection != null) {
        LOGGER.log(Level.WARNING, "Trying to open USB device while deviceConnection isn't null.");
        break;
      }

      if (this.device == null) {
        LOGGER.log(Level.SEVERE, "Trying to open device a null device.");
        break;
      }

      // Check if we have permission.
      if (!usbManager.hasPermission(device)) {
        LOGGER.log(Level.WARNING,
            appName + " doesn't have permission to access device with Product ID: "
                + device.getProductId() + ", Vendor ID: " + device.getVendorId() + " and " +
                "serial number: " + device.getSerialNumber());

        // Throw exception, so others will know that this operation failed.
        throw new InsufficientPermissions(device);
      }

      // Attempt to open the device.
      this.deviceConnection = usbManager.openDevice(device);
      if (this.deviceConnection == null) {
        LOGGER.log(Level.SEVERE, "Unable to open device.");
        return;
      }

      // Look for the HID interface.
      if (!lookForHidInterface()) {
        LOGGER.log(Level.SEVERE, "Couldn't find any HID device.");
        break;
      }

      // Find endpoints.
      if (!findEndpoints()) {
        LOGGER.log(Level.SEVERE, "Couldn't find endpoints.");
        break;
      }

      // Claim interface.
      if (!this.deviceConnection.claimInterface(usbInterface, true)) {
        LOGGER.log(Level.SEVERE, "Couldn't claim interface.");
        break;
      }

      // Start rx thread.
      this.startRxThread();

      // If everything ok, leave (don't let it get to clean section below).
      return;

    } while (false);

    // We should never get here... unless something went wrong.
    // Clean whatever we done here.
    close();
  }

  /*
   * Close the device.
   */
  public void close() {
    // Stop the rx thread (if exists).
    if (rxThread != null) {
      stopRxThread();
    }

    // Release and close devise connection.
    if (this.deviceConnection != null) {
      this.deviceConnection.releaseInterface(usbInterface);
      this.deviceConnection.close();
      this.deviceConnection = null;
    }

    // Clean endpoints and interface number.
    this.inputEndpoint = null;
    this.outputEndpoint = null;
    this.interfaceNumber = -1;

    // Allow this device to be opened again.
    this.atomicOpen.set(false);
  }

  private void releaseResources() {

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

  public int getPacketCount() {
    return packetCount;
  }

  public void setPacketSize(int packetSize) {
    this.packetSize = packetSize;
  }

  public void setPacketCount(int packetCount) {
    this.packetCount = packetCount;
  }

  private boolean lookForHidInterface() {
    int interfaceCount = this.device.getInterfaceCount();
    this.interfaceNumber = -1;
    for (int i = 0; i < interfaceCount; i++) {
      UsbInterface iface = this.device.getInterface(i);

      // The interface that we are looking for, should match the class
      // 0x03 (HID). If we find it, save this interface.
      if (iface.getInterfaceClass() == USB_CLASS_HID) {
        this.usbInterface = iface;
        this.interfaceNumber = i;
        break;
      }
      ;
    }

    if (this.interfaceNumber < 0 || this.usbInterface == null) {
      return false;
    }
    return true;
  }

  private boolean findEndpoints() {
    int endpointCount = this.usbInterface.getEndpointCount();

    // We should have 2 endpoints, but this is not required.
    // If there is no EP for OUT then we can use CTRL EP.
    // The IN EP is required.
    if (endpointCount > 2) {
      LOGGER.log(Level.SEVERE,
          "Found " + endpointCount + " endpoints on the HID interface while it " +
              "was expected to have up to 2.");
      return false;
    }

    for (int i = 0; i < endpointCount; i++) {
      UsbEndpoint endpoint = this.usbInterface.getEndpoint(i);
      if ((endpoint.getAddress() & USB_INPUT_ENDPOINT_ADDRESS) != 0) {
        inputEndpoint = endpoint;
      } else {
        outputEndpoint = endpoint;
      }
    }

    if (inputEndpoint == null) {
      return false;
    }
    return true;
  }

  /*
   * Thread to read data from USB and enqueue to be read later.
   */
  private Runnable rxTask = new Runnable() {

    public void run() {

      while (atomicOpen.get()) {
        synchronized (locker) {
          if (device == null || usbInterface == null || inputEndpoint == null) {
            LOGGER.log(Level.SEVERE, "Internal Error on rxTask. The device/usbInterface/" +
                "outputEndpoint is null.");
            return;
          }

          packetSize = inputEndpoint.getMaxPacketSize();
          byte[] packet = new byte[packetSize];
          int received = deviceConnection.bulkTransfer(inputEndpoint, packet, packetSize,
              50);

          // We might receive less bytes than packetSize, so we need to add only the
          // received data to a new array and pass it to our queue.
          if (received >= 0) {
            byte[] receivedData = new byte[received];
            rxQueue.add(Util.getSubArray(packet, 0, received));
          }
        }

        // Sleep for 10 ms to pause, so other thread can write data or anything.
        // As both read and write data methods lock each other - they cannot be run in
        // parallel.
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          // This thread has been interrupted.
          return;
        }
      }
    }
  };

  /*
   * Starts the Rx Thread.
   */
  public void startRxThread() {
    if (rxThread != null) {
      LOGGER.log(Level.SEVERE, "RX thread has already been started.");
      return;
    }

    rxThread = new Thread(rxTask);
    rxThread.start();
  }

  /*
   * Starts the Rx Thread.
   */
  public void stopRxThread() {
    if (rxThread == null) {
      LOGGER.log(Level.SEVERE, "RX thread isn't running.");
      return;
    }

    rxThread.interrupt();
    try {
      rxThread.join(100);
      rxThread = null;
    } catch (InterruptedException e) {
      // Main thread interrupted.
      // Just leave.
    }
  }
}
