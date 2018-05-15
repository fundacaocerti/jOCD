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

import br.org.certi.jocd.dapaccess.CmsisDapProtocol.IdInfo;
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.Pins;
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.Port;
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.Reg;
import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;
import br.org.certi.jocd.dapaccess.connectioninterface.UsbFactory;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferError;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DapAccessCmsisDap {

  // Logging
  private final static String CLASS_NAME = DapAccessCmsisDap.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  //CMSIS-DAP values
  public static final byte AP_ACC = 1 << 0;
  public static final byte DP_ACC = 0 << 0;
  public static final byte READ = 1 << 1;
  public static final byte WRITE = 0 << 1;

  // Set to True to enable logging of packet filling logic.
  public static boolean LOG_PACKET_BUILDS = false;

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

  public ArrayDeque getCommandsToRead() {
    return commandsToRead;
  }

  public Command getCrntCmd() {
    return crntCmd;
  }

  /*
   * Get the connected USB devices
   */
  private static List<ConnectionInterface> getDevices() {
    if (DapSettings.useWs) {
      // Not implemented!
      LOGGER.log(Level.SEVERE, "Not implemented! Trying to use WS interface.");
      return null;
    }
    return UsbFactory.getUSBInterface(androidUsbManager).getAllConnectedDevices();
  }

  /*
   * Return an array of all mbed boards connected
   */
  public static List<DapAccessCmsisDap> getConnectedDevices() {

    // Store all the DAP links.
    List<DapAccessCmsisDap> allDAPLinks = new ArrayList<DapAccessCmsisDap>();

    // Get all the connected interfaces.
    List<ConnectionInterface> allDevices = getDevices();

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
        LOGGER.log(Level.SEVERE,
            "Exception caught while trying to iterate over " + "all connections interfaces.");
        return null;
      }
    }
    return allDAPLinks;
  }

  public void open() throws DeviceError, TimeoutException, InsufficientPermissions {
    if (connectionInterface == null) {
      List<ConnectionInterface> allDevices = this.getDevices();
      for (ConnectionInterface device : allDevices) {
        try {
          String uniqueId = getUniqueId(device);
          if (this.uniqueId.equals(uniqueId)) {
            // This assert could indicate that two boards had the same ID
            assert this.connectionInterface == null;
            this.connectionInterface = device;
          }
        } catch (Exception exception) {
          LOGGER.log(Level.SEVERE, "Failed to get unique id for open", exception);
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
      LOGGER.log(Level.FINE, "Limiting packet count to" + this.packetCount);
    } else {
      this.packetCount = ((Byte) this.protocol.dapInfo(IdInfo.PACKET_COUNT))
          .intValue();
    }

    this.connectionInterface.setPacketCount(this.packetCount);
    this.packetSize = (Integer) this.protocol.dapInfo(IdInfo.PACKET_SIZE);
    this.connectionInterface.setPacketSize(this.packetSize);

    this.initDeferredBuffers();
  }

  public void close() throws TransferError, TimeoutException {
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

  public void reset() throws InterruptedException, DeviceError, TimeoutException {
    this.flush();
    this.protocol.setSWJPins((byte) 0, Pins.nRESET.getValue());
    Thread.sleep(1000);
    this.protocol.setSWJPins((byte) 0x80, Pins.nRESET.getValue());
    Thread.sleep(1000);
  }

  public void assertReset(boolean asserted) throws DeviceError, TimeoutException {
    this.flush();
    if (asserted) {
      this.protocol.setSWJPins((byte) 0, Pins.nRESET.getValue());
    } else {
      this.protocol.setSWJPins((byte) 0x80, Pins.nRESET.getValue());
    }
  }

  public Port getSwjMode() {
    return this.dapPort;
  }

  public void flush() throws TransferError, TimeoutException {
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
  public void connect() throws DeviceError, TimeoutException {
    connect(Port.DEFAULT);
  }

  public void connect(Port port) throws DeviceError, TimeoutException {
    this.dapPort = this.protocol.connect(port);

    // Set clock frequency.
    this.protocol.setSWJClock(this.frequency);

    // Configure transfer.
    this.protocol.transferConfigure();
  }

  public void disconnect() throws DeviceError, TimeoutException {
    this.flush();
    this.protocol.disconnect();
  }

  public void swjSequence() throws DeviceError, TimeoutException {
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
      LOGGER.log(Level.SEVERE, "Unexpected DAP port: " + this.dapPort.toString());
    }
  }

  public void writeReg(long regId, long value) throws TransferError, TimeoutException {
    this.writeReg(regId, value, (byte) 0);
  }

  public void writeReg(long regId, long value, byte dapIndex)
      throws TransferError, TimeoutException {
    assert Reg.containsReg(regId);

    byte request = WRITE;
    if (regId < 4) {
      request |= DP_ACC;
    } else {
      request |= AP_ACC;
    }
    request |= (regId % 4) * 4;
    long[] transferData = new long[1];
    transferData[0] = value;
    this.write(dapIndex, 1, request, transferData);
  }

  public byte readRegNow(long regId) throws Exception {
    return this.readRegNow(regId, (byte) 0);
  }

  public byte readRegNow(long regId, byte dapIndex)
      throws Exception {
    Transfer transfer = readReg(regId, dapIndex);
    return readRegAsync(transfer);
  }

  public Transfer readReg(long regId) throws TransferError, TimeoutException {
    return this.readReg(regId, (byte) 0);
  }

  public Transfer readReg(long regId, byte dapIndex)
      throws TransferError, TimeoutException {
    assert Reg.containsReg(regId);

    byte request = READ;
    if (regId < 4) {
      request |= DP_ACC;
    } else {
      request |= AP_ACC;
    }
    request |= (regId % 4) << 2;
    Transfer transfer = this.write(dapIndex, 1, request, null);
    assert transfer != null;
    return transfer;
  }

  public byte readRegAsync(Transfer transfer) throws Exception {
    byte[] res = transfer.getResult();
    assert res.length == 1;
    return res[0];
  }

  /*
   * Send the command to switch from SWD to jtag.
   */
  private void jtagToSwd() throws DeviceError, TimeoutException {
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

  public void setClock(int frequency) throws DeviceError, TimeoutException {
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
  public void readPacket() throws TransferError, TimeoutException {
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
  private void sendPacket() throws TransferError, TimeoutException {
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
   * Write one or more commands
   */
  private Transfer write(byte dapIndex, int transferCount, byte transferRequest,
      long[] transferData)
      throws TransferError, TimeoutException {
    assert dapIndex == 0;
    assert transferData == null || transferData.length != 0;

    // Create transfer and add to transfer list
    Transfer transfer = null;
    if ((transferRequest & READ) != 0) {
      transfer = new Transfer(this, dapIndex, transferCount, transferRequest, transferData);
      this.transferList.add(transfer);
    }

    // Build physical packet by adding it to command
    Command cmd = this.crntCmd;
    boolean isRead = (transferRequest & READ) != 0;
    int sizeToTransfer = transferCount;
    int transDataPos = 0;
    while (sizeToTransfer > 0) {
      // Get the size remaining in the current packet for the given request.
      int size = cmd.getRequestSpace(sizeToTransfer, transferRequest, dapIndex);

      // This request doesn't fit in the packet so send it.
      if (size == 0) {
        if (LOG_PACKET_BUILDS) {
          LOGGER.log(Level.FINE, "write: send packet [size==0]");
        }
        this.sendPacket();
        cmd = this.crntCmd;
        continue;
      }

      byte[] data;
      // Add request to packet.
      if (transferData == null) {
        data = null;
      } else {
        data = new byte[transDataPos + size];
        System.arraycopy(transferData, 2, data, 0, data.length);
      }
      cmd.add(size, transferRequest, data, dapIndex);
      sizeToTransfer -= size;
      transDataPos += size;

      // Packet has been filled so send it
      if (cmd.getFull()) {
        if (LOG_PACKET_BUILDS) {
          LOGGER.log(Level.FINE, "write: send packet [full]");
        }
        this.sendPacket();
        cmd = this.crntCmd;
      }
    }

    if (!this.deferredTransfer) {
      this.flush();
    }

    return transfer;
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
        try {
          this.connectionInterface.read();
        } catch (TimeoutException e) {
          // Couldn't read. Keep on loop to clean the "pendingReads".
        }
      }
    }
  }
}
