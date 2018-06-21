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

public class MemoryRegion extends MemoryRangeBase {

  public final MemoryMap.RegionType regionType;
  public int blockSize = 0;
  public String name;
  public boolean isBootMemory;
  public boolean isPoweredOnBoot;
  public boolean isCacheable;
  public boolean invalidateCacheOnRun;

  public static final int DEFAULT_BLOCK_SIZE = 0;
  public static final boolean DEFAULT_IS_BOOT_MEMORY = false;
  public static final boolean DEFAULT_IS_POWERED_ON_BOOT = true;
  public static final boolean DEFAULT_IS_CACHEABLE = true;
  public static final boolean DEFAULT_INVALIDATE_CACHE_ON_RUN = true;

  public MemoryRegion(MemoryMap.RegionType type, long start, long length, int blockSize,
      String name, boolean isBootMemory, boolean isPoweredOnBoot, boolean isCacheable,
      boolean invalidateCacheOnRun) {

    super(start, length);
    this.regionType = type;
    this.blockSize = blockSize;
      this.name = (name == null || name.isEmpty()) ? this.regionType.toString() : name;
    this.isBootMemory = isBootMemory;
    this.isPoweredOnBoot = isPoweredOnBoot;
    this.isCacheable = isCacheable;
    this.invalidateCacheOnRun = invalidateCacheOnRun;
  }
}
