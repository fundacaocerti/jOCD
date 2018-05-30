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

import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import java.util.concurrent.TimeoutException;

/*
 * A wrapper object representing a command invoked by the layer above.
 * The transfer class contains a logical register read or a block of reads to the same register.
 */
public class Transfer {

  private Error error;
  private int sizeBytes = 0;
  private long[] result;
  private DapAccessCmsisDap dapLink;
  private byte dapIndex;
  private int transferCount;
  private byte transferRequest;
  private long[] transferData;

  /*
   * Constructor.
   */
  public Transfer(DapAccessCmsisDap dapLink, byte dapIndex, int transferCount,
      byte transferRequest, long[] transferData) throws Error {
    // Writes should not need a transfer object since they don't have any response data
    // Assert (transferRequest & DapAccessCmsisDap.READ) != 0.
    if ((transferRequest & DapAccessCmsisDap.READ) == 0) {
      throw new Error("Transfer: (transferRequest & DapAccessCmsisDap.READ) == 0");
    }

    this.dapLink = dapLink;
    this.dapIndex = dapIndex;
    this.transferCount = transferCount;
    this.transferRequest = transferRequest;
    this.transferData = transferData;
    if ((transferRequest & DapAccessCmsisDap.READ) != 0) {
      this.sizeBytes = transferCount * 4;
    }
  }

  /*
   * Get the size in bytes of the return value of this transfer
   */
  public int getDataSize() {
    return this.sizeBytes;
  }

  /*
   * Add data read from the remote device to this object.
   * The size of data added must match exactly the size that get_data_size returns.
   */
  public void addResponse(byte[] data) throws Error {
    // Assert data.length == this.sizeBytes.
    if (data.length != this.sizeBytes) {
      throw new Error("addResponse: data.length != this.sizeBytes");
    }

    int resultSize = this.sizeBytes / 4;
    long[] result = new long[resultSize];
    int count = 0;
    for (int i = 0; i < this.sizeBytes; i += 4) {
      long word = 0;
      word |= 0x000000FFL & (data[0 + i] << 0);
      word |= 0x0000FF00L & (data[1 + i] << 8);
      word |= 0x00FF0000L & (data[2 + i] << 16);
      word |= 0xFF000000L & (data[3 + i] << 24);
      result[count] = word;
      count++;
    }
    this.result = result;
  }

  /*
   * Attach an exception to this transfer rather than data.
   */
  public void addError(Error error) throws Error {
    // Assert error instanceof Error.
    if (!(error instanceof Error)) {
      throw new Error("getResult: !(error instanceof Error)");
    }

    this.error = error;
  }

  /*
   * Get the result of this transfer.
   */
  public long[] getResult() throws TimeoutException, Error {
    while (this.result == null) {
      if (this.dapLink.getCommandsToRead().size() > 0) {
        this.dapLink.readPacket();
      } else {

        // Assert !this.dapLink.getCrntCmd().getEmpty().
        if (this.dapLink.getCrntCmd().getEmpty()) {
          throw new Error("getResult: this.dapLink.getCrntCmd().getEmpty()");
        }

        this.dapLink.flush();
      }
    }

    if (this.error != null) {
      throw this.error;
    }

    // Assert this.result != null.
    if (this.result == null) {
      throw new Error("getResult: result == null");
    }

    return this.result;
  }
}
