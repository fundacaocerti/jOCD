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

/*
* A wrapper object representing a command invoked by the layer above.
* The transfer class contains a logical register read or a block of reads to the same register.
*/
public class Transfer {

  private Exception error;
  private int sizeBytes = 0;
  private int[] result;
  private DapAccessCmsisDap dapLink;
  private byte dapIndex;
  private int transferCount;
  private byte transferRequest;
  private long[] transferData;

  /*
   * Constructor.
   */
  public Transfer(DapAccessCmsisDap dapLink, byte dapIndex, int transferCount,
      byte transferRequest, long[] transferData) {
    // Writes should not need a transfer object since they don't have any response data
    assert (transferRequest & DapAccessCmsisDap.READ) != 0;

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
  public void addResponse(byte[] data) {
    assert data.length == this.sizeBytes;
    int resultSize = this.sizeBytes / 4;
    int[] result = new int[resultSize];
    int count = 0;
    for (int i = 0; i < this.sizeBytes; i += 4) {
      int word = (((data[0 + i] << 0) | (data[1 + i] << 8) | (data[2 + i] << 16) | (
          data[3 + i] << 24)));
      result[count] = word;
      count++;
    }
    this.result = result;
  }

  /*
   * Attach an exception to this transfer rather than data.
   */
  public void addError(Exception error) {
    assert error instanceof Error;
    this.error = error;
  }

  /*
   * Get the result of this transfer.
   */
  public int[] getResult() throws Exception {
    while (this.result == null) {
      if (this.dapLink.getCommandsToRead().size() > 0) {
        this.dapLink.readPacket();
      } else {
        assert !this.dapLink.getCrntCmd().getEmpty();
        this.dapLink.flush();
      }
    }

    if (this.error != null) {
      throw this.error;
    }

    assert this.result != null;
    return this.result;
  }
}
