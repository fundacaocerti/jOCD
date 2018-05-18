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

import br.org.certi.jocd.core.MemoryRegion;
import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.core.Target.State;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.tools.ProgressUpdateInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class is responsible to flash a new binary in a target.
 */
public class Flash {

  // Logging
  private final static String CLASS_NAME = Flash.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  FlashAlgo flashAlgo;
  Target target;

  boolean flashAlgoDebug = false;

  Long beginStack;
  Long beginData;
  Long staticBase;
  int minProgramLength;
  List<Long> pageBuffers;

  Boolean doubleBufferSupported;

  /*
   * Constructor.
   */
  public Flash() {
  }

  /*
   * Must be called right after constructor.
   */
  public void setup(Target target, FlashAlgo flashAlgo) {
    this.target = target;
    this.flashAlgo = flashAlgo;
    this.flashAlgoDebug = false;

    if (flashAlgo != null) {
      this.beginStack = flashAlgo.beginStack;
      this.beginData = flashAlgo.beginData;
      this.staticBase = flashAlgo.staticBase;
      this.minProgramLength = flashAlgo.minProgramLength;

      // Check for double buffering support.
      if (flashAlgo.pageBuffers != null) {
        this.pageBuffers = flashAlgo.pageBuffers;
      } else {
        this.pageBuffers = new ArrayList<Long>();
        this.pageBuffers.add(this.beginData);
      }

      this.doubleBufferSupported = this.pageBuffers.size() > 1;
    } else {
      this.beginStack = null;
      this.beginData = null;
      this.staticBase = null;
    }

  }

  /*
   * Download the flash algorithm in RAM.
   */
  public void init() throws InterruptedException, TimeoutException, Error {
    this.target.halt();
    this.target.setTargetState(State.PROGRAM);
  }

  public long[] computeCrcs(List<Sectors> sectors) throws TimeoutException, Error {
    long[] words = new long[sectors.size()];
    int i = 0;

    // Convert address, size pairs into commands for the crc computation algorithm to preform.
    for (Sectors sector : sectors) {
      int sizeVal = msb(sector.size);
      long addressVal = sector.address / sector.size;

      // Size must be a power of 2.
      assert (1 << sizeVal) == sector.size;

      // Address must be a multiple of size.
      assert (sector.address % sector.size) == 0;

      int val = (int) ((sizeVal << 0) | (addressVal << 16));
      words[i] = val;
      i++;
    }

    // Update core register to execute the subroutine.
    this.target.writeBlockMemoryAligned32(this.beginData, words);

    callFunctionAndWait(this.flashAlgo.analyzerAddress, this.beginData, (long) words.length, null,
        null, null);

    // Read back the CRCs for each section.
    words = this.target.readBlockMemoryAligned32(this.beginData, words.length);
    return words;
  }

  /*
   * Erase all the flash.
   */
  public void eraseAll() {
    // Update core register to execute the eraseAll subroutine.
    int result = this.callFunctionAndWait(this.flashAlgo.pcEraseAll, null, null, null, null, null);

    // Check the return code.
    if (result != 0) {
      LOGGER.log(Level.SEVERE, "eraseAll error: %i" + result);
    }
  }

  /*
   * Erase one page.
   */
  public void erasePage(long flashPtr) {
    // Update core register to execute the erasePage subroutine.
    int result = this
        .callFunctionAndWait(this.flashAlgo.pcEraseSector, flashPtr, null, null, null, null);

    // Check the return code
    if (result != 0) {
      LOGGER
          .log(Level.SEVERE, "erasePage(" + String.format("%08X", flashPtr) + ") error: " + result);
    }
  }

  public boolean isDoubleBufferingSupported() {
    return this.doubleBufferSupported == null ? false : this.doubleBufferSupported;
  }

  /*
   * Flash one page.
   */
  public void programPage(long flashPtr, byte[] data) throws TimeoutException, Error {
    // Prevent security settings from locking the device.
    data = overrideSecurityBits(flashPtr, data);

    // First transfer in RAM.
    this.target.writeBlockMemoryUnaligned8(this.beginData, data);

    // Get info about this page.
    PageInfo pageInfo = this.getPageInfo(flashPtr);

    // Update core register to execute the program_page subroutine.
    int result = this
        .callFunctionAndWait(this.flashAlgo.pcProgramPage, flashPtr, (long) data.length,
            this.beginData, null, null);

    // Check the return code.
    if (result != 0) {
      LOGGER.log(Level.SEVERE, String.format("ProgramPage(0x%08x) error: %i", flashPtr, result));
    }
  }

