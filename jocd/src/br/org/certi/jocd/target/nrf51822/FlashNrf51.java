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

import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.flash.Flash;
import br.org.certi.jocd.flash.FlashAlgo;

public class FlashNrf51 extends Flash {

  // Logging
  private static final String TAG = "FlashNrf51";

  FlashAlgo flashAlgo;

  /*
   * Constructor.
   */
  public FlashNrf51(Target target) {
    // TODO - pass flashalgo.

    this.flashAlgo = new FlashAlgo();

    super.setup(target, flashAlgo);
  }
}
