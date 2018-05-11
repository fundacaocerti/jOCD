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
package br.org.certi.jocd.dapaccess.dapexceptions;

public class InsufficientPermissions extends Error {

  // Logging
  private static final String TAG = "InsufficientPermissions";

  // Hold the UsbDevice from android.hardware.usb.UsbDevice.
  // To keep this library independent from Android, hold this as an object.
  public final Object usbDevice;

  public InsufficientPermissions(Object usbDevice) {
    this("Insufficient permissions to access device", usbDevice);
  }

  public InsufficientPermissions(String message, Object usbDevice) {
    super(message);
    this.usbDevice = usbDevice;
  }

}
