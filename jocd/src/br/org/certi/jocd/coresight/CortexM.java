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

import br.org.certi.jocd.core.MemoryMap;
import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.dapaccess.Transfer;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.debug.BreakpointManager;
import br.org.certi.jocd.debug.breakpoints.SoftwareBreakpointProvider;
import br.org.certi.jocd.util.Conversion;
import br.org.certi.jocd.util.Mask;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class has basic functions to access a Cortex M core:
 *   - init
 *   - read/write memory
 *   - read/write core registers
 *   - set/remove hardware breakpoints
 */
public class CortexM extends Target {

  // Logging
  private final static String CLASS_NAME = CortexM.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  // Map from register name to DCRSR register index.
  // The CONTROL, FAULTMASK, BASEPRI, and PRIMASK registers are special in that they share the
  // same DCRSR register index and are returned as a single value. In this dict, these registers
  // have negative values to signal to the register read/write functions that special handling
  // is necessary. The values are the byte number containing the register value, plus 1 and then
  // negated. So -1 means a mask of 0xFF, -2 is 0xFF00, and so on. The actual DCRSR register index
  // for these combined registers has the key of 'cfbp'.
  public enum CortexMRegister implements CoreRegister {
    R0(0L),
    R1(1L),
    R2(2L),
    R3(3L),
    R4(4L),
    R5(5L),
    R6(6L),
    R7(7L),
    R8(8L),
    R9(9L),
    R10(10L),
    R11(11L),
    R12(12L),
    SP(13L),
    R13(13L),
    LR(14L),
    R14(14L),
    PC(15L),
    R15(15L),
    XPSR(16L),
    MSP(17L),
    PSP(18L),
    CFBP(20L),
    CONTROL(-4L),
    FAULTMASK(-3L),
    BASEPRI(-2L),
    PRIMASK(-1L),
    FPSCR(33L),
    S0(0x40L),
    S1(0x41L),
    S2(0x42L),
    S3(0x43L),
    S4(0x44L),
    S5(0x45L),
    S6(0x46L),
    S7(0x47L),
    S8(0x48L),
    S9(0x49L),
    S10(0x4AL),
    S11(0x4BL),
    S12(0x4CL),
    S13(0x4DL),
    S14(0x4EL),
    S15(0x4FL),
    S16(0x50L),
    S17(0x51L),
    S18(0x52L),
    S19(0x53L),
    S20(0x54L),
    S21(0x55L),
    S22(0x56L),
    S23(0x57L),
    S24(0x58L),
    S25(0x59L),
    S26(0x5AL),
    S27(0x5BL),
    S28(0x5CL),
    S29(0x5DL),
    S30(0x5EL),
    S31(0x5FL);

    public final long value;

    CortexMRegister(long id) {
      this.value = id;
    }

    public long getValue() {
      return value;
    }
  }

  // CPUID PARTNO values
  public enum CpuId {
    ARM_CortexM0(0xC20L),
    ARM_CortexM1(0xC21L),
    ARM_CortexM3(0xC23L),
    ARM_CortexM4(0xC24L),
    ARM_CortexM0plus(0xC60L);

    public final long value;

    CpuId(long id) {
      this.value = id;
    }

    public long getValue() {
      return value;
    }

    public static CpuId getCpuId(long value) {
      for (CpuId cpuId : CpuId.values()) {
        if (cpuId.getValue() == value) {
          return cpuId;
        }
      }
      return null;
    }
  }

  // Debug Fault Status Register
  public static final long DFSR = 0xE000ED30L;
  public static final long DFSR_EXTERNAL = (1L << 4);
  public static final long DFSR_VCATCH = (1L << 3);
  public static final long DFSR_DWTTRAP = (1L << 2);
  public static final long DFSR_BKPT = (1L << 1);
  public static final long DFSR_HALTED = (1L << 0);

