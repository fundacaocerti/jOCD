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
    DAP_INFO(0x00),
    DAP_LED(0x01),
    DAP_CONNECT(0x02),
    DAP_DISCONNECT(0x03),
    DAP_TRANSFER_CONFIGURE(0x04),
    DAP_TRANSFER(0x05),
    DAP_TRANSFER_BLOCK(0x06),
    DAP_TRANSFER_ABORT(0x07),
    DAP_WRITE_ABORT(0x08),
    DAP_DELAY(0x09),
    DAP_RESET_TARGET(0x0a),
    DAP_SWJ_PINS(0x10),
    DAP_SWJ_CLOCK(0x11),
    DAP_SWJ_SEQUENCE(0x12),
    DAP_SWD_CONFIGURE(0x13),
    DAP_JTAG_SEQUENCE(0x14),
    DAP_JTAG_CONFIGURE(0x15),
    DAP_JTAG_IDCODE(0x16),
    DAP_VENDOR0(0x80);

    public final int value;

    CommandId(int id) {
      this.value = id;
    }

    public int getValue() {
      return value;
    }
  }

  public static enum IdInfo {
    VENDOR_ID(0x01),
    PRODUCT_ID(0x02),
    SERIAL_NUMBER(0x03),
    CMSIS_DAP_FW_VERSION(0x04),
    TARGET_DEVICE_VENDOR(0x05),
    TARGET_DEVICE_NAME(0x06),
    CAPABILITIES(0xf0),
    SWO_BUFFER_SIZE(0xfd),
    PACKET_COUNT(0xfe),
    PACKET_SIZE(0xff);

    public final int value;

    IdInfo(int id) {
      this.value = id;
    }

    public int getValue() {
      return value;
    }
  }

}
