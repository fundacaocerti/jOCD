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

import br.org.certi.jocd.tools.ProgressUpdateInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlashBuilder {

  // Logging
  private final static String CLASS_NAME = FlashBuilder.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  final Flash flash;
  final long flashStart;

  boolean enableDoubleBuffering;

  // List of flash operations.
  List<FlashOperation> flashOperations = new ArrayList<FlashOperation>();

  List<FlashPage> pageList = new ArrayList<FlashPage>();

  // TODO
  public FlashBuilder(Flash flash, long baseAddress) {
    this.flash = flash;
    this.flashStart = baseAddress;

    this.enableDoubleBuffering = true;

    // TODO
  }

  /*
   * Add a block of data to be programmed
   * Note - programming does not start until the method program is called.
   */
  public void addData(long address, byte[] data) {
    // Protection.
    if (address < this.flashStart) {
      LOGGER.log(Level.SEVERE,
          "Invalid flash address " + String.format("%08X", address) + " is before flash start "
              + String.format("%08X", this, flashStart));
      return;
    }

    // Add operation to list.
    flashOperations.add(new FlashOperation(address, data));

    // Keep list of operations sorted.
    Collections.sort(flashOperations);

    // Verify this does not overlap.
    FlashOperation prevOp = null;
    for (FlashOperation op : flashOperations) {
      if (prevOp != null) {
        if ((prevOp.address + prevOp.data.length) > op.address) {
          LOGGER.log(Level.SEVERE,
              "Error adding data - Data at " + String.format("%08X", prevOp.address) + ".." + String
                  .format("%08X", prevOp.address + prevOp.data.length) + " overlaps with " + String
                  .format("%08X", op.address) + ".." + String
                  .format("%08X", op.address + op.data.length));
          return;
        }
      }
      prevOp = op;
    }
  }

  /*
   * Determine fastest method of flashing and then run flash programming.
   * Data must have already been added with addData.
   */
  // TODO
  public ProgrammingInfo program(boolean chipErase, ProgressUpdateInterface progressUpdate,
      boolean smartFlash, boolean fastVerify) {

    // Assumptions
    // 1. Page erases must be on page boundaries ( page_erase_addr % page_size == 0 )
    // 2. Page erase can have a different size depending on location
    // 3. It is safe to program a page with less than a page of data

    // Examples
    // - lpc4330     -Non 0 base address
    // - nRF51       -UICR location far from flash (address 0x10001000)
    // - LPC1768     -Different sized pages

    long startTime = System.currentTimeMillis();

    // There must be at least 1 flash operation.
    if (flashOperations.size() == 0) {
      LOGGER.log(Level.WARNING, "No pages were programmed");
      return null;
      // TODO we should have a better way to advise UI.
    }

    // Convert the list of flash operations into flash pages.
    int programByteCount = 0;
    long flashAddress = flashOperations.get(0).address;
    PageInfo pageInfo = this.flash.getPageInfo(flashAddress);
    long pageAddress = flashAddress - (flashAddress % pageInfo.size);
    FlashPage currentPage = new FlashPage(pageAddress, pageInfo.size, new byte[]{},
        pageInfo.eraseWeight, pageInfo.programWeight);
    this.pageList.add(currentPage);

    int pos;
    for (FlashOperation op : flashOperations) {
      pos = 0;

      while (pos < op.data.length) {

        // Check if operation is in next page.
        flashAddress = op.address + pos;
        if (flashAddress >= (currentPage.address + currentPage.size)) {
          pageInfo = this.flash.getPageInfo(flashAddress);
          pageAddress = flashAddress - (flashAddress % pageInfo.size);
          currentPage = new FlashPage(pageAddress, pageInfo.size, new byte[]{},
              pageInfo.eraseWeight, pageInfo.programWeight);
          this.pageList.add(currentPage);
        }

        // Fill the page gap if there is one.
        long pageDataEnd = currentPage.address + currentPage.data.length;
        if (flashAddress != pageDataEnd) {
          byte[] oldData = this.flash.target
              .readBlockMemoryUnaligned8(pageDataEnd, flashAddress - pageDataEnd);
          // TODO - this method copies all the data, and might be optimized.
          currentPage.data = appendDataInArray(currentPage.data, oldData);
        }

        // Copy data to page and increment pos
        int spaceLeftInPage = pageInfo.size - currentPage.data.length;
        int spaceLeftInData = op.data.length - pos;
        int amount = (spaceLeftInPage < spaceLeftInData) ? spaceLeftInPage : spaceLeftInData;
        // TODO - this method copies all the data, and might be optimized.
        currentPage.data = appendDataInArray(currentPage.data, op.data, pos, amount);
        programByteCount += amount;
      }
    }

    // If smart flash was set to false then mark all pages
    // as requiring programming.
    if (!smartFlash) {

    }

    // TODO - continue this method.

    // TODO remove this dummy progress update.
    int progress;
    for (progress = 0; progress < 100; progress++) {
      progressUpdate.progressUpdateCallback(progress);
      try {
        Thread.sleep(100);
      } catch (InterruptedException exception) {
        LOGGER.log(Level.SEVERE, exception.toString());
      }
      progress += 50;
    }
    progressUpdate.progressUpdateCallback(progress);

    // TODO - Return the right ProgrammingInfo.
    return null;
  }

  /*
   * Extend an existing byte array with another array and return
   * this new concatenated array.
   *
   * As we can not expand the size of an array in Java, we need
   * to create a new one, and concatenate both arrays.
   */
  private byte[] appendDataInArray(byte[] a, byte[] b, int start, int end) {

    // Sanity check.
    int count = end - start;
    if (count < 0) {
      LOGGER.log(Level.SEVERE, "Internal error: end position is lower than start position at" +
          "appendDataInArray.");
      return null;
    }

    // Create a new array to extend its size.
    byte[] newArray = new byte[a.length + end - start];

    // Copy currentPage data to the new array.
    System.arraycopy(a, 0, newArray, 0, a.length);

    // Copy oldData to the new array.
    System.arraycopy(b, start, newArray, a.length, count);

    // Overwrite currentPage.data with the new array.
    return newArray;

  }

  /*
   * Extend an existing byte array with another array and return
   * this new concatenated array.
   *
   * As we can not expand the size of an array in Java, we need
   * to create a new one, and concatenate both arrays.
   */
  private byte[] appendDataInArray(byte[] a, byte[] b) {

    // Create a new array to extend its size.
    byte[] newArray = new byte[a.length + b.length];

    // Copy currentPage data to the new array.
    System.arraycopy(a, 0, newArray, 0, a.length);

    // Copy oldData to the new array.
    System.arraycopy(b, 0, newArray, a.length, b.length);

    // Overwrite currentPage.data with the new array.
    return newArray;

  }

  private class FlashOperation implements Comparable<FlashOperation> {

    public final long address;
    final byte[] data;

    public FlashOperation(long address, byte[] data) {
      this.address = address;
      this.data = data;
    }

    @Override
    public int compareTo(FlashOperation f) {

      if (address > f.address) {
        return 1;
      } else if (address < f.address) {
        return -1;
      } else {
        return 0;
      }

    }
  }
}
