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
import java.util.concurrent.TimeoutException;

public class AhbAp extends MemAp {

  public AhbAp(DebugPort dp, int apNum) {
    super(dp, apNum);
  }

  @Override
  public void initRomTable() throws TimeoutException, Error {
    // Turn on DEMCR.TRCENA before reading the ROM table. Some ROM table entries will come back as garbage if TRCENA is not set.
    try {
      long demcr = this.read32(DEMCR);
      this.write32(DEMCR, demcr | DEMCR_TRCENA);
      this.dp.flush();
    } catch (Error error) {
      // Ignore exception and read whatever we can of the ROM table.
    }

    //Invoke superclass
    super.initRomTable();
  }
}
