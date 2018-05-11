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

  public void reset() {
    throw new InternalError("Not implemented");
  }

  public byte[] readBlockMemoryUnaligned8(long address, long size) {
    throw new InternalError("Not implemented");
  }

  public Flash getFlash() {
    throw new InternalError("Not implemented");
  }

  public boolean massErase() {
    throw new InternalError("Not implemented");
  }


}
