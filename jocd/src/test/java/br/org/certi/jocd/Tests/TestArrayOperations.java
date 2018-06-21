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

import br.org.certi.jocd.util.Util;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Test;

public class TestArrayOperations {

  @Test
  public void testAppendByteArray() {
    // Generate random size for both, source array and the array to be appended.
    byte[] srcData = new byte[ThreadLocalRandom.current().nextInt(0, 100)];
    byte[] appendData = new byte[ThreadLocalRandom.current().nextInt(0, 100)];

    // Create an array with the combined size from both array.
    byte[] expectedData = new byte[srcData.length + appendData.length];

    // Generate random data for the source array.
    int expectedIndex = 0;
    for (int i = 0; i < srcData.length; i++) {
      srcData[i] = (byte) ThreadLocalRandom.current().nextInt(0, 0xFF);

      // Add also to the expected array.
      expectedData[expectedIndex++] = srcData[i];
    }

    // Generate random data for the array to be appended.
    for (int i = 0; i < appendData.length; i++) {
      appendData[i] = (byte) ThreadLocalRandom.current().nextInt(0, 0xFF);

      // Add also to the expected array.
      expectedData[expectedIndex++] = appendData[i];
    }

    // Call the append method.
    srcData = Util.appendDataInArray(srcData, appendData);

    // Compare if everything is ok.
    assertEquals(srcData.length, expectedData.length);
    for (int i = 0; i < srcData.length; i++) {
      assertEquals(srcData[i], expectedData[i]);
    }
  }

  @Test
  public void testAppendLongArray() {
    // Generate random size for both, source array and the array to be appended.
    long[] srcData = new long[ThreadLocalRandom.current().nextInt(0, 100)];
    long[] appendData = new long[ThreadLocalRandom.current().nextInt(0, 100)];

    // Create an array with the combined size from both array.
    long[] expectedData = new long[srcData.length + appendData.length];

    // Generate random data for the source array.
    int expectedIndex = 0;
    for (int i = 0; i < srcData.length; i++) {
      srcData[i] = ThreadLocalRandom.current().nextLong(0, 0xFFFFFFFFL);

      // Add also to the expected array.
      expectedData[expectedIndex++] = srcData[i];
    }

    // Generate random data for the array to be appended.
    for (int i = 0; i < appendData.length; i++) {
      appendData[i] = ThreadLocalRandom.current().nextLong(0, 0xFFFFFFFFL);

      // Add also to the expected array.
      expectedData[expectedIndex++] = appendData[i];
    }

    // Call the append method.
    srcData = Util.appendDataInArray(srcData, appendData);

    // Compare if everything is ok.
    assertEquals(srcData.length, expectedData.length);
    for (int i = 0; i < srcData.length; i++) {
      assertEquals(srcData[i], expectedData[i]);
    }
  }

  @Test
  public void testAppendLong() {
    // Generate random size for source array.
    long[] srcData = new long[ThreadLocalRandom.current().nextInt(0, 100)];
    long appendData = ThreadLocalRandom.current().nextLong(0, 0xFFFFFFFFL);

    // Create an array with the combined size.
    long[] expectedData = new long[srcData.length + 1];

    // Generate random data for the source array.
    int expectedIndex = 0;
    for (int i = 0; i < srcData.length; i++) {
      srcData[i] = ThreadLocalRandom.current().nextLong(0, 0xFFFFFFFFL);

      // Add also to the expected array.
      expectedData[expectedIndex++] = srcData[i];
    }

    expectedData[expectedIndex++] = appendData;

    // Call the append method.
    srcData = Util.appendDataInArray(srcData, appendData);

    // Compare if everything is ok.
    assertEquals(srcData.length, expectedData.length);
    for (int i = 0; i < srcData.length; i++) {
      assertEquals(srcData[i], expectedData[i]);
    }
  }

  @Test
  public void testAppendByte() {
    // Generate random size for source array.
    byte[] srcData = new byte[ThreadLocalRandom.current().nextInt(0, 100)];
    byte appendData = (byte) ThreadLocalRandom.current().nextInt(0, 0xFF);

    // Create an array with the combined size.
    long[] expectedData = new long[srcData.length + 1];

    // Generate random data for the source array.
    int expectedIndex = 0;
    for (int i = 0; i < srcData.length; i++) {
      srcData[i] = (byte) ThreadLocalRandom.current().nextInt(0, 0xFF);

      // Add also to the expected array.
      expectedData[expectedIndex++] = srcData[i];
    }

    expectedData[expectedIndex++] = appendData;

    // Call the append method.
    srcData = Util.appendDataInArray(srcData, appendData);

    // Compare if everything is ok.
    assertEquals(srcData.length, expectedData.length);
    for (int i = 0; i < srcData.length; i++) {
      assertEquals(srcData[i], expectedData[i]);
    }
  }

