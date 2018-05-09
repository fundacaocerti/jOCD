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
package br.org.certi.jocd.target.nrf51822;

import android.util.Log;
import br.org.certi.jocd.core.CoreSightTarget;
import br.org.certi.jocd.core.FlashRegion;
import br.org.certi.jocd.core.MemoryMap;
import br.org.certi.jocd.core.MemoryRegion;
import br.org.certi.jocd.core.RamRegion;
import br.org.certi.jocd.flash.Flash;
import br.org.certi.jocd.flash.FlashAlgo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Nrf51 extends CoreSightTarget {

  // Logging
  private static final String TAG = "Nrf51";

  final FlashAlgo flashAlgo;

  // nRF51 specific registers
  final int reset;
  final int resetEnable;

  public Nrf51() {
    List<MemoryRegion> memoryRegions = new ArrayList<MemoryRegion>();

    memoryRegions.add(
        new FlashRegion(
            0x00000000,
            0x40000,
            0x400)
    );

    // User Information Configation Registers (UICR) as a flash region.
    memoryRegions.add(
        new FlashRegion(
            0x10001000,
            0x100,
            0x100)
    );
    memoryRegions.add(
        new RamRegion(
            0x20000000,
            0x4000)
    );

    memoryMap = new MemoryMap(memoryRegions);

    this.flashAlgo = new FlashAlgo();
    this.flashAlgo.loadAddress = 0x20000000;
    this.flashAlgo.instructions = Arrays
        .asList(0xE00ABE00, 0x062D780D, 0x24084068, 0xD3000040, 0x1E644058, 0x1C49D1FA, 0x2A001E52,
            0x4770D1F2, 0x47702000, 0x47702000, 0x4c26b570, 0x60602002, 0x60e02001, 0x68284d24,
            0xd00207c0, 0x60602000, 0xf000bd70, 0xe7f6f82c, 0x4c1eb570, 0x60612102, 0x4288491e,
            0x2001d302, 0xe0006160, 0x4d1a60a0, 0xf81df000, 0x07c06828, 0x2000d0fa, 0xbd706060,
            0x4605b5f8, 0x4813088e, 0x46142101, 0x4f126041, 0xc501cc01, 0x07c06838, 0x1e76d006,
            0x480dd1f8, 0x60412100, 0xbdf84608, 0xf801f000, 0x480ce7f2, 0x06006840, 0xd00b0e00,
            0x6849490a, 0xd0072900, 0x4a0a4909, 0xd00007c3, 0x1d09600a, 0xd1f90840, 0x00004770,
            0x4001e500, 0x4001e400, 0x10001000, 0x40010400, 0x40010500, 0x40010600, 0x6e524635,
            0x00000000);
    this.flashAlgo.pcInit = 0x20000021;
    this.flashAlgo.pcEraseAll = 0x20000029;
    this.flashAlgo.pcEraseSector = 0x20000049;
    this.flashAlgo.pcProgramPage = 0x20000071;
    this.flashAlgo.beginData = 0x20002000; // Analyzer uses a max of 1 KB data (256 pages * 4 bytes / page)
    this.flashAlgo.pageBuffers = Arrays.asList(0x20002000, 0x20002400); // Enable double buffering
    this.flashAlgo.beginStack = 0x20001000;
    this.flashAlgo.staticBase = 0x20000170;
    this.flashAlgo.minProgramLength = 4;
    this.flashAlgo.analyzerSupported = true;
    this.flashAlgo.analyzerAddress = 0x20003000; // Analyzer 0x20003000..0x20003600

    this.reset = 0x40000544;
    this.resetEnable = (1 << 0);

    super.setup(memoryMap);
  }

  @Override
  public Flash getFlash() {
    return new FlashNrf51(this);
  }

  @Override
  public boolean massErase() {
    // Not implemented.
    Log.e(TAG, "Not implemented method massErase for nRF51");
    return false;
  }

  /*
   * reset a core. After a call to this function, the core
   * is running.
   */
  public void resetN() {
    // Regular reset will kick NRF out of DBG mode
    Log.d(TAG, "target_nrf51.reset: enable reset pin");
    writeMemory(reset, resetEnable);

    // Reset.
    Log.d(TAG, "target_nrf51.reset: trigger nRST pin");
    reset();
  }
}
