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

import br.org.certi.jocd.core.Target;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class has basic functions to access a Cortex M core:
 *   - init
 *   - read/write memory
 *   - read/write core registers
 *   - set/remove hardware breakpoints
 */
public class CortexM {

  // Logging
  private final static String CLASS_NAME = CortexM.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  // Debug Fault Status Register
  public static final long DFSR = 0xE000ED30;
  public static final int DFSR_EXTERNAL = (1 << 4);
  public static final int DFSR_VCATCH = (1 << 3);
  public static final int DFSR_DWTTRAP = (1 << 2);
  public static final int DFSR_BKPT = (1 << 1);
  public static final int DFSR_HALTED = (1 << 0);

  // Debug Exception and Monitor Control Register
  public static final long DEMCR = 0xE000EDFC;
  // DWTENA in armv6 architecture reference manual
  public static final int DEMCR_TRCENA = (1 << 24);
  public static final int DEMCR_VC_HARDERR = (1 << 10);
  public static final int DEMCR_VC_INTERR = (1 << 9);
  public static final int DEMCR_VC_BUSERR = (1 << 8);
  public static final int DEMCR_VC_STATERR = (1 << 7);
  public static final int DEMCR_VC_CHKERR = (1 << 6);
  public static final int DEMCR_VC_NOCPERR = (1 << 5);
  public static final int DEMCR_VC_MMERR = (1 << 4);
  public static final int DEMCR_VC_CORERESET = (1 << 0);

  // CPUID Register
  public static final long CPUID = 0xE000ED00;

  // CPUID masks
  public static final long CPUID_IMPLEMENTER_MASK = 0xff000000;
  public static final int CPUID_IMPLEMENTER_POS = 24;
  public static final long CPUID_VARIANT_MASK = 0x00f00000;
  public static final int CPUID_VARIANT_POS = 20;
  public static final long CPUID_ARCHITECTURE_MASK = 0x000f0000;
  public static final int CPUID_ARCHITECTURE_POS = 16;
  public static final long CPUID_PARTNO_MASK = 0x0000fff0;
  public static final int CPUID_PARTNO_POS = 4;
  public static final long CPUID_REVISION_MASK = 0x0000000f;
  public static final int CPUID_REVISION_POS = 0;

  public static final int CPUID_IMPLEMENTER_ARM = 0x41;
  public static final int ARMv6M = 0xC;
  public static final int ARMv7M = 0xF;

  // Debug Core Register Selector Register
  public static final long DCRSR = 0xE000EDF4;
  public static final long DCRSR_REGWnR = (1 << 16);
  public static final long DCRSR_REGSEL = 0x1F;

  // Debug Halting Control and Status Register
  public static final long DHCSR = 0xE000EDF0;
  public static final int C_DEBUGEN = (1 << 0);
  public static final int C_HALT = (1 << 1);
  public static final int C_STEP = (1 << 2);
  public static final int C_MASKINTS = (1 << 3);
  public static final int C_SNAPSTALL = (1 << 5);
  public static final int S_REGRDY = (1 << 16);
  public static final int S_HALT = (1 << 17);
  public static final int S_SLEEP = (1 << 18);
  public static final int S_LOCKUP = (1 << 19);
  public static final int S_RETIRE_ST = (1 << 24);
  public static final int S_RESET_ST = (1 << 25);

  // Debug Core Register Data Register
  public static final long DCRDR = 0xE000EDF8;

  // Coprocessor Access Control Register
  public static final long CPACR = 0xE000ED88;
  public static final int CPACR_CP10_CP11_MASK = (3 << 20) | (3 << 22);

  public static final long NVIC_AIRCR = 0xE000ED0C;
  public static final int NVIC_AIRCR_VECTKEY = (0x5FA << 16);
  public static final int NVIC_AIRCR_VECTRESET = (1 << 0);
  public static final int NVIC_AIRCR_SYSRESETREQ = (1 << 2);

  public static final int DBGKEY = (0xA05F << 16);

  private int runToken = 0;

  /*
   * Write a memory location.
     By default the transfer size is a word
   */
  public void writeMemory(long address, long value, Integer transferSize) {
    // Load default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }

    // TODO
    //this.ap.writeMemory(address, int value, transferSize);
  }

