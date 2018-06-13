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
package br.org.certi.jocd.flash;

public class FlashPage {

  final long address;
  final long size;
  public byte[] data;
  final double eraseWeight;
  final double programWeight;
  Boolean erased = null;
  Boolean same = null;

  public long crc;

  // Number of bytes in a page to read to quickly determine if the page has the same data
  public static final int PAGE_ESTIMATE_SIZE = 32;
  public static final double PAGE_READ_WEIGHT = 0.3;

  // ~40KB/s, depends on clock speed, theoretical limit for HID is 56,000 B/s
  public static final int DATA_TRANSFER_RATE_B_PER_S = (40 * 1000);


  /*
   * Constructor
   */
  public FlashPage(long address, long size, byte data[], double eraseWeight, double programWeight) {
    this.address = address;
    this.size = size;
    this.data = data;
    this.eraseWeight = eraseWeight;
    this.programWeight = programWeight;
  }

  /*
   * Get time to program a page including the data transfer.
   */
  public double getProgramWeight() {
    return this.programWeight + ((double) data.length / (double) DATA_TRANSFER_RATE_B_PER_S);
  }

  /*
   * Get time to erase and program a page including data transfer time.
   */
  public double getEraseProgramWeight() {
    double test= this.eraseWeight + this.programWeight + ((double) data.length
        / (double) DATA_TRANSFER_RATE_B_PER_S);
    return test;
  }

  /*
   * Get time to verify a page.
   */
  public double getVerifyWeight() {
    return ((double) this.size / (double) DATA_TRANSFER_RATE_B_PER_S);
  }
}
