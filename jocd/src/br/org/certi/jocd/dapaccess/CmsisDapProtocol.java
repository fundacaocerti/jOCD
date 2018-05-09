/*
 * Copyright 2018 Fundação CERTI
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import android.util.Log;


import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;
import br.org.certi.jocd.dapaccess.dapexceptions.CommandError;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import java.util.EnumSet;

/*
 * This class implements the CMSIS-DAP wire protocol.
 */
public class CmsisDapProtocol {

  // Logging
  private static final String TAG = "CmsisDapProtocol";

  private ConnectionInterface connectionInterface = null;

  public static enum Port {
    DEFAULT((byte) 0x00),
    SWD((byte) 0x01),
    JTAG((byte) 0x02);

    public final byte value;

    Port(byte id) {
      this.value = id;
    }

    public byte getValue() {
      return value;
    }

    public static Port fromId(int id) {
      for (Port port : Port.values()) {
        if (port.getValue() == id) {
          return port;
        }
      }
      return null;
    }
  }

  private static final byte DAP_OK = 0x00;

  /*
   * Constructor.
   */
  public CmsisDapProtocol(ConnectionInterface connectionInterface) {
    super();
    this.connectionInterface = connectionInterface;
  }

  public Object dapInfo(CmsisDapCore.IdInfo id) throws DeviceError {
    byte[] cmd = new byte[2];
    cmd[0] = ((byte) CmsisDapCore.CommandId.DAP_INFO.getValue());
    cmd[1] = (byte) id.getValue();
    this.connectionInterface.write(cmd);
    byte[] response = this.connectionInterface.read();

    if (response[0] != CmsisDapCore.CommandId.DAP_INFO.getValue()) {
      // Response is to a different command
      throw new DeviceError();
    }

    if (response[1] == 0) {
      return null;
    }

    EnumSet<CmsisDapCore.IdInfo> intIdInfo = EnumSet
        .of(CmsisDapCore.IdInfo.CAPABILITIES, CmsisDapCore.IdInfo.SWO_BUFFER_SIZE,
            CmsisDapCore.IdInfo.PACKET_COUNT, CmsisDapCore.IdInfo.PACKET_SIZE);

    // Integer values
    if (intIdInfo.contains(id)) {
      if (response[1] == 1) {
        return response[2];
      }
      if (response[1] == 2) {
        return (response[3] << 8) | response[2];
      }
    }

    // String values. They are sent as C strings with a terminating null char, so we strip it out.
    int arraySize = response[1];
    // The data starts at 2 and the last position is array size minus 1
    byte lastCharacter = response[2 + arraySize - 1];
    if (lastCharacter == '\0') {
      arraySize -= 1;
    }
    byte[] data = new byte[arraySize];
    System.arraycopy(response, 2, data, 0, arraySize);
    String dataString = new String(data);
    return dataString;
  }

  /*
   * Overload for connect(mode), using default value: Port.Default.
   */
  public Port connect() throws DeviceError, CommandError {
    return connect(Port.DEFAULT);
  }

  public Port connect(Port mode) throws DeviceError, CommandError {
    byte[] cmd = new byte[2];
    cmd[0] = CmsisDapCore.CommandId.DAP_CONNECT.getValue();
    cmd[1] = mode.getValue();
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();
    if (response[0] != CmsisDapCore.CommandId.DAP_CONNECT.getValue()) {
      // Response is to a different command.
      throw new DeviceError();
    }

    Port port = Port.fromId(response[1]);

    if (port == Port.DEFAULT) {
      // DAP connect failed.
      throw new CommandError();
    } else if (port == Port.SWD) {
      // DAP SWD MODE initialized.
      Log.d(TAG, "DAP SWD MODE initialized");
    } else if (port == Port.JTAG) {
      // DAP JTAG MODE initialized.
      Log.d(TAG, "DAP JTAG MODE initialized");
    } else {
      // Unexpected port.
      throw new CommandError();
    }

    return port;
  }

  /*
   * Overload for transferConfigure(byte idleCycles, int waitRetry, int matchRetry) using default
   * values.
   */
  public byte transferConfigure() throws DeviceError, CommandError {
    return transferConfigure((byte) 0x00, 0x0050, 0x0000);
  }

  public byte transferConfigure(byte idleCycles, int waitRetry, int matchRetry)
      throws DeviceError, CommandError {
    byte[] cmd = new byte[6];
    cmd[0] = CmsisDapCore.CommandId.DAP_TRANSFER_CONFIGURE.getValue();
    cmd[1] = idleCycles;

    // Split waitRetry (16 bit) in 2 bytes.
    byte lowWaitRetry = (byte) (waitRetry & 0x00FF);
    byte highWaitRetry = (byte) ((waitRetry >> 8) & 0x00FF);
    cmd[2] = lowWaitRetry;
    cmd[3] = highWaitRetry;

    // Split matchRetry (16 bit) in 2 bytes.
    byte lowMatchRetry = (byte) (waitRetry & 0x00FF);
    byte highMatchRetry = (byte) ((waitRetry >> 8) & 0x00FF);
    cmd[4] = lowMatchRetry;
    cmd[5] = highMatchRetry;

    // Write the command.
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();
    if (response[0] != CmsisDapCore.CommandId.DAP_TRANSFER_CONFIGURE.getValue()) {
      // Response is to a different command.
      throw new DeviceError();
    }

    if (response[1] != DAP_OK) {
      // DAP Transfer Configure failed.
      throw new CommandError();
    }

    return response[1];
  }

  public byte setSWJClock(int freq) {
    // TODO
    return 0x00;
  }
}
