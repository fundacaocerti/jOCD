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

import br.org.certi.jocd.Jocd.ErrorCode;
import br.org.certi.jocd.board.MbedBoard;
import br.org.certi.jocd.board.MbedBoard.NoBoardConnectedException;
import br.org.certi.jocd.board.MbedBoard.UniqueIDNotFoundException;
import br.org.certi.jocd.board.MbedBoard.UnspecifiedBoardIDException;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import br.org.certi.jocd.dapaccess.dapexceptions.Error;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.flash.FlashBuilder;
import br.org.certi.jocd.flash.PageInfo;
import br.org.certi.jocd.target.TargetFactory.targetEnum;
import cz.jaybee.intelhex.IntelHexException;
import cz.jaybee.intelhex.Parser;
import cz.jaybee.intelhex.listeners.RangeDetector;
import java.io.File;
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
    List<MbedBoard> boards = null;

    try {
      boards = MbedBoard.getAllConnectedBoards();
    } catch (Error exception) {
      LOGGER.log(Level.SEVERE,
          "Error while trying to get all connected boards. Exception: " + exception.getMessage());
      return "[Error]";
    }

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
  public ErrorCode flashBoard(String file, ProgressUpdateInterface progressUpdate) {

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

  /*
   * Overload to call flashBoard with default values.
   */
  public ErrorCode flashBoard(String file, ProgressUpdateInterface progressUpdate,
      String uniqueId) {

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
        uniqueId,      // The board unique ID to use.
        null, // targetEnum - use the default target.
        null);      // frequency - use the default frequency.
  }

  public ErrorCode flashBoard(
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
      String uniqueId,      // The board unique ID to use.
      targetEnum targetOverride, // Override the detected target (detected by serial number).
      Integer frequency    // Set the SWD clock frequency in Hz."
  ) {

    // Select default values.
    if (count == null) {
      count = 1;
    }
    if (skip == null) {
      skip = 1;
    }

    MbedBoard selectedBoard = null;
    try {
      if (uniqueId == null) {
        selectedBoard = MbedBoard.chooseBoard();
      } else {
        selectedBoard = MbedBoard.chooseBoard(uniqueId);
      }
    } catch (UnspecifiedBoardIDException e) {
      LOGGER.log(Level.SEVERE, "Unspecified board ID. Exception: " + e.toString());
      return ErrorCode.INVALID_BOARD;
    } catch (TimeoutException e) {
      LOGGER.log(Level.SEVERE, e.toString());
      return ErrorCode.COMMUNICATION_FAILURE;
    } catch (NoBoardConnectedException e) {
      LOGGER.log(Level.SEVERE, "No board connected. Exception: " + e.toString());
      return ErrorCode.NO_BOARD_CONNECTED;
    } catch (UniqueIDNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Board unique ID not found. Exception: " + e.toString());
      return ErrorCode.INVALID_BOARD;
    } catch (Error error) {
      LOGGER.log(Level.SEVERE, "DAP Access error. Exception: " + error.toString());
      return ErrorCode.COMMUNICATION_FAILURE;
    }
    // As we throw exceptions when MbedBoard.chooseBoard
    // can't find the board, we should never get here with
    // selectedBoard == null.
    if (selectedBoard == null) {
      LOGGER.log(Level.SEVERE, "Unexpected null pointer on flashBoard(). selectedBoard is null");
      return ErrorCode.INVALID_BOARD;
    }

    if (chipErase) {
      LOGGER.log(Level.FINE, "Mass erasing device...");
      if (selectedBoard.target.massErase()) {
        LOGGER.log(Level.FINE, "Successfully erased.");
      } else {
        LOGGER.log(Level.SEVERE, "Error while mass erasing board.");
        return ErrorCode.MASS_ERASING_ERROR;
      }
    }

    if (file == null || file.isEmpty()) {
      try {
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
                pageAddr -= delta;
              }
            }
            LOGGER.log(Level.FINE, "Erasing sector " + String.format("%08X", pageAddr));
            selectedBoard.flash.erasePage(pageAddr);
            pageAddr += pageInfo.size;
          }
        } else {
          LOGGER.log(Level.FINE, "No operation performed");
          return ErrorCode.NO_OPERATION_PERFORMED;
        }
      } catch (InterruptedException e) {
        LOGGER.log(Level.SEVERE, e.toString());
        return ErrorCode.COMMUNICATION_FAILURE;
      } catch (TimeoutException e) {
        LOGGER.log(Level.SEVERE, e.toString());
        return ErrorCode.COMMUNICATION_FAILURE;
      } catch (Error error) {
        LOGGER.log(Level.SEVERE, "DAP Access error. Exception: " + error.toString());
        return ErrorCode.COMMUNICATION_FAILURE;
      }
      return ErrorCode.SUCCESS;
    }

    // Check if the format was provided. If no format
    // were provided, than use the file's extension.
    if (format == null || format.isEmpty()) {
      format = file.substring(file.lastIndexOf("."));
    }

    // If it is a binary file.
    if (format.equals(".bin")) {
      // If no address is specified use the start of rom.
      if (address == null) {
        address = selectedBoard.flash.getFlashInfo().romStart;
      }

      // Open the file.
      InputStream is = null;
      try {
        File inputFile = new File(file);
        is = new FileInputStream(file);
        byte[] byteBuffer = new byte[(int) inputFile.length()];

        if (is.read(byteBuffer) == -1) {
          throw new IOException(
              "EOF reached while trying to read the file");
        }

        selectedBoard.flash
            .flashBlock(address, byteBuffer, true, chipErase, progressUpdate, fastProgram);
      } catch (FileNotFoundException e) {
        LOGGER.log(Level.SEVERE, "File not found: " + file);
        return ErrorCode.FILE_NOT_FOUND;
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Couldn't read the file: " + file);
        return ErrorCode.FILE_NOT_FOUND;
      } catch (InterruptedException e) {
        LOGGER.log(Level.WARNING,
            "InterruptedException while trying to parse IntelHex. Exception: " + e.toString());
        return ErrorCode.CORRUPT_HEX_FILE;
      } catch (Error e) {
        LOGGER.log(Level.SEVERE, "Error. Exception caught: " + e.getMessage());
        return ErrorCode.COMMUNICATION_FAILURE;
      } catch (TimeoutException e) {
        LOGGER.log(Level.SEVERE, "Timeout exception on program. Exception: " + e.toString());
        return ErrorCode.COMMUNICATION_FAILURE;
      } catch (OutOfMemoryError e) {
        LOGGER.log(Level.SEVERE, "Couldn't allocate memory to program the file: " + file);
        return ErrorCode.NO_OPERATION_PERFORMED;
      } finally {
        try {
          is.close();
        } catch (IOException e) {
          LOGGER.log(Level.FINE, "Couldn't close the file: " + file);
        }
      }
    }
    // Intel Hex format.
    else if (format.equals(".hex")) {
      FileInputStream is = null;
      try {
        // Create input stream of some IntelHex data.
        is = new FileInputStream(new File(file));

        // Create IntelHexParserObject.
        Parser intelhexParser = new Parser(is);

        // Create a listener to IntelHex.
        // 1st iteration - detect all ranges.
        RangeDetector rangeDetector = new RangeDetector();
        intelhexParser.setDataListener(rangeDetector);
        intelhexParser.parse();
        is.getChannel().position(0);

        // Create another listener to IntelHex.
        // 2nd iteration - write the data into each range.
        IntelHexToFlash listener = new IntelHexToFlash(rangeDetector.getMemoryRegions(),
            selectedBoard.flash);
        intelhexParser.setDataListener(listener);
        intelhexParser.parse();

        FlashBuilder flashBuilder = listener.getFlashBuilder();
        flashBuilder.program(chipErase, progressUpdate, true, fastProgram);
      } catch (FileNotFoundException e) {
        LOGGER.log(Level.SEVERE, "File not found: " + file);
        return ErrorCode.FILE_NOT_FOUND;
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE,
            "IOException while trying program using IntelHex. Exception: " + e.toString());
        return ErrorCode.CORRUPT_HEX_FILE;
      } catch (IntelHexException e) {
        LOGGER.log(Level.SEVERE,
            "IntelHexException while trying to parse IntelHex. Exception: " + e.toString());
        return ErrorCode.CORRUPT_HEX_FILE;
      } catch (InterruptedException e) {
        LOGGER.log(Level.WARNING,
            "InterruptedException while trying to parse IntelHex. Exception: " + e.toString());
        return ErrorCode.CORRUPT_HEX_FILE;
      } catch (Error e) {
        LOGGER.log(Level.SEVERE, "Error. Exception caught: " + e.getMessage());
        return ErrorCode.COMMUNICATION_FAILURE;
      } catch (TimeoutException e) {
        LOGGER.log(Level.SEVERE, "Timeout exception on program. Exception: " + e.toString());
        return ErrorCode.COMMUNICATION_FAILURE;
      } finally {
        try {
          is.close();
        } catch (IOException e) {
          LOGGER.log(Level.FINE, "Couldn't close the file: " + file);
        }
      }
    }

    selectedBoard.uninit(true);
    return ErrorCode.SUCCESS;
  }
}