  // Debug Exception and Monitor Control Register
  public static final long DEMCR = 0xE000EDFCL;
  // DWTENA in armv6 architecture reference manual
  public static final long DEMCR_TRCENA = (1L << 24);
  public static final long DEMCR_VC_HARDERR = (1L << 10);
  public static final long DEMCR_VC_INTERR = (1L << 9);
  public static final long DEMCR_VC_BUSERR = (1L << 8);
  public static final long DEMCR_VC_STATERR = (1L << 7);
  public static final long DEMCR_VC_CHKERR = (1L << 6);
  public static final long DEMCR_VC_NOCPERR = (1L << 5);
  public static final long DEMCR_VC_MMERR = (1L << 4);
  public static final long DEMCR_VC_CORERESET = (1L << 0);

  // CPUID Register
  public static final long CPUID = 0xE000ED00L;

  // CPUID masks
  public static final long CPUID_IMPLEMENTER_MASK = 0xFF000000L;
  public static final int CPUID_IMPLEMENTER_POS = 24;
  public static final long CPUID_VARIANT_MASK = 0x00F00000L;
  public static final int CPUID_VARIANT_POS = 20;
  public static final long CPUID_ARCHITECTURE_MASK = 0x000F0000L;
  public static final int CPUID_ARCHITECTURE_POS = 16;
  public static final long CPUID_PARTNO_MASK = 0x0000FFF0L;
  public static final int CPUID_PARTNO_POS = 4;
  public static final long CPUID_REVISION_MASK = 0x0000000FL;
  public static final int CPUID_REVISION_POS = 0;

  public static final int CPUID_IMPLEMENTER_ARM = 0x41;
  public static final int ARMv6M = 0xC;
  public static final int ARMv7M = 0xF;

  // Debug Core Register Selector Register
  public static final long DCRSR = 0xE000EDF4L;
  public static final long DCRSR_REGWnR = (1 << 16);
  public static final long DCRSR_REGSEL = 0x1FL;

  // Debug Halting Control and Status Register
  public static final long DHCSR = 0xE000EDF0L;
  public static final long C_DEBUGEN = (1L << 0);
  public static final long C_HALT = (1L << 1);
  public static final long C_STEP = (1L << 2);
  public static final long C_MASKINTS = (1L << 3);
  public static final long C_SNAPSTALL = (1L << 5);
  public static final long S_REGRDY = (1L << 16);
  public static final long S_HALT = (1L << 17);
  public static final long S_SLEEP = (1L << 18);
  public static final long S_LOCKUP = (1L << 19);
  public static final long S_RETIRE_ST = (1L << 24);
  public static final long S_RESET_ST = (1L << 25);

  // Debug Core Register Data Register
  public static final long DCRDR = 0xE000EDF8L;

  // Coprocessor Access Control Register
  public static final long CPACR = 0xE000ED88L;
  public static final long CPACR_CP10_CP11_MASK = (3L << 20) | (3L << 22);

  public static final long NVIC_AIRCR = 0xE000ED0CL;
  public static final long NVIC_AIRCR_VECTKEY = (0x5FAL << 16);
  public static final long NVIC_AIRCR_VECTRESET = (1L << 0);
  public static final long NVIC_AIRCR_SYSRESETREQ = (1L << 2);

  public static final long DBGKEY = (0xA05FL << 16);

  private int runToken = 0;

  public DebugPort dp;
  public AccessPort ap;
  public Fpb fpb;
  public Dwt dwt;
  public SoftwareBreakpointProvider swBp;
  public BreakpointManager bpManager;
  public boolean haltOnConnect = true;
  public long arch;
  public CpuId coreType;
  public boolean hasFpu = false;

  /*
   * Constructor.
   */
  public CortexM(DebugPort dp, AccessPort ap) {
    this.dp = dp;
    this.ap = ap;

    // Set up breakpoints manager.
    this.fpb = new Fpb(this.ap);
    this.dwt = new Dwt(this.ap);
    this.swBp = new SoftwareBreakpointProvider(this);
    this.bpManager = new BreakpointManager(this);
    this.bpManager.addProvider(this.fpb, BreakpointTypes.HW);
    this.bpManager.addProvider(this.swBp, BreakpointTypes.SW);
  }

  /*
   * Must be called right after constructor.
   * Overload for protected method setup.
   */
  @Override
  public void setup(DapAccessCmsisDap link) {
    this.setup(link, null);
  }

