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

import br.org.certi.jocd.board.MbedBoard;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.flash.FlashBuilder;
import br.org.certi.jocd.flash.PageInfo;
import br.org.certi.jocd.target.TargetFactory.targetEnum;
import cz.jaybee.intelhex.IntelHexException;
import cz.jaybee.intelhex.Parser;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlashTool {

  // Logging
  private final static String CLASS_NAME = FlashTool.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  List<String> supportedFormats = Arrays.asList("bin", "hex");

  // Since we only support nrf51, or EnumSet is just nrf51.
  // In a future release, we should change this set to
  // "complementOf(TargetFactory.targetEnum.cortex_m)"
  // (All others targets except cortex_m: No generic programming)
  EnumSet<targetEnum> supportedTargets = EnumSet.of(targetEnum.nrf51);

  /*
   * Constructor.
   */
  public FlashTool() {
    LOGGER.log(Level.FINE, "Constructor");
  }

  public String listConnectedBoards()
      throws DeviceError, TimeoutException, InsufficientPermissions {
    List<MbedBoard> boards = MbedBoard.getAllConnectedBoards();

    String resp = "";
    int i = 0;
    for (MbedBoard board : boards) {
      i++;
      resp = resp + "Board " + i + ": " + board.name + " (Board ID: " + board.boardId + ")" + "\n";
    }
    LOGGER.log(Level.FINE, resp);
    return resp;
  }

  /*
   * Overload to call flashBoard with default values.
   */
  public boolean flashBoard(ProgressUpdateInterface progressUpdate)
      throws MbedBoard.NoBoardConnectedException, MbedBoard.UniqueIDNotFoundException,
      MbedBoard.UnspecifiedBoardIDException, InternalError, DeviceError, TimeoutException,
      InsufficientPermissions {

    // TODO - change this a to path with the correct file.
    String file = "microbit.hex";
    return flashBoard(
        progressUpdate,
        file,
        false,   // massErase
        false,   // chipErase
        false,  // sectorErase
        null,      // address
        null,       // count
        null,       // format (use the file extension).
        null,            // skip
        false,  // fastProgram
        null,      // boardId - use the first one (we should have only one board connected)
        null, // targetEnum - use the default target.
        null);      // frequency - use the default frequency.
  }

  public boolean flashBoard(
      ProgressUpdateInterface progressUpdate,
      String file,
      boolean massErase,   // Mass erase the target device.
      boolean chipErase,
      // chipErase and  sectorErase can be used alone as individual commands, or they can be used in conjunction with flashing a binary or hex file. For the former, only the erase option
      boolean sectorErase,
      // will be performed. With a file, the erase options specify whether to erase the entire chip before flashing the file, or just to erase only those sectors occupied by the file.
      Long address,        // Start address to erase sector.
      Integer count,       // count - number the sectors to erase. Default is 1.
      String format,
      // The format file to use. If not provided the file extension will be used.
      Integer skip,
      // Skip programming the first N bytes.  This can only be used with binary files
      boolean fastProgram,
      // Use only the CRC of each page to determine if it already has the same data.
      String boardId,      // The boardId to use.
      targetEnum targetOverride, // Override the detected target (detected by serial number).
      Integer frequency    // Set the SWD clock frequency in Hz."
  )
      throws MbedBoard.NoBoardConnectedException, MbedBoard.UniqueIDNotFoundException,
      MbedBoard.UnspecifiedBoardIDException, InternalError, DeviceError, TimeoutException,
      InsufficientPermissions {

    // Select default values.
    if (count == null) {
      count = 1;
    }
    if (skip == null) {
      skip = 1;
    }

    MbedBoard selectedBoard = MbedBoard.chooseBoard();

    // As we throw exceptions when MbedBoard.chooseBoard
    // can't find the board, we should never get here with
    // selectedBoard == null.
    if (selectedBoard == null) {
      LOGGER.log(Level.SEVERE, "Unexpected null pointer on flashBoard(). selectedBoard is " +
          "null after chooseBoard");
      throw new InternalError();
    }

    if (chipErase) {
      LOGGER.log(Level.FINE, "Mass erasing device...");
      if (selectedBoard.target.massErase()) {
        LOGGER.log(Level.FINE, "Successfully erased.");
      } else {
        LOGGER.log(Level.SEVERE, "Error while mass erasing board.");
        return false;
      }
    }

    if (file == null || file.isEmpty()) {
      if (chipErase) {
        LOGGER.log(Level.FINE, "Erasing chip...");
        selectedBoard.flash.init();
        selectedBoard.flash.eraseAll();
        LOGGER.log(Level.FINE, "Done.");
      } else if (sectorErase) {
        selectedBoard.flash.init();
        Long pageAddr = address;

        for (int i = 0; i < count; i++) {
          PageInfo pageInfo = selectedBoard.flash.getPageInfo(pageAddr);
          if (pageInfo == null) {
            break;
          }

          // Align page address on first time through.
          if (i == 0) {
            Long delta = pageAddr % pageInfo.size;

            if (delta > 0) {
              // Address unaligned.
              LOGGER.log(Level.WARNING, "Warning: sector address " +
                  String.format("%08X", pageAddr) + " is unaligned");
              // TODO implement a better way to give this feedback to user.
              pageAddr -= delta;
            }
          }
          LOGGER.log(Level.FINE, "Erasing sector " + String.format("%08X", pageAddr));
          selectedBoard.flash.erasePage(pageAddr);
          pageAddr += pageInfo.size;
        }
      } else {
        // TODO implement a better way to give this feedback to user.
        LOGGER.log(Level.FINE, "No operation performed");
        return false;
      }
      return true;
    }

    // Check if the format was provided. If no format
    // were provided, than use the file's extension.
    if (format == null || format.isEmpty()) {
      format = file.substring(file.lastIndexOf("."));
    }

    // If it is a binary file.
    if (format.equals("bin")) {
      // If no address is specified use the start of rom.
      if (address == null) {
        address = selectedBoard.flash.getFlashInfo().romStart;
      }

      // Open the file.
      try {
        InputStream is = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // TODO flashBlock (....???????...)
        //selectedBoard.flash.flashBlock(address);
      } catch (FileNotFoundException e) {
        LOGGER.log(Level.SEVERE, "File not found: " + file);
      }
    }
    // Intel Hex format.
    else if (format.equals("hex")) {
      try {
        // Create input stream of some IntelHex data.
        InputStream is = new FileInputStream(file);

        // Create IntelHexParserObject.
        Parser intelhexParser = new Parser(is);

        // Create a listener to IntelHex.
        IntelHexToFlash listener = new IntelHexToFlash(selectedBoard.flash);

        // Register parser listener.
        intelhexParser.setDataListener(listener);

        // IntelHexToFlash hava a callback for each region data, and will
        // create Flash Builder after parsing...
        // TODO verify how it works on Python...
        intelhexParser.parse();

        FlashBuilder flashBuilder = listener.getFlashBuilder();
        flashBuilder.program(chipErase, progressUpdate, true, fastProgram);
      } catch (FileNotFoundException e) {
        LOGGER.log(Level.SEVERE, "File not found: " + file);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "IOException while trying program using IntelHex.");
      } catch (IntelHexException e) {
        LOGGER.log(Level.SEVERE, "IntelHexException while trying to parse IntelHex.");
      }

    }
    return true;
  }
}