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

import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.TransferError;
import br.org.certi.jocd.flash.Flash;
import java.util.concurrent.TimeoutException;

public class Target {

  public static final int TARGET_RUNNING = 1;   // Core is executing code.
  public static final int TARGET_HALTED = 2;    // Core is halted in debug mode.
  public static final int TARGET_RESET = 3;     // Core is being held in reset.
  public static final int TARGET_SLEEPING = 4;  // Core is sleeping due to a wfi or wfe instruction.
  public static final int TARGET_LOCKUP = 5;    // Core is locked up.

  // Types of breakpoints.
  //
  // Auto will select the best type given the address and available breakpoints.
  public enum BreakpointTypes {
    HW((byte) 0),
    SW((byte) 1),
    AUTO((byte) 2);

    public final byte value;

    BreakpointTypes(byte id) {
      this.value = id;
    }

    public byte getValue() {
      return value;
    }
  }

  // Types of watchpoints.
  public enum WatchpointTypes {
    READ((byte) 0),
    WRITE((byte) 1),
    READ_WRITE((byte) 2);

    public final byte value;

    WatchpointTypes(byte id) {
      this.value = id;
    }

    public byte getValue() {
      return value;
    }
  }

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
  public DapAccessCmsisDap link;
  public Flash flash;

  /*
   * Must be called right after constructor.
   * Overload for protected method setup.
   */
  public void setup(DapAccessCmsisDap link) {
    this.setup(link, null);
  }

  /*
   * Must be called right after constructor.
   */
  protected void setup(DapAccessCmsisDap link, MemoryMap memoryMap) {
    this.link = link;
    this.memoryMap = (memoryMap == null) ? new MemoryMap(null) : memoryMap;
  }

  public void init() throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void setFlash(Flash flash) {
    this.flash = flash;
  }

  public void flush() throws TimeoutException, Error {
    this.link.flush();
  }

  public void halt() throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void step(Boolean disableInterrupts) {
    throw new InternalError("Not implemented");
  }

  public void resume() {
    throw new InternalError("Not implemented");
  }

  public boolean massErase() {
    throw new InternalError("Not implemented");
  }

  public void writeMemory(long address, long value) {
    throw new InternalError("Not implemented");
  }

  public void writeMemory(long address, long value, Integer transferSize) {
    throw new InternalError("Not implemented");
  }

  public void write32(long address, long value) {
    writeMemory(address, value, 32);
  }

  public void write16(long address, int value) {
    writeMemory(address, value, 16);
  }

  public void write8(long address, int value) {
    writeMemory(address, value, 8);
  }

  public long readMemory(long address, Integer transferSize) {
    return readMemoryNow(address, transferSize);
  }

  public long readMemoryNow(long address, Integer transferSize) {
    throw new InternalError("Not implemented");
  }

  public void readMemoryLater(long address, Integer transferSize) {
    throw new InternalError("Not implemented");
  }

  public long read32(long address) {
    return read32Now(address);
  }

  public long read32Now(long address) {
    return readMemoryNow(address, 32);
  }

  public void read32Later(long address) {
    readMemoryLater(address, 32);
  }

  public long read16(long address) {
    return read16Now(address);
  }

  public long read16Now(long address) {
    return readMemoryNow(address, 16);
  }

  public void read1Later(long address) {
    readMemoryLater(address, 16);
  }

  public long read8(long address) {
    return read8Now(address);
  }

  public long read8Now(long address) {
    return readMemoryNow(address, 8);
  }

  public void read8Later(long address) {
    readMemoryLater(address, 8);
  }

  public byte[] readBlockMemoryUnaligned8(long address, int size) {
    throw new InternalError("Not implemented");
  }

  public long[] readBlockMemoryAligned32(long address, int size) {
    throw new InternalError("Not implemented");
  }

  public void writeBlockMemoryUnaligned8(long address, byte[] data) {
    throw new InternalError("Not implemented");
  }

  public void writeBlockMemoryAligned32(long address, long[] words) {
    throw new InternalError("Not implemented");
  }

  public void reset(Boolean softwareReset)
      throws InterruptedException, TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public void resetStopOnReset(Boolean softwareReset)
      throws InterruptedException, TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public int getState() {
    throw new InternalError("Not implemented");
  }

  public boolean isRunning() {
    return getState() == Target.TARGET_RUNNING;
  }

  public boolean isHalted() {
    return getState() == Target.TARGET_HALTED;
  }

  public MemoryMap getMemoryMap() {
    return memoryMap;
  }

  public Flash getFlash() {
    throw new InternalError("Not implemented");
  }


}
