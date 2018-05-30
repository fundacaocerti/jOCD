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
      long word = 0;
      word |= 0x000000FFL & (data[0 + i * 4] << 0);
      word |= 0x0000FF00L & (data[1 + i * 4] << 8);
      word |= 0x00FF0000L & (data[2 + i * 4] << 16);
      word |= 0xFF000000L & (data[3 + i * 4] << 24);
      Util.appendDataInArray(res, word);
    }
    return res;
  }

  /*
   * Convert a word array into a byte array
   */
  public static byte[] u32leListToByteList(long[] words) {
    byte[] res = new byte[4 * words.length];
    for (int i = 0; i < words.length; i++) {
      long word = words[i];
      res[i * 4 + 0] = (byte) ((word >> 0) & 0xFF);
      res[i * 4 + 1] = (byte) ((word >> 8) & 0xFF);
      res[i * 4 + 2] = (byte) ((word >> 16) & 0xFF);
      res[i * 4 + 3] = (byte) ((word >> 24) & 0xFF);
    }
    return res;
  }

  /*
   * Convert an IEEE754 float to a 32-bit int.
   */
  public static long float32beToU32be(long word) {
    throw new InternalError("Not implemented");
  }

  /*
   * Convert a 32-bit int to an IEEE754 float.
   */
  public static long u32BEToFloat32BE(long word) {
    throw new InternalError("Not implemented");
  }
}
