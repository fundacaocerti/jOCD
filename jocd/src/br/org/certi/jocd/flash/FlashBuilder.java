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

import static br.org.certi.jocd.flash.FlashPage.PAGE_ESTIMATE_SIZE;
import static java.lang.Math.min;

import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.tools.ProgressUpdateInterface;
import br.org.certi.jocd.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class FlashBuilder {

  // Logging
  private final static String CLASS_NAME = FlashBuilder.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  final Flash flash;
  final long flashStart;
  ProgrammingInfo perf = new ProgrammingInfo();

  boolean enableDoubleBuffering = true;
  int maxErrors = 10;
  int chipEraseCount;
  double chipEraseWeight;
  int sectorEraseCount;
  int pageEraseCount;
  double pageEraseWeight;

  // Type of flash operation.
  public final static int FLASH_PAGE_ERASE = 1;
  public final static int FLASH_CHIP_ERASE = 2;

  // Type of flash analysis.
  public final static String FLASH_ANALYSIS_CRC32 = "CRC32";
  public final static String FLASH_ANALYSIS_PARTIAL_PAGE_READ = "PAGE_READ";

  // List of flash operations.
  List<FlashOperation> flashOperations = new ArrayList<FlashOperation>();

  List<FlashPage> pageList = new ArrayList<FlashPage>();

  public FlashBuilder(Flash flash, long baseAddress) {
    this.flash = flash;
    this.flashStart = baseAddress;
  }

  private boolean same(byte[] d1, byte[] d2, int size) {
    if (d1.length < size || d2.length < size) {
      return false;
    }

    for (int i = 0; i < size; i++) {
      if (d1[i] != d2[i]) {
        return false;
      }
    }
    return true;
  }

  private boolean erased(byte[] data) {
    for (int i = 0; i < data.length; i++) {
      if (data[i] != 0xFF) {
        return false;
      }
    }
    return true;
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
  public ProgrammingInfo program(Boolean chipErase, ProgressUpdateInterface progressUpdate,
      boolean smartFlash, boolean fastVerify)
      throws InterruptedException, TimeoutException, Error {

    // Assumptions
    // 1. Page erases must be on page boundaries ( page_erase_addr % page_size == 0 )
    // 2. Page erase can have a different size depending on location
    // 3. It is safe to program a page with less than a page of data

    // Examples
    // - lpc4330     -Non 0 base address
    // - nRF51       -UICR location far from flash (address 0x10001000)
    // - LPC1768     -Different sized pages

    long programStartTime = System.currentTimeMillis();

    // There must be at least 1 flash operation.
    if (flashOperations.size() == 0) {
      LOGGER.log(Level.WARNING, "No pages were programmed");
      throw new InternalError("No flash operations listed.");
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
              .readBlockMemoryUnaligned8(pageDataEnd, (int) (flashAddress - pageDataEnd));
          currentPage.data = Util.appendDataInArray(currentPage.data, oldData);
        }

        // Copy data to page and increment pos
        int spaceLeftInPage = pageInfo.size - currentPage.data.length;
        int spaceLeftInData = op.data.length - pos;
        int amount = min(spaceLeftInPage, spaceLeftInData);
        currentPage.data = Util
            .appendDataInArray(currentPage.data, Util.getSubArray(op.data, pos, pos + amount));
        programByteCount += amount;

        // Increment position.
        pos += amount;
      }
    }

    // If smart flash was set to false then mark all pages as requiring programming.
    if (!smartFlash) {
      this.markAllPagesForProgramming();
    }

    // If the first page being programmed is not the first page in ROM then don't use a chip erase.
    if (this.pageList.get(0).address > this.flashStart) {
      if (chipErase == null) {
        chipErase = false;
      } else if (chipErase == true) {
        LOGGER.log(Level.WARNING, String.format(
            "Chip erase used when flash address 0x%08x is not the same as flash start 0x%08x",
            this.pageList.get(0).address, this.flashStart));
      }
    }

    this.flash.init();

    // Update this.chipEraseCount, and this.chipEraseWeight.
    computeChipErasePagesAndWeight();
    double chipEraseProgramTime = this.chipEraseWeight;
    double pageEraseMinProgramTime = this.computePageErasePagesWeightMin();

    // If chip_erase hasn't been specified determine if chip erase is faster than page erase
    // regardless of contents.
    if (chipErase == null && (chipEraseProgramTime < pageEraseMinProgramTime)) {
      chipErase = true;
    }

    int sectorEraseCount = 0;
    double pageProgramTime = 0;
    // If chip erase isn't true then analyze the flash.
    if (chipErase == null || chipErase == false) {
      long analyzeStartTime = System.currentTimeMillis();

      if (this.flash.getFlashInfo().crcSupported) {
        this.computePageErasePagesAndWeightCrc32(fastVerify);
        sectorEraseCount = this.sectorEraseCount;
        pageProgramTime = this.pageEraseWeight;
        this.perf.analyzeType = FlashBuilder.FLASH_ANALYSIS_CRC32;
      } else {
        this.computePageErasePagesAndWeightSectorRead();
        sectorEraseCount = this.sectorEraseCount;
        pageProgramTime = this.pageEraseWeight;
        this.perf.analyzeType = FlashBuilder.FLASH_ANALYSIS_PARTIAL_PAGE_READ;
      }

      long analyzeFinishTime = System.currentTimeMillis();
      this.perf.analyzeTime = analyzeFinishTime - analyzeStartTime;
      LOGGER.log(Level.FINE, "Analyze time: " + this.perf.analyzeTime);
    }

    // If chip erase hasn't been set then determine fastest method to program.
    if (chipErase == null) {
      LOGGER.log(Level.FINE,
          "Chip erase count " + chipEraseCount + ", Page erase est count " + sectorEraseCount);
      LOGGER.log(Level.FINE,
          "Chip erase weight " + chipEraseProgramTime + ", Page erase weight " + pageProgramTime);
      chipErase = chipEraseProgramTime < pageProgramTime;
    }

    int operation;
    if (chipErase) {
      if (this.flash.isDoubleBufferingSupported() && this.enableDoubleBuffering) {
        LOGGER.log(Level.FINE, "Using double buffer chip erase program");
        operation = this.chipEraseProgramDoubleBuffer(progressUpdate);
      } else {
        operation = this.chipEraseProgram(progressUpdate);
      }
    } else {
      if (this.flash.isDoubleBufferingSupported() && this.enableDoubleBuffering) {
        LOGGER.log(Level.FINE, "Using double buffer page erase program");
        operation = this.pageEraseProgramDoubleBuffer(progressUpdate);
      } else {
        operation = this.pageEraseProgram(progressUpdate);
      }
    }

    this.flash.target.resetStopOnReset(null);

    long programFinishTime = System.currentTimeMillis();
    this.perf.programTime = programFinishTime - programStartTime;
    this.perf.programType = operation;

    LOGGER.log(Level.FINE, String
        .format("Programmed %d bytes (%d pages) at %.02f kB/s", programByteCount, pageList.size(),
            (float) ((float) (programByteCount / 1024) / this.perf.programTime)));

    return this.perf;
  }

  private void markAllPagesForProgramming() {
    for (FlashPage page : this.pageList) {
      page.erased = false;
      page.same = false;
    }
  }

  /*
   * Estimate how many pages are the same.
   *
   * Quickly estimate how many pages are the same.  These estimates are used by page_erase_program
   * so it is recommended to call this before beginning programming This is done automatically by
   * smart_program.
   */
  private void computePageErasePagesAndWeightSectorRead() throws TimeoutException, Error {
    // Quickly estimate how many pages are the same.
    int pageEraseCount = 0;
    double pageEraseWeight = 0;

    for (FlashPage page : this.pageList) {
      // Analyze pages that haven't been analyzed yet.
      if (page.same == null) {
        int size = min(PAGE_ESTIMATE_SIZE, page.data.length);
        byte[] data = this.flash.target.readBlockMemoryUnaligned8(page.address, size);
        boolean pageSame = same(data, page.data, size);
        if (pageSame == false) {
          page.same = false;
        }
      }
    }

    // Put together page and time estimate.
    for (FlashPage page : this.pageList) {
      if (page.same == null) {
        // Page is probably the same but must be read to confirm.
        pageEraseWeight += page.getVerifyWeight();
      } else if (page.same == false) {
        pageEraseCount += 1;
        pageEraseWeight += page.getEraseProgramWeight();
      }

      // If page.same is true, page is confirmed to be the same. So there is no programming weight.
      // We don't need to do anything.
    }

    this.pageEraseCount = pageEraseCount;
    this.pageEraseWeight = pageEraseWeight;
  }

  /*
   * Compute the number of erased pages.
   * Determine how many pages in the new data are already erased.
   */
  private void computeChipErasePagesAndWeight() {
    int chipEraseCount = 0;
    double chipEraseWeight = 0;
    chipEraseWeight += this.flash.getFlashInfo().eraseWeight;
    for (FlashPage page : this.pageList) {
      if (page.erased == null) {
        page.erased = erased(page.data);
      }
      if (!page.erased) {
        chipEraseCount += 1;
        chipEraseWeight += page.getProgramWeight();
      }
    }

    this.chipEraseCount = chipEraseCount;
    this.chipEraseWeight = chipEraseWeight;
  }

  private double computePageErasePagesWeightMin() {
    double pageEraseMinWeight = 0;
    for (FlashPage page : this.pageList) {
      pageEraseMinWeight += page.getVerifyWeight();
    }
    return pageEraseMinWeight;
  }

  /*
   * Estimate how many pages are the same.
   *
   * Quickly estimate how many pages are the same.  These estimates are used
   * by page_erase_program so it is recommended to call this before beginning programming
   * This is done automatically by smart_program.
   *
   * If assume_estimate_correct is set to True, then pages with matching CRCs
   * will be marked as the same.  There is a small chance that the CRCs match even though the
   * data is different, but the odds of this happing are low: ~1/(2^32) = ~2.33*10^-8%.
   */
  private void computePageErasePagesAndWeightCrc32(Boolean assumeEstimateCorrect)
      throws InterruptedException, TimeoutException, Error {
    // Set the default value, if null.
    if (assumeEstimateCorrect == null) {
      assumeEstimateCorrect = false;
    }

    // Build list of all the pages that need to be analyzed.
    List<Sectors> sectorList = new ArrayList<Sectors>();
    List<FlashPage> pageList = new ArrayList<FlashPage>();

    for (FlashPage page : this.pageList) {
      if (page.same == null) {
        // Add sector to computeCrcs.
        sectorList.add(new Sectors(page.address, page.size));
        pageList.add(page);

        // Compute CRC of data (Padded with 0xFF).
        byte[] data = page.data;
        data = Util.fillArray(data, (int) page.size, (byte) 0xFF);
        CRC32 crc = new CRC32();
        crc.update(data);
        page.crc = (crc.getValue() & 0xFFFFFFFFL);
      }
    }

    // Analyze pages.
    int pageEraseCount = 0;
    double pageEraseWeight = 0;
    if (pageList.size() > 0) {
      long[] crcs = this.flash.computeCrcs(sectorList);
      for (int i = 0; i < pageList.size() && i < crcs.length; i++) {
        boolean pageSame = (pageList.get(i).crc == crcs[i]);
        if (assumeEstimateCorrect) {
          pageList.get(i).same = pageSame;
        } else if (pageSame == false) {
          pageList.get(i).same = false;
        }
      }
    }

    // Put together page and time estimate.
    for (FlashPage page : this.pageList) {
      if (page.same == null) {
        // Page is probably the same but must be read to confirm.
        pageEraseWeight += page.getVerifyWeight();
      } else if (page.same == false) {
        pageEraseCount += 1;
        pageEraseWeight += page.getEraseProgramWeight();
      }

      // If page.same is true, page is confirmed to be the same. So there is no programming weight.
      // We don't need to do anything.
    }

    this.pageEraseCount = pageEraseCount;
    this.pageEraseWeight = pageEraseWeight;
  }

  /*
   * Program by first performing a chip erase.
   */
  private int chipEraseProgram(ProgressUpdateInterface progressUpdate)
      throws InterruptedException, TimeoutException, Error {
    LOGGER.log(Level.FINE, "Smart chip erase");
    LOGGER.log(Level.FINE,
        (this.pageList.size() - this.chipEraseCount) + " of " + this.pageList.size()
            + "pages already erased.");

    progressUpdate.progressUpdateCallback(0);
    double progress = 0;

    this.flash.eraseAll();
    progress += this.flash.getFlashInfo().eraseWeight;

    for (FlashPage page : this.pageList) {
      if (page.erased == null || page.erased == false) {
        this.flash.programPage(page.address, page.data);
        progress += page.getProgramWeight();
        progressUpdate.progressUpdateCallback((int) ((100 * progress) / chipEraseWeight));
      }
    }
    progressUpdate.progressUpdateCallback(100);
    return FlashBuilder.FLASH_CHIP_ERASE;
  }

  private int nextUnerasedPage(int startIndex) {

    FlashPage page;

    for (int i = startIndex; i < pageList.size(); i++) {
      page = this.pageList.get(i);

      if (page.erased == false) {
        return i;
      }
    }
    return -1;
  }

  /*
   * Program by first performing a chip erase.
   */
  private int chipEraseProgramDoubleBuffer(ProgressUpdateInterface progressUpdate)
      throws InterruptedException, TimeoutException, Error {
    LOGGER.log(Level.FINE, "Smart chip erase");
    LOGGER.log(Level.FINE,
        (this.pageList.size() - this.chipEraseCount) + " of " + this.pageList.size()
            + "pages already erased.");

    progressUpdate.progressUpdateCallback(0);
    double progress = 0;

    this.flash.eraseAll();
    progress += this.flash.getFlashInfo().eraseWeight;

    // Set up page and buffer info.
    int errorCount = 0;
    int currentBuffer = 0;
    int nextBuffer = 1;

    int pageIndex = nextUnerasedPage(0);
    if (pageIndex < 0) {
      LOGGER
          .log(Level.SEVERE, "Unexpected nextUnerasedPage result - All pages are already erased.");
    }
    FlashPage page = this.pageList.get(pageIndex);

    // Load first page buffer.
    this.flash.loadPageBuffer(currentBuffer, page.address, page.data);

    while (pageIndex >= 0) {
      // Kick off this page program.
      long currentAddress = page.address;
      double currentWeight = page.getProgramWeight();
      this.flash.startProgramPageWithBuffer(currentBuffer, currentAddress);

      // Get next page and load it.
      pageIndex = nextUnerasedPage(pageIndex + 1);
      if (pageIndex >= 0) {
        page = this.pageList.get(pageIndex);
        this.flash.loadPageBuffer(nextBuffer, page.address, page.data);
      }

      // Wait for the program to complete.
      long result = this.flash.waitForCompletion();

      // Check the return code.
      if (result != 0) {
        LOGGER.log(Level.SEVERE,
            String.format("programPage(0x%08x) error: %d.", currentAddress, result));
        errorCount++;
        if (errorCount > this.maxErrors) {
          LOGGER.log(Level.SEVERE, "Too many page programming errors, aborting program operation.");
          break;
        }
      }

      // Swap buffers.
      int temp = currentBuffer;
      currentBuffer = nextBuffer;
      nextBuffer = temp;

      // Update progress.
      progress += currentWeight;
      progressUpdate.progressUpdateCallback((int) (100 * progress / this.chipEraseWeight));
    }

    progressUpdate.progressUpdateCallback(100);
    return FlashBuilder.FLASH_CHIP_ERASE;
  }

  /*
   * Program by performing sector erases.
   */
  private int pageEraseProgram(ProgressUpdateInterface progressUpdate)
      throws InterruptedException, TimeoutException, Error {
    int actualPageEraseCount = 0;
    double actualPageEraseWeight = 0;
    double progress = 0;

    progressUpdate.progressUpdateCallback(0);

    for (FlashPage page : this.pageList) {

      if (page.same != null && page.same == false) {
        // If the page is not the same.
        progress += page.getProgramWeight();
      }

      if (page.same == null) {
        // Read page data if unknown - after this page.same will be True or False.
        byte[] data = this.flash.target.readBlockMemoryUnaligned8(page.address, page.data.length);
        page.same = same(page.data, data, data.length);
        progress += page.getVerifyWeight();
      }

      if (page.same == false) {
        this.flash.erasePage(page.address);
        this.flash.programPage(page.address, page.data);
        actualPageEraseCount++;
        actualPageEraseWeight += page.getEraseProgramWeight();
      }

      // Update progress.
      if (this.pageEraseWeight > 0) {
        progressUpdate.progressUpdateCallback((int) (100 * progress / pageEraseWeight));
      }
    }

    progressUpdate.progressUpdateCallback(100);

    LOGGER.log(Level.FINE, "Estimated page erase count: " + this.pageEraseCount);
    LOGGER.log(Level.FINE, "Actual page erase count: " + actualPageEraseCount);
    return FlashBuilder.FLASH_PAGE_ERASE;
  }

  /*
   * Program by performing sector erases.
   */
  private double scanPagesForSame(ProgressUpdateInterface progressUpdate)
      throws TimeoutException, Error {
    double progress = 0;
    int count = 0;
    int sameCount = 0;

    for (FlashPage page : this.pageList) {
      // Read page data if unknown - after this page.same will be True or False.
      if (page.same == null) {
        byte[] data = this.flash.target.readBlockMemoryUnaligned8(page.address, page.data.length);

        page.same = same(page.data, data, data.length);
        progress += page.getVerifyWeight();
        count++;

        if (page.same) {
          sameCount++;
        }

        // Update progress.
        progressUpdate.progressUpdateCallback((int) (100 * progress / this.pageEraseWeight));
      }
    }
    return progress;
  }

  private int nextNonsamePage(int startIndex) {
    FlashPage page;

    for (int i = startIndex; i < pageList.size(); i++) {
      page = this.pageList.get(i);

      if (page.same == false) {
        return i;
      }
    }
    return -1;
  }

  /*
   * Program by performing sector erases.
   */
  public int pageEraseProgramDoubleBuffer(ProgressUpdateInterface progressUpdate)
      throws InterruptedException, TimeoutException, Error {
    int actualPageEraseCount = 0;
    double actualPageEraseWeight = 0;
    double progress = 0;

    progressUpdate.progressUpdateCallback(0);

    // Fill in same flag for all pages. This is done up front so we're not trying to read from flash
    // while simultaneously programming it.
    progress = this.scanPagesForSame(progressUpdate);

    // Set up page and buffer info.
    int errorCount = 0;
    int currentBuffer = 0;
    int nextBuffer = 1;

    int pageIndex = nextNonsamePage(0);
    FlashPage page;

    // Make sure there are actually pages to program differently from current flash contents.
    if (pageIndex >= 0) {
      page = pageList.get(pageIndex);

      // Load first page buffer.
      this.flash.loadPageBuffer(currentBuffer, page.address, page.data);

      while (pageIndex >= 0) {
        // Assert (page.same != null).
        if (page.same == null) {
          throw new Error("pageEraseProgramDoubleBuffer: page.same == null");
        }

        // Kick off this page program.
        long currentAddress = page.address;
        double currentWeight = page.getEraseProgramWeight();
        this.flash.erasePage(currentAddress);
        this.flash.startProgramPageWithBuffer(currentBuffer, currentAddress);
        actualPageEraseCount++;
        actualPageEraseWeight += page.getEraseProgramWeight();

        // Get next page and load it.
        pageIndex = nextNonsamePage(pageIndex + 1);
        if (pageIndex >= 0) {
          page = pageList.get(pageIndex);
          this.flash.loadPageBuffer(nextBuffer, page.address, page.data);
        }

        // Wait for the program to complete.
        long result = this.flash.waitForCompletion();

        // Check the return code.
        if (result != 0) {
          LOGGER.log(Level.SEVERE,
              String.format("programPage(0x%08x) error: %d", currentAddress, result));
          errorCount++;
          if (errorCount > this.maxErrors) {
            LOGGER
                .log(Level.SEVERE, "Too many page programming errors, aborting program operation.");
            break;
          }
        }

        // Swap buffers.
        int temp = currentBuffer;
        currentBuffer = nextBuffer;
        nextBuffer = temp;

        // Update progress.
        progress += currentWeight;
        if (this.pageEraseWeight > 0) {
          progressUpdate.progressUpdateCallback((int) (100 * progress / this.pageEraseWeight));
        }
      }
    }

    progressUpdate.progressUpdateCallback(100);

    LOGGER.log(Level.FINE, "Estimated page erase count: " + this.pageEraseCount);
    LOGGER.log(Level.FINE, "Actual page erase count: " + actualPageEraseCount);

    return FlashBuilder.FLASH_PAGE_ERASE;
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
