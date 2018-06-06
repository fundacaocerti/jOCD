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

import br.org.certi.jocd.dapaccess.CmsisDapProtocol.CommandId;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferError;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferFaultError;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferTimeoutError;
import br.org.certi.jocd.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  private final static String CLASS_NAME = Command.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private int size;
  private int readCount = 0;
  private int writeCount = 0;
  private boolean blockAllowed = true;
  private Byte blockRequest;
  private List<DataTuple> data = new ArrayList<DataTuple>();
  private Byte dapIndex;
  private boolean dataEncoded = false;

  /*
   * Constructor.
   */
  public Command(int size) {
    super();
    this.size = size;
    LOGGER.log(Level.FINE, "New Command");
  }

  /*
   * Return the number of words free in the transmit packet
   */
  public int getFreeWords(boolean blockAllowed, boolean isRead) {
    if (blockAllowed) {
      // DAP_TransferBlock request packet:
      //   BYTE | BYTE *****| SHORT**********| BYTE *************| WORD *********|
      // > 0x06 | DAP Index | Transfer Count | Transfer Request  | Transfer Data |
      //  ******|***********|****************|*******************|+++++++++++++++|
      int send = this.size - 5 - 4 * this.writeCount;

      // DAP_TransferBlock response packet:
      //   BYTE | SHORT *********| BYTE *************| WORD *********|
      // < 0x06 | Transfer Count | Transfer Response | Transfer Data |
      //  ******|****************|*******************|+++++++++++++++|
      int recv = this.size - 4 - 4 * this.readCount;

      if (isRead) {
        return recv / 4;
      } else {
        return send / 4;
      }
    } else {
      // DAP_Transfer request packet:
      //   BYTE | BYTE *****| BYTE **********| BYTE *************| WORD *********|
      // > 0x05 | DAP Index | Transfer Count | Transfer Request  | Transfer Data |
      //  ******|***********|****************|+++++++++++++++++++++++++++++++++++|
      int send = this.size - 3 - 1 * this.readCount - 5 * this.writeCount;

      // DAP_Transfer response packet:
      //   BYTE | BYTE **********| BYTE *************| WORD *********|
      // < 0x05 | Transfer Count | Transfer Response | Transfer Data |
      //  ******|****************|*******************|+++++++++++++++|
      int recv = this.size - 3 - 4 * this.readCount;

      if (isRead) {
        // 1 request byte in request packet, 4 data bytes in response packet
        return Math.min(send, recv / 4);
      } else {
        // 1 request byte + 4 data bytes
        return send / 5;
      }
    }
  }

  public int getRequestSpace(int count, byte request, Byte dapIndex) throws Error {
    // Assert this.dataEncoded == false.
    if (this.dataEncoded) {
      throw new Error("getRequestSpace: Unexpected dataEncoded value ( " + dataEncoded + ")");
    }

    // Must create another command if the dap index is different.
    if (this.dapIndex != null && !this.dapIndex.equals(dapIndex)) {
      return 0;
    }

    // Block transfers must use the same request.
    boolean blockAllowed = this.blockAllowed;
    if (this.blockRequest != null && request != this.blockRequest.byteValue()) {
      blockAllowed = false;
    }

    // Compute the portion of the request that will fit in this packet.
    boolean isRead = (request & DapAccessCmsisDap.READ) != 0;
    int free = this.getFreeWords(blockAllowed, isRead);
    int size = Math.min(count, free);

    // Non-block transfers only have 1 byte for request count.
    if (!blockAllowed) {
      int maxCount = this.writeCount + this.readCount + size;
      int delta = maxCount - 255;
      size = Math.min(size - delta, size);
      LOGGER.log(Level.FINE, String.format(
          "get_request_space(%d, %02x:%s)[wc=%d, rc=%d, ba=%b->%b] -> (sz=%d, free=%d, delta=%d)",
          count, request, isRead ? 'r' : 'w', this.writeCount, this.readCount, this.blockAllowed,
          blockAllowed, size, free, delta));
    } else {
      LOGGER.log(Level.FINE, String
          .format("get_request_space(%d, %02x:%s)[wc=%d, rc=%d, ba=%b->%b] -> (sz=%d, free=%d)",
              count, request, isRead ? 'r' : 'w', this.writeCount, this.readCount,
              this.blockAllowed, blockAllowed, size, free));
    }

    // We can get a negative free count if the packet already contains more data than can be
    // sent by a DAP_Transfer command, but the new request forces DAP_Transfer. In this case,
    // just return 0 to force the DAP_Transfer_Block to be sent.
    return Math.max(size, 0);
  }

  public boolean getFull() {
    return (this.getFreeWords(this.blockAllowed, true) == 0) || (
        this.getFreeWords(this.blockAllowed, false) == 0);
  }

  /*
   * Return true if no transfers have been added to this packet
   */
  public boolean getEmpty() {
    return this.data == null || this.data.size() == 0;
  }

  /*
   * Add a single or block register transfer operation to this command
   */
  public void add(int count, byte request, long[] words, Byte dapIndex) throws Error {
    // Assert this.dataEncoded == false.
    if (this.dataEncoded) {
      throw new Error("add: Unexpected dataEncoded value ( " + dataEncoded + ")");
    }

    if (this.dapIndex == null) {
      this.dapIndex = dapIndex;
    }

    // Assert this.dapIndex == dapIndex.
    if (!this.dapIndex.equals(dapIndex)) {
      throw new Error("add: Unexpected dapIndex value ( " + dapIndex + ")");
    }

    if (this.blockRequest == null) {
      this.blockRequest = request;
    } else if (request != this.blockRequest.byteValue()) {
      this.blockAllowed = false;
    }

    // Assert !this.blockAllowed || this.blockRequest == request.
    if (this.blockAllowed && this.blockRequest.byteValue() != request) {
      throw new Error(
          "add: Unexpected blockAllowed(" + blockAllowed + ") - blockRequest(" + blockRequest
              + "), request: (" + request + ")");
    }

    if ((request & DapAccessCmsisDap.READ) != 0) {
      this.readCount += count;
    } else {
      this.writeCount += count;
    }

    this.data.add(new DataTuple(count, request, words));

    LOGGER.log(Level.FINE, String
        .format("add(%d, %02x:%s) -> [wc=%d, rc=%d, ba=%b]", count, request,
            ((request & DapAccessCmsisDap.READ) != 0) ? 'r' : 'w', this.writeCount, this.readCount,
            this.blockAllowed));
  }

  /*
   * Encode this command into a byte array that can be sent
   * The data returned by this function is a bytearray in
   * the format that of a DAP_Transfer CMSIS-DAP command.
   */
  public byte[] encodeTransferData() throws Error {
    // Assert this.getEmpty() == false.
    if (this.getEmpty()) {
      throw new Error("encodeTransferData: Unexpected getEmpty() value ( " + getEmpty() + ")");
    }

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
      long[] writeList = dt.getData();

      // Assert writeList == null || writeList.length <= count.
      if (writeList != null && writeList.length > count) {
        throw new Error(
            "encodeTransferData: missing writeList. writeList isn't null and length > count.");
      }

      int writePos = 0;
      for (int i = 0; i < count; i++) {
        buf[pos] = (byte) request;
        pos += 1;
        if ((request & DapAccessCmsisDap.READ) == 0) {
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 0)) & 0xFF);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 1)) & 0xFF);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 2)) & 0xFF);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 3)) & 0xFF);
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
  private byte[] decodeTransferData(byte[] data) throws TransferError, Error {
    // Assert this.getEmpty() == false.
    if (this.getEmpty()) {
      throw new Error("decodeTransferData: Unexpected getEmpty() value ( " + getEmpty() + ")");
    }

    if ((data[0] & 0xFF) != CommandId.DAP_TRANSFER.getValue()) {
      throw new IllegalArgumentException("DAP_TRANSFER response error");
    }

    if ((data[2] & 0xFF) != CmsisDapProtocol.DAP_TRANSFER_OK) {
      if ((data[2] & 0xFF) == CmsisDapProtocol.DAP_TRANSFER_FAULT) {
        throw new TransferFaultError();
      } else if ((data[2] & 0xFF) == CmsisDapProtocol.DAP_TRANSFER_WAIT) {
        throw new TransferTimeoutError();
      }
      throw new TransferError();
    }

    // Check for count mismatch after checking for DAP_TRANSFER_FAULT
    // This allows TransferFaultError or TransferTimeoutError to get
    // thrown instead of TransferFaultError
    if ((data[1] & 0xFF) != this.readCount + this.writeCount) {
      throw new TransferError();
    }

    return Util.getSubArray(data, 3, 3 + (4 * this.readCount));
  }

  /*
   * Encode this command into a byte array that can be sent
   * The data returned by this function is a bytearray in
   * the format that of a DAP_TransferBlock CMSIS-DAP command.
   */
  public byte[] encodeTransferBlockData() throws Error {
    // Assert this.getEmpty() == false.
    if (this.getEmpty()) {
      throw new Error("encodeTransferBlockData: Unexpected getEmpty() value ( " + getEmpty() + ")");
    }

    byte[] buf = new byte[this.size];
    int transferCount = this.readCount + this.writeCount;

    // Assert !(this.readCount != 0 && this.writeCount != 0).
    if (this.readCount != 0 && this.writeCount != 0) {
      throw new Error("encodeTransferBlockData: readCount or writeCount != 0.");
    }

    // Assert this.blockRequest != null.
    if (this.blockRequest == null) {
      throw new Error("encodeTransferBlockData: blockRequest == null.");
    }

    int pos = 0;
    buf[pos] = CommandId.DAP_TRANSFER_BLOCK.getValue();
    pos += 1;
    buf[pos] = this.dapIndex;
    pos += 1;
    buf[pos] = (byte) (transferCount & 0xFF);
    pos += 1;
    buf[pos] = (byte) ((transferCount >> 8) & 0xFF);
    pos += 1;
    buf[pos] = this.blockRequest;
    pos += 1;
    for (DataTuple dt : this.data) {
      int count = dt.getCount();
      int request = dt.getRequest();
      long[] writeList = dt.getData();

      // Assert writeList == null || writeList.length <= count.
      if (!(writeList == null || writeList.length <= count)) {
        throw new Error(
            "encodeTransferBlockData: Expected: writeList == null || writeList.length <= count");
      }

      // Assert request == this.blockRequest.
      if (request != this.blockRequest.byteValue()) {
        throw new Error("encodeTransferBlockData: request == this.blockRequest");
      }

      int writePos = 0;
      if ((request & DapAccessCmsisDap.READ) == 0) {
        for (int i = 0; i < count; i++) {
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 0)) & 0xFF);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 1)) & 0xFF);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 2)) & 0xFF);
          pos += 1;
          buf[pos] = (byte) ((writeList[writePos] >> (8 * 3)) & 0xFF);
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
  private byte[] decodeTransferBlockData(byte[] data) throws TransferError, Error {
    // Assert this.getEmpty() == false.
    if (this.getEmpty()) {
      throw new Error("decodeTransferBlockData: Unexpected getEmpty() value ( " + getEmpty() + ")");
    }

    if ((data[0] & 0xFF) != CommandId.DAP_TRANSFER_BLOCK.getValue()) {
      throw new IllegalArgumentException("DAP_TRANSFER_BLOCK response error");
    }

    if ((data[3] & 0xFF) != CmsisDapProtocol.DAP_TRANSFER_OK) {
      if ((data[3] & 0xFF) == CmsisDapProtocol.DAP_TRANSFER_FAULT) {
        throw new TransferFaultError();
      } else if ((data[3] & 0xFF) == CmsisDapProtocol.DAP_TRANSFER_WAIT) {
        throw new TransferTimeoutError();
      }
      throw new TransferError();
    }

    // Check for count mismatch after checking for DAP_TRANSFER_FAULT
    // This allows TransferFaultError or TransferTimeoutError to get
    // thrown instead of TransferFaultError
    int transferCount = (data[1] & 0xFF) | ((data[2] & 0xFF) << 8);
    if (transferCount != this.readCount + this.writeCount) {
      throw new TransferError();
    }

    return Util.getSubArray(data, 4, 4 + (4 * this.readCount));
  }

  /*
   * Encode this command into a byte array that can be sent
   * The actual command this is encoded into depends on the data that was added.
   */
  public byte[] encodeData() throws Error {
    // Assert this.getEmpty() == false.
    if (this.getEmpty()) {
      throw new Error("encodeData: Unexpected getEmpty() value ( " + getEmpty() + ")");
    }

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
  public byte[] decodeData(byte[] data) throws TransferError, Error {
    // Assert this.getEmpty() == false.
    if (this.getEmpty()) {
      throw new Error("decodeData: Unexpected getEmpty() value ( " + getEmpty() + ")");
    }

    // Assert this.dataEncoded == true.
    if (this.dataEncoded == false) {
      throw new Error("decodeData: Unexpected dataEncoded value ( " + this.dataEncoded + ")");
    }

    if (this.blockAllowed) {
      data = this.decodeTransferBlockData(data);
    } else {
      data = this.decodeTransferData(data);
    }
    return data;
  }
}
