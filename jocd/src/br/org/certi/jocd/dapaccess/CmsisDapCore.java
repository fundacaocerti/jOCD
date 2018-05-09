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

public class CmsisDapCore {

  // Logging
  private static final String TAG = "CmsisDapCore";

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
}
