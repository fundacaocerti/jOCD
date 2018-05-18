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
package br.org.certi.jocd.flash;

import java.util.List;

public class FlashAlgo {

  public long loadAddress;
  public List<Long> instructions;
  public long pcInit;
  public long pcEraseAll;
  public long pcEraseSector;
  public long pcProgramPage;
  public long beginData;
  public List<Long> pageBuffers;
  public long beginStack;
  public long staticBase;
  public int minProgramLength;
  public boolean analyzerSupported;
  public long analyzerAddress;
}
