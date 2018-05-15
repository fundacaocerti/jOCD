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

import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {

  // Logging
  private final static String CLASS_NAME = Util.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  public static enum OperatingSystem {
    Android,
    Windows,
    Linux,
    MacOsX,
    SunOs,
    FreeBsd
  }

  public static OperatingSystem getOS() {
    // Try to identify which OS is running this library.
    String osName = System.getProperty("os.name");
    OperatingSystem os = null;

    do {
      if (osName.startsWith("Linux")) {
        if (System.getProperty("java.vm.vendor").equals("The Android Project")) {
          os = OperatingSystem.Android;
          break;
        }
        os = OperatingSystem.Linux;
        break;
      }

      if (osName.startsWith("Windows")) {
        os = OperatingSystem.Windows;
        break;
      }

      if (osName.startsWith("Mac OS X")) {
        os = OperatingSystem.MacOsX;
        break;
      }

      if (osName.startsWith("SunOS")) {
        os = OperatingSystem.SunOs;
        break;
      }

      if (osName.startsWith("FreeBSD")) {
        os = OperatingSystem.SunOs;
        break;
      }
    } while (false);

    LOGGER.log(Level.FINE, os.toString());
    return os;
  }

  /*
   * Extend an existing byte array with another array and return
   * this new concatenated array.
   *
   * As we can not expand the size of an array in Java, we need
   * to create a new one, and concatenate both arrays.
   */
  public static byte[] appendDataInArray(byte[] src, byte[] append, int start, int end) {

    // Sanity check.
    int count = end - start;
    if (count < 0) {
      LOGGER.log(Level.SEVERE, "Internal error: end position is lower than start position at" +
          "appendDataInArray.");
      return null;
    }

    // Create a new array to extend its size.
    byte[] newArray = new byte[src.length + end - start];

    // Copy currentPage data to the new array.
    System.arraycopy(src, 0, newArray, 0, src.length);

    // Copy oldData to the new array.
    System.arraycopy(append, start, newArray, append.length, count);

    // Return the appended array.
    return newArray;
  }

  /*
   * Extend an existing byte array with another array and return
   * this new concatenated array.
   *
   * As we can not expand the size of an array in Java, we need
   * to create a new one, and concatenate both arrays.
   */
  public static byte[] appendDataInArray(byte[] src, byte[] append) {

    // Create a new array to extend its size.
    byte[] newArray = new byte[src.length + append.length];

    // Copy currentPage data to the new array.
    System.arraycopy(src, 0, newArray, 0, src.length);

    // Copy oldData to the new array.
    System.arraycopy(append, 0, newArray, src.length, append.length);

    // Return the appended array.
    return newArray;
  }

  /*
   * Fill and existing byte array with a value up to "size".
   *
   * As we can not expand the size of an array in Java, we need
   * to create a new one, and concatenate both arrays.
   */
  public static byte[] fillArray(byte[] src, int size, byte fillWith) {
    int bytesLeft = size - src.length;

    if (bytesLeft <= 0) {
      // There is no bytes left to fill.
      return src;
    }

    byte[] append = new byte[bytesLeft];
    for (int i = 0; i < bytesLeft; i++) {
      append[i] = fillWith;
    }

    // Return the appended array.
    return appendDataInArray(src, append);
  }

  /*
   * Extract an array from a given byte-array.
   *
   * As we can not expand the size of an array in Java, we need
   * to create a new one, and concatenate both arrays.
   */
  public static byte[] getSubArray(byte[] src, Integer startPos, Integer endPos) {

    // Set default values if not given.
    if (startPos == null) {
      startPos = 0;
    }
    if (endPos == null) {
      endPos = src.length;
    }

    // Calculate the new array size.
    int newSize = endPos - startPos;

    // Sanity check.
    if (newSize < 0) {
      LOGGER.log(Level.SEVERE, "getSubArray: Start position can't be higher than end position.");
      return null;
    }

    // Create a new array to shrink its size.
    byte[] newArray = new byte[newSize];

    // Copy the data, skipping the first "index" characters.
    System.arraycopy(src, startPos, newArray, 0, newSize);

    // Return the shrunk array.
    return newArray;
  }

  /*
   * Extract an array from a given long-array.
   *
   * As we can not expand the size of an array in Java, we need
   * to create a new one, and concatenate both arrays.
   */
  public static long[] getSubArray(long[] src, Integer startPos, Integer endPos) {

    // Set default values if not given.
    if (startPos == null) {
      startPos = 0;
    }
    if (endPos == null) {
      endPos = src.length;
    }

    // Calculate the new array size.
    int newSize = endPos - startPos;

    // Sanity check.
    if (newSize < 0) {
      LOGGER.log(Level.SEVERE, "getSubArray: Start position can't be higher than end position.");
      return null;
    }

    // Create a new array to shrink its size.
    long[] newArray = new long[newSize];

    // Copy the data, skipping the first "index" characters.
    System.arraycopy(src, startPos, newArray, 0, newSize);

    // Return the shrunk array.
    return newArray;
  }

}
