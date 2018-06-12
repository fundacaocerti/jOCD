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
package br.org.certi.jocdconnusb4java;

import br.org.certi.jocd.Jocd;
import br.org.certi.jocdconnusb4java.connectioninterface.Usb4JavaDevice;

/**
 * JocdConnUsb4Java is a companion library for jOCD to provide access to USB devices via Usb4Java library
 */
public class JocdConnUsb4Java {

  /**
   * Initializes the Connection Interface in jOCD. It must be executed before any other jOCD
   * library call.
   */
  public static void init() {
    Jocd.connectionInterface = new Usb4JavaDevice();
  }

}
