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
package br.org.certi.jocdconnusb4java.connectioninterface;

import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.util.Util;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;
import javax.usb.UsbConfiguration;
import javax.usb.UsbInterface;
import javax.usb.UsbEndpoint;
import javax.usb.UsbPipe;
import javax.usb.UsbControlIrp;
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbPlatformException;

public class Usb4JavaDevice implements ConnectionInterface {

  // Logging
  private final static String CLASS_NAME = Usb4JavaDevice.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  protected int vendorId;
  protected int productId;
  protected String productName;
  protected String manufacturerName;
  protected String serialNumber;
  protected UsbDevice device;
  private List<UsbDevice> usbDeviceList = new ArrayList<UsbDevice>();

  private AtomicBoolean atomicOpen = new AtomicBoolean(false);

  private int packetCount = 1;
  private int packetSize = 64;

  private UsbInterface usbInterface;

  // Interface number for HID.
  private int interfaceNumber;
  private UsbEndpoint inputEndpoint;
  private UsbEndpoint outputEndpoint;
  private UsbPipe inputPipe;
  private UsbPipe outputPipe;

  /*
   * Constructor
   */
  public Usb4JavaDevice() {
  }

  /*
   * Returns all connected devices.
   */
  @Override
  public List<ConnectionInterface> getAllConnectedDevices() {

    // Get the USB services and dump information about them

    UsbServices services;
    try {
      services = UsbHostManager.getUsbServices();

      // Reentrant method that will search all devices and fill our usbDeviceList.
      listDevices(services.getRootUsbHub());
    } catch (UsbException e) {
      LOGGER.log(Level.SEVERE, "Couldn't list devices. Exception: " + e.getMessage());
      return new ArrayList<ConnectionInterface>();
    }

    List<ConnectionInterface> deviceList = new ArrayList<ConnectionInterface>();

    LOGGER.log(Level.FINE, "Listing connected devices...");
    for (UsbDevice device : this.usbDeviceList) {

      Usb4JavaDevice board = new Usb4JavaDevice();
      board.device = device;

      UsbDeviceDescriptor usbDescriptor = device.getUsbDeviceDescriptor();
      byte iManufacturer = usbDescriptor.iManufacturer();
      byte iProduct = usbDescriptor.iProduct();

      board.vendorId = usbDescriptor.idVendor();
      board.productId = usbDescriptor.idProduct();

      try {
        try {
          board.productName = device.getProductString();
        } catch (UnsupportedEncodingException e) {
          LOGGER.log(Level.SEVERE, "getProductString: Unsupported encoding.");
          board.productName = "";
        }

        try {
          board.manufacturerName = device.getManufacturerString();
        } catch (UnsupportedEncodingException e) {
          LOGGER.log(Level.SEVERE, "getManufacturerString: Unsupported encoding.");
          board.manufacturerName = "";
        }

        try {
          board.serialNumber = device.getSerialNumberString();
        } catch (UnsupportedEncodingException e) {
          LOGGER.log(Level.SEVERE, "getSerialNumberString: Unsupported encoding.");
          board.serialNumber = "";
        }
      } catch (UsbException e) {
        // On Linux this can fail because user has no write permission on the
        // USB device file. On Windows it can fail because no libusb device
        // driver is installed for the device.
        LOGGER.log(Level.INFO,
            "Couldn't get root USB hub. This device will be ignored. Exception: " + e.getMessage());
        continue;
      }

      // Add this board to the list of devices.
      deviceList.add(board);
      LOGGER.log(Level.FINE, "Vendor ID: " + board.vendorId +
          "\nProduct ID: " + board.productId +
          "\nProduct Name: " + board.productName +
          "\nManufacturer Name: " + board.manufacturerName +
          "\nSerial Number: " + board.serialNumber);
    }
    return deviceList;
  }

  private void listDevices(final UsbDevice device) {
    // If device is a hub then process all child devices by calling this
    // reentrant method.
    if (device.isUsbHub()) {
      UsbHub hub = (UsbHub) device;
      for (UsbDevice child : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
        listDevices(child);
      }
    }

    // If this is not a hub then add this device to usbDeviceList.
    else {
      this.usbDeviceList.add(device);
      System.out.println(device.toString());
    }
  }

