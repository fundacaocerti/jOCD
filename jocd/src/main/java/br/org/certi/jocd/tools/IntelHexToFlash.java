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
import java.util.logging.Level;
import java.util.logging.Logger;

public class IntelHexToFlash implements DataListener {

  // Logging
  private final static String CLASS_NAME = IntelHexToFlash.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private final MemoryRegions detectedRegions;
  private final FlashBuilder flashBuilder;

  // List to hold each region, and inside each, store respective the data.
  private byte[][] buffer;

  public IntelHexToFlash(MemoryRegions detectedRegions, Flash flash) {
    this.flashBuilder = flash.getFlashBuilder();
    this.detectedRegions = detectedRegions;

    buffer = new byte[detectedRegions.size()][];
    // Initialize the buffer for each memory region.
    for (int i = 0; i < detectedRegions.size(); i++) {
      buffer[i] = new byte[(int) detectedRegions.get(i).getLength()];
    }
  }

  @Override
  public void data(long address, byte[] data) {
    // Add data in each detected region.
    for (int i = 0; i < detectedRegions.size(); i++) {
      Region region = detectedRegions.get(i);

      // Check if these data belongs to this region.
      if ((address >= region.getAddressStart()) && (address <= region.getAddressEnd())) {
        int length = data.length;

        // Make sure it doesn't overlap the region.
        if ((address + length) > region.getAddressEnd()) {
          length = (int) (region.getAddressEnd() - address + 1);
        }

        // Copy the data to the respective buffer.
        int offset = (int) (address - region.getAddressStart());
        System.arraycopy(data, 0, buffer[i], offset, length);
        return;
      }
    }

    // We should never get here since each data will have it own region to be included and will
    // return after be copied.
    LOGGER.log(Level.SEVERE, "Unexpected out of range data.");
    throw new InternalError("Unexpected out of range data.");
  }

  @Override
  public void eof() {
    // Add this data range to Flash Builder.
    for (int i = 0; i < detectedRegions.size(); i++) {
      flashBuilder.addData(detectedRegions.get(i).getAddressStart(), buffer[i]);
    }
  }

  /*
   * Return the FlashBuilder. FlashBuilder will be ready to program,
   * if call this class method be called after "parser" InterHex.
   */
  public FlashBuilder getFlashBuilder() {
    return flashBuilder;
  }
}