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

public class Watchpoint extends HardwareBreakpoint {

  public long address = 0;
  public int size = 0;
  public int func = 0;

  public Watchpoint(long compRegisterAddress, BreakpointProvider provider) {
    super(compRegisterAddress, provider);
  }
}
