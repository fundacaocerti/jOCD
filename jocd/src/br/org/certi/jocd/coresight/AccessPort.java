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

import br.org.certi.jocd.coresight.DebugPort.AP_REG;
import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.dapaccess.Transfer;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class AccessPort {

  public static final long AP_ROM_TABLE_ADDR_REG = 0xF8L;
  public static final long AP_ROM_TABLE_FORMAT_MASK = 0x2L;
  public static final long AP_ROM_TABLE_ENTRY_PRESENT_MASK = 0x1L;


  // Map from PIDR to component name (eventually class).
  public static final Map<Long, Long> MEM_AP_IDR_TO_WRAP_SIZE;

  static {
    MEM_AP_IDR_TO_WRAP_SIZE = new HashMap<Long, Long>();
    MEM_AP_IDR_TO_WRAP_SIZE.put(0x24770011L,
        0x1000L);// Used on m4 & m3 - Documented in arm_cortexm4_processor_trm_100166_0001_00_en.pdf and arm_cortexm3_processor_trm_100165_0201_00_en.pdf
    MEM_AP_IDR_TO_WRAP_SIZE
        .put(0x44770001L, 0x400L);// Used on m1 - Documented in DDI0413D_cortexm1_r1p0_trm.pdf
    MEM_AP_IDR_TO_WRAP_SIZE.put(0x04770031L, 0x400L);// Used on m0+? at least on KL25Z, KL46, LPC812
    MEM_AP_IDR_TO_WRAP_SIZE.put(0x04770021L, 0x400L);// Used on m0? used on nrf51, lpc11u24
    MEM_AP_IDR_TO_WRAP_SIZE.put(0x64770001L, 0x400L);// Used on m7
    MEM_AP_IDR_TO_WRAP_SIZE.put(0x74770001L, 0x400L);// Used on m0+ on KL28Z
  }

  // AP Control and Status Word definitions
  public static final long CSW_SIZE = 0x00000007L;
  public static final long CSW_SIZE8 = 0x00000000L;
  public static final long CSW_SIZE16 = 0x00000001L;
  public static final long CSW_SIZE32 = 0x00000002L;
  public static final long CSW_ADDRINC = 0x00000030L;
  public static final long CSW_NADDRINC = 0x00000000L;
  public static final long CSW_SADDRINC = 0x00000010L;
  public static final long CSW_PADDRINC = 0x00000020L;
  public static final long CSW_DBGSTAT = 0x00000040L;
  public static final long CSW_TINPROG = 0x00000080L;
  public static final long CSW_HPROT = 0x02000000L;
  public static final long CSW_MSTRTYPE = 0x20000000L;
  public static final long CSW_MSTRCORE = 0x00000000L;
  public static final long CSW_MSTRDBG = 0x20000000L;
  public static final long CSW_RESERVED = 0x01000000L;

  public static final long CSW_VALUE = (CSW_RESERVED | CSW_MSTRDBG | CSW_HPROT | CSW_DBGSTAT
      | CSW_SADDRINC);

  public static final Map<Integer, Long> TRANSFER_SIZE;

  static {
    TRANSFER_SIZE = new HashMap<Integer, Long>();
    TRANSFER_SIZE.put(8, CSW_SIZE8);
    TRANSFER_SIZE.put(16, CSW_SIZE16);
    TRANSFER_SIZE.put(32, CSW_SIZE32);
  }

  // Debug Exception and Monitor Control Register
  public static final long DEMCR = 0xE000EDFCL;
  // DWTENA in armv6 architecture reference manual
  public static final long DEMCR_TRCENA = (0x1L << 24);

  public DebugPort dp;
  public int apNum;
  public DapAccessCmsisDap link;
  public long idr = 0;
  public long romAddr = 0;
  public boolean hasRomTable = false;
  public RomTable romTable;
  public boolean initedPrimary = false;
  public boolean initedSecondary = false;

  public long getRomAddr() {
    return romAddr;
  }

  public AccessPort(DebugPort dp, int apNum) {
    this.dp = dp;
    this.apNum = apNum;
    this.link = dp.getLink();
  }

  public void init(Boolean busAccessible) throws TimeoutException, Error {
    if (!this.initedPrimary) {
      this.idr = this.readRegNow(AP_REG.IDR.getValue());

      // Init ROM table
      this.romAddr = this.readRegNow(AP_ROM_TABLE_ADDR_REG);
      this.hasRomTable =
          (this.romAddr != 0xFFFFFFFFL) && ((this.romAddr & AP_ROM_TABLE_ENTRY_PRESENT_MASK) != 0);
      // Clear format and present bits
      this.romAddr &= 0xFFFFFFFCL;
      this.initedPrimary = true;
    }

    if (!this.initedSecondary && this.hasRomTable && busAccessible) {
      this.initRomTable();
      this.initedSecondary = true;
    }
  }

  public void initRomTable() throws TimeoutException, Error {
    this.romTable = new RomTable(this);
    this.romTable.init();
  }

  public long readRegNow(long addr) throws TimeoutException, Error {
    return this.dp.readAPNow(((this.apNum << DebugPort.APSEL_SHIFT) | addr));
  }

  public ArrayList<Object> readRegLater(long addr) throws TimeoutException, Error {
    return this.dp.readAP(((this.apNum << DebugPort.APSEL_SHIFT) | addr));
  }

  public void writeReg(long addr, Long data) throws TimeoutException, Error {
    this.dp.writeAP((this.apNum << DebugPort.APSEL_SHIFT) | addr, data);
  }

  public void writeMemory(long addr, long data) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void writeMemory(long addr, long data, Integer transferSize)
      throws Error, TimeoutException {
    throw new InternalError("Not implemented");
  }

  public long readMemory(long address, Integer transferSize) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public long readMemoryNow(long addr, Integer transferSize) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public ArrayList<Object> readMemoryLater(long addr, Integer transferSize)
      throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public long readMemoryAsync(Transfer transfer, int numDp, long addr, Integer transferSize,
      int num)
      throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void writeBlock32(long addr, long[] word) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public long[] readBlock32(long addr, int size) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void write32(long addr, long value) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void write16(long addr, long value) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void write8(long addr, long value) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public long read32(long addr) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public int read16(long addr) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public byte read8(long addr) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public byte[] readBlockMemoryUnaligned8(long addr, int size) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void writeBlockMemoryUnaligned8(long addr, byte[] data) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void writeBlockMemoryAligned32(long addr, long[] data) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public long[] readBlockMemoryAligned32(long addr, int size) throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void handleError(Error error, int num) throws Error, TimeoutException {
    throw new InternalError("Not implemented");
  }
}
