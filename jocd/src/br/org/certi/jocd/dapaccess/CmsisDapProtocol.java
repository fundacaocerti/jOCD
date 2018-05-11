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

import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;
import br.org.certi.jocd.dapaccess.dapexceptions.CommandError;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import java.util.EnumSet;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class implements the CMSIS-DAP wire protocol.
 */
public class CmsisDapProtocol {

  // Logging
  private final static String CLASS_NAME = CmsisDapProtocol.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private ConnectionInterface connectionInterface = null;

  public static enum CommandId {
    DAP_INFO((byte) 0x00),
    DAP_LED((byte) 0x01),
    DAP_CONNECT((byte) 0x02),
    DAP_DISCONNECT((byte) 0x03),
    DAP_TRANSFER_CONFIGURE((byte) 0x04),
    DAP_TRANSFER((byte) 0x05),
    DAP_TRANSFER_BLOCK((byte) 0x06),
    DAP_TRANSFER_ABORT((byte) 0x07),
    DAP_WRITE_ABORT((byte) 0x08),
    DAP_DELAY((byte) 0x09),
    DAP_RESET_TARGET((byte) 0x0a),
    DAP_SWJ_PINS((byte) 0x10),
    DAP_SWJ_CLOCK((byte) 0x11),
    DAP_SWJ_SEQUENCE((byte) 0x12),
    DAP_SWD_CONFIGURE((byte) 0x13),
    DAP_JTAG_SEQUENCE((byte) 0x14),
    DAP_JTAG_CONFIGURE((byte) 0x15),
    DAP_JTAG_IDCODE((byte) 0x16),
    DAP_VENDOR0((byte) 0x80);

    public final byte value;

    CommandId(byte id) {
      this.value = id;
    }

    public byte getValue() {
      return value;
    }
  }

  public static enum IdInfo {
    VENDOR_ID((byte) 0x01),
    PRODUCT_ID((byte) 0x02),
    SERIAL_NUMBER((byte) 0x03),
    CMSIS_DAP_FW_VERSION((byte) 0x04),
    TARGET_DEVICE_VENDOR((byte) 0x05),
    TARGET_DEVICE_NAME((byte) 0x06),
    CAPABILITIES((byte) 0xf0),
    SWO_BUFFER_SIZE((byte) 0xfd),
    PACKET_COUNT((byte) 0xfe),
    PACKET_SIZE((byte) 0xff);

    public final byte value;

    IdInfo(byte id) {
      this.value = id;
    }

    public byte getValue() {
      return value;
    }
  }

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

  // Responses to DAP_Transfer and DAP_TransferBlock
  public static final int DAP_TRANSFER_OK = 1;
  public static final int DAP_TRANSFER_WAIT = 2;
  public static final int DAP_TRANSFER_FAULT = 4;

  /*
   * Constructor.
   */
  public CmsisDapProtocol(ConnectionInterface connectionInterface) {
    super();
    this.connectionInterface = connectionInterface;
  }

