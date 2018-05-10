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
package br.org.certi.jocd.dapaccess;

import static br.org.certi.jocd.dapaccess.connectioninterface.UsbFactory.connectionInterfaceEnum.androidUsbManager;

import android.content.Context;
import android.util.Log;
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.IdInfo;
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.Port;
import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;
import br.org.certi.jocd.dapaccess.connectioninterface.UsbFactory;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferError;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DapAccessCmsisDap {

  // Logging
  private static final String TAG = "DapAccessCmsisDap";

  //CMSIS-DAP values
  public static final byte READ = 1 << 1;

  public static final int FREQUENCY = 1000000; // 1MHz default clock

  private ConnectionInterface connectionInterface = null;
  private boolean deferredTransfer = false;
  private int packetCount = 0;
  private String uniqueId;
  private int frequency;
  private Port dapPort = Port.DEFAULT;
  private int packetSize = 0;
  private CmsisDapProtocol protocol;
  private LinkedList<Transfer> transferList;
  private Command crntCmd;
  private ArrayDeque commandsToRead;
  private byte[] commandsResponseBuf;

  /*
   * Constructor.
   */
  public DapAccessCmsisDap(String uniqueId) {
    super();
    this.uniqueId = uniqueId;
    this.frequency = FREQUENCY;
  }

  /*
   * Get the connected USB devices
   */
  private static List<ConnectionInterface> getDevices(Context context) {
    if (DapSettings.useWs) {
      // Not implemented!
      Log.e(TAG, "Not implemented! Trying to use WS interface.");
      return null;
    }
    return UsbFactory.getUSBInterface(context, androidUsbManager).getAllConnectedDevices();
  }

  /*
   * Return an array of all mbed boards connected
   */
  public static List<DapAccessCmsisDap> getConnectedDevices(Context context) {

    // Store all the DAP links.
    List<DapAccessCmsisDap> allDAPLinks = new ArrayList<DapAccessCmsisDap>();

    // Get all the connected interfaces.
    List<ConnectionInterface> allDevices = getDevices(context);

    // For each interface connected try to create a DAP
    // link and add to our allDAPLinks.
    for (ConnectionInterface iface : allDevices) {
      // Get only CMSIS-DAP devices.
      if (!iface.getProductName().contains("CMSIS-DAP")) {
        continue;
      }

      try {
        String uniqueId = iface.getSerialNumber();
        DapAccessCmsisDap dapLink = new DapAccessCmsisDap(uniqueId);
        allDAPLinks.add(dapLink);
      } catch (Exception e) {
        Log.e(TAG,
            "Exception caught while trying to iterate over " + "all connections interfaces.");
        return null;
      }
    }
    return allDAPLinks;
  }

  public void open(Context context) throws DeviceError {
    if (connectionInterface == null) {
      List<ConnectionInterface> allDevices = this.getDevices(context);
      for (ConnectionInterface device : allDevices) {
        try {
          String uniqueId = getUniqueId(device);
          if (this.uniqueId.equals(uniqueId)) {
            // This assert could indicate that two boards had the same ID
            assert this.connectionInterface == null;
            this.connectionInterface = device;
          }
        } catch (Exception exception) {
          Log.e(TAG, "Failed to get unique id for open", exception);
        }
      }
      if (connectionInterface == null) {
        throw new DeviceError("Unable to open device");
      }
    }

    this.connectionInterface.open();
    this.protocol = new CmsisDapProtocol(connectionInterface);

    if (DapSettings.limitPackets) {
      this.packetCount = 1;
      Log.d(TAG, "Limiting packet count to" + this.packetCount);
    } else {
      this.packetCount = ((Byte) this.protocol.dapInfo(IdInfo.PACKET_COUNT))
          .intValue();
    }

    this.connectionInterface.setPacketCount(this.packetCount);
    this.packetSize = (Integer) this.protocol.dapInfo(IdInfo.PACKET_SIZE);
    this.connectionInterface.setPacketSize(this.packetSize);

    this.initDeferredBuffers();
  }

  public void close() throws TransferError {
    if (connectionInterface == null) {
      return;
    }

    flush();
    connectionInterface.close();
  }

  private String getUniqueId(ConnectionInterface device) {
    return device.getSerialNumber();
  }

  public String getUniqueId() {
    return uniqueId;
  }

  private void flush() throws TransferError {
    // Send current packet
    this.sendPacket();
    // Read all backlogged
    for (Iterator itr = this.commandsToRead.iterator(); itr.hasNext(); ) {
      this.readPacket();
    }
  }

  /*
   * Overload for connect(port), using default value: Port.Default.
   */
  public void connect() throws DeviceError {
    connect(Port.DEFAULT);
  }

  public void connect(Port port) throws DeviceError {
    Port actualPort = this.protocol.connect(port);

    // Set clock frequency.
    this.protocol.setSWJClock(this.frequency);

    // Configure transfer.
    this.protocol.transferConfigure();
  }

  public void disconnect() throws DeviceError {
    this.flush();
    this.protocol.disconnect();
  }

  public void swjSequence() throws DeviceError {
    if (this.dapPort == Port.SWD) {
      // Configure swd protocol.
      this.protocol.swdConfigure();
      // Switch from jtag to swd.
      jtagToSwd();
    } else if (this.dapPort == Port.JTAG) {
      // Configure jtag protocol.
      this.protocol.jtagConfigure((byte) 4);
      // Test logic reset, run test idle.
      this.protocol.swjSequence(new byte[]{0x1F});
    } else {
      Log.e(TAG, "Unexpected DAP port: " + this.dapPort.toString());
    }
  }

  /*
   * Send the command to switch from SWD to jtag.
   */
  private void jtagToSwd() throws DeviceError {
    byte[] data;
    data = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff};
    this.protocol.swjSequence(data);

    data = new byte[]{(byte) 0x9e, (byte) 0xe7};
    this.protocol.swjSequence(data);

    data = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff};
    this.protocol.swjSequence(data);

    data = new byte[]{(byte) 0x00};
    this.protocol.swjSequence(data);
  }

  public void setAttributes() {
    // TODO
    // Not implemented. We don't support WS.
  }

  public void setClock(int frequency) throws DeviceError {
    this.flush();
    this.protocol.setSWJClock(frequency);
    this.frequency = frequency;
  }

  /*
   * Initialize or reinitialize all the deferred transfer buffers
   * Calling this method will drop all pending transactions so use with care.
   */
  private void initDeferredBuffers() {
    // List of transfers that have been started, but not completed
    // (started by write_reg, read_reg, reg_write_repeat and reg_read_repeat)
    this.transferList = new LinkedList<Transfer>();
    // The current packet - this can contain multiple  different transfers
    this.crntCmd = new Command(this.packetSize);
    // Packets that have been sent but not read
    this.commandsToRead = new ArrayDeque();
    // Buffer for data returned for completed commands. This data will be added to transfers
    this.commandsResponseBuf = new byte[0];
  }

  /*
  * Reads and decodes a single packet
  * Reads a single packet from the device and  stores the data from it in the current Command object
   */
  private void readPacket() throws TransferError {
    // Grab command, send it and decode response
    Command command = (Command) this.commandsToRead.poll();
    byte[] decodedData;
    try {
      byte[] rawData = this.connectionInterface.read();
      decodedData = command.decodeData(rawData);
    } catch (Exception exception) {
      this.abortAllTransfers(exception);
      throw exception;
    }

    byte[] c = new byte[decodedData.length + this.commandsResponseBuf.length];
    System.arraycopy(decodedData, 0, c, 0, decodedData.length);
    System.arraycopy(this.commandsResponseBuf, 0, c, decodedData.length,
        this.commandsResponseBuf.length);

    // Attach data to transfers
    int pos = 0;
    while (true) {
      int sizeLeft = this.commandsResponseBuf.length - pos;
      if (sizeLeft == 0) {
        // If size left is 0 then the transfer list might be empty, so don't try to access element 0
        break;
      }
      Transfer transfer = this.transferList.get(0);
      int size = transfer.getDataSize();
      if (size > sizeLeft) {
        break;
      }

      this.transferList.poll();
      int arraySize = pos + size;
      byte[] data = new byte[arraySize];
      System.arraycopy(this.commandsResponseBuf, pos, data, 0, arraySize);
      pos = size;
      transfer.addResponse(data);
    }

    // Remove used data from _command_response_buf
    if (pos > 0) {
      int arraySize = this.commandsResponseBuf.length - pos;
      byte[] data = new byte[arraySize];
      System.arraycopy(this.commandsResponseBuf, pos, data, 0, arraySize);
      this.commandsResponseBuf = data;
    }
  }

  /*
   * Send a single packet to the interface
   * This function guarantees that the number of packets that are stored in daplink's buffer
   * (the number of packets written but not read) does not exceed the number supported by
   * the given device.
   */
  private void sendPacket() throws TransferError {
    Command command = this.crntCmd;
    if (command.getEmpty()) {
      return;
    }

    int maxPackets = this.connectionInterface.getPacketCount();
    if (this.commandsToRead.size() >= maxPackets) {
      this.readPacket();
    }
    byte[] data = command.encodeData();
    try {
      this.connectionInterface.write(data);
    } catch (Exception exception) {
      this.abortAllTransfers(exception);
      throw exception;
    }
    this.commandsToRead.add(command);
    this.crntCmd = new Command(this.packetSize);
  }

  /*
   * Abort any ongoing transfers and clear all buffers
   */
  private void abortAllTransfers(Exception exception) {
    int pendingReads = this.commandsToRead.size();
    // Invalidate transferList
    for (Transfer transfer : this.transferList) {
      transfer.addError(exception);
    }
    // Clear allDeferredBuffers
    this.initDeferredBuffers();

    // Finish all pending reads and ignore the data
    // Only do this if the error is a transfer error.
    // Otherwise this could cause another exception
    if (exception instanceof TransferError) {
      for (int i = 0; i < pendingReads; i++) {
        this.connectionInterface.read();
      }
    }
  }
}
