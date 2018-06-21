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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import br.org.certi.jocd.core.MemoryMap;
import br.org.certi.jocd.core.MemoryMap.RegionType;
import org.junit.Test;

public class TestMemoryRegion extends TestMemoryMap {

  @Test
  public void testFlashAttrs() {
    assertEquals(flash.start, 0L);
    assertEquals(flash.end, 0x3FFL);
    assertEquals(flash.length, 0x400);
    assertEquals(flash.blockSize, 0x100);
    assertEquals(flash.name, "flash");
    assertEquals(flash.regionType, MemoryMap.RegionType.Flash);
    assertNotEquals(flash.regionType, RegionType.Ram);
    assertNotEquals(flash.regionType, RegionType.Rom);
    assertTrue(flash.isBootMemory);
    assertTrue(flash.isCacheable);
    assertTrue(flash.isPoweredOnBoot);
  }

  @Test
  public void testRomAttrs() {
    assertEquals(rom.start, 0x1C000000L);
    assertEquals(rom.end, 0x1C003FFFL);
    assertEquals(rom.length, 0x4000);
    assertEquals(rom.blockSize, 0);
    assertEquals(rom.name, "rom");
    assertNotEquals(rom.regionType, MemoryMap.RegionType.Flash);
    assertNotEquals(rom.regionType, RegionType.Ram);
    assertEquals(rom.regionType, RegionType.Rom);
    assertTrue(!rom.isBootMemory);
    assertTrue(rom.isCacheable);
    assertTrue(rom.isPoweredOnBoot);
  }

  @Test
  public void testRam1Attrs() {
    assertEquals(ram1.start, 0x20000000L);
    assertEquals(ram1.end, 0x200003FFL);
    assertEquals(ram1.length, 0x400);
    assertEquals(ram1.blockSize, 0);
    assertEquals(ram1.name, "ram1");
    assertNotEquals(ram1.regionType, MemoryMap.RegionType.Flash);
    assertEquals(ram1.regionType, RegionType.Ram);
    assertNotEquals(ram1.regionType, RegionType.Rom);
    assertTrue(!ram1.isBootMemory);
    assertTrue(ram1.isCacheable);
    assertTrue(ram1.isPoweredOnBoot);
  }

  @Test
  public void testRam2Attrs() {
    assertEquals(ram2.start, 0x20000400L);
    assertEquals(ram2.end, 0x200007FFL);
    assertEquals(ram2.length, 0x400);
    assertEquals(ram2.blockSize, 0);
    assertEquals(ram2.name, "ram2");
    assertNotEquals(ram2.regionType, MemoryMap.RegionType.Flash);
    assertEquals(ram2.regionType, RegionType.Ram);
    assertNotEquals(ram2.regionType, RegionType.Rom);
    assertTrue(!ram2.isBootMemory);
    assertTrue(!ram2.isCacheable);
    assertTrue(ram2.isPoweredOnBoot);
  }

  @Test
  public void testFlashRange() {
    assertTrue(flash.containsAddress(0L));
    assertTrue(flash.containsAddress(0x3FFL));
    assertTrue(!flash.containsAddress(0x400L));
    assertTrue(flash.containsRange(0L, null, 0x400, null));
    assertTrue(flash.containsRange(0L, 0x3FFL, null, null));
    assertTrue(flash.containsRange(0x100L, null, 0x100, null));
    assertTrue(!flash.containsRange(0x300L, 0x720L, null, null));
  }
}
