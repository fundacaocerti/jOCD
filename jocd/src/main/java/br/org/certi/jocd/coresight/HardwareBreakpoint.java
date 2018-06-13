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

import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.core.Target.BreakpointTypes;
import br.org.certi.jocd.debug.breakpoints.Breakpoint;
import br.org.certi.jocd.debug.breakpoints.BreakpointProvider;

public class HardwareBreakpoint extends Breakpoint {

  public long compRegisterAddress;
  public Target.BreakpointTypes type;

  public HardwareBreakpoint(long compRegisterAddress, BreakpointProvider provider) {
    super(provider);

    this.compRegisterAddress = compRegisterAddress;
    this.type = BreakpointTypes.HW;
  }
}
