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
package br.org.certi.jocd.Tests;

import static org.junit.Assert.assertEquals;

import br.org.certi.jocd.core.MemoryRangeBase;
import java.util.List;
import org.junit.Test;

public class TestCheckRange {

  @Test
  public void test1() {
    final long startAddressInput = 0;
    final long endAddressInput = 0x1FFL;

    List<Object> result = MemoryRangeBase
        .checkRange(startAddressInput, endAddressInput, null, null);
    final long startAddressResult = (long) result.get(0);
    final long endAddressResult = (long) result.get(1);

    assertEquals(startAddressInput, startAddressResult);
    assertEquals(endAddressInput, endAddressResult);
  }

  @Test
  public void test2() {
    final long startAddressInput = 0;
    final Integer length = 0x200;
    final long expectedEndAddress = 0x1FFL;

    List<Object> result = MemoryRangeBase.checkRange(startAddressInput, null, length, null);
    final long startAddressResult = (long) result.get(0);
    final long endAddressResult = (long) result.get(1);

    assertEquals(startAddressInput, startAddressResult);
    assertEquals(expectedEndAddress, endAddressResult);
  }

  @Test(expected = InternalError.class)
  public void test4() {
    List<Object> result = MemoryRangeBase.checkRange(0x100L, null, null, null);
  }

  @Test(expected = InternalError.class)
  public void test5() {
    List<Object> result = MemoryRangeBase.checkRange(0x100L, 0x1FFL, 0x100, null);
  }
}
