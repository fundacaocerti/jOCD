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
import br.org.certi.jocd.core.Target.CoreRegister;
import br.org.certi.jocd.core.Target.State;
import br.org.certi.jocd.coresight.CortexM;
import br.org.certi.jocd.coresight.CortexM.CortexMRegister;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.tools.ProgressUpdateInterface;
import br.org.certi.jocd.util.Util;
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

  // Program to compute the CRC of sectors.  This works on cortex-m processors.
  // Code is relocatable and only needs to be on a 4 byte boundary.
  // 200 bytes of executable data below + 1024 byte crc table = 1224 bytes
  // Usage requirements:
  // -In memory reserve 0x600 for code & table
  // -Make sure data buffer is big enough to hold 4 bytes for each page that could be checked (ie.  >= num pages * 4)
  long[] analyzer = new long[]{0x2180468CL, 0x2600B5F0L, 0x4F2C2501L, 0x447F4C2CL, 0x1C2B0049L,
      0x425B4033L, 0x40230872L, 0x085A4053L, 0x425B402BL, 0x40534023L, 0x402B085AL, 0x4023425BL,
      0x085A4053L, 0x425B402BL, 0x40534023L, 0x402B085AL, 0x4023425BL, 0x085A4053L, 0x425B402BL,
      0x40534023L, 0x402B085AL, 0x4023425BL, 0x085A4053L, 0x425B402BL, 0x40534023L, 0xC7083601L,
      0xD1D2428EL, 0x2B004663L, 0x4663D01FL, 0x46B4009EL, 0x24FF2701L, 0x44844D11L, 0x1C3A447DL,
      0x88418803L, 0x4351409AL, 0xD0122A00L, 0x22011856L, 0x780B4252L, 0x40533101L, 0x009B4023L,
      0x0A12595BL, 0x42B1405AL, 0x43D2D1F5L, 0x4560C004L, 0x2000D1E7L, 0x2200BDF0L, 0x46C0E7F8L,
      0x000000B6L, 0xEDB88320L, 0x00000044L};

  FlashAlgo flashAlgo;
  Target target;

  boolean flashAlgoDebug = false;

  Long beginStack;
  Long beginData;
  Long staticBase;
  int minProgramLength;
  List<Long> pageBuffers;
  long savedVectorCatch;

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

    // Update core register to execute the init subroutine.
    long result = this.callFunctionAndWait(this.flashAlgo.pcInit, null, null, null, null, true);

    // Check the return code.
    if (result != 0) {
      LOGGER.log(Level.SEVERE, "Init error: " + result);
    }
  }

  public long[] computeCrcs(List<Sectors> sectors)
      throws InterruptedException, TimeoutException, Error {
    long[] words = new long[sectors.size()];
    int i = 0;

    // Convert address, size pairs into commands for the crc computation algorithm to preform.
    for (Sectors sector : sectors) {
      int sizeVal = msb(sector.size);
      long addressVal = sector.address / sector.size;

      // Size must be a power of 2.
      // Assert (1 << sizeVal) == sector.size.
      if ((1 << sizeVal) != sector.size) {
        throw new Error("computeCrcs: (1 << sizeVal) != sector.size");
      }

      // Address must be a multiple of size.
      // Assert (sector.address % sector.size) == 0.
      if ((sector.address % sector.size) != 0) {
        throw new Error("computeCrcs: (sector.address % sector.size) != 0");
      }

      long val = ((sizeVal << 0) | (addressVal << 16));
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
  public void eraseAll() throws InterruptedException, TimeoutException, Error {
    // Update core register to execute the eraseAll subroutine.
    long result = this.callFunctionAndWait(this.flashAlgo.pcEraseAll, null, null, null, null, null);

    // Check the return code.
    if (result != 0) {
      LOGGER.log(Level.SEVERE, "eraseAll error: %i" + result);
    }
  }

  /*
   * Erase one page.
   */
  public void erasePage(long flashPtr) throws InterruptedException, TimeoutException, Error {
    // Update core register to execute the erasePage subroutine.
    long result = this
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
  public void programPage(long flashPtr, byte[] data)
      throws InterruptedException, TimeoutException, Error {
    // Prevent security settings from locking the device.
    data = overrideSecurityBits(flashPtr, data);

    // First transfer in RAM.
    this.target.writeBlockMemoryUnaligned8(this.beginData, data);

    // Get info about this page.
    PageInfo pageInfo = this.getPageInfo(flashPtr);

    // Update core register to execute the program_page subroutine.
    long result = this
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
  public void startProgramPageWithBuffer(int bufferNumber, long flashPtr)
      throws TimeoutException, Error {
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
    if (region == null) {
      return null;
    }

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

  public void callFunction(long pc, Long r0, Long r1, Long r2, Long r3, Boolean init)
      throws TimeoutException, Error {
    // Use default value if null.
    if (init == null) {
      init = false;
    }

    List<CoreRegister> regList = new ArrayList<CoreRegister>();
    List<Long> dataList = new ArrayList<Long>();

    if (this.flashAlgoDebug) {
      // Save vector catch state for use in waitForCompletion().
      this.savedVectorCatch = this.target.getVectorCatch();
      this.target.setVectorCatch(Target.CATCH_ALL);
    }

    if (init) {
      // Download flash algo in RAM.
      this.target
          .writeBlockMemoryAligned32(this.flashAlgo.loadAddress, this.flashAlgo.instructions);
      if (this.flashAlgo.analyzerSupported) {
        this.target.writeBlockMemoryAligned32(this.flashAlgo.analyzerAddress, analyzer);
      }
    }

    // We want to write registers from Cortex-M.
    // If our selected core isn't a Cortex-M, than something is wrong or it's not implemented yet.
    if (!(this.target.getSelectedCore() instanceof CortexM)) {
      throw new InternalError(
          "callFunction: Unexpected core. " + this.target.getSelectedCore().toString());
    }
    regList.add(CortexMRegister.PC);
    dataList.add(pc);

    if (r0 != null) {
      regList.add(CortexMRegister.R0);
      dataList.add(r0);
    }

    if (r1 != null) {
      regList.add(CortexMRegister.R1);
      dataList.add(r1);
    }

    if (r2 != null) {
      regList.add(CortexMRegister.R2);
      dataList.add(r2);
    }

    if (r3 != null) {
      regList.add(CortexMRegister.R3);
      dataList.add(r3);
    }

    if (init) {
      regList.add(CortexMRegister.R9);
      dataList.add(this.staticBase);
      regList.add(CortexMRegister.SP);
      dataList.add(this.beginStack);
    }

    regList.add(CortexMRegister.LR);
    dataList.add(this.flashAlgo.loadAddress + 1);
    this.target.writeCoreRegisterRaw(regList, Util.getArrayFromList(dataList));

    // Resume target.
    this.target.resume();
  }

  /*
   * Wait until the breakpoint is hit.
   */
  public long waitForCompletion() throws InterruptedException, TimeoutException, Error {
    int retries = 20;
    while (this.target.getState() == Target.State.TARGET_RUNNING) {
      Thread.sleep(10);
      if (--retries == 0) {
        LOGGER.log(Level.SEVERE,
            "Couldn't init the flash - waiting for completation never gets to expected result. "
                + "You might be able to fix this using openOCD.");
        throw new TimeoutException("Timeout while expecting for target state == RUNNING");
      }
    }

    if (this.flashAlgoDebug) {

      // Frame pointer.
      long expectedFp = this.flashAlgo.staticBase;
      // Stack pointer.
      long expectedSp = this.flashAlgo.beginStack;
      // PC
      long expectedPc = this.flashAlgo.loadAddress;
      long[] expectedFlashAlgo = this.flashAlgo.instructions;
      long[] expectedAnalyzer = null;

      if (this.flashAlgo.analyzerSupported) {
        expectedAnalyzer = this.analyzer;
      }

      // We want to write registers from Cortex-M.
      // If our selected core isn't a Cortex-M, than something is wrong or it's not implemented yet.
      if (!(this.target.getSelectedCore() instanceof CortexM)) {
        throw new InternalError(
            "waitForCompletion: Unexpected core. " + this.target.getSelectedCore().toString());
      }
      long finalFp = this.target.readCoreRegister(CortexMRegister.R9);
      long finalSp = this.target.readCoreRegister(CortexMRegister.S9);
      long finalPc = this.target.readCoreRegister(CortexMRegister.PC);

      boolean error = false;
      if (finalFp != expectedFp) {
        // Frame pointer should not change.
        LOGGER.log(Level.SEVERE,
            String.format("Frame pointer should be 0x%08X but is 0x%08X", expectedFp, finalFp));
        error = true;
      }
      if (finalSp != expectedSp) {
        // Stack pointer should return to original value after function call
        LOGGER.log(Level.SEVERE,
            String.format("Stack pointer should be 0x%08X but is 0x%08X", expectedSp, finalSp));
        error = true;
      }
      if (finalPc != expectedPc) {
        // Stack pointer should return to original value after function call
        LOGGER.log(Level.SEVERE,
            String.format("PC should be 0x%08X but is 0x%08X", expectedPc, finalPc));
        error = true;
      }

      if (error) {
        throw new Error("Error while reading readCoreRegister. Unexpected values.");
      }

      this.target.setVectorCatch(this.savedVectorCatch);
    }

    return this.target.readCoreRegister(CortexMRegister.R0);
  }

  public long callFunctionAndWait(long pc, Long r0, Long r1, Long r2, Long r3, Boolean init)
      throws InterruptedException, TimeoutException, Error {
    // Use default value if null.
    if (init == null) {
      init = false;
    }

    this.callFunction(pc, r0, r1, r2, r3, init);
    return this.waitForCompletion();
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
