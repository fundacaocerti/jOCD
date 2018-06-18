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

package br.org.certi.jocdconnandroid;

import android.content.Context;
import br.org.certi.jocd.Jocd;
import br.org.certi.jocdconnandroid.connectioninterface.AndroidApplicationContext;
import br.org.certi.jocdconnandroid.connectioninterface.AndroidUsbDevice;

/**
 * JocdConnAndroid is a companion library for jOCD to provide access to USB devices via Android's
 * USB Host APIs.
 */
public class JocdConnAndroid {

  /**
   * Initializes the Android Application Context and the Connection Interface in jOCD. It must be executed before any other jOCD
   * library call.
   *
   * @param context the android application context
   */
  public static void init(Context context) {
    AndroidApplicationContext.getInstance().init(context);
    Jocd.connectionInterface = new AndroidUsbDevice();
  }

}
