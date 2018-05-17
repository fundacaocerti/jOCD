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

import br.org.certi.jocd.dapaccess.CmsisDapProtocol;
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.Port;
import br.org.certi.jocd.dapaccess.CmsisDapProtocol.Reg;
import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.dapaccess.Transfer;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferFaultError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DebugPort {

  // Logging
  private final static String CLASS_NAME = DebugPort.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  public static enum DP_REG {
    IDCODE(Reg.DP_0x0.getValue()),
    ABORT(Reg.DP_0x0.getValue()),
    CTRL_STAT(Reg.DP_0x4.getValue()),
    SELECT(Reg.DP_0x8.getValue());

    public final byte value;

    DP_REG(byte id) {
      this.value = id;
    }

    public byte getValue() {
      return value;
    }
  }

  public static enum AP_REG {
    CSW((byte) 0x00),
    TAR((byte) 0x04),
    DRW((byte) 0x0C),
    IDR((byte) 0xFC);

    public final byte value;

    AP_REG(byte id) {
      this.value = id;
    }

    public byte getValue() {
      return value;
    }
  }

  // DP Control / Status Register bit definitions
  private static final long CTRLSTAT_STICKYORUN = 0x00000002;
  private static final long CTRLSTAT_STICKYCMP = 0x00000010;
  private static final long CTRLSTAT_STICKYERR = 0x00000020;

  public static final byte IDCODE = 0 << 2;
  public static final byte AP_ACC = 1 << 0;
  public static final byte DP_ACC = 0 << 0;
  public static final byte READ = 1 << 1;
  public static final byte WRITE = 0 << 1;
  public static final byte VALUE_MATCH = 1 << 4;
  public static final byte MATCH_MASK = 1 << 5;

  public static final byte A32 = (byte) 0x0C;
  public static final int APSEL_SHIFT = 24;
  public static final long APSEL = 0xFF000000;
  public static final long APBANKSEL = 0x000000F0;
  public static final long APREG_MASK = 0x000000FC;

  private static final long DPIDR_MIN_MASK = 0x10000;
  private static final long DPIDR_VERSION_MASK = 0xF000;
  private static final int DPIDR_VERSION_SHIFT = 12;

  private static final long CSYSPWRUPACK = 0x80000000;
  private static final long CDBGPWRUPACK = 0x20000000;
  private static final long CSYSPWRUPREQ = 0x40000000;
  private static final long CDBGPWRUPREQ = 0x10000000;

  private static final long TRNNORMAL = 0x00000000;
  private static final long MASKLANE = 0x00000F00;

  private DapAccessCmsisDap link;
  private HashMap<Long, Long> csw = new HashMap<>();
  private long dpSelect = -1;
  private int accessNumber = 0;
  private long dpidr;
  private long dpVersion;
  private boolean isMindp;

  public DebugPort(DapAccessCmsisDap link) {
    this.link = link;
  }

  public static long apAddrToReg(long addr) {
    CmsisDapProtocol.Reg reg = CmsisDapProtocol.Reg.getReg((4 + ((addr & A32) >> 2)));
    if (reg != null) {
      return reg.getValue();
    } else {
      return Long.parseLong(null);
    }
  }

  public int nextAccessNumber() {
    this.accessNumber += 1;
    return this.accessNumber;
  }

  public DapAccessCmsisDap getLink() {
    return link;
  }

  /*
   * Connect to the target
   */
  public void init() throws TimeoutException, Error {
    this.link.connect();
    this.link.swjSequence();
    this.readIdCode();
    this.clearStickyErr();
  }

  /*
   * Read ID register and get DP version
   */
  public long readIdCode() throws TimeoutException, Error {
    this.dpidr = this.readRegNow(DP_REG.IDCODE.getValue());
    this.dpVersion = ((this.dpidr & DPIDR_VERSION_MASK) >> DPIDR_VERSION_SHIFT);
    this.isMindp = (this.dpidr & DPIDR_MIN_MASK) != 0;
    return this.dpidr;
  }

  public void flush() throws TimeoutException, Error {
    try {
      this.link.flush();
    } catch (Error error) {
      this.handleError(error, this.nextAccessNumber());
      throw error;
    } finally {
      this.csw = new HashMap<>();
      this.dpSelect = -1;
    }
  }

  public long readRegNow(long addr) throws TimeoutException, Error {
    return this.readDPNow(addr);
  }

  public void readRegLater(long addr) throws TimeoutException, Error {
    this.readDPLater(addr);
  }

  public void writeReg(long addr, long word) throws TimeoutException, Error {
    this.writeDP(addr, word);
  }

  public void powerUpDebug() throws TimeoutException, Error {
    // Select bank 0 (to access DRW and TAR)
    this.writeReg(DP_REG.SELECT.getValue(), 0);
    this.writeReg(DP_REG.CTRL_STAT.getValue(), (CSYSPWRUPREQ | CDBGPWRUPREQ));

    while (true) {
      long r = this.readRegNow(DP_REG.CTRL_STAT.getValue());
      if ((r & (CDBGPWRUPACK | CSYSPWRUPACK)) == (CDBGPWRUPACK | CSYSPWRUPACK)) {
        break;
      }
    }

    this.writeReg(DP_REG.CTRL_STAT.getValue(),
        (long) (CSYSPWRUPREQ | CDBGPWRUPREQ | TRNNORMAL | MASKLANE));
    this.writeReg(DP_REG.SELECT.getValue(), 0);
  }

  public void powerDownDebug() throws TimeoutException, Error {
    // Select bank 0 (to access DRW and TAR)
    this.writeReg(DP_REG.SELECT.getValue(), 0);
    this.writeReg(DP_REG.CTRL_STAT.getValue(), 0);
  }

  public void reset() throws InterruptedException, TimeoutException, Error {
    try {
      this.link.reset();
    } finally {
      this.csw = new HashMap<>();
      this.dpSelect = -1;
    }
  }

  public void assertReset(boolean asserted) throws TimeoutException, Error {
    this.link.assertReset(asserted);
    this.csw = new HashMap<>();
    this.dpSelect = -1;
  }

  public void setClock(int frequency) throws TimeoutException, Error {
    this.link.setClock(frequency);
  }

  public void findAps() {
    int apNum = 0;
    while (true) {
      try {
        long idr = this.readAPNow(((apNum << APSEL_SHIFT) | AP_REG.IDR.getValue()));
        if (idr == 0) {
          break;
        }
        LOGGER.log(Level.INFO, String.format("AP#%d IDR = 0x%08x", apNum, idr));
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE,
            String.format("Exception reading AP#%d IDR: %s", apNum, e.toString()));
        break;
      }
      apNum += 1;
    }
  }

  public long readDPNow(long addr) throws TimeoutException, Error {
    ArrayList<Object> result = readDPLater(addr);
    Transfer transfer = (Transfer) result.get(0);
    int num = (int) result.get(1);
    return readDPAsync(transfer, num);
  }

  public ArrayList<Object> readDPLater(long addr) throws TimeoutException, Error {
    assert Reg.containsReg(addr);
    int num = this.nextAccessNumber();

    Transfer transfer;
    try {
      transfer = this.link.readReg(addr);
      return new ArrayList<>(Arrays.asList(transfer, num));
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
  }

  // Read callback returned for async reads.
  public long readDPAsync(Transfer transfer, int num) throws TimeoutException, Error {
    try {
      long result = this.link.readRegAsync(transfer);
      return result;
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
  }

  public boolean writeDP(long addr, long word) throws Error, TimeoutException {
    assert Reg.containsReg(addr);
    int num = this.nextAccessNumber();

    // Skip writing DP SELECT register if its value is not changing
    if (addr == DP_REG.SELECT.getValue()) {
      if (word == this.dpSelect) {
        LOGGER.log(Level.INFO,
            String.format("writeDP:%06d cached (addr=0x%08x) = 0x%08x", num, addr, word));
        return false;
      }
      this.dpSelect = word;
    }

    // Write the DP register
    try {
      LOGGER.log(Level.INFO, String.format("writeDP:%06d (addr=0x%08x) = 0x%08x", num, addr, word));
      this.link.writeReg(addr, word);
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }

    return true;
  }

  public boolean writeAP(long addr, Long word) throws TimeoutException, Error {
    int num = this.nextAccessNumber();
    long apSel = (addr & APSEL);
    long bankSel = (addr & APBANKSEL);
    long apRegaddr = (addr & APREG_MASK);

    // Don't need to write CSW if it's not changing value
    if (apRegaddr == AP_REG.CSW.getValue()) {
      if (this.csw.containsKey(apSel) && word == this.csw.get(apSel)) {
        LOGGER.log(Level.INFO,
            String.format("writeAP:%06d cached (addr=0x%08x) = 0x%08x", num, addr, word));
        return false;
      }
      this.csw.put(apSel, word);
    }

    // Select the AP and bank.
    this.writeDP(DP_REG.SELECT.getValue(), (apSel | bankSel));

    // Perform the AP register write.
    long apReg = apAddrToReg((WRITE | AP_ACC | (addr & A32)));
    try {
      LOGGER.log(Level.INFO, String.format("writeAP:%06d (addr=0x%08x) = 0x%08x", num, addr, word));
      this.link.writeReg(apReg, word);
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }

    return true;
  }

  public long readAPNow(long addr) throws TimeoutException, Error {
    ArrayList<Object> result = this.readAP(addr);
    Transfer transfer = (Transfer) result.get(0);
    int num = (int) result.get(1);
    return readAPAsync(transfer, num);
  }

  public ArrayList<Object> readAP(long addr) throws TimeoutException, Error {
    int num = this.nextAccessNumber();
    long apReg = apAddrToReg((READ | AP_ACC | (addr & A32)));

    Transfer transfer;
    try {
      long apSel = (addr & APSEL);
      long bankSel = (addr & APBANKSEL);
      this.writeDP(DP_REG.SELECT.getValue(), (apSel | bankSel));
      transfer = this.link.readReg(apReg);
      return new ArrayList<>(Arrays.asList(transfer, num));
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
  }

  public long readAPAsync(Transfer transfer, int num) throws TimeoutException, Error {
    try {
      long result = this.link.readRegAsync(transfer);
      return result;
    } catch (Error error) {
      this.handleError(error, num);
      throw error;
    }
  }

  public void handleError(Error error, int num) throws TimeoutException, Error {
    LOGGER.log(Level.INFO, String.format("error:%06d %s", num, error));
    // Invalidate cached registers
    this.csw = new HashMap<>();
    this.dpSelect = -1;
    // Clear sticky error for Fault errors only
    if (error instanceof TransferFaultError) {
      this.clearStickyErr();
    }
  }

  public void clearStickyErr() throws TimeoutException, Error {
    Port mode = this.link.getSwjMode();
    if (mode.getValue() == Port.SWD.getValue()) {
      this.link.writeReg(DP_REG.CTRL_STAT.getValue(), CTRLSTAT_STICKYERR);
    } else {
      assert false;
    }
  }
}
