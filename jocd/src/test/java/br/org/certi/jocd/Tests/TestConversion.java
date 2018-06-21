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

import static org.junit.Assert.assertArrayEquals;

import br.org.certi.jocd.util.Conversion;
import org.junit.Test;

public class TestConversion {

  @Test
  public void testByteListToU32leList() {
    byte[] data = new byte[32];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }

    long[] expected = new long[] {
        0x03020100L,
        0x07060504L,
        0x0B0A0908L,
        0x0F0E0D0CL,
        0x13121110L,
        0x17161514L,
        0x1B1A1918L,
        0x1F1E1D1CL
    };

    assertArrayEquals(Conversion.byteListToU32leList(data), expected);
  }

  @Test
  public void testU32leListToByteList() {
    long[] words = new long[] {
        0x03020100L,
        0x07060504L,
        0x0B0A0908L,
        0x0F0E0D0CL,
        0x13121110L,
        0x17161514L,
        0x1B1A1918L,
        0x1F1E1D1CL
    };

    byte[] expected = new byte[32];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = (byte) i;
    }

    assertArrayEquals(Conversion.u32leListToByteList(words), expected);
  }
}
