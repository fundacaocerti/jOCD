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

import br.org.certi.jocd.dapaccess.connectioninterface.android.AndroidUsbDevice;
import br.org.certi.jocd.util.Util;
import br.org.certi.jocd.util.Util.OperatingSystem;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsbFactory {

  // Logging
  private final static String CLASS_NAME = UsbFactory.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  public static enum connectionInterfaceEnum {
    androidUsbManager
  }

  public static ConnectionInterface getUSBInterface(connectionInterfaceEnum intfEnum) {
    if (intfEnum == null) {
      // Try to guess the right interface for this os.
      OperatingSystem os = Util.getOS();
      if (os != null) {
        switch (os) {
          case Android:
            intfEnum = connectionInterfaceEnum.androidUsbManager;
            break;

          default:
            throw new InternalError("Not implemented");
        }
      }
    }

    switch (intfEnum) {
      case androidUsbManager:
        return new AndroidUsbDevice();
      default:
        LOGGER.log(Level.SEVERE,
            "Default case on switch ConnectionIntf. " + "Unexpected interface: " + intfEnum
                .toString());
        return null;
    }
  }
}
