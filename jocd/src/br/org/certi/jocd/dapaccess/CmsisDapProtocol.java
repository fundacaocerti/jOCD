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

import java.util.EnumSet;

import br.org.certi.jocd.dapaccess.connectioninterface.ConnectionInterface;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;

/*
* This class implements the CMSIS-DAP wire protocol.
*/
public class CmsisDapProtocol {

    // Logging
    private static final String TAG = "CmsisDapProtocol";

    private ConnectionInterface connectionInterface = null;

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

        EnumSet<CmsisDapCore.IdInfo> intIdInfo = EnumSet.of(CmsisDapCore.IdInfo.CAPABILITIES,
                CmsisDapCore.IdInfo.SWO_BUFFER_SIZE, CmsisDapCore.IdInfo.PACKET_COUNT,
                CmsisDapCore.IdInfo.PACKET_SIZE);

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
}
