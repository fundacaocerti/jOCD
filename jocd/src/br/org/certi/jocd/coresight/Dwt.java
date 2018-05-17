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

import br.org.certi.jocd.debug.breakpoints.BreakpointProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dwt extends BreakpointProvider {

  // Logging
  private final static String CLASS_NAME = Dwt.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  // Need a local copy to prevent circular import.
  // Debug Exception and Monitor Control Register
  public static final long DEMCR = 0xE000EDFC;

  // DWTENA in armv6 architecture reference manual
  public static final long DEMCR_TRCENA = (1 << 24);
  public static final long DEMCR_VC_HARDERR = (1 << 10);
  public static final long DEMCR_VC_BUSERR = (1 << 8);
  public static final long DEMCR_VC_CORERESET = (1 << 0);

  // DWT (data watchpoint & trace)
  public static final long DWT_CTRL = 0xE0001000;
  public static final long DWT_COMP_BASE = 0xE0001020;
  public static final int DWT_MASK_OFFSET = 4;
  public static final int DWT_FUNCTION_OFFSET = 8;
  public static final int DWT_COMP_BLOCK_SIZE = 0x10;

  public AccessPort ap;
  public List<Watchpoint> watchpoints = new ArrayList<Watchpoint>();
  public int watchpointUsed = 0;
  public boolean dwtConfigured = false;

  public Dwt(AccessPort ap) {
    this.ap = ap;
  }

  /*
   * Inits the DWT.
   *
   * Reads the number of hardware watchpoints available on the core  and makes sure that they
   * are all disabled and ready for future use.
   */
  @Override
  public void init() {
    long demcr = this.ap.readMemory(DEMCR, null);
    demcr = demcr | DEMCR_TRCENA;
    this.ap.writeMemory(DEMCR, demcr, null);
    long dwtCtrl = this.ap.readMemory(Dwt.DWT_CTRL, null);
    int watchpointCount = (int) ((dwtCtrl >> 28) & 0x0F);
    LOGGER.log(Level.FINE, watchpointCount + "hardware watchpoints.");

    for (int i = 0; i < watchpointCount; i++) {
      this.watchpoints.add(new Watchpoint(Dwt.DWT_COMP_BASE + Dwt.DWT_COMP_BLOCK_SIZE * i, this));
      this.ap
          .writeMemory(Dwt.DWT_COMP_BASE + Dwt.DWT_COMP_BLOCK_SIZE * i + Dwt.DWT_FUNCTION_OFFSET, 0,
              null);
    }
    this.dwtConfigured = true;
  }

  // TODO
}
