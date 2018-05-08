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

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import br.org.certi.jocd.dapaccess.connectioninterface.UsbFactory;
import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;

import static br.org.certi.jocd.dapaccess.connectioninterface.UsbFactory.connectionInterfaceEnum.androidUsbManager;

public class DapAccessCmsisDap {

    // Logging
    private static final String TAG = "DapAccessCmsisDap";

    private ConnectionInterface usbInterface = null;
    private boolean deferredTransfer = false;
    private int packetCount = 0;
    private String uniqueId;
    private int frequency =  1000000; // 1MHz default clock
    private int dapPort = 0;
    // TODO _transfer_list
    // TODO _crnt_cmd
    private int packetSize = 0;
    // TODO commandsToRead
    // TODO commandResponseBuf

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
        for (ConnectionInterface iface: allDevices) {
            // Get only CMSIS-DAP devices.
            if (!iface.getProductName().contains("CMSIS-DAP")) {
                continue;
            }

            try {
                String uniqueId = iface.getSerialNumber();
                DapAccessCmsisDap dapLink = new DapAccessCmsisDap(uniqueId);
                allDAPLinks.add(dapLink);
            }
            catch (Exception e) {
                Log.e(TAG, "Exception caught while trying to iterate over " +
                        "all connections interfaces.");
                return null;
            }
        }
        return allDAPLinks;
    }

    public void open() {
        // TODO
    }

    public void close() {
        if (usbInterface == null) return;

        flush();
        usbInterface.close();
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
