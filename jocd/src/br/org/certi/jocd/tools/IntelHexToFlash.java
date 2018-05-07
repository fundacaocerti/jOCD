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
package br.org.certi.jocd.tools;

import br.org.certi.jocd.flash.Flash;
import br.org.certi.jocd.flash.FlashBuilder;
import cz.jaybee.intelhex.DataListener;
import cz.jaybee.intelhex.MemoryRegions;
import cz.jaybee.intelhex.Region;

public class IntelHexToFlash implements DataListener {

    // Logging
    private static final String TAG = "IntelHexToFlash";

    private final MemoryRegions regions = new MemoryRegions();

    private final FlashBuilder flashBuilder;

    public IntelHexToFlash(Flash flash) {
        this.flashBuilder = flash.getFlashBuilder();
    }

    @Override
    public void data(long address, byte[] data) {
        regions.add(address, data.length);

        // Add this data range to Flash Builder.
        flashBuilder.addData(address, data);
    }

    @Override
    public void eof() {
        regions.compact();
    }

    public void reset() {
        regions.clear();
    }

    public Region getFullRangeRegion() {
        return regions.getFullRangeRegion();
    }

    public MemoryRegions getMemoryRegions() {
        return regions;
    }

    /*
     * Return the FlashBuilder. FlashBuilder will be ready to program,
     * if call this class method be called after "parser" InterHex.
     */
    public FlashBuilder getFlashBuilder() {
        return flashBuilder;
    }
}