  /*
   * Must be called right after constructor.
   */
  @Override
  protected void setup(DapAccessCmsisDap link, MemoryMap memoryMap) {
    super.setup(link, memoryMap);
  }

  /*
   * Cortex M initialization. The bus must be accessible when this method is called.
   */
  @Override
  public void init() throws TimeoutException, Error {
    if (this.haltOnConnect) {
      this.halt();
    }
    this.readCoreType();
    this.checkForFpu();
    this.buildTargetXml();
    this.fpb.init();
    this.dwt.init();
    this.swBp.init();
  }


  public void disconnect() throws TimeoutException, Error {
    // Remove breakpoints.
    this.bpManager.removeAllBreakpoints();

    // Disable other debug blocks.
    this.write32(CortexM.DEMCR, 0);

    // Disable core debug.
    this.write32(CortexM.DHCSR, CortexM.DBGKEY | 0x0000);
  }

  public void buildTargetXml() {
    // Not implemented.
    // Will throw exception if someone try to get TargetXML (by accessing target.getTargetXml()).
  }

  /*
   * Read the CPUID register and determine core type.
   */
  public void readCoreType() throws TimeoutException, Error {
    // Read CPUID register
    long cpuid = read32(CortexM.CPUID);

    long implementer = (cpuid & CortexM.CPUID_IMPLEMENTER_MASK) >> CortexM.CPUID_IMPLEMENTER_POS;
    if (implementer != CortexM.CPUID_IMPLEMENTER_ARM) {
      LOGGER.log(Level.FINE, "CPU implementer is not ARM!");
    }

    this.arch = (cpuid & CortexM.CPUID_ARCHITECTURE_MASK) >> CortexM.CPUID_ARCHITECTURE_POS;
    long coreType = (cpuid & CortexM.CPUID_PARTNO_MASK) >> CortexM.CPUID_PARTNO_POS;
    this.coreType = CpuId.getCpuId(coreType);
    LOGGER.log(Level.FINE, "CPU core is " + this.coreType.toString());
  }

  /*
   * Determine if a Cortex-M4 has an FPU.
   * The core type must have been identified prior to calling this function.
   */
  public void checkForFpu() throws TimeoutException, Error {
    if (this.coreType != CpuId.ARM_CortexM4) {
      this.hasFpu = false;
      return;
    }

    long originalCpacr = read32(CortexM.CPACR);
    long cpacr = originalCpacr | CortexM.CPACR_CP10_CP11_MASK;
    write32(CortexM.CPACR, cpacr);

    cpacr = read32(CortexM.CPACR);
    this.hasFpu = (cpacr & CortexM.CPACR_CP10_CP11_MASK) != 0;

    // Restore previous value.
    write32(CortexM.CPACR, originalCpacr);

    if (this.hasFpu) {
      LOGGER.log(Level.INFO, "FPU present.");
    }
  }

  @Override
  public void writeMemory(long address, long value) throws TimeoutException, Error {
    this.writeMemory(address, value, 32);
  }

  /*
   * Write a memory location.
     By default the transfer size is a word
   */
  @Override
  public void writeMemory(long address, long value, Integer transferSize)
      throws TimeoutException, Error {
    // Load default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }

    this.ap.writeMemory(address, value, transferSize);
  }

  /*
   * Read a memory location. By default, a word will be read.
   */
  @Override
  public long readMemoryNow(long address, Integer transferSize) throws TimeoutException, Error {
    // Load default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }

    long result = this.ap.readMemoryNow(address, transferSize);
    return this.bpManager.filterMemory(address, transferSize, result);
  }

  /*
   * Read a memory location. By default, a word will be read.
   */
  @Override
  public ArrayList<Object> readMemoryLater(long address, Integer transferSize)
      throws TimeoutException, Error {
    // Load default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }

    return this.ap.readMemoryLater(address, transferSize);
  }

  /*
   * Read a memory location. By default, a word will be read.
   */
  @Override
  public long readMemoryAsync(Transfer transfer, int numDp, long addr, Integer transferSize,
      int num) throws TimeoutException, Error {
    // Load default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }

    return this.ap.readMemoryAsync(transfer, numDp, addr, transferSize, num);
  }

