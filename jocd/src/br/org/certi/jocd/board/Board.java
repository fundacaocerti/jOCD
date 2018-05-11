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

import android.util.Log;
import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.flash.Flash;
import br.org.certi.jocd.target.TargetFactory;
import br.org.certi.jocd.target.TargetFactory.targetEnum;

public class Board {

  // Logging
  private static final String TAG = "Board";

  // Link to the dap CMSIS DAP.
  public DapAccessCmsisDap dapAccessLink;

  public Target target;
  public Flash flash;
  boolean closed;

  // Default frequency value.
  // TODO this might be redundant. DapAccessCmsisDap already have a default frequency.
  public int frequency = 1000000;

  /*
   * Must be called right after constructor.
   */
  public void setup(DapAccessCmsisDap link, targetEnum target, int frequency) {
    this.dapAccessLink = link;
    this.target = TargetFactory.getTarget(target);
    this.flash = this.target.getFlash();
    if (frequency > 0) {
      this.frequency = frequency;
    }
    this.closed = false;

    // TODO
    // target.setFlash
  }


  /*
   * Initialize the board.
   */
  public void init() {
    Log.d(TAG, "init board");
    // TODO
  }
}
