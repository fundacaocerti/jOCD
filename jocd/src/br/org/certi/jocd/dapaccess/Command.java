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
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.CommandId;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferError;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferFaultError;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferTimeoutError;
import java.util.List;

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
  private Byte blockRequest;
  private List<DataTuple> data;
  private byte dapIndex;
  private boolean dataEncoded = false;

  /*
   * Constructor.
   */
  public Command(int size) {
    super();
    this.size = size;
    Log.d(TAG, "New Command");
  }

  /*
 * Return true if no transfers have been added to this packet
 */
  public boolean getEmpty() {
    return this.data == null || this.data.size() == 0;
  }

  /*
   * Encode this command into a byte array that can be sent
   * The data returned by this function is a bytearray in
   * the format that of a DAP_Transfer CMSIS-DAP command.
   */
  public byte[] encodeTransferData() {
    assert this.getEmpty() == false;
    byte[] buf = new byte[this.size];
    int transferCount = this.readCount + this.writeCount;
    int pos = 0;
    buf[pos] = CommandId.DAP_TRANSFER.getValue();
    pos += 1;
    buf[pos] = this.dapIndex;
    pos += 1;
    buf[pos] = (byte) transferCount;
    pos += 1;
    for (DataTuple dt : this.data) {
      int count = dt.getCount();
      byte request = dt.getRequest();
      byte[] writeList = dt.getData();
      assert writeList == null || writeList.length <= count;
      int writePos = 0;
      for (int i = 0; i <= count; i++) {
        buf[pos] = (byte) request;
        pos += 1;
        if ((request & DapAccessCmsisDap.READ) > 0) {
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 0)) & 0xff);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 1)) & 0xff);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 2)) & 0xff);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 3)) & 0xff);
          pos += 1;
          writePos += 1;
        }
      }
    }
    return buf;
  }

  /*
   * Take a byte array and extract the data from it
   * Decode the response returned by a DAP_Transfer CMSIS-DAP command
   * and return it as an array of bytes.
   */
  private byte[] decodeTransferData(byte[] data) throws TransferError {
    assert this.getEmpty() == false;
    if (data[0] != CommandId.DAP_TRANSFER.getValue()) {
      throw new IllegalArgumentException("DAP_TRANSFER response error");
    }

    if (data[2] != CmsisDapProtocol.DAP_TRANSFER_OK) {
      if (data[2] == CmsisDapProtocol.DAP_TRANSFER_FAULT) {
        throw new TransferFaultError();
      } else if (data[2] == CmsisDapProtocol.DAP_TRANSFER_WAIT) {
        throw new TransferTimeoutError();
      }
      throw new TransferError();
    }

    // Check for count mismatch after checking for DAP_TRANSFER_FAULT
    // This allows TransferFaultError or TransferTimeoutError to get
    // thrown instead of TransferFaultError
    if (data[1] != this.readCount + this.writeCount) {
      throw new TransferError();
    }

    int arraySize = 4 * this.readCount;
    byte[] decodedData = new byte[arraySize];
    System.arraycopy(data, 3, decodedData, 0, arraySize);
    return decodedData;
  }

  /*
   * Encode this command into a byte array that can be sent
   * The data returned by this function is a bytearray in
   * the format that of a DAP_TransferBlock CMSIS-DAP command.
   */
  public byte[] encodeTransferBlockData() {
    assert this.getEmpty() == false;
    byte[] buf = new byte[this.size];
    int transferCount = this.readCount + this.writeCount;
    assert !(this.readCount != 0 && this.writeCount != 0);
    assert this.blockRequest != null;
    int pos = 0;
    buf[pos] = CommandId.DAP_TRANSFER_BLOCK.getValue();
    pos += 1;
    buf[pos] = this.dapIndex;
    pos += 1;
    buf[pos] = (byte) (transferCount & 0xff);
    pos += 1;
    buf[pos] = (byte) ((transferCount >> 8) & 0xff);
    pos += 1;
    buf[pos] = this.blockRequest;
    pos += 1;
    for (DataTuple dt : this.data) {
      int count = dt.getCount();
      int request = dt.getRequest();
      byte[] writeList = dt.getData();
      assert writeList == null || writeList.length <= count;
      assert request == this.blockRequest;
      int writePos = 0;
      if ((request & DapAccessCmsisDap.READ) > 0) {
        for (int i = 0; i <= count; i++) {
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 0)) & 0xff);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 1)) & 0xff);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 2)) & 0xff);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 3)) & 0xff);
          pos += 1;
          writePos += 1;
        }
      }
    }
    return buf;
  }

  /*
   * Take a byte array and extract the data from it
   * Decode the response returned by a DAP_TransferBlock
   * CMSIS-DAP command and return it as an array of bytes.
   */
  private byte[] decodeTransferBlockData(byte[] data) throws TransferError {
    assert this.getEmpty() == false;
    if (data[0] != CommandId.DAP_TRANSFER_BLOCK.getValue()) {
      throw new IllegalArgumentException("DAP_TRANSFER_BLOCK response error");
    }

    if (data[3] != CmsisDapProtocol.DAP_TRANSFER_OK) {
      if (data[3] == CmsisDapProtocol.DAP_TRANSFER_FAULT) {
        throw new TransferFaultError();
      } else if (data[3] == CmsisDapProtocol.DAP_TRANSFER_WAIT) {
        throw new TransferTimeoutError();
      }
      throw new TransferError();
    }

    // Check for count mismatch after checking for DAP_TRANSFER_FAULT
    // This allows TransferFaultError or TransferTimeoutError to get
    // thrown instead of TransferFaultError
    int transferCount = data[1] | (data[2] << 8);
    if (transferCount != this.readCount + this.writeCount) {
      throw new TransferError();
    }

    int arraySize = 4 * this.readCount;
    byte[] decodedData = new byte[arraySize];
    System.arraycopy(data, 4, decodedData, 0, arraySize);
    return decodedData;
  }

  /*
   * Encode this command into a byte array that can be sent
   * The actual command this is encoded into depends on the data that was added.
   */
  public byte[] encodeData() {
    assert this.getEmpty() == false;
    this.dataEncoded = true;
    byte[] data;
    if (this.blockAllowed) {
      data = this.encodeTransferBlockData();
    } else {
      data = this.encodeTransferData();
    }
    return data;
  }

  /*
   * Decode the response data
   */
  public byte[] decodeData(byte[] data) throws TransferError {
    assert this.getEmpty() == false;
    assert this.dataEncoded == true;
    if (this.blockAllowed) {
      data = this.decodeTransferBlockData(data);
    } else {
      data = this.decodeTransferData(data);
    }
    return data;
  }
}
