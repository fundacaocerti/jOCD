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
package br.org.certi.jocd.coresight;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoreSightComponent {

  // Logging
  private final static String CLASS_NAME = CoreSightComponent.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  public static final long PIDR4 = 0xFD0;
  public static final long PIDR0 = 0xFE0;
  public static final long CIDR0 = 0xFF0;
  public static final long DEVTYPE = 0xFCC;
  public static final long DEVID = 0xFC8;
  public static final int IDR_COUNT = 12;
  public static final int PIDR4_OFFSET = 0;
  public static final int PIDR0_OFFSET = 4;
  public static final int CIDR0_OFFSET = 8;

  public static final long CIDR_PREAMBLE_MASK = 0xFFFF0FFF;
  public static final long CIDR_PREAMBLE_VALUE = 0xB105000D;

  public static final long CIDR_COMPONENT_CLASS_MASK = 0xF000;
  public static final long CIDR_COMPONENT_CLASS_SHIFT = 12;

  public static final long CIDR_ROM_TABLE_CLASS = 0x1;
  public static final long CIDR_CORESIGHT_CLASS = 0x9;

  public static final long PIDR_4KB_COUNT_MASK = 0xF000000000L;
  public static final long PIDR_4KB_COUNT_SHIFT = 36;

  public static final long ROM_TABLE_ENTRY_PRESENT_MASK = 0x1;

  // Mask for ROM table entry size. 1 if 32-bit, 0 if 8-bit.
  public static final long ROM_TABLE_32BIT_MASK = 0x2;

  // 2's complement offset to debug component from ROM table base address.
  public static final long ROM_TABLE_ADDR_OFFSET_NEG_MASK = 0x80000000;
  public static final long ROM_TABLE_ADDR_OFFSET_MASK = 0xfffff000;
  public static final long ROM_TABLE_ADDR_OFFSET_SHIFT = 12;

  // 9 entries is enough entries to cover the standard Cortex-M4 ROM table for devices with ETM.
  public static final int ROM_TABLE_ENTRY_READ_COUNT = 9;
  public static final int ROM_TABLE_MAX_ENTRIES = 960;

  // Map from PIDR to component name (eventually class).
  public static final Map<Long, String> PID_TABLE;

  static {
    PID_TABLE = new HashMap<Long, String>();
    PID_TABLE.put(0x4001bb932L, "MTB-M0+");
    PID_TABLE.put(0x00008e000L, "MTBDWT");
    PID_TABLE.put(0x4000bb9a6L, "CTI");
    PID_TABLE.put(0x4000bb4c0L, "ROM");
    PID_TABLE.put(0x4000bb008L, "SCS-M0+");
    PID_TABLE.put(0x4000bb00aL, "DWT-M0+");
    PID_TABLE.put(0x4000bb00bL, "BPU");
    PID_TABLE.put(0x4000bb00cL, "SCS-M4");
    PID_TABLE.put(0x4003bb002L, "DWT");
    PID_TABLE.put(0x4002bb003L, "FPB");
    PID_TABLE.put(0x4003bb001L, "ITM");
    PID_TABLE.put(0x4000bb9a1L, "TPIU-M4");
    PID_TABLE.put(0x4000bb925L, "ETM-M4");
    PID_TABLE.put(0x4003bb907L, "ETB");
    PID_TABLE.put(0x4001bb908L, "CSTF");
    PID_TABLE.put(0x4000bb000L, "SCS-M3");
    PID_TABLE.put(0x4003bb923L, "TPIU-M3");
    PID_TABLE.put(0x4003bb924L, "ETM-M3");
  }

  public AccessPort ap;
  public long address;
  public long topAddress;
  public long componentClass = 0;
  public boolean isRomTable = false;
  public long cidr = 0;
  public long pidr = 0;
  public long devtype = 0;
  public long devid = 0;
  public long count4kb = 0;
  public String name = "";
  public boolean valid = false;

  public CoreSightComponent() {
  }

  /*
   * Must be called right after constructor.
   */
  public void setup(AccessPort ap, long topAddress) {
    this.ap = ap;
    this.address = topAddress;
    this.topAddress = topAddress;
  }

  public void readIdRegisters() throws Exception {
    // Read Component ID and Peripheral ID registers. This is done as a single block
    // read for performance reasons.
    long[] regs = this.ap.readBlockMemoryAligned32(this.topAddress + PIDR4, IDR_COUNT);
    this.cidr = this.extractIdRegisterValue(regs, CIDR0_OFFSET);
    this.pidr = (this.extractIdRegisterValue(regs, PIDR4_OFFSET) << 32) | this
        .extractIdRegisterValue(regs, PIDR0_OFFSET);

    // Check if the component has a valid CIDR value
    if ((this.cidr & CIDR_PREAMBLE_MASK) != CIDR_PREAMBLE_VALUE) {
      LOGGER.log(Level.WARNING, String.format("Invalid coresight component, cidr=0x%x", this.cidr));
      return;
    }

    this.name = PID_TABLE.get(this.pidr);

    long componentClass = (this.cidr & CIDR_COMPONENT_CLASS_MASK) >> CIDR_COMPONENT_CLASS_SHIFT;
    boolean isRomTable = (componentClass == CIDR_ROM_TABLE_CLASS);

    long count4kb = 1 << ((this.pidr & PIDR_4KB_COUNT_MASK) >> PIDR_4KB_COUNT_SHIFT);
    if (count4kb > 1) {
      long address = this.topAddress - (4096 * (count4kb - 1));
    }

    // From section 10.4 of ARM Debug InterfaceArchitecture Specification ADIv5.0 to ADIv5.2
    // In a ROM Table implementation:
    // - The Component class field, CIDR1.CLASS is 0x1, identifying the component as a ROM Table.
    // - The PIDR4.SIZE field must be 0. This is because a ROM Table must occupy a single 4KB block of memory.
    if (isRomTable && count4kb != 1) {
      LOGGER.log(Level.WARNING, String.format("Invalid rom table size=%x * 4KB", count4kb));
      return;
    }

    if (componentClass == CIDR_CORESIGHT_CLASS) {
      long[] result = this.ap.readBlockMemoryAligned32(this.topAddress + DEVID, 2);
      this.devid = result[0];
      this.devtype = result[1];
    }

    this.componentClass = componentClass;
    this.isRomTable = isRomTable;
    this.count4kb = count4kb;
    this.valid = true;
  }

  public long extractIdRegisterValue(long[] regs, int offset) {
    long result = 0;
    for (int i = 0; i < 4; i++) {
      long value = regs[offset + i];
      result |= (value & 0xFF) << (i * 8);
    }
    return result;
  }

  public String toString() {
    if (!this.valid) {
      return String.format("<%08x:%s cidr=%x, pidr=%x, component invalid>", this.address, this.name,
          this.cidr, this.pidr);
    }
    if (this.componentClass == CIDR_CORESIGHT_CLASS) {
      return String
          .format("<%08x:%s cidr=%x, pidr=%x, class=%d, devtype=%x, devid=%x>", this.address,
              this.name, this.cidr, this.pidr, this.componentClass, this.devtype, this.devid);
    } else {
      return String
          .format("<%08x:%s cidr=%x, pidr=%x, class=%d>", this.address, this.name, this.cidr,
              this.pidr, this.componentClass);
    }
  }
}