  /*
   * Overload to read(timeout).
   * Use 20ms as the default timeout.
   */
  @Override
  public byte[] read() throws TimeoutException {
    return this.read(200);
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

    int received = 0;
    packetSize = inputEndpoint.getUsbEndpointDescriptor().wMaxPacketSize();
    byte[] packet = new byte[packetSize];
    while (received == 0) {
      try {
        received = this.inputPipe.syncSubmit(packet);
      } catch (UsbException e) {
        LOGGER.log(Level.SEVERE, "Could't write data. Exception: " + e.getMessage());
      }

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
    return packet;
  }

  /*
   * Overload to write(data, timeout).
   * Use 20ms as the default timeout.
   */
  @Override
  public void write(byte[] data) throws Error {
    write(data, 20);
  }

  /*
   * Write data on the OUT endpoint associated to the HID interface.
   */
  public void write(byte[] data, int timeout) throws Error {
    if (device == null || usbInterface == null) {
      //TODO Throw an error to notify the failure to the writer
      LOGGER.log(Level.SEVERE, "Internal Error on write. The device/usbInterface is null");
      return;
    }

    // If we have an output endpoint, get its packet size.
    int reportSize = this.packetSize;
    if (outputEndpoint != null) {
      reportSize = outputEndpoint.getUsbEndpointDescriptor().wMaxPacketSize();
    }

    // Fill the packet the left space, appending 0 on the end of data.
    byte[] packet = Util.fillArray(data, reportSize, (byte) 0x00);

    // If we don't have an output endpoint, than send it as a control transfer
    // using endpoint 0.
    if (outputEndpoint == null) {
      // Host to device request of type Class of Recipient Interface.
      // Request type bitmask:
      //    Direction   | Type  | Recipient |
      // Host to device | Class | Interface |
      //       0        |  01   |   00001   |
      // requestType = 0 01 00000 = 0010 0001 = 0x21
      byte requestType = (byte) 0x21;

      // Set_REPORT (HID class-specific request for transferring data over EP0)
      // Request:
      // SET_CONFIGURATION = 0x09
      byte request = (byte) 0x09;

      // Issuing an OUT report.
      short value = 0x200;

      // mBed Board interface number for HID.
      short index = (short) this.interfaceNumber;
      UsbControlIrp usbControlIrp = this.device
          .createUsbControlIrp(requestType, request, value, index);
      usbControlIrp.setData(data);

      try {
        this.device.syncSubmit(usbControlIrp);
      } catch (UsbException e) {
        throw new Error("Could't write data. Exception: " + e.getMessage());
      }
      return;
    }

    // If we got here, means that we have an output endpoint.
    try {
      int written = this.outputPipe.syncSubmit(packet);
    } catch (UsbException e) {
      throw new Error("Couldn't write data. Exception: " + e.getMessage());
    }
  }

  /*
   * Open the device.
   */
  @Override
  public void open() throws InsufficientPermissions {
    // From now, no one else can open this device until we do not set it to false again.
    if (!atomicOpen.compareAndSet(false, true)) {
      LOGGER.log(Level.WARNING, "Trying to open USB device while is already opened.");
      return;
    }

    // Do it once, and break to clean if anything goes wrong.
    do {
      if (this.device == null) {
        LOGGER.log(Level.SEVERE, "Trying to open device a null device.");
        break;
      }

      // Look for the HID interface.
      if (!lookForHidInterface()) {
        LOGGER.log(Level.SEVERE, "Couldn't find any HID device.");
        break;
      }

      // Claim interface.
      try {
        try {
          this.usbInterface.claim();
        } catch (UsbPlatformException e) {
          // The interface might be in use for some kernel driver.
          // Try to force it.
          this.usbInterface.claim(new UsbInterfacePolicy() {
            @Override
            public boolean forceClaim(UsbInterface usbInterface) {
              return true;
            }
          });
        }
      } catch (Exception e) {
        throw new InsufficientPermissions("Couldn't claim interface. " + e);
      }

      // Find endpoints.
      if (!findEndpoints()) {
        LOGGER.log(Level.SEVERE, "Couldn't find endpoints.");
        break;
      }

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
  @Override
  public void close() {
    try {
      this.inputPipe.close();
    } catch (UsbException e) {
      // Nothing to do.
    }
    try {
      if (this.outputPipe != null) {
        this.outputPipe.close();
      }
    } catch (UsbException e) {
      // Nothing to do.
    }
    try {
      this.usbInterface.release();
    } catch (UsbException e) {
      // Nothing to do.
    }

    // Clean endpoints and interface number.
    this.inputEndpoint = null;
    this.outputEndpoint = null;
    this.inputPipe = null;
    this.outputPipe = null;
    this.interfaceNumber = -1;

    // Allow this device to be opened again.
    this.atomicOpen.set(false);
  }

  public int getVendorId() {
    return this.vendorId;
  }

  public int getProductId() {
    return this.productId;
  }

  public String getDeviceName() {
    return this.device.toString();
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

  @Override
  public int getPacketCount() {
    return packetCount;
  }

  @Override
  public void setPacketSize(int packetSize) {
    this.packetSize = packetSize;
  }

  @Override
  public void setPacketCount(int packetCount) {
    this.packetCount = packetCount;
  }

  private boolean lookForHidInterface() {
    UsbConfiguration configuration = this.device.getActiveUsbConfiguration();
    List<UsbInterface> ifaceList = configuration.getUsbInterfaces();

    int i = 0;
    for (UsbInterface iface : ifaceList) {
      if (iface.getUsbInterfaceDescriptor().bInterfaceClass()
          == ConnectionInterface.USB_CLASS_HID) {
        this.usbInterface = iface;
      }
      this.interfaceNumber = i;
      i++;
    }

    if (this.interfaceNumber < 0 || this.usbInterface == null) {
      return false;
    }
    return true;
  }

  private boolean findEndpoints() {
    List<UsbEndpoint> endpointList = this.usbInterface.getUsbEndpoints();
    int endpointCount = endpointList.size();

    // We should have 2 endpoints, but this is not required.
    // If there is no EP for OUT then we can use CTRL EP.
    // The IN EP is required.
    if (endpointCount > 2) {
      LOGGER.log(Level.SEVERE,
          "Found " + endpointCount + " endpoints on the HID interface while it " +
              "was expected to have up to 2.");
      return false;
    }

    for (UsbEndpoint endpoint : endpointList) {
      if ((endpoint.getUsbEndpointDescriptor().bEndpointAddress()
          & ConnectionInterface.USB_INPUT_ENDPOINT_ADDRESS)
          != 0) {
        this.inputEndpoint = endpoint;
        this.inputPipe = endpoint.getUsbPipe();
        try {
          this.inputPipe.open();
        } catch (UsbException e) {
          LOGGER.log(Level.SEVERE, "Couldn't open input pipe. Exception: " + e.getMessage());
        }
      } else {
        this.outputEndpoint = endpoint;
        this.outputPipe = endpoint.getUsbPipe();
        try {
          this.outputPipe.open();
        } catch (UsbException e) {
          LOGGER.log(Level.SEVERE, "Couldn't open input pipe. Exception: " + e.getMessage());
        }
      }
    }

    if (inputEndpoint == null) {
      return false;
    }
    return true;
  }
}
