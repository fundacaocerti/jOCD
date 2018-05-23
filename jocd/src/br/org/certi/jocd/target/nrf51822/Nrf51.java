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
package br.org.certi.jocd.target.nrf51822;

import br.org.certi.jocd.core.CoreSightTarget;
import br.org.certi.jocd.core.FlashRegion;
import br.org.certi.jocd.core.MemoryMap;
import br.org.certi.jocd.core.MemoryRegion;
import br.org.certi.jocd.core.RamRegion;
import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.flash.Flash;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Nrf51 extends CoreSightTarget {

  // Logging
  private final static String CLASS_NAME = Nrf51.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  // nRF51 specific registers
  long reset;
  int resetEnable;

  /*
   * Must be called right after constructor.
   */
  @Override
  public void setup(DapAccessCmsisDap link) {
    List<MemoryRegion> memoryRegions = new ArrayList<MemoryRegion>();

    memoryRegions.add(
        new FlashRegion(
            0x00000000,
            0x40000,
            0x400)
    );

    // User Information Configuration Registers (UICR) as a flash region.
    memoryRegions.add(
        new FlashRegion(
            0x10001000,
            0x100,
            0x100)
    );
    memoryRegions.add(
        new RamRegion(
            0x20000000,
            0x4000)
    );

    memoryMap = new MemoryMap(memoryRegions);

    this.reset = 0x40000544L;
    this.resetEnable = (1 << 0);

    super.setup(link, memoryMap);
  }

  @Override
  public Flash getFlash() {
    return new FlashNrf51(this);
  }

  @Override
  public boolean massErase() {
    // Not implemented.
    LOGGER.log(Level.SEVERE, "Not implemented method massErase for nRF51");
    return false;
  }

  /*
   * reset a core. After a call to this function, the core
   * is running.
   */
  public void resetN() throws TimeoutException, InterruptedException, Error {
    // Regular reset will kick NRF out of DBG mode
    LOGGER.log(Level.FINE, "target_nrf51.reset: enable reset pin");
    writeMemory(reset, resetEnable);

    // Reset.
    LOGGER.log(Level.FINE, "target_nrf51.reset: trigger nRST pin");
    reset(null);
  }
}
