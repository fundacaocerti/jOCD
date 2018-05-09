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
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.Port;
import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;
import br.org.certi.jocd.dapaccess.connectioninterface.UsbFactory;
import br.org.certi.jocd.dapaccess.dapexceptions.CommandError;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class DapAccessCmsisDap {

  // Logging
  private static final String TAG = "DapAccessCmsisDap";

  private ConnectionInterface connectionInterface = null;
  private boolean deferredTransfer = false;
  private int packetCount = 0;
  private String uniqueId;
  private int frequency = 1000000; // 1MHz default clock
  private int dapPort = 0;
  // TODO _transfer_list
  // TODO _crnt_cmd
  private int packetSize = 0;
  // TODO commandsToRead
  // TODO commandResponseBuf
  private CmsisDapProtocol protocol;
  private ArrayDeque transferList;
  private Command crntCmd;
  private ArrayDeque commandsToRead;
  private List<Byte> commandsResponseBuf;

  /*
   * Constructor.
   */
  public DapAccessCmsisDap(String uniqueId) {
    super();
    this.uniqueId = uniqueId;
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
      this.packetCount = ((Byte) this.protocol.dapInfo(CmsisDapCore.IdInfo.PACKET_COUNT))
          .intValue();
    }

    this.connectionInterface.setPacketCount(this.packetCount);
    this.packetSize = (Integer) this.protocol.dapInfo(CmsisDapCore.IdInfo.PACKET_SIZE);
    this.connectionInterface.setPacketSize(this.packetSize);

    this.initDeferredBuffers();
  }

  private String getUniqueId(ConnectionInterface device) {
    return device.getSerialNumber();
  }

  /*
   * Initialize or reinitialize all the deferred transfer buffers
   * Calling this method will drop all pending transactions so use with care.
   */
  private void initDeferredBuffers() {
    // List of transfers that have been started, but not completed
    // (started by write_reg, read_reg, reg_write_repeat and reg_read_repeat)
    this.transferList = new ArrayDeque();
    // The current packet - this can contain multiple  different transfers
    this.crntCmd = new Command(this.packetSize);
    // Packets that have been sent but not read
    this.commandsToRead = new ArrayDeque();
    // Buffer for data returned for completed commands. This data will be added to transfers
    this.commandsResponseBuf = new ArrayList<Byte>();
  }

  public void close() {
    if (connectionInterface == null) {
      return;
    }

    flush();
    connectionInterface.close();
  }

  /*
   * Overload for connect(port), using default value: Port.Default.
   */
  public void connect() throws DeviceError, CommandError {
    connect(Port.DEFAULT);
  }

  public void connect(Port port) throws DeviceError, CommandError {
    Port actualPort = this.protocol.connect(port);

    // Set clock frequency.
    // TODO
    this.protocol.setSWJClock(this.frequency);

    // Configure transfer.
    this.protocol.transferConfigure();
  }

  private void flush() {
    // TODO
  }

  public void setAttributes() {
    // TODO
    // Not implemented. We don't support WS.
  }

  public String getUniqueId() {
    return uniqueId;
  }
}
