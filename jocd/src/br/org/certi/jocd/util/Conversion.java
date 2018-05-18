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
package br.org.certi.jocd.util;

import java.util.logging.Logger;

public class Conversion {

  /*
   * Convert a list of bytes to a list of 32-bit integers (little endian)
   */
  public static long[] byteListToU32leList(byte[] data) {
    int remainder = (data.length % 4 > 0) ? 1 : 0;
    int newSize = data.length / 4 + remainder;
    long[] res = new long[newSize];
    for (int i = 0; i < (data.length / 4); i++) {
      Util.appendDataInArray(res,
          data[i * 4 + 0] | data[i * 4 + 1] << 8 | data[i * 4 + 2] << 16 | data[i * 4 + 3] << 24);
    }
    return res;
  }

  /*
   * Convert a word array into a byte array
   */
  public static byte[] u32leListToByteList(long[] words) {
    byte[] res = new byte[4 * words.length];
    for (long d : words) {
      res[0] = (byte) ((d >> 0) & 0xFF);
      res[1] = (byte) ((d >> 8) & 0xFF);
      res[2] = (byte) ((d >> 16) & 0xFF);
      res[3] = (byte) ((d >> 24) & 0xFF);
    }
    return res;
  }

  /*
   * Convert an IEEE754 float to a 32-bit int.
   */
  public static long float32beToU32be(long word) {
    throw new InternalError("Not implemented");
  }
}