  /*
   * Read a memory location. By default, a word will be read.
   */
  public int readMemory(long address, Integer transferSize) {
    // Load default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }

    // TODO
    //int result = this.ap.readMemory(address, transferSize, false);
    return 0;
  }

  /*
   * Read a memory location. By default, a word will be read.
   */
  public int readMemoryNow(long address, Integer transferSize) {
    // Load default value if null.
    if (transferSize == null) {
      transferSize = 32;
    }

    // TODO
    //int result = this.ap.readMemory(address, transferSize, true);
    //return this.bpManager.filterMemory(address, transferSize, result);
    return 0;
  }

  /*
   * Read a block of unaligned bytes in memory. Returns an array of byte values.
   */
  public byte[] readBlockMemoryUnaligned8(long address, long size) {
    // TODO
    //byte[] data = this.selected_core.readBlockMemoryAligned32(address, size);
    //return this.bp_manager.filterMemoryUnaligned8(address, size, data);
    return new byte[]{};
  }

  /*
   * Read a block of aligned words in memory. Returns an array of word values.
   */
  public int[] readBlockMemoryAligned32(long address, long size) {
    // TODO
    //return this.selected_core.readBlockMemoryAligned32(address, size);
    return new int[]{};
  }

  /*
   * Write a block of unaligned bytes in memory.
   */
  public void writeBlockMemoryUnaligned8(long address, byte[] data) {
    // TODO
    //this.ap.writeBlockMemoryUnaligned8(address, data)
  }

  /*
   * Write a block of aligned words in memory.
   */
  public void writeBlockMemoryAligned32(long address, int[] data) {
    // TODO
    //this.ap.writeBlockMemoryAligned32(address, data)
  }

  /*
   * Reset a core. After a call to this function, the core is running.
   */
  public void reset(Boolean softwareReset) {
    if (softwareReset == null) {
      // Set default value to software reset if nothing is specified.
      softwareReset = true;
    }

    this.runToken++;

    if (softwareReset) {
      // Perform the reset.
      this.writeMemory(CortexM.NVIC_AIRCR,
          CortexM.NVIC_AIRCR_VECTKEY | CortexM.NVIC_AIRCR_SYSRESETREQ, null);
    } else {
      // TODO
      //this.dp.reset();
    }

    // TODO
  }

  /*
   * Halt the core.
   */
  public void halt() {
    this.writeMemory(CortexM.DHCSR, CortexM.DBGKEY | CortexM.C_DEBUGEN | CortexM.C_HALT, null);
    // TODO
    // this.dp.flush();
  }

  /*
   * Perform a reset and stop the core on the reset handler.
   */
  public void resetStopOnReset(Boolean softwareReset) {
    if (softwareReset == null) {
      // Set default value to software reset if nothing is specified.
      softwareReset = true;
    }

    LOGGER.log(Level.FINE, "Reset stop on Reset.");

    // Halt the target.
    halt();

    // Save CortexM.DEMCR.
    int demcr = readMemory(CortexM.DEMCR, null);

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

  public int getState() {
    int dhcsr = readMemory(CortexM.DHCSR, null);

    if ((dhcsr & CortexM.S_RESET_ST) != 0) {
      // Reset is a special case because the bit is sticky and really means "core was reset since
      // last read of DHCSR".We have to re - read the DHCSR, check if S_RESET_ST is still set and
      // make sure no instructions were executed by checking S_RETIRE_ST.
      int newDhcsr = readMemory(CortexM.DHCSR, null);
      if ((newDhcsr & CortexM.S_RESET_ST) != 0 && (newDhcsr & CortexM.S_RETIRE_ST) == 0) {
        return Target.TARGET_RESET;
      }
    }
    if ((dhcsr & CortexM.S_LOCKUP) != 0) {
      return Target.TARGET_LOCKUP;
    } else if ((dhcsr & CortexM.S_SLEEP) != 0) {
      return Target.TARGET_SLEEPING;
    } else if ((dhcsr & CortexM.S_HALT) != 0) {
      return Target.TARGET_HALTED;
    } else {
      return Target.TARGET_RUNNING;
    }
  }

  public boolean isRunning() {
    return (getState() == Target.TARGET_RUNNING);
  }

  public boolean isHalted() {
    return (getState() == Target.TARGET_HALTED);
  }
}
