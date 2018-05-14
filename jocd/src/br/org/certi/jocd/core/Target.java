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
package br.org.certi.jocd.core;

import br.org.certi.jocd.flash.Flash;

public class Target {

  public static final int TARGET_RUNNING = 1;   // Core is executing code.
  public static final int TARGET_HALTED = 2;    // Core is halted in debug mode.
  public static final int TARGET_RESET = 3;     // Core is being held in reset.
  public static final int TARGET_SLEEPING = 4;  // Core is sleeping due to a wfi or wfe instruction.
  public static final int TARGET_LOCKUP = 5;    // Core is locked up.

  // Types of breakpoints.
  //
  // Auto will select the best type given the
  // address and available breakpoints.
  public static final int BREAKPOINT_HW = 1;
  public static final int BREAKPOINT_SW = 2;
  public static final int BREAKPOINT_AUTO = 3;

  public static final int WATCHPOINT_READ = 1;
  public static final int WATCHPOINT_WRITE = 2;
  public static final int WATCHPOINT_READ_WRITE = 3;

  // Vector catch option masks.
  public static final int CATCH_NONE = 0;
  public static final int CATCH_HARD_FAULT = (1 << 0);
  public static final int CATCH_BUS_FAULT = (1 << 1);
  public static final int CATCH_MEM_FAULT = (1 << 2);
  public static final int CATCH_INTERRUPT_ERR = (1 << 3);
  public static final int CATCH_STATE_ERR = (1 << 4);
  public static final int CATCH_CHECK_ERR = (1 << 5);
  public static final int CATCH_COPROCESSOR_ERR = (1 << 6);
  public static final int CATCH_CORE_RESET = (1 << 7);
  public static final int CATCH_ALL = (CATCH_HARD_FAULT | CATCH_BUS_FAULT | CATCH_MEM_FAULT
      | CATCH_INTERRUPT_ERR | CATCH_STATE_ERR | CATCH_CHECK_ERR | CATCH_COPROCESSOR_ERR
      | CATCH_CORE_RESET);

  public MemoryMap memoryMap;

  /*
   * Must be called right after constructor.
   */
  public void setup(MemoryMap memoryMap) {
    this.memoryMap = memoryMap;
  }

  public MemoryMap getMemoryMap() {
    return memoryMap;
  }

  public void writeMemory(long address, long value) {
    throw new InternalError("Not implemented");
  }

  public void writeMemory(long address, long value, int transferSize) {
    throw new InternalError("Not implemented");
  }

  public byte[] readBlockMemoryUnaligned8(long address, long size) {
    throw new InternalError("Not implemented");
  }

  public int[] readBlockMemoryAligned32(long address, long size) {
    throw new InternalError("Not implemented");
  }

  public void writeBlockMemoryUnaligned8(long address, byte[] data) {
    throw new InternalError("Not implemented");
  }

  public void writeBlockMemoryAligned32(long address, int[] data) {
    throw new InternalError("Not implemented");
  }

  public void reset(Boolean softwareReset) {
    throw new InternalError("Not implemented");
  }

  public void resetStopOnReset(Boolean softwareReset) {
    throw new InternalError("Not implemented");
  }

  public Flash getFlash() {
    throw new InternalError("Not implemented");
  }

  public boolean massErase() {
    throw new InternalError("Not implemented");
  }


}
