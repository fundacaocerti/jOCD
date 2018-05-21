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
package br.org.certi.jocd.board;

import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.flash.Flash;
import br.org.certi.jocd.target.TargetFactory;
import br.org.certi.jocd.target.TargetFactory.targetEnum;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Board {

  // Logging
  private final static String CLASS_NAME = Board.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  // Link to the dap CMSIS DAP.
  public DapAccessCmsisDap dapAccessLink;

  public Target target;
  public Flash flash;
  boolean closed;
  public Integer frequency;

  /*
   * Must be called right after constructor.
   */
  public void setup(DapAccessCmsisDap link, targetEnum target, Integer frequency) {
    this.dapAccessLink = link;
    this.target = TargetFactory.getTarget(target, link);
    this.flash = this.target.getFlash();
    this.frequency = frequency;
    this.closed = false;

    this.target.setFlash(this.flash);
  }

  /*
   * Initialize the board.
   */
  public void init() throws TimeoutException, Error {
    LOGGER.log(Level.FINE, "init board");
    if (frequency != null) {
      this.dapAccessLink.setClock(this.frequency);
    }
    this.dapAccessLink.setDeferredTransfer(true);
    this.target.init();
  }
}
