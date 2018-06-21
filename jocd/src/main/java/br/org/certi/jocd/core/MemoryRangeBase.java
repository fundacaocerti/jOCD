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

import java.util.ArrayList;
import java.util.List;

public class MemoryRangeBase implements Comparable<MemoryRangeBase> {

  public long start = 0;
  public long end = 0;
  public long length = 0;

  public static final long DEFAULT_START = 0;
  public static final long DEFAULT_END = 0;
  public static final int DEFAULT_LENGTH = 0;

  public MemoryRangeBase(long start, long length) {
    this.start = start;
    if (length != 0) {
      this.end = this.start + length - 1;
    } else {
      this.end = DEFAULT_END;
    }
    this.length = length;
  }

  public boolean containsAddress(long address) {
    return (address >= this.start) && (address <= this.end);
  }

  public boolean containsRange(long startAddress, Long endAddress, Integer length,
      MemoryRange range) {
    List<Object> result = checkRange(startAddress, endAddress, length, range);
    return this.containsAddress((long) result.get(0)) & this.containsAddress((long) result.get(1));
  }

  public static List<Object> checkRange(long startAddress, Long endAddress, Integer length,
      MemoryRange range) {
    // Only one of endAddress, length or range should be received.
    int count = 0;
    if (endAddress != null) {
      count++;
    }
    if (length != null) {
      count++;
    }
    if (range != null) {
      count++;
    }

    if (count == 0) {
      throw new InternalError("endAddress/length/range should be not null (one must be set).");
    }
    if (count > 1) {
      throw new InternalError("Only one of endAddress/length/range should be set.");
    }

    if (range != null) {
      startAddress = range.start;
      endAddress = range.end;
    } else if (endAddress == null) {
      endAddress = startAddress + length - 1;
    }

    List<Object> result = new ArrayList<Object>();
    result.add(startAddress);
    result.add(endAddress.longValue());
    return result;
  }

  @Override
  public int compareTo(MemoryRangeBase memoryRangeBase) {
    return Long.compare(this.start, memoryRangeBase.start);
  }
}