  /*
   * Read a block of unaligned bytes in memory. Returns an array of byte values.
   */
  @Override
  public byte[] readBlockMemoryUnaligned8(long address, int size) throws TimeoutException, Error {
    byte[] data = this.ap.readBlockMemoryUnaligned8(address, size);
    return this.bpManager.filterMemoryUnaligned8(address, size, data);
  }

  /*
   * Read a block of aligned words in memory. Returns an array of word values.
   */
  @Override
  public long[] readBlockMemoryAligned32(long address, int size) throws TimeoutException, Error {
    long[] words = this.ap.readBlockMemoryAligned32(address, size);
    return this.bpManager.filterMemoryAligned32(address, size, words);
  }

  /*
   * Write a block of unaligned bytes in memory.
   */
  @Override
  public void writeBlockMemoryUnaligned8(long address, byte[] data) throws TimeoutException, Error {
    this.ap.writeBlockMemoryUnaligned8(address, data);
  }

  /*
   * Write a block of aligned words in memory.
   */
  @Override
  public void writeBlockMemoryAligned32(long address, long[] words) throws TimeoutException, Error {
    this.ap.writeBlockMemoryAligned32(address, words);
  }

  public void clearDebugCauseBits() throws TimeoutException, Error {
    this.writeMemory(CortexM.DFSR, CortexM.DFSR_DWTTRAP | CortexM.DFSR_BKPT | CortexM.DFSR_HALTED);
  }

  /*
   * Reset a core. After a call to this function, the core is running.
   */
  @Override
  public void reset(Boolean softwareReset)
      throws InterruptedException, TimeoutException, Error {
    if (softwareReset == null) {
      // Set default value to software reset if nothing is specified.
      softwareReset = true;
    }

    this.runToken++;

    if (softwareReset) {
      // Perform the reset.
      try {
        this.writeMemory(CortexM.NVIC_AIRCR,
            CortexM.NVIC_AIRCR_VECTKEY | CortexM.NVIC_AIRCR_SYSRESETREQ, null);
        // Without a flush a transfer error can occur.
        this.dp.flush();
      } catch (Exception e) {
        this.dp.flush();
      }
    } else {
      this.dp.reset();
    }

    // Now wait for the system to come out of reset. Keep reading the DHCSR until we get a good
    // response with S_RESET_ST cleared, or we time out.
    long startTime = System.currentTimeMillis();
    long timeoutMs = 2000;
    while ((System.currentTimeMillis() - startTime) < timeoutMs) {
      try {
        long dhcsr = read32(CortexM.DHCSR);
        if ((dhcsr & CortexM.S_RESET_ST) == 0) {
          break;
        }
      } catch (Exception e) {
        this.dp.flush();
        Thread.sleep(10);
      }
    }
  }

  /*
   * Halt the core.
   */
  @Override
  public void halt() throws TimeoutException, Error {
    this.writeMemory(CortexM.DHCSR, CortexM.DBGKEY | CortexM.C_DEBUGEN | CortexM.C_HALT, null);
    this.dp.flush();
    State state = this.getState();
  }

  /*
   * Perform a reset and stop the core on the reset handler.
   */
  @Override
  public void resetStopOnReset(Boolean softwareReset)
      throws InterruptedException, TimeoutException, Error {
    if (softwareReset == null) {
      // Set default value to software reset if nothing is specified.
      softwareReset = true;
    }

    LOGGER.log(Level.FINE, "Reset stop on Reset.");

    // Halt the target.
    halt();

    // Save CortexM.DEMCR.
    long demcr = readMemoryNow(CortexM.DEMCR, null);

    // Enable the vector catch.
    writeMemory(CortexM.DEMCR, demcr | CortexM.DEMCR_VC_CORERESET, null);
    reset(softwareReset);

    // Wait until the unit resets.
    while (this.isRunning()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        LOGGER.log(Level.SEVERE, e.toString());
      }
    }

