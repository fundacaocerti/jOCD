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

import android.util.Log;
import br.org.certi.jocd.core.MemoryRegion;
import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.tools.ProgressUpdateInterface;

/*
 * This class is responsible to flash a new binary in a target.
 */
public class Flash {

  // Logging
  private static final String TAG = "Flash";

  FlashAlgo flashAlgo;
  Target target;

  /*
   * Constructor.
   */
  public Flash() {
  }

  /*
   * Must be called right after constructor.
   */
  public void setup(Target target, FlashAlgo flashAlgo) {
    this.flashAlgo = flashAlgo;
    this.target = target;
  }

  /*
   * Download the flash algorithm in RAM.
   */
  public void init() {
    // TODO
  }

  /*
   * Erase all the flash.
   */
  public void eraseAll() {
    // TODO
  }

  /*
   * Erase one page.
   */
  public void erasePage(long flashPtr) {
    // Update core register to execute the erasePage subroutine.
    int result = this.callFunctionAndWait(this.flashAlgo.pcEraseSector, flashPtr);

    // Check the return code
    if (result != 0) {
      Log.e(TAG, "erasePage(" + String.format("%08X", flashPtr) + ") error: " + result);
    }
  }

  /*
   * Get info about the page that contains this address.
   * Override this function if variable page sizes are supported.
   */
  public PageInfo getPageInfo(long address) {

    // Get the region that this address belongs to.
    MemoryRegion region = this.target.getMemoryMap().getRegionForAddress(address);

    PageInfo info = new PageInfo();
    info.eraseWeight = PageInfo.DEFAULT_PAGE_ERASE;
    info.programWeight = PageInfo.DEFAULT_PAGE_PROGRAM_WEIGHT;
    info.size = region.blockSize;
    info.baseAddress = address - (address % info.size);
    return info;
  }

  public int callFunctionAndWait(int pcEraseSector, long flashPtr) {
    // TODO

    // Return no errors.
    return 0;
  }

  /*
   * Get info about the flash.
   * Override this function to return different values.
   */
  public FlashInfo getFlashInfo() {
    // TODO
    return null;
  }


  /*
   * Flash a block of data.
   */
  public ProgrammingInfo flashBlock(long address, byte[] data, boolean smartFlash,
      boolean chipErase, ProgressUpdateInterface progressUpdate, boolean fastVerify) {
    long flashStart = this.getFlashInfo().romStart;
    FlashBuilder flashBuilder = new FlashBuilder(this, flashStart);
    flashBuilder.addData(address, data);
    ProgrammingInfo programmingInfo = flashBuilder.program(chipErase, progressUpdate,
        smartFlash, fastVerify);
    return programmingInfo;
  }

  public FlashBuilder getFlashBuilder() {
    return new FlashBuilder(this, getFlashInfo().romStart);
  }
}