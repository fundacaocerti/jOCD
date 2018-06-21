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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.org.certi.jocd.core.FlashRegion;
import br.org.certi.jocd.core.MemoryMap;
import br.org.certi.jocd.core.MemoryRegion;
import br.org.certi.jocd.core.RamRegion;
import br.org.certi.jocd.core.RomRegion;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TestMemoryMap {

  public FlashRegion flash = new FlashRegion(0, 1 * 1024, 0x100, "flash", true,
      MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT, MemoryRegion.DEFAULT_IS_CACHEABLE,
      MemoryRegion.DEFAULT_INVALIDATE_CACHE_ON_RUN);

  public RomRegion rom = new RomRegion(0x1c000000L, 16 * 1024, "rom",
      MemoryRegion.DEFAULT_IS_BOOT_MEMORY, MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT,
      MemoryRegion.DEFAULT_IS_CACHEABLE, MemoryRegion.DEFAULT_INVALIDATE_CACHE_ON_RUN);

  public RamRegion ram1 = new RamRegion(0x20000000L, 1 * 1024, MemoryRegion.DEFAULT_BLOCK_SIZE,
      "ram1", MemoryRegion.DEFAULT_IS_BOOT_MEMORY, MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT,
      MemoryRegion.DEFAULT_IS_CACHEABLE, MemoryRegion.DEFAULT_INVALIDATE_CACHE_ON_RUN);

  public RamRegion ram2 = new RamRegion(0x20000400L, 1 * 1024, MemoryRegion.DEFAULT_BLOCK_SIZE,
      "ram2", MemoryRegion.DEFAULT_IS_BOOT_MEMORY, MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT, false,
      MemoryRegion.DEFAULT_INVALIDATE_CACHE_ON_RUN);

  public MemoryMap memmap() {
    List<MemoryRegion> regions = new ArrayList<MemoryRegion>();
    regions.add(flash);
    regions.add(rom);
    regions.add(ram1);
    regions.add(ram2);
    return new MemoryMap(regions);
  }

  @Test
  public void testEmptyMap() {
    MemoryMap mmap = new MemoryMap(null);
    assertEquals(mmap.memoryRegions.size(), 0);
    assertEquals(mmap.getBootMemory(), null);
    assertEquals(mmap.getRegionForAddress(0x1000L), null);
  }

  @Test
  public void testRegions() {
    List<MemoryRegion> memoryRegions = memmap().memoryRegions;

    assertEquals(memoryRegions.size(), 4);

    // Sorted order.
    assertTrue((memoryRegions.get(0).start < memoryRegions.get(1).start) &
        (memoryRegions.get(1).start < memoryRegions.get(2).start) &
        (memoryRegions.get(2).start < memoryRegions.get(3).start));
  }

  @Test
  public void testBootMemory() {
    MemoryRegion bootMemory = memmap().getBootMemory();

    assertTrue(bootMemory != null);
    assertEquals(bootMemory.name, "flash");
    assertEquals(bootMemory.start, 0L);
    assertEquals(bootMemory.end, 0x3FFL);
    assertTrue(bootMemory.isBootMemory);
  }

  @Test
  public void testRegionForAddress() {
    MemoryMap mmap = memmap();

    assertEquals(mmap.getRegionForAddress(0L).name, "flash");
    assertEquals(mmap.getRegionForAddress(0x20000000L).name, "ram1");
    assertEquals(mmap.getRegionForAddress(0x20000500L).name, "ram2");
  }

  @Test
  public void testX() {
    RamRegion ramRegion = new RamRegion(0x1FFFA000L, 0x18000L, MemoryRegion.DEFAULT_BLOCK_SIZE,
        "core0 ram", MemoryRegion.DEFAULT_IS_BOOT_MEMORY, MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT,
        true, MemoryRegion.DEFAULT_INVALIDATE_CACHE_ON_RUN);

    assertTrue(ramRegion.containsRange(0x1FFFC9F8L, 0x1FFFC9FCL, null, null));
  }
}
