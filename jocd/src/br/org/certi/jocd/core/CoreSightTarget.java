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

public class CoreSightTarget extends Target {

  /*
   * Must be called right after constructor.
   */
  public void setup(MemoryMap memoryMap) {
    super.setup(memoryMap);
  }

  @Override
  public void writeMemory(long address, long value) {
    // 32 is the default transfer size.
    writeMemory(address, value, 32);
  }

  @Override
  public void writeMemory(long address, long value, int transferSize) {
    // TODO
    //this.selected_core.writeMemory(addr, value, transfer_size)
  }

  @Override
  public void reset() {
    // TODO
    //this.selected_core.reset(software_reset=software_reset)
  }

  @Override
  public byte[] readBlockMemoryUnaligned8(long address, long size) {
    // TODO
    // return this.selected_core.readBlockMemoryUnaligned8(address, size);
    return new byte[]{};
  }
}
