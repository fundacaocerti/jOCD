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

import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.util.Mask;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RomTable extends CoreSightComponent {

  // Logging
  private final static String CLASS_NAME = RomTable.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private RomTable parent;
  private long number;
  private long entrySize = 0;
  private ArrayList<CoreSightComponent> components = new ArrayList<CoreSightComponent>();

  public RomTable(AccessPort ap) {
    this(ap, null, null);
  }

  public RomTable(AccessPort ap, long addr) {
    this(ap, addr, null);
  }

  public RomTable(AccessPort ap, RomTable parentTable) {
    this(ap, null, parentTable);
  }

  public RomTable(AccessPort ap, Long topAddress, RomTable parentTable) {
    // If no table address is provided, use the root ROM table for the AP.
    if (topAddress == null) {
      topAddress = ap.getRomAddr();
    }
    super.setup(ap, topAddress);
    this.parent = parentTable;
    this.number = this.parent != null ? (this.parent.number + 1) : 0;
  }

  public void init() throws TimeoutException, Error {
    this.readIdRegisters();
    if (!this.isRomTable) {
      LOGGER.log(Level.WARNING, String
          .format("Warning: ROM table @ 0x%08x has unexpected CIDR component class (0x%x)",
              this.address, this.componentClass));
      return;
    }
    if (this.count4kb != 1) {
      LOGGER.log(Level.WARNING, String
          .format("Warning: ROM table @ 0x%08x is larger than 4kB (%d 4kb pages)", this.address,
              this.count4kb));
    }
    this.readTable();
  }

  public void readTable() throws TimeoutException, Error {
    LOGGER.log(Level.INFO, String
        .format("ROM table #%d @ 0x%08x cidr=%x pidr=%x", this.number, this.address, this.cidr,
            this.pidr));
    this.components = new ArrayList<CoreSightComponent>();

    // Switch to the 8-bit table entry reader if we already know the entry size.
    if (this.entrySize == 8) {
      this.readTable8();
    }

    long entryAddress = this.address;
    boolean foundEnd = false;
    int entriesRead = 0;
    while (!foundEnd && entriesRead < ROM_TABLE_MAX_ENTRIES) {
      // Read several entries at a time for performance.
      int readCount = Math.min(ROM_TABLE_MAX_ENTRIES - entriesRead, ROM_TABLE_ENTRY_READ_COUNT);
      long[] entries = this.ap.readBlockMemoryAligned32(entryAddress, readCount);
      entriesRead += readCount;

      // Determine entry size if unknown.
      if (this.entrySize == 0) {
        if ((entries[0] & ROM_TABLE_32BIT_MASK) != 0) {
          this.entrySize = 32;
        } else {
          this.entrySize = 8;
        }
        if (this.entrySize == 8) {
          // Read 8-bit table.
          this.readTable8();
          return;
        }
      }

      for (long entry : entries) {
        // Zero entry indicates the end of the table.
        if (entry == 0) {
          foundEnd = true;
          break;
        }
        this.handleTableEntry(entry);

        entryAddress += 4;
      }
    }
  }

  public void readTable8() throws TimeoutException, Error {
    long entryAddress = this.address;

    while (true) {
      // Read the full 32-bit table entry spread across four bytes.
      long entry = this.ap.read8(entryAddress);
      entry |= this.ap.read8(entryAddress + 4) << 8;
      entry |= this.ap.read8(entryAddress + 8) << 16;
      entry |= this.ap.read8(entryAddress + 12) << 24;

      // Zero entry indicates the end of the table.
      if (entry == 0) {
        break;
      }
      this.handleTableEntry(entry);

      entryAddress += 16;
    }
  }

  public void handleTableEntry(long entry) throws TimeoutException, Error {
    // Nonzero entries can still be disabled, so check the present bit before handling.
    if ((entry & ROM_TABLE_ENTRY_PRESENT_MASK) == 0) {
      return;
    }

    // Get the component's top 4k address.
    long offset = entry & ROM_TABLE_ADDR_OFFSET_MASK;
    long address = this.address + offset;

    // Create component instance.
    CoreSightComponent cmp = new CoreSightComponent();
    cmp.setup(this.ap, address);
    cmp.readIdRegisters();

    LOGGER.log(Level.INFO, String.format("[%d]%s", this.components.size(), cmp.toString()));

    // Recurse into child ROM tables.
    if (cmp.isRomTable) {
      RomTable romTableCmp = new RomTable(this.ap, address, this);
      romTableCmp.init();
    }
    this.components.add(cmp);
  }
}