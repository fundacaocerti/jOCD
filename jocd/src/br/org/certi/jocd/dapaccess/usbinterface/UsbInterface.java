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
package br.org.certi.jocd.dapaccess.usbinterface;

import java.util.List;

public interface UsbInterface {

    // Logging
    static final String TAG = "UsbInterface";

    public List<UsbInterface> getAllConnectedDevices();

    public int getVendorId();
    public int getProductId();
    public String getDeviceName();
    public String getProductName();
    public String getManufacturerName();
    public String getSerialNumber();
    public void rxHandler();
    public void read();
    public void write();
    public void open();
    public void close();
}
