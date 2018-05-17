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

import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.flash.Flash;
import br.org.certi.jocd.flash.FlashAlgo;
import java.util.Arrays;

public class FlashNrf51 extends Flash {

  final FlashAlgo flashAlgo;

  /*
   * Constructor.
   */
  public FlashNrf51(Target target) {
    this.flashAlgo = new FlashAlgo();
    this.flashAlgo.loadAddress = 0x20000000;
    this.flashAlgo.instructions = Arrays
        .asList(
            (long) 0xE00ABE00, (long) 0x062D780D, (long) 0x24084068, (long) 0xD3000040,
            (long) 0x1E644058, (long) 0x1C49D1FA, (long) 0x2A001E52, (long) 0x4770D1F2,
            (long) 0x47702000, (long) 0x47702000, (long) 0x4c26b570, (long) 0x60602002,
            (long) 0x60e02001, (long) 0x68284d24, (long) 0xd00207c0, (long) 0x60602000,
            (long) 0xf000bd70, (long) 0xe7f6f82c, (long) 0x4c1eb570, (long) 0x60612102,
            (long) 0x4288491e, (long) 0x2001d302, (long) 0xe0006160, (long) 0x4d1a60a0,
            (long) 0xf81df000, (long) 0x07c06828, (long) 0x2000d0fa, (long) 0xbd706060,
            (long) 0x4605b5f8, (long) 0x4813088e, (long) 0x46142101, (long) 0x4f126041,
            (long) 0xc501cc01, (long) 0x07c06838, (long) 0x1e76d006, (long) 0x480dd1f8,
            (long) 0x60412100, (long) 0xbdf84608, (long) 0xf801f000, (long) 0x480ce7f2,
            (long) 0x06006840, (long) 0xd00b0e00, (long) 0x6849490a, (long) 0xd0072900,
            (long) 0x4a0a4909, (long) 0xd00007c3, (long) 0x1d09600a, (long) 0xd1f90840,
            (long) 0x00004770, (long) 0x4001e500, (long) 0x4001e400, (long) 0x10001000,
            (long) 0x40010400, (long) 0x40010500, (long) 0x40010600, (long) 0x6e524635,
            (long) 0x00000000);
    this.flashAlgo.pcInit = 0x20000021;
    this.flashAlgo.pcEraseAll = 0x20000029;
    this.flashAlgo.pcEraseSector = 0x20000049;
    this.flashAlgo.pcProgramPage = 0x20000071;
    this.flashAlgo.beginData = 0x20002000; // Analyzer uses a max of 1 KB data (256 pages * 4 bytes / page)
    this.flashAlgo.pageBuffers = Arrays
        .asList((long) 0x20002000, (long) 0x20002400); // Enable double buffering
    this.flashAlgo.beginStack = 0x20001000;
    this.flashAlgo.staticBase = 0x20000170;
    this.flashAlgo.minProgramLength = 4;
    this.flashAlgo.analyzerSupported = true;
    this.flashAlgo.analyzerAddress = 0x20003000; // Analyzer 0x20003000..0x20003600

    super.setup(target, flashAlgo);
  }
}
