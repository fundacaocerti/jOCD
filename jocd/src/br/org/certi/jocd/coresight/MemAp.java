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
import br.org.certi.jocd.dapaccess.Transfer;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferFaultError;
import br.org.certi.jocd.util.Conversion;
import br.org.certi.jocd.util.Mask;
import br.org.certi.jocd.util.Util;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MemAp extends AccessPort {

  // Logging
  private final static String CLASS_NAME = MemAp.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  // Default to the smallest size supported by all targets.
  // A size smaller than the supported size will decrease performance due to
  // the extra address writes, but will not create any read/write errors.
  private Long autoIncrementPageSize = 0x400L;

  public MemAp(DebugPort dp, int apNum) {
    super(dp, apNum);
  }

  @Override
  public void init(Boolean busAccessible) throws TimeoutException, Error {
    super.init(busAccessible);

    // Look up the page size based on AP ID.
    this.autoIncrementPageSize = MEM_AP_IDR_TO_WRAP_SIZE.get(this.idr);
    if (this.autoIncrementPageSize == null) {
      LOGGER.log(Level.WARNING, String.format("Unknown MEM-AP IDR: 0x%x", this.idr));
    }
  }

  @Override
  public void writeMemory(long addr, long data) throws TimeoutException, Error {
    this.writeMemory(addr, data, 32);
  }

  /*
   * Write a single memory location.
   * By default the transfer size is a word
   */
  @Override
  public void writeMemory(long addr, long data, Integer transferSize)
      throws Error, TimeoutException {
    // Set default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }
    int num = this.dp.nextAccessNumber();
    LOGGER.log(Level.INFO, String
        .format("writeMem:%06d (addr=0x%08x, size=%d) = 0x%08x {", num, addr, transferSize, data));
    this.writeReg(AP_REG.CSW.getValue(), CSW_VALUE | TRANSFER_SIZE.get(transferSize));
    if (transferSize == 8) {
      data = data << ((addr & 0x03) << 3);
    } else if (transferSize == 16) {
      data = data << ((addr & 0x02) << 3);
    }

    try {
      this.writeReg(AP_REG.TAR.getValue(), addr);
      this.writeReg(AP_REG.DRW.getValue(), data);
    } catch (TransferFaultError error) {
      // Annotate error with target address.
      this.handleError(error, num);
      error.setFaultAddress(addr);
      throw error;
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
    LOGGER.log(Level.INFO, String.format("writeMem:%06d }", num));
  }

  @Override
  public long readMemory(long address, Integer transferSize) throws TimeoutException, Error {
    return this.readMemoryNow(address, transferSize);
  }

  /*
   * Read a memory location.
   * By default, a word will be read.
   */
  public long readMemoryNow(long addr, Integer transferSize) throws TimeoutException, Error {
    // Set default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }
    ArrayList<Object> result = this.readMemoryLater(addr, transferSize);
    Transfer transfer = (Transfer) result.get(0);
    int numDp = (int) result.get(1);
    int num = (int) result.get(2);
    return readMemoryAsync(transfer, numDp, addr, transferSize, num);
  }

  /*
   * Read a memory location.
   * By default, a word will be read.
   */
  @Override
  public ArrayList<Object> readMemoryLater(long addr, Integer transferSize)
      throws TimeoutException, Error {
    // Set default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }
    int num = this.dp.nextAccessNumber();
    LOGGER.log(Level.INFO,
        String.format("readMem:%06d (addr=0x%08x, size=%d) {", num, addr, transferSize));

    ArrayList<Object> result = new ArrayList<>();
    try {
      this.writeReg(AP_REG.CSW.getValue(), CSW_VALUE | TRANSFER_SIZE.get(transferSize));
      this.writeReg(AP_REG.TAR.getValue(), addr);
      result = this.readRegLater(AP_REG.DRW.getValue());

    } catch (TransferFaultError error) {
      // Annotate error with target address.
      this.handleError(error, num);
      error.setFaultAddress(addr);
      throw error;
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
    result.add(num);
    return result;
  }

  @Override
  public long readMemoryAsync(Transfer transfer, int numDp, long addr, Integer transferSize,
      int num) throws TimeoutException, Error {
    long res = 0;
    try {
      res = this.dp.readAPAsync(transfer, numDp);
      if (transferSize == 8) {
        res = (res >> ((addr & 0x03L) << 3) & 0xFFL);
      } else if (transferSize == 16) {
        res = (res >> ((addr & 0x02L) << 3) & 0xFFFFL);
      }
    } catch (TransferFaultError error) {
      // Annotate error with target address.
      this.handleError(error, num);
      error.setFaultAddress(addr);
      throw error;
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
    return res;
  }

  @Override
  public void writeBlock32(long addr, long[] word) throws TimeoutException, Error {
    int num = this.dp.nextAccessNumber();
    LOGGER.log(Level.INFO,
        String.format("writeBlock32:%06d (addr=0x%08x, size=%d) {", num, addr, word.length));

    // Put address in TAR
    this.writeReg(AP_REG.CSW.getValue(), CSW_VALUE | CSW_SIZE32);
    this.writeReg(AP_REG.TAR.getValue(), addr);

    try {
      long reg = DebugPort.apAddrToReg(
          (this.apNum << DebugPort.APSEL_SHIFT) | DebugPort.WRITE | DebugPort.AP_ACC | AP_REG.DRW
              .getValue());
      this.link.regWriteRepeat(word.length, reg, word, null);
    } catch (TransferFaultError error) {
      // Annotate error with target address.
      this.handleError(error, num);
      error.setFaultAddress(addr);
      throw error;
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
    LOGGER.log(Level.INFO, String.format("writeBlock32:%06d }", num));
  }

  /*
   * Read aligned word (the size is in words)
   */
  @Override
  public long[] readBlock32(long addr, int size) throws TimeoutException, Error {
    int num = this.dp.nextAccessNumber();
    LOGGER.log(Level.INFO,
        String.format("_readBlock32:%06d (addr=0x%08x, size=%d) {", num, addr, size));
    long[] resp;

    // Put address in TAR
    this.writeReg(AP_REG.CSW.getValue(), CSW_VALUE | CSW_SIZE32);
    this.writeReg(AP_REG.TAR.getValue(), addr);
    try {
      long reg = DebugPort.apAddrToReg(
          (this.apNum << DebugPort.APSEL_SHIFT) | DebugPort.READ | DebugPort.AP_ACC | AP_REG.DRW
              .getValue());
      resp = this.link.regReadRepeat(size, reg, null);
    } catch (TransferFaultError error) {
      // Annotate error with target address.
      this.handleError(error, num);
      error.setFaultAddress(addr);
      throw error;
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
    LOGGER.log(Level.INFO, String.format("readBlock32:%06d }", num));

    return resp;
  }

  /*
   * Shorthand to write a 32-bit word.
   */
  @Override
  public void write32(long addr, long value) throws TimeoutException, Error {
    this.writeMemory(addr, value, 32);
  }

  /*
   * Shorthand to write a 16-bit halfword.
   */
  @Override
  public void write16(long addr, long value) throws TimeoutException, Error {
    this.writeMemory(addr, value, 16);
  }

  /*
   * Shorthand to write a byte.
   */
  @Override
  public void write8(long addr, long value) throws TimeoutException, Error {
    this.writeMemory(addr, value, 8);
  }

  /*
   * Shorthand to read a 32-bit word.
   */
  @Override
  public long read32(long addr) throws TimeoutException, Error {
    return this.readMemory(addr, 32);
  }

  /*
   * Shorthand to read a 16-bit halfword.
   */
  @Override
  public int read16(long addr) throws TimeoutException, Error {
    return (int) this.readMemory(addr, 16);
  }

  /*
   * Shorthand to read a byte.
   */
  @Override
  public byte read8(long addr) throws TimeoutException, Error {
    return (byte) this.readMemory(addr, 8);
  }

  /*
   * Read a block of unaligned bytes in memory.
   * @return an array of byte values
   */
  @Override
  public byte[] readBlockMemoryUnaligned8(long addr, int size) throws TimeoutException, Error {
    byte[] res = new byte[0];

    // Try to read 8bits data
    if ((size > 0) && (addr & 0x01) != 0) {
      byte mem = this.read8(addr);
      res = Util.appendDataInArray(res, mem);
      size -= 1;
      addr += 1;
    }

    // Try to read 16bits data
    if ((size > 1) && (addr & 0x02) != 0) {
      int mem = this.read16(addr);
      res = Util.appendDataInArray(res, (byte) (mem & 0xFF));
      res = Util.appendDataInArray(res, (byte) ((mem >> 8) & 0xFF));
      size -= 2;
      addr += 2;
    }

    // Try to read aligned block of 32bits
    if (size >= 4) {
      long[] mem = this.readBlockMemoryAligned32(addr, size / 4);
      res = Util.appendDataInArray(res, Conversion.u32leListToByteList(mem));
      size -= 4 * mem.length;
      addr += 4 * mem.length;
    }

    if (size > 1) {
      int mem = this.read16(addr);
      res = Util.appendDataInArray(res, (byte) (mem & 0xFF));
      res = Util.appendDataInArray(res, (byte) ((mem >> 8) & 0xFF));
      size -= 2;
      addr += 2;
    }

    if (size > 0) {
      byte mem = this.read8(addr);
      res = Util.appendDataInArray(res, mem);
      size -= 1;
      addr += 1;
    }

    return res;
  }

  @Override
  public void writeBlockMemoryUnaligned8(long addr, byte[] data) throws TimeoutException, Error {
    int size = data.length;
    int idx = 0;

    // Try to write 8 bits data
    if ((size > 0) && (addr & 0x01) != 0) {
      this.writeMemory(addr, data[idx], 8);
      size -= 1;
      addr += 1;
      idx += 1;
    }

    // Try to write 16 bits data
    if ((size > 1) && (addr & 0x02) != 0) {
      this.writeMemory(addr, data[idx] | (data[idx + 1] << 8), 16);
      size -= 2;
      addr += 2;
      idx += 2;
    }

    // Write aligned block of 32 bits
    if (size >= 4) {
      long[] data32 = Conversion
          .byteListToU32leList(
              Util.getSubArray(data, idx, idx + (int) (size & Mask.invert32(0x03))));
      this.writeBlockMemoryAligned32(addr, data32);
      addr += size & Mask.invert32(0x03);
      idx += size & Mask.invert32(0x03);
      size -= size & Mask.invert32(0x03);
    }

    // Try to write 16 bits data
    if (size > 1) {
      this.writeMemory(addr, data[idx] | (data[idx + 1] << 8), 16);
      size -= 2;
      addr += 2;
      idx += 2;
    }

    // Try to write 8 bits data
    if (size > 0) {
      this.writeMemory(addr, data[idx], 8);
      size -= 1;
      addr += 1;
      idx += 1;
    }
  }

  /*
   * Write a block of aligned words in memory.
   */
  @Override
  public void writeBlockMemoryAligned32(long addr, long[] data) throws TimeoutException, Error {
    int size = data.length;
    while (size > 0) {
      long n = this.autoIncrementPageSize - (addr & (this.autoIncrementPageSize - 1));
      if (size * 4 < n) {
        n = (size * 4) & 0xFFFFFFFCL;
      }
      this.writeBlock32(addr, Util.getSubArray(data, 0, (int) (n / 4)));
      Util.getSubArray(data, (int) (n / 4), null);
      size -= n / 4;
      addr += n;
    }
  }

  /*
   * Read a block of aligned words in memory.
   * @return An array of word values
   */
  @Override
  public long[] readBlockMemoryAligned32(long addr, int size) throws TimeoutException, Error {
    long[] resp = new long[0];
    while (size > 0) {
      long n = this.autoIncrementPageSize - (addr & (this.autoIncrementPageSize - 1));
      if (size * 4 < n) {
        n = (size * 4) & 0xFFFFFFFCL;
      }
      resp = Util.appendDataInArray(resp, this.readBlock32(addr, (int) n / 4));
      size -= n / 4;
      addr += n;
    }
    return resp;
  }

  @Override
  public void handleError(Error error, int num) throws Error, TimeoutException {
    this.dp.handleError(error, num);
  }
}
