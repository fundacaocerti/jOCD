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

import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.debug.breakpoints.BreakpointProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Fpb extends BreakpointProvider {

  // Logging
  private final static String CLASS_NAME = Fpb.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  public static final long FP_CTRL = 0xE0002000L;
  public static final int FP_CTRL_KEY = (1 << 1);
  public static final long FP_COMP0 = 0xE0002008L;

  public AccessPort ap;
  public long nbCode;
  public long nbLit;
  public List<HardwareBreakpoint> hwBreakpoints = new ArrayList<HardwareBreakpoint>();
  public boolean enabled = false;

  public Fpb(AccessPort ap) {
    super();
    this.ap = ap;
  }

  /*
   * Inits the FPB.
   *
   * Reads the number of hardware breakpoints available on the core and disable the FPB
   * (Flash Patch and Breakpoint Unit), which will be enabled when the first breakpoint is set.
   */
  @Override
  public void init() throws TimeoutException, Error {
    long fpcr = this.ap.readMemory(Fpb.FP_CTRL, null);
    this.nbCode = ((fpcr >> 8) & 0x70L) | ((fpcr >> 4) & 0x0FL);
    this.nbLit = (fpcr >> 7) & 0x0FL;
    LOGGER.log(Level.FINE,
        this.nbCode + "hardware breakpoints, " + this.nbLit + " literal comparators");

    for (int i = 0; i < this.nbCode; i++) {
      this.hwBreakpoints.add(new HardwareBreakpoint(Fpb.FP_COMP0 + 4 * i, this));
    }
    // Disable FPB (will be enabled on first bp set).
    this.disable();

    for (HardwareBreakpoint bp : hwBreakpoints) {
      this.ap.writeMemory(bp.compRegisterAddress, 0, null);
    }
  }

  public void enable() throws TimeoutException, Error {
    this.ap.writeMemory(Fpb.FP_CTRL, Fpb.FP_CTRL_KEY | 1, null);
    this.enabled = false;
    LOGGER.log(Level.FINE, "fpb has been enabled");
  }

  public void disable() throws TimeoutException, Error {
    this.ap.writeMemory(Fpb.FP_CTRL, Fpb.FP_CTRL_KEY | 0, null);
    this.enabled = false;
    LOGGER.log(Level.FINE, "fpb has been disabled");
  }
}
