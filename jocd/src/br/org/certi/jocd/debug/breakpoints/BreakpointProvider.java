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
package br.org.certi.jocd.debug.breakpoints;

import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import java.util.concurrent.TimeoutException;

public class BreakpointProvider {

  public void init() throws TimeoutException, Error {
    throw new InternalError("Not implemented");
  }

  public boolean doFilterMemory() {
    return false;
  }

  public int availableBreakpoints() {
    throw new InternalError("Not implemented");
  }

  public Breakpoint findBreakpoint(long address) {
    throw new InternalError("Not implemented");
  }

  public boolean setBreakpoint(long address) {
    throw new InternalError("Not implemented");
  }

  public void removeBreakpoint(Breakpoint bp) {
    throw new InternalError("Not implemented");
  }

  public long filterMemory(long address, int size, long word) {
    return word;
  }

  public byte filterMemory(long address, byte data) {
    return data;
  }

  public void flush() {
  }
}