  /*
   * Flash one page.
   */
  public void startProgramPageWithBuffer(int bufferNumber, long flashPtr) {
    if (bufferNumber >= this.pageBuffers.size()) {
      LOGGER.log(Level.SEVERE, "Invalid buffer number.");
      return;
    }

    // Get info about this page.
    PageInfo pageInfo = this.getPageInfo(flashPtr);

    // Update core register to execute the program_page subroutine.
    this.callFunction(this.flashAlgo.pcProgramPage, flashPtr, (long) pageInfo.size,
        this.pageBuffers.get(bufferNumber), null, null);
  }

  public void loadPageBuffer(int bufferNumber, long flashPtr, byte[] data)
      throws TimeoutException, Error {
    if (bufferNumber >= this.pageBuffers.size()) {
      LOGGER.log(Level.SEVERE, "Invalid buffer number.");
      return;
    }

    // Prevent security settings from locking the device.
    data = this.overrideSecurityBits(flashPtr, data);

    // Transfer the buffer to device RAM.
    this.target.writeBlockMemoryUnaligned8(this.pageBuffers.get(bufferNumber), data);
  }

  /*
   * Get info about the page that contains this address.
   * Override this function if variable page sizes are supported.
   */
  public PageInfo getPageInfo(long address) {

    // Get the region that this address belongs to.
    MemoryRegion region = this.target.getMemoryMap().getRegionForAddress(address);

    PageInfo info = new PageInfo();
    info.eraseWeight = PageInfo.DEFAULT_PAGE_ERASE_WEIGHT;
    info.programWeight = PageInfo.DEFAULT_PAGE_PROGRAM_WEIGHT;
    info.size = region.blockSize;
    info.baseAddress = address - (address % info.size);
    return info;
  }

  /*
   * Get info about the flash.
   * Override this function to return different values.
   */
  public FlashInfo getFlashInfo() {
    MemoryRegion bootRegion = this.target.getMemoryMap().getBootMemory();

    FlashInfo info = new FlashInfo();
    info.romStart = bootRegion == null ? 0 : bootRegion.start;
    info.eraseWeight = PageInfo.DEFAULT_CHIP_ERASE_WEIGHT;
    info.crcSupported = this.flashAlgo.analyzerSupported;
    return info;
  }

  public FlashBuilder getFlashBuilder() {
    return new FlashBuilder(this, getFlashInfo().romStart);
  }

  /*
   * Flash a block of data.
   */
  public ProgrammingInfo flashBlock(long address, byte[] data, Boolean smartFlash,
      Boolean chipErase, ProgressUpdateInterface progressUpdate, Boolean fastVerify)
      throws TimeoutException, InterruptedException, Error {
    long flashStart = this.getFlashInfo().romStart;
    FlashBuilder flashBuilder = new FlashBuilder(this, flashStart);
    flashBuilder.addData(address, data);
    ProgrammingInfo programmingInfo = flashBuilder.program(chipErase, progressUpdate,
        smartFlash, fastVerify);
    return programmingInfo;
  }

  public void callFunction(long pc, Long r0, Long r1, Long r2, Long r3, Boolean init) {
    // TODO
  }

  public int waitForCompletion() {
    // TODO
    return 0;
  }

  public int callFunctionAndWait(long pc, Long r0, Long r1, Long r2, Long r3, Boolean init) {
    // Use default value if null.
    if (init == null) {
      init = false;
    }

    // TODO
    // def callFunction(pc, r0, r1, r2, r3, init):

    // Return no errors.
    return 0;
  }

  private byte[] overrideSecurityBits(long flashPtr, byte[] data) {
    return data;
  }

  /*
   * Return the index of the MSB.
   */
  private int msb(long bitmask) {
    int index = 0;
    while (1 < bitmask) {
      bitmask = (bitmask >> 1);
      index++;
    }
    return index;
  }
}