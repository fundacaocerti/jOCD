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

public class RamRegion extends MemoryRegion {

  // Logging
  private static final String TAG = "RamRegion";

  /*
   * Constructor.
   */
  public RamRegion(long start, long end, long length, int blockSize, String name,
      boolean isBootMemory, boolean isPoweredOnBoot, boolean isCacheable,
      boolean invalidateCacheOnRun) {
    super(MemoryMap.RegionType.Ram, start, end, length, blockSize, name, isBootMemory,
        isPoweredOnBoot, isCacheable, invalidateCacheOnRun);
  }

  /*
   * Constructor.
   */
  public RamRegion(int start, long length) {
    super(MemoryMap.RegionType.Ram, start, MemoryRangeBase.DEFAULT_END, length,
        MemoryRegion.DEFAULT_BLOCK_SIZE, null, MemoryRegion.DEFAULT_IS_BOOT_MEMORY,
        MemoryRegion.DEFAULT_IS_POWERED_ON_BOOT, MemoryRegion.DEFAULT_IS_CACHEABLE,
        MemoryRegion.DEFAULT_INVALIDATE_CACHE_ON_RUN);
  }
}
