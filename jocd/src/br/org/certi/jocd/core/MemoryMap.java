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

import java.util.List;

public class MemoryMap {

    // Logging
    private static final String TAG = "MemoryMap";

    public static enum RegionType {
        Ram,
        Rom,
        Flash,
        Device,
        Alias
    }

    final List<MemoryRegion> memoryRegions;

    public MemoryMap(List<MemoryRegion> memoryRegions) {
        this.memoryRegions = memoryRegions;
    }

    /*
     * Look for which region this address belongs to.
     */
    public MemoryRegion getRegionForAddress(long address) {
        for (MemoryRegion region : this.memoryRegions) {
            if (region.containsAddress(address)) {
                return region;
            }
        }
        return null;
    }
    // TODO
}
