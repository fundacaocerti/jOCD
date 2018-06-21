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
package br.org.certi.jocd.debug;

import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.core.Target.CoreRegister;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.util.Conversion;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class DebugContext {

  private Target core;

  public DebugContext(Target core) {
    this.core = core;
  }

  public Target getCore() {
    return this.core;
  }

  public void writeMemory(long address, long value, Integer transferSize)
      throws TimeoutException, Error {
    this.core.writeMemory(address, value, transferSize);
  }

  public long readMemoryNow(long address, Integer transferSize) throws TimeoutException, Error {
    return this.core.readMemoryNow(address, transferSize);
  }

  public void writeBlockMemoryUnaligned8(long address, byte[] data) throws TimeoutException, Error {
    this.core.writeBlockMemoryUnaligned8(address, data);
  }

  public void writeBlockMemoryAligned32(long address, long[] words) throws TimeoutException, Error {
    this.core.writeBlockMemoryAligned32(address, words);
  }

  public byte[] readBlockMemoryUnaligned8(long address, int size) throws TimeoutException, Error {
    return this.core.readBlockMemoryUnaligned8(address, size);
  }

  public long[] readBlockMemoryAligned32(long address, int size) throws TimeoutException, Error {
    return this.core.readBlockMemoryAligned32(address, size);
  }

  public void write32(long address, long value) throws TimeoutException, Error {
    this.writeMemory(address, value, 32);
  }

  public void write16(long address, long value) throws TimeoutException, Error {
    this.writeMemory(address, value, 16);
  }

  public void write8(long address, long value) throws TimeoutException, Error {
    this.writeMemory(address, value, 8);
  }

  public long read32(long address) throws TimeoutException, Error {
    return this.readMemoryNow(address, 32);
  }

  public long read16(long address) throws TimeoutException, Error {
    return this.readMemoryNow(address, 16);
  }

  public long read8(long address) throws TimeoutException, Error {
    return this.readMemoryNow(address, 8);
  }

  /*
   * Read CPU register
   * Unpack floating point register values
   */
  public long readCoreRegister(CoreRegister reg) throws TimeoutException, Error {
    long regValue = this.readCoreRegisterRaw(reg);
    return regValue;
  }

  /*
   * Read a core register (r0 .. r16).
   * If reg is a string, find the number associated to this register in the lookup table
   * CORE_REGISTER.
   */
  public long readCoreRegisterRaw(CoreRegister reg) throws TimeoutException, Error {
    List<CoreRegister> regList = new ArrayList<CoreRegister>();
    regList.add(reg);
    return this.readCoreRegisterRaw(regList)[0];
  }

  public long[] readCoreRegisterRaw(List<CoreRegister> regList) throws TimeoutException, Error {
    return this.core.readCoreRegisterRaw(regList);
  }

  /*
   * Write a CPU register.
   * Will need to pack floating point register values before writing.
   */
  public void writeCoreRegister(CoreRegister reg, long word) throws TimeoutException, Error {
    this.writeCoreRegisterRaw(reg, word);
  }

  /*
   * Write a core register (r0 .. r16)
   * If reg is a string, find the number associated to this register in the lookup table
   * CORE_REGISTER.
   */
  public void writeCoreRegisterRaw(CoreRegister reg, long word) throws TimeoutException, Error {
    List<CoreRegister> regList = new ArrayList<CoreRegister>();
    regList.add(reg);
    long[] words = new long[1];
    this.writeCoreRegistersRaw(regList, words);
  }

  public void writeCoreRegistersRaw(List<CoreRegister> regList, long[] words)
      throws TimeoutException, Error {
    this.core.writeCoreRegisterRaw(regList, words);
  }

  public void flush() throws TimeoutException, Error {
    this.core.flush();
  }
}
