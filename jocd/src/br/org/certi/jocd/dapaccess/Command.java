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

import android.util.Log;

/*
* A wrapper object representing a command send to the layer below (ex. USB).
* This class wraps the phyiscal commands DAP_Transfer and DAP_TransferBlock
* to provide a uniform way to build the command to most efficiently transfer
* the data supplied.  Register reads and writes individually or in blocks
* are added to a command object until it is full.  Once full, this class
* decides if it is more efficient to use DAP_Transfer or DAP_TransferBlock.
* The payload to send over the layer below is constructed with
* encode_data.  The response to the command is decoded with decode_data.
*/
public class Command {

  // Logging
  private static final String TAG = "Command";

  private int size;
  private int readCount = 0;
  private int writeCount = 0;
  private boolean blockAllowed = true;
  private int blockRequest;
  private byte[] data;
  private int dapIndex;
  private boolean dataEncoded = false;

  /*
   * Constructor.
   */
  public Command(int size) {
    super();
    this.size = size;
    Log.d(TAG, "New Command");
  }
}
