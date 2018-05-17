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

public class BreakpointProvider {

  public void init() {
    throw new InternalError("Not implemented");
  }

  public boolean doFilterMemory() {
    return false;
  }

  public int availableBreakpoints() {
    throw new InternalError("Not implemented");
  }

  public Breakpoint find_breakpoint(long address) {
    throw new InternalError("Not implemented");
  }

  public boolean setBreakpoint(long address) {
    throw new InternalError("Not implemented");
  }

  public void removeBreakpoint(Breakpoint bp) {
    throw new InternalError("Not implemented");
  }

  public long[] filterMemory(long address, int size, long[] words) {
    return words;
  }

  public void flush() {
  }
}
