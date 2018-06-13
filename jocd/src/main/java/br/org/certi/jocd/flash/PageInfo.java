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

public class PageInfo {

  // Default time to program a page in seconds.
  public static final double DEFAULT_PAGE_PROGRAM_WEIGHT = 0.130;

  // Default time to erase a page in milliseconds.
  public static final double DEFAULT_PAGE_ERASE_WEIGHT = 0.048;

  // Default time to erase a chip in milliseconds.
  public static final double DEFAULT_CHIP_ERASE_WEIGHT = 0.174;

  // Start address of this page in milliseconds
  long baseAddress;

  // Time it takes to erase a page in milliseconds
  double eraseWeight;

  // Time it takes to program a page (Not including data transfer time)
  double programWeight;

  // Size of page.
  public int size;

  // Is the function computeCrcs supported?
  public boolean crcSupported;
}