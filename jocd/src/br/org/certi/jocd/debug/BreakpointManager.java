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

public class BreakpointManager {

  private final Target core;

  public BreakpointManager(Target core) {
    this.core = core;
  }

  public void addProvider(Target.BreakpointTypes type) {
    if (type == null) {
      return;
    }
  }

  public long filterMemory(long address, int size, long word) {
    // TODO
    return 0;
  }

  public byte[] filterMemoryUnaligned8(long address, int size, byte[] data) {
    // TODO
    return null;
  }

  public long[] filterMemoryAligned32(long address, int size, long[] words) {
    // TODO
    return null;
  }
}