  public Object dapInfo(IdInfo id) throws DeviceError, TimeoutException {
    byte[] cmd = new byte[2];
    cmd[0] = ((byte) CommandId.DAP_INFO.getValue());
    cmd[1] = (byte) id.getValue();
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();

    if (response[0] != CommandId.DAP_INFO.getValue()) {
      // Response is to a different command
      throw new DeviceError();
    }

    if (response[1] == 0) {
      return null;
    }

    EnumSet<IdInfo> intIdInfo = EnumSet
        .of(IdInfo.CAPABILITIES, IdInfo.SWO_BUFFER_SIZE,
            IdInfo.PACKET_COUNT, IdInfo.PACKET_SIZE);

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
  public Port connect() throws DeviceError, TimeoutException {
    return connect(Port.DEFAULT);
  }

  public Port connect(Port mode) throws DeviceError, TimeoutException {
    byte[] cmd = new byte[2];
    cmd[0] = CommandId.DAP_CONNECT.getValue();
    cmd[1] = mode.getValue();
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();
    if (response[0] != CommandId.DAP_CONNECT.getValue()) {
      // Response is to a different command.
      throw new DeviceError();
    }

    Port port = Port.fromId(response[1]);

    if (port == Port.DEFAULT) {
      // DAP connect failed.
      throw new CommandError();
    } else if (port == Port.SWD) {
      // DAP SWD MODE initialized.
      LOGGER.log(Level.FINE, "DAP SWD MODE initialized");
    } else if (port == Port.JTAG) {
      // DAP JTAG MODE initialized.
      LOGGER.log(Level.FINE, "DAP JTAG MODE initialized");
    } else {
      // Unexpected port.
      throw new CommandError();
    }

    return port;
  }

  public byte disconnect() throws DeviceError, TimeoutException {
    byte[] cmd = new byte[2];
    cmd[0] = CommandId.DAP_DISCONNECT.getValue();
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();
    if (response[0] != CommandId.DAP_DISCONNECT.getValue()) {
      // Response is to a different command.
      throw new DeviceError();
    }

    if (response[1] != DAP_OK) {
      // DAP Transfer Configure failed.
      throw new CommandError();
    }

    return response[1];
  }

  /*
   * Overload for transferConfigure(byte idleCycles, int waitRetry, int matchRetry) using default
   * values.
   */
  public byte transferConfigure() throws DeviceError, TimeoutException {
    return transferConfigure((byte) 0x00, 0x0050, 0x0000);
  }

  public byte transferConfigure(byte idleCycles, int waitRetry, int matchRetry)
      throws DeviceError, TimeoutException {
    byte[] cmd = new byte[6];
    cmd[0] = CommandId.DAP_TRANSFER_CONFIGURE.getValue();
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
    if (response[0] != CommandId.DAP_TRANSFER_CONFIGURE.getValue()) {
      // Response is to a different command.
      throw new DeviceError();
    }

    if (response[1] != DAP_OK) {
      // DAP Transfer Configure failed.
      throw new CommandError();
    }

    return response[1];
  }

  public byte setSWJClock() throws DeviceError, TimeoutException {
    return setSWJClock(DapAccessCmsisDap.FREQUENCY);
  }

  public byte setSWJClock(int clock) throws DeviceError, TimeoutException {
    byte[] cmd = new byte[5];
    cmd[0] = (byte) CommandId.DAP_SWJ_CLOCK.getValue();
    cmd[1] = (byte) (clock & 0xff);
    cmd[2] = (byte) ((clock >> 8) & 0xff);
    cmd[3] = (byte) ((clock >> 16) & 0xff);
    cmd[4] = (byte) ((clock >> 24) & 0xff);
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();
    if (response[0] != CommandId.DAP_SWJ_CLOCK.getValue()) {
      // Response is to a different command
      throw new DeviceError();
    }
    if (response[1] != DAP_OK) {
      // DAP SWJ Clock failed
      throw new CommandError();
    }

    return response[1];
  }

  /*
   * Overload for swdConfigure(byte conf) using default conf.
   */
  public byte swdConfigure() throws DeviceError, TimeoutException {
    return swdConfigure((byte) 0x00);
  }

  public byte swdConfigure(byte conf) throws DeviceError, TimeoutException {
    byte[] cmd = new byte[2];
    cmd[0] = CommandId.DAP_SWD_CONFIGURE.getValue();
    cmd[1] = conf;

    // Write the command.
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();
    if (response[0] != CommandId.DAP_SWD_CONFIGURE.getValue()) {
      // Response is to a different command.
      throw new DeviceError();
    }

    if (response[1] != DAP_OK) {
      // DAP Transfer Configure failed.
      throw new CommandError();
    }

    return response[1];
  }

  public byte swjSequence(byte[] data) throws DeviceError, TimeoutException {
    // Swj sequence will have the following structure:
    //     CMD  -  Bit Count - DATA....
    //   1 byte +   1 byte   + SIZEOF(DATA)
    byte[] cmd = new byte[1 + 1 + data.length];
    cmd[0] = CommandId.DAP_SWJ_SEQUENCE.getValue();
    // The second byte will carry the BIT count.
    cmd[1] = (byte) (data.length * 8);

    for (int i = 0; i < data.length; i++) {
      cmd[2 + i] = data[i];
    }

    // Write the command.
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();
    if (response[0] != CommandId.DAP_SWJ_SEQUENCE.getValue()) {
      // Response is to a different command.
      throw new DeviceError();
    }

    if (response[1] != DAP_OK) {
      // DAP Transfer Configure failed.
      throw new CommandError();
    }

    return response[1];
  }

  /*
   * Overload for jtagConfigure(byte irlen, byte devNum) using default devNum.
   */
  public byte[] jtagConfigure(byte irlen) throws DeviceError, TimeoutException {
    return jtagConfigure(irlen, (byte) 0x01);
  }

  public byte[] jtagConfigure(byte irlen, byte devNum) throws DeviceError, TimeoutException {
    byte[] cmd = new byte[3];
    cmd[0] = CommandId.DAP_JTAG_CONFIGURE.getValue();
    cmd[1] = devNum;
    cmd[2] = irlen;

    // Write the command.
    this.connectionInterface.write(cmd);

    byte[] response = this.connectionInterface.read();
    if (response[0] != CommandId.DAP_JTAG_CONFIGURE.getValue()) {
      // Response is to a different command.
      throw new DeviceError();
    }

    if (response[1] != DAP_OK) {
      // DAP Transfer Configure failed.
      throw new CommandError();
    }

    byte[] ret = new byte[response.length - 2];
    System.arraycopy(response, 2, ret, 0, ret.length);
    return ret;
  }
}