  @Test
  public void testFillWithBytes() {
    // Generate random size for both, source array and the final array.
    byte[] srcData = new byte[ThreadLocalRandom.current().nextInt(0, 99)];
    int finalSize = ThreadLocalRandom.current().nextInt(srcData.length, 101);

    // Create an array with the combined size.
    byte[] expectedData = new byte[finalSize];

    // Create a value to fill with.
    byte fillWith = (byte) ThreadLocalRandom.current().nextInt(0, 0xFF);

    // Generate random data for the source array.
    for (int i = 0; i < srcData.length; i++) {
      srcData[i] = (byte) ThreadLocalRandom.current().nextInt(0, 0xFF);

      // Add also to the expected array.
      expectedData[i] = srcData[i];
    }

    // Fill...
    for (int i = srcData.length; i < expectedData.length; i++) {
      expectedData[i] = fillWith;
    }

    // Call the fillArray method.
    srcData = Util.fillArray(srcData, finalSize, fillWith);

    // Compare if everything is ok.
    assertEquals(srcData.length, finalSize);
    assertEquals(srcData.length, expectedData.length);
    for (int i = 0; i < srcData.length; i++) {
      assertEquals(srcData[i], expectedData[i]);
    }
  }

  @Test
  public void testGetByteSubArray() {
    // Generate random size for both, source array and the final array.
    byte[] srcData = new byte[ThreadLocalRandom.current().nextInt(1, 100)];
    int startPos = ThreadLocalRandom.current().nextInt(0, srcData.length - 1);
    Integer endPos = ThreadLocalRandom.current().nextInt(startPos, 100);

    // endPos can't be over srcData.
    int finalSize;
    if (endPos > srcData.length) {
      endPos = null;
      finalSize = srcData.length - startPos;
    }
    else {
      finalSize = endPos - startPos;
    }

    // Create an array with final size.
    byte[] expectedData = new byte[finalSize];

    // Generate random data for the source array.
    int expectedIndex = 0;
    for (int i = 0; i < srcData.length; i++) {
      srcData[i] = (byte) ThreadLocalRandom.current().nextInt(0, 0xFF);
    }

    int srcPos = startPos;
    for (int i = 0; i < expectedData.length; i++) {
      // Extract the sub-array.
      expectedData[i] = srcData[srcPos++];
    }

    // Call the fillArray method.
    srcData = Util.getSubArray(srcData, startPos, endPos);

    // Compare if everything is ok.
    assertEquals(srcData.length, expectedData.length);
    assertEquals(srcData.length, finalSize);
    for (int i = 0; i < srcData.length; i++) {
      assertEquals(srcData[i], expectedData[i]);
    }
  }

  @Test
  public void testGetLongSubArray() {
    // Generate random size for both, source array and the final array.
    long[] srcData = new long[ThreadLocalRandom.current().nextInt(1, 100)];
    int startPos = ThreadLocalRandom.current().nextInt(0, srcData.length - 1);
    Integer endPos = ThreadLocalRandom.current().nextInt(startPos, 100);

    // endPos can't be over srcData.
    int finalSize;
    if (endPos > srcData.length) {
      endPos = null;
      finalSize = srcData.length - startPos;
    }
    else {
      finalSize = endPos - startPos;
    }

    // Create an array with final size.
    long[] expectedData = new long[finalSize];

    // Generate random data for the source array.
    int expectedIndex = 0;
    for (int i = 0; i < srcData.length; i++) {
      srcData[i] = ThreadLocalRandom.current().nextLong(0, 0xFFFFFFFFL);
    }

    int srcPos = startPos;
    for (int i = 0; i < expectedData.length; i++) {
      // Extract the sub-array.
      expectedData[i] = srcData[srcPos++];
    }

    // Call the fillArray method.
    srcData = Util.getSubArray(srcData, startPos, endPos);

    // Compare if everything is ok.
    assertEquals(srcData.length, expectedData.length);
    assertEquals(srcData.length, finalSize);
    for (int i = 0; i < srcData.length; i++) {
      assertEquals(srcData[i], expectedData[i]);
    }
  }
}
