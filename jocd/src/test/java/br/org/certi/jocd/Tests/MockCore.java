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
package br.org.certi.jocd.Tests;

import br.org.certi.jocd.core.FlashRegion;
import br.org.certi.jocd.core.MemoryMap;
import br.org.certi.jocd.core.MemoryRegion;
import br.org.certi.jocd.core.RamRegion;
import br.org.certi.jocd.core.Target.CoreRegister;
import br.org.certi.jocd.coresight.CortexM.CortexMRegister;
import br.org.certi.jocd.util.Conversion;
import br.org.certi.jocd.util.Util;
import java.util.ArrayList;
import java.util.List;

public class MockCore {

  private int runToken = 1;

  private final FlashRegion flashRegion = new FlashRegion(0x00000000L, 1024, 1024, "flash",
      MemoryRegion.DEFAULT_IS_BOOT_MEMORY, MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT,
      MemoryRegion.DEFAULT_IS_CACHEABLE, MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT);
  private final RamRegion ramRegion = new RamRegion(0x20000000, 1024,
      MemoryRegion.DEFAULT_BLOCK_SIZE,
      "ram", MemoryRegion.DEFAULT_IS_BOOT_MEMORY, MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT,
      MemoryRegion.DEFAULT_IS_CACHEABLE, MemoryRegion.DEFAULT_INVALIDATE_CACHE_ON_RUN);
  private final RamRegion ram2Region = new RamRegion(0x20000400, 1024,
      MemoryRegion.DEFAULT_BLOCK_SIZE,
      "ram2", MemoryRegion.DEFAULT_IS_BOOT_MEMORY, MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT, false,
      MemoryRegion.DEFAULT_INVALIDATE_CACHE_ON_RUN);

  private List<List<Object>> regions = new ArrayList<List<Object>>();
  private MemoryMap memoryMap;
  private boolean hasFpu = true;

  private byte[] ram = new byte[1024];
  private byte[] ram2 = new byte[1024];
  private byte[] flash = new byte[1024];
  private long[] regs = new long[(int) CortexMRegister.S31.getValue()];

  public MockCore() {

    List<MemoryRegion> memoryRegions = new ArrayList<MemoryRegion>();
    memoryRegions.add(this.flashRegion);
    memoryRegions.add(this.ramRegion);
    memoryRegions.add(this.ram2Region);
    MemoryMap memoryMap = new MemoryMap(memoryRegions);

    // Set initial flash value ("erased flash").
    for (byte b : this.flash) {
      b = (byte) 0xFF;
    }

    List<Object> regionFlash = new ArrayList<Object>();
    regionFlash.add(this.flashRegion);
    regionFlash.add(this.flash);

    List<Object> regionRam = new ArrayList<Object>();
    regionRam.add(this.ramRegion);
    regionRam.add(this.ram);

    List<Object> regionRam2 = new ArrayList<Object>();
    regionRam2.add(this.ram2Region);
    regionRam2.add(this.ram2);

    regions.add(regionFlash);
    regions.add(regionRam);
    regions.add(regionRam2);

    clearAllRegs();
  }

  public void clearAllRegs() {
    // r0-15, xpsr, msp, psp
    for (long reg : this.regs) {
      reg = 0x00;
    }
    this.regs[(int) CortexMRegister.CFBP.getValue()] = 0;
  }

  public boolean isRunning() {
    return false;
  }

  public long[] readCoreRegistersRaw(List<CoreRegister> regList) {
    long[] results = new long[regList.size()];

    for (int i = 0; i < regList.size(); i++) {
      CoreRegister reg = regList.get(i);
      long value;
      if (reg.getValue() < 0 && reg.getValue() >= -4) {
        value = this.regs[(int) CortexMRegister.CFBP.getValue()];
        value = (value >> ((-reg.getValue() - 1) * 8)) & 0xFFL;
      } else {
        value = this.regs[(int) reg.getValue()];
      }
      results[i] = value;
    }

    return results;
  }

  public void writeCoreRegistersRaw(List<CoreRegister> regList, long[] words) {
    long[] results = new long[regList.size()];

    for (int i = 0; i < regList.size(); i++) {
      CoreRegister reg = regList.get(i);
      long word = words[i];

      if (reg.getValue() < 0 && reg.getValue() >= -4) {
        long shift = (-reg.getValue() - 1) * 8;
        long mask = 0xFFFFFFFFL ^ (0xFFL << shift);
        word = (this.regs[(int) CortexMRegister.CFBP.getValue()] & mask) | ((word & 0xFFL) << shift);
        this.regs[(int) CortexMRegister.CFBP.getValue()] = word;
      } else {
        this.regs[(int) reg.getValue()] = word;
      }
    }
  }

  public long readMemoryNow(long address, Integer transferSize) {
    if (transferSize == null) {
      transferSize = 32;
    }

    if (transferSize == 8) {
      return 0x12L;
    }

    if (transferSize == 16) {
      return 0x1234L;
    }

    if (transferSize == 32) {
      return 0x12345678L;
    }

    return 0;
  }

  public byte[] readBlockMemoryUnaligned8(long address, int size) {
    for (int i  = 0; i < this.regions.size(); i++) {
      MemoryRegion region = (MemoryRegion) regions.get(i).get(0);
      byte[] memory = (byte[]) regions.get(i).get(1);

      if (region.containsRange(address, null, size, null)) {
        address -= region.start;
        return Util.getSubArray(memory, (int) address, (int) (address + size));
      }
    }

    byte[] ret = new byte[size];
    for (byte r : ret) {
      r = (byte) 0x55;
    }
    return ret;
  }

  public long[] readBlockMemoryAligned32(long address, int size) {
    return Conversion.byteListToU32leList(this.readBlockMemoryUnaligned8(address, size * 4));
  }

  public void writeMemory(long address, long value, Integer transferSize) {
    if (transferSize == null) {
      transferSize = 32;
    }
  }

  public void writeBlockMemoryUnaligned8(long address, byte[] data) {
    for (int i  = 0; i < this.regions.size(); i++) {
      MemoryRegion region = (MemoryRegion) regions.get(i).get(0);
      byte[] memory = (byte[]) regions.get(i).get(1);

      if (region.containsRange(address, null, data.length, null)) {
        address -= region.start;

        int dataIndex = 0;
        int memoryIndex = (int) address;
        while (dataIndex < data.length) {
          memory[memoryIndex] = data[dataIndex];

          dataIndex++;
          memoryIndex++;
        }
      }
    }
  }

  public void writeBlockMemoryAligned32(long address, long[] data) {
    this.writeBlockMemoryUnaligned8(address, Conversion.u32leListToByteList(data));
  }

}
