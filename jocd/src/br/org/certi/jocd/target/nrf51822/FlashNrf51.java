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
    this.flashAlgo.loadAddress = 0x20000000L;
    this.flashAlgo.instructions = new long[]{0xE00ABE00L, 0x062D780DL, 0x24084068L, 0xD3000040L,
        0x1E644058L, 0x1C49D1FAL, 0x2A001E52L, 0x4770D1F2L, 0x47702000L, 0x47702000L, 0x4C26B570L,
        0x60602002L, 0x60E02001L, 0x68284D24L, 0xD00207C0L, 0x60602000L, 0xF000BD70L, 0xE7F6F82CL,
        0x4C1EB570L, 0x60612102L, 0x4288491EL, 0x2001D302L, 0xE0006160L, 0x4D1A60A0L, 0xF81DF000L,
        0x07C06828L, 0x2000D0FAL, 0xBD706060L, 0x4605B5F8L, 0x4813088EL, 0x46142101L, 0x4F126041L,
        0xC501CC01L, 0x07C06838L, 0x1E76D006L, 0x480DD1F8L, 0x60412100L, 0xBDF84608L, 0xF801F000L,
        0x480CE7F2L, 0x06006840L, 0xD00B0E00L, 0x6849490AL, 0xD0072900L, 0x4A0A4909L, 0xD00007C3L,
        0x1D09600AL, 0xD1F90840L, 0x00004770L, 0x4001E500L, 0x4001E400L, 0x10001000L, 0x40010400L,
        0x40010500L, 0x40010600L, 0x6E524635L, 0x00000000L};
    this.flashAlgo.pcInit = 0x20000021L;
    this.flashAlgo.pcEraseAll = 0x20000029L;
    this.flashAlgo.pcEraseSector = 0x20000049L;
    this.flashAlgo.pcProgramPage = 0x20000071L;
    this.flashAlgo.beginData = 0x20002000L; // Analyzer uses a max of 1 KB data (256 pages * 4 bytes / page)
    this.flashAlgo.pageBuffers = Arrays
        .asList(0x20002000L, 0x20002400L); // Enable double buffering
    this.flashAlgo.beginStack = 0x20001000L;
    this.flashAlgo.staticBase = 0x20000170L;
    this.flashAlgo.minProgramLength = 4;
    this.flashAlgo.analyzerSupported = true;
    this.flashAlgo.analyzerAddress = 0x20003000L; // Analyzer 0x20003000..0x20003600

    super.setup(target, flashAlgo);
  }
}
