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

import android.content.Context;
import android.util.Log;

public class UsbFactory {

    // Logging
    private static final String TAG = "UsbFactory";

    public static enum connectionInterfaceEnum {
        androidUsbManager
    }

    public static ConnectionInterface getUSBInterface(Context context,
                                                      connectionInterfaceEnum intfEnum) {
        switch (intfEnum) {
            case androidUsbManager: return new AndroidUsbDevice(context);
            default:
                Log.e(TAG,"Default case on switch ConnectionIntf. "
                        + "Unexpected interface: " + intfEnum.toString());
                return null;
        }
    }
}
