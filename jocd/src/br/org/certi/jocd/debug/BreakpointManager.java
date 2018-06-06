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
import br.org.certi.jocd.core.Target.BreakpointTypes;
import br.org.certi.jocd.debug.breakpoints.Breakpoint;
import br.org.certi.jocd.debug.breakpoints.BreakpointProvider;
import br.org.certi.jocd.util.Mask;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BreakpointManager {

  // Logging
  private final static String CLASS_NAME = BreakpointManager.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private final Target core;
  private BreakpointProvider[] providers = new BreakpointProvider[BreakpointTypes.values().length];
  private List<Breakpoint> breakpoints = new ArrayList<Breakpoint>();

  public BreakpointManager(Target core) {
    this.core = core;
  }

  public void addProvider(BreakpointProvider provider, Target.BreakpointTypes type) {
    if (provider == null) {
      LOGGER.log(Level.SEVERE, "Unexpected provider (null).");
      return;
    }
    if (type == null) {
      LOGGER.log(Level.SEVERE, "Unexpected type (null).");
      return;
    }

    providers[type.getValue()] = provider;
  }

  public void removeBreakpoint(long address) {
    LOGGER.log(Level.FINE, String.format("Remove breakpoint at 0x%08X", address));

    // Clear Thumn bit in case it is set.
    address = address & Mask.invert32(0x01);

    // Get bp and remove from list.
    Breakpoint bp = null;
    int index;
    for (Breakpoint item : this.breakpoints) {
      if (item.address == address) {
        bp = item;
      }
    }

    if (bp == null) {
      LOGGER.log(Level.SEVERE,
          String.format("Couldn't find breakpoint at address 0x%08X", address));
      return;
    }
    this.breakpoints.remove(bp);

    // Assert bp.provider is null.
    if (bp.provider != null) {
      throw new InternalError("removeBreakpoint: bp.provider != null.");
    }
  }

  public long filterMemory(long address, int size, long word) {
    for (BreakpointProvider provider : this.providers) {
      if (provider != null && provider.doFilterMemory()) {
        word = provider.filterMemory(address, size, word);
      }
    }
    return word;
  }

  public byte[] filterMemoryUnaligned8(long address, int size, byte[] data) {
    for (BreakpointProvider provider : this.providers) {
      if (provider != null && provider.doFilterMemory()) {
        for (int i = 0; i < data.length; i++) {
          data[i] = provider.filterMemory(address + i, data[i]);
        }
      }
    }
    return data;
  }

  public long[] filterMemoryAligned32(long address, int size, long[] words) {
    for (BreakpointProvider provider : this.providers) {
      if (provider != null && provider.doFilterMemory()) {
        for (int i = 0; i < words.length; i++) {
          words[i] = provider.filterMemory(address, size, words[i]);
        }
      }
    }
    return words;
  }

  public void removeAllBreakpoints() {
    for (Breakpoint bp : breakpoints) {
      bp.provider.removeBreakpoint(bp);
    }
  }
}
