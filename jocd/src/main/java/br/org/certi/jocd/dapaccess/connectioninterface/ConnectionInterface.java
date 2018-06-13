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
package br.org.certi.jocd.dapaccess.connectioninterface;

import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import java.util.List;
import java.util.concurrent.TimeoutException;

public interface ConnectionInterface {

  public static final byte USB_CLASS_HID = (byte) 0x03;
  public static final byte USB_INPUT_ENDPOINT_ADDRESS = (byte) 0x80;

  public List<ConnectionInterface> getAllConnectedDevices();

  public int getVendorId();

  public int getProductId();

  public String getDeviceName();

  public String getProductName();

  public String getManufacturerName();

  public String getSerialNumber();

  public byte[] read() throws TimeoutException;

  public void write(byte[] data) throws Error;

  public void open() throws InsufficientPermissions;

  public void close();

  public void setPacketCount(int packetCount);

  public void setPacketSize(int packetSize);

  public int getPacketCount();
}
