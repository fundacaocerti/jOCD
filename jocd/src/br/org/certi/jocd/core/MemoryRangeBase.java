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

public class MemoryRangeBase {

    // Logging
    private static final String TAG = "MemoryRangeBase";

    long start = 0;
    long end = 0;
    long length = 0;

    public static final long DEFAULT_START = 0;
    public static final long DEFAULT_END = 0;
    public static final int DEFAULT_LENGTH = 0;

    public MemoryRangeBase(long start, long end, long length) {
        this.start = start;
        this.end = end;
        this.length = length;
    }

    public boolean containsAddress(long address) {
        return (address >= this.start) && (address <= this.end);
    }
}