    // Restore vector catch setting.
    writeMemory(CortexM.DEMCR, demcr, null);
  }

  @Override
  public void setTargetState(State state) throws InterruptedException, TimeoutException, Error {
    if (state == State.PROGRAM) {
      this.resetStopOnReset(true);
      // Write the thumb bit in case the reset handler points to an ARM address.
      this.writeCoreRegister(CortexMRegister.XPSR, 0x1000000);
    }
  }

  @Override
  public State getState() throws TimeoutException, Error {
    long dhcsr = readMemoryNow(CortexM.DHCSR, null);

    if ((dhcsr & CortexM.S_RESET_ST) != 0) {
      // Reset is a special case because the bit is sticky and really means "core was reset since
      // last read of DHCSR".We have to re - read the DHCSR, check if S_RESET_ST is still set and
      // make sure no instructions were executed by checking S_RETIRE_ST.
      long newDhcsr = readMemoryNow(CortexM.DHCSR, null);
      if ((newDhcsr & CortexM.S_RESET_ST) != 0 && (newDhcsr & CortexM.S_RETIRE_ST) == 0) {
        return Target.State.TARGET_RESET;
      }
    }
    if ((dhcsr & CortexM.S_LOCKUP) != 0) {
      return Target.State.TARGET_LOCKUP;
    } else if ((dhcsr & CortexM.S_SLEEP) != 0) {
      return Target.State.TARGET_SLEEPING;
    } else if ((dhcsr & CortexM.S_HALT) != 0) {
      return Target.State.TARGET_HALTED;
    } else {
      return Target.State.TARGET_RUNNING;
    }
  }

  @Override
  public boolean isRunning() throws TimeoutException, Error {
    return (getState() == Target.State.TARGET_RUNNING);
  }

  @Override
  public boolean isHalted() throws TimeoutException, Error {
    return (getState() == Target.State.TARGET_HALTED);
  }

  private long mapToVectorCatchMask(long mask) {
    long result = 0;

    if ((mask & Target.CATCH_HARD_FAULT) != 0) {
      result |= CortexM.DEMCR_VC_HARDERR;
    }
    if ((mask & Target.CATCH_BUS_FAULT) != 0) {
      result |= CortexM.DEMCR_VC_BUSERR;
    }
    if ((mask & Target.CATCH_MEM_FAULT) != 0) {
      result |= CortexM.DEMCR_VC_MMERR;
    }
    if ((mask & Target.CATCH_INTERRUPT_ERR) != 0) {
      result |= CortexM.DEMCR_VC_INTERR;
    }
    if ((mask & Target.CATCH_STATE_ERR) != 0) {
      result |= CortexM.DEMCR_VC_STATERR;
    }
    if ((mask & Target.CATCH_CHECK_ERR) != 0) {
      result |= CortexM.DEMCR_VC_CHKERR;
    }
    if ((mask & Target.CATCH_COPROCESSOR_ERR) != 0) {
      result |= CortexM.DEMCR_VC_NOCPERR;
    }
    if ((mask & Target.CATCH_CORE_RESET) != 0) {
      result |= CortexM.DEMCR_VC_CORERESET;
    }
    return result;
  }

  private long mapFromVectorCatchMask(long mask) {
    long result = 0;

    if ((mask & CortexM.DEMCR_VC_HARDERR) != 0) {
      result |= Target.CATCH_HARD_FAULT;
    }
    if ((mask & CortexM.DEMCR_VC_BUSERR) != 0) {
      result |= Target.CATCH_BUS_FAULT;
    }
    if ((mask & CortexM.DEMCR_VC_MMERR) != 0) {
      result |= Target.CATCH_MEM_FAULT;
    }
    if ((mask & CortexM.DEMCR_VC_INTERR) != 0) {
      result |= Target.CATCH_INTERRUPT_ERR;
    }
    if ((mask & CortexM.DEMCR_VC_STATERR) != 0) {
      result |= Target.CATCH_STATE_ERR;
    }
    if ((mask & CortexM.DEMCR_VC_CHKERR) != 0) {
      result |= Target.CATCH_CHECK_ERR;
    }
    if ((mask & CortexM.DEMCR_VC_NOCPERR) != 0) {
      result |= Target.CATCH_COPROCESSOR_ERR;
    }
    if ((mask & CortexM.DEMCR_VC_CORERESET) != 0) {
      result |= Target.CATCH_CORE_RESET;
    }
    return result;
  }

  @Override
  public void setVectorCatch(long enableMask) throws TimeoutException, Error {
    long demcr = this.readMemory(CortexM.DEMCR, null);
    demcr |= this.mapToVectorCatchMask(enableMask);
    demcr &= Mask.invert32(this.mapToVectorCatchMask(Mask.invert32(enableMask)));
    this.writeMemory(CortexM.DEMCR, demcr);
  }

  @Override
  public long getVectorCatch() throws TimeoutException, Error {
    long demcr = this.readMemory(CortexM.DEMCR, null);
    return this.mapFromVectorCatchMask(demcr);
  }

  /*
   * Resume the execution.
   */
  @Override
  public void resume() throws TimeoutException, Error {
    if (this.getState() != State.TARGET_HALTED) {
      LOGGER.log(Level.FINE, "Cannot resume: target not halted");
      return;
    }

    this.runToken++;
    this.clearDebugCauseBits();
    this.writeMemory(CortexM.DHCSR, CortexM.DBGKEY | CortexM.C_DEBUGEN);
    this.dp.flush();
  }

  /*
   * Read CPU register
   * Unpack floating point register values.
   */
  @Override
  public long readCoreRegister(CoreRegister reg) throws TimeoutException, Error {

    long regValue = this.readCoreRegisterRaw(reg);
    // Convert int to float.
    if (regValue >= 0x40L) {
      regValue = Conversion.u32BEToFloat32BE(regValue);
    }

    return regValue;
  }

  /*
   * Read a core register (r0 .. r16).
   */
  @Override
  public long readCoreRegisterRaw(CoreRegister reg) throws TimeoutException, Error {
    List<CoreRegister> regList = new ArrayList<CoreRegister>();
    regList.add(reg);
    long[] result = this.readCoreRegisterRaw(regList);
    if (result.length < 1) {
      throw new Error("readCoreRegisterRaw: Unexpected length of result (0).");
    }
    return result[0];
  }

  /*
   * Read one or more core registers.
   */
  @Override
  public long[] readCoreRegisterRaw(List<CoreRegister> regList) throws TimeoutException, Error {
    // Sanity check register values.
    for (CoreRegister reg : regList) {
      if ((reg.getValue() >= 0x40 || reg.getValue() == 33) && (this.hasFpu == false)) {
        throw new InternalError("attempt to read FPU register without FPU");
      }
    }

    // Each result will return a list of 3 objects.
    // Then, we need a list to store each result: a list of lists.
    List<List<Object>> dhcsrCbList = new ArrayList<List<Object>>();
    List<List<Object>> regCbList = new ArrayList<List<Object>>();

    // Begin all reads and writes.
    for (int i = 0; i < regList.size(); i++) {
      CoreRegister reg = regList.get(i);

      // Special register.
      if ((reg.getValue() < 0) && (reg.getValue() >= -4)) {
        reg = CortexMRegister.CFBP;
      }

      // Write id in DCRSR.
      this.writeMemory(CortexM.DCRSR, reg.getValue());

      // Technically, we need to poll S_REGRDY in DHCSR here before reading DCRDR. But we're running
      // so slow compared to the target that it's not necessary.
      // TODO Check if this is true in our case.

      // Read it and assert that S_REGRDY is set
      dhcsrCbList.add(this.readMemoryLater(CortexM.DHCSR, null));
      regCbList.add(this.readMemoryLater(CortexM.DCRDR, null));
    }

    // Read all results.
    long[] regValues = new long[regList.size()];
    for (int i = 0; i < regList.size(); i++) {
      CoreRegister reg = regList.get(i);

      List<Object> result;
      Transfer transfer;
      int numDp;
      int num;

      result = dhcsrCbList.get(i);
      transfer = (Transfer) result.get(0);
      numDp = (int) result.get(1);
      num = (int) result.get(2);
      long dhcsrVal = this.readMemoryAsync(transfer, numDp, CortexM.DHCSR, null, num);

      // assert dhcsr_val & CortexM.S_REGRDY
      if ((dhcsrVal & CortexM.S_REGRDY) == 0) {
        throw new Error("readCoreRegisterRaw: Unexpected value of dhcsrVal = " + dhcsrVal);
      }

      result = regCbList.get(i);
      transfer = (Transfer) result.get(0);
      numDp = (int) result.get(1);
      num = (int) result.get(2);
      long value = this.readMemoryAsync(transfer, numDp, CortexM.DCRDR, null, num);

      // Special handling for registers that are combined into a single DCRSR number.
      if ((reg.getValue() < 0) && (reg.getValue() >= -4)) {
        value = (value >> ((-reg.getValue() - 1) * 8)) & 0xFFL;
      }

      regValues[i] = value;
    }

    return regValues;
  }

  /*
   * write a CPU register.
   * Will need to pack floating point register values before writing.
   */
  @Override
  public void writeCoreRegister(CoreRegister reg, long word) throws TimeoutException, Error {
    // Convert float to int.
    if (reg.getValue() >= 0x40) {
      word = Conversion.float32beToU32be(word);
    }
    this.writeCoreRegisterRaw(reg, word);
  }

  /*
   *  Write a core register (r0 .. r16).
   */
  @Override
  public void writeCoreRegisterRaw(CoreRegister reg, long word) throws TimeoutException, Error {
    List<CoreRegister> regList = new ArrayList<CoreRegister>();
    regList.add(reg);
    long[] words = new long[1];
    words[0] = word;

    this.writeCoreRegisterRaw(regList, words);
  }

  /*
   *  Write one or more core registers
   *
   *  Write core registers in reg_list with the associated value in data_list.  If any register in
   *  reg_list is a string, find the number associated to this register in the lookup table
   *  CORE_REGISTER.
   */
  @Override
  public void writeCoreRegisterRaw(List<CoreRegister> regList, long[] words)
      throws TimeoutException, Error {
    if (regList.size() != words.length) {
      throw new InternalError("writeCoreRegisterRaw: regList.size() != words.length");
    }

    // Sanity check register values.
    for (CoreRegister reg : regList) {
      if ((reg.getValue() >= 0x40 || reg.getValue() == 33) && (this.hasFpu == false)) {
        throw new InternalError("attempt to write FPU register without FPU");
      }
    }

    // Each result will return a list of 3 objects.
    // Then, we need a list to store each result: a list of lists.
    List<List<Object>> results = new ArrayList<List<Object>>();
    for (int i = 0; i < regList.size(); i++) {
      CoreRegister reg = regList.get(i);
      long word = words[i];

      // Read special register if it is present in the list.
      if ((reg.getValue() < 0) && (reg.getValue() >= -4)) {
        long specialRegValue = this.readCoreRegister(CortexMRegister.CFBP);

        // Mask in the new special register value so we don't modify the other register values that
        // share the same DCRSR number.
        long shift = (-(reg.getValue()) - 1) * 8;
        long mask = 0xFFFFFFFFL ^ (0xFFL << shift);
        word = (specialRegValue & mask) | ((word & 0xFFL) << shift);
        // Update special register for other writes that might be in the list.
        specialRegValue = word;
        reg = CortexMRegister.CFBP;
      }

      // Write DCRDR.
      this.writeMemory(CortexM.DCRDR, word);

      // Write id in DCRSR and flag to start write transfer.
      this.writeMemory(CortexM.DCRSR, reg.getValue() | CortexM.DCRSR_REGWnR);

      // Technically, we need to poll S_REGRDY in DHCSR here to ensure the register write has
      // completed.
      // TODO Check if this is true in our case.
      // Read it and assert that S_REGRDY is set.
      results.add(this.readMemoryLater(CortexM.DHCSR, null));
    }

    // Make sure S_REGRDY was set for all register writes.
    for (List<Object> result : results) {
      Transfer transfer = (Transfer) result.get(0);
      int numDp = (int) result.get(1);
      int num = (int) result.get(2);
      long dhcsrVal = this.readMemoryAsync(transfer, numDp, CortexM.DHCSR, null, num);

      // assert dhcsr_val & CortexM.S_REGRDY
      if ((dhcsrVal & CortexM.S_REGRDY) == 0) {
        throw new Error("writeCoreRegisterRaw: Unexpected value of dhcsrVal = " + dhcsrVal);
      }
    }
  }
}
