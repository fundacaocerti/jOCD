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
package br.org.certi.jocd.core;

import br.org.certi.jocd.coresight.AccessPort;
import br.org.certi.jocd.coresight.AhbAp;
import br.org.certi.jocd.coresight.CortexM;
import br.org.certi.jocd.coresight.DebugPort;
import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoreSightTarget extends Target {

  // Logging
  private final static String CLASS_NAME = CoreSightTarget.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);


  public List<AccessPort> apList = new ArrayList<AccessPort>();
  public DebugPort dp;

  private List<Target> coreList = new ArrayList<Target>();
  private int selectedCore = 0;

  /*
   * Must be called right after constructor.
   * Overload for protected method setup.
   */
  @Override
  public void setup(DapAccessCmsisDap link) {
    this.setup(link, null);
  }

  /*
   * Must be called right after constructor.
   */
  @Override
  protected void setup(DapAccessCmsisDap link, MemoryMap memoryMap) {
    super.setup(link, memoryMap);

    this.dp = new DebugPort(link);
  }

  @Override
  public Target getSelectedCore() throws Error {
    if (selectedCore >= this.coreList.size()) {
      LOGGER.log(Level.SEVERE, "getSelectedCore: unexpected core index: " + selectedCore);
      throw new Error("getSelectedCore: unexpected core index: " + selectedCore);
    }

    return this.coreList.get(selectedCore);
  }

  public void selectCore(int coreIndex) {
    if (selectedCore >= this.coreList.size()) {
      LOGGER.log(Level.SEVERE, "selectCore: unexpected core index: " + selectedCore);
      return;
    }

    this.selectedCore = coreIndex;
  }

  public void addAp(AccessPort ap) {
    this.apList.add(ap);
  }

  public void addCore(Target core) {
    this.coreList.add(core);
  }

  /*
   * Overload for protected init(Boolean busAccessible).
   */
  @Override
  public void init() throws TimeoutException, Error {
    this.init(true);
  }

  /*
   * Protected init. This will be called from public method init or from child classes.
   */
  protected void init(Boolean busAccessible) throws TimeoutException, Error {
    // Set default value if null.
    if (busAccessible == null) {
      busAccessible = true;
    }

    // Create the DP and turn on debug.
    this.dp.init();
    this.dp.powerUpDebug();

    // Create an AHB-AP for the CPU.
    AccessPort ap0 = new AhbAp(this.dp, 0);
    ap0.init(busAccessible);
    this.addAp(ap0);

    // Create CortexM core.
    Target core0 = new CortexM(this.dp, this.apList.get(0));
    core0.setup(this.link, this.memoryMap);
    if (busAccessible) {
      core0.init();
      this.addCore(core0);
    }
  }

  @Override
  public void flush() throws TimeoutException, Error {
    this.getSelectedCore().flush();
  }

  @Override
  public void halt() throws TimeoutException, Error {
    this.getSelectedCore().halt();
  }

  @Override
  public void resume() throws TimeoutException, Error {
    this.getSelectedCore().resume();
  }

  @Override
  public void writeMemory(long address, long value) throws TimeoutException, Error {
    // 32 is the default transfer size.
    writeMemory(address, value, 32);
  }

  @Override
  public void writeMemory(long address, long value, Integer transferSize)
      throws TimeoutException, Error {
    this.getSelectedCore().writeMemory(address, value, transferSize);
  }

  @Override
  public byte[] readBlockMemoryUnaligned8(long address, int size) throws TimeoutException, Error {
    return this.getSelectedCore().readBlockMemoryUnaligned8(address, size);
  }

  @Override
  public long[] readBlockMemoryAligned32(long address, int size) throws TimeoutException, Error {
    return this.getSelectedCore().readBlockMemoryAligned32(address, size);
  }

  @Override
  public void writeBlockMemoryUnaligned8(long address, byte[] data) throws TimeoutException, Error {
    this.getSelectedCore().writeBlockMemoryUnaligned8(address, data);
  }

  @Override
  public void writeBlockMemoryAligned32(long address, long[] words) throws TimeoutException, Error {
    this.getSelectedCore().writeBlockMemoryAligned32(address, words);
  }

  @Override
  public void writeCoreRegister(CoreRegister reg, long word) throws TimeoutException, Error {
    this.getSelectedCore().writeCoreRegister(reg, word);
  }

  @Override
  public long readCoreRegisterRaw(CoreRegister reg) throws Error {
    return this.getSelectedCore().readCoreRegisterRaw(reg);
  }

  @Override
  public long[] readCoreRegisterRaw(List<CoreRegister> regList) throws Error {
    return this.getSelectedCore().readCoreRegisterRaw(regList);
  }

  @Override
  public void writeCoreRegisterRaw(CoreRegister reg, long word) throws TimeoutException, Error {
    this.getSelectedCore().writeCoreRegisterRaw(reg, word);
  }

  @Override
  public void writeCoreRegisterRaw(List<CoreRegister> regList, long[] words)
      throws TimeoutException, Error {
    this.getSelectedCore().writeCoreRegisterRaw(regList, words);
  }

  @Override
  public void reset(Boolean softwareReset) throws InterruptedException, TimeoutException, Error {
    this.getSelectedCore().reset(softwareReset);
  }

  @Override
  public void resetStopOnReset(Boolean softwareReset)
      throws InterruptedException, TimeoutException, Error {
    this.getSelectedCore().resetStopOnReset(softwareReset);
  }

  @Override
  public void setTargetState(State state) throws InterruptedException, TimeoutException, Error {
    this.getSelectedCore().setTargetState(state);
  }

  @Override
  public State getState() throws TimeoutException, Error {
    return this.getSelectedCore().getState();
  }

  @Override
  public void setVectorCatch(long enableMask) throws TimeoutException, Error {
    this.getSelectedCore().setVectorCatch(enableMask);
  }

  @Override
  public long getVectorCatch() throws TimeoutException, Error {
    return this.getSelectedCore().getVectorCatch();
  }
}
