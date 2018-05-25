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
package br.org.certi.jocd;

import br.org.certi.jocd.board.MbedBoard;
import br.org.certi.jocd.dapaccess.connectioninterface.android.AndroidApplicationContext;
import br.org.certi.jocd.tools.FlashTool;
import br.org.certi.jocd.tools.ProgressUpdateInterface;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * jOCD is a Java library for programming and debugging ARM Cortex-M microcontrollers using
 * CMSIS-DAP.
 */
public class Jocd {

  // Logging
  private final static String CLASS_NAME = FlashTool.class.getName();
  private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

  public enum ErrorCode {
    SUCCESS,
    BOARD_UNIQUE_ID_NOT_FOUND,
    NO_BOARD_CONNECTED,
    UNSPECIFIED_BOARD,
    UNSUPPORTED_BOARD_EXCEPTION,
    NO_BOARD_SELECTED,
    TIMEOUT_EXCEPTION,
    INTERRUPTED_EXCEPTION,
    DAP_ACCESS_ERROR,
    NO_OPERATION_PERFORMED,
    MASS_ERASING_ERROR,
    CORRUPT_HEX_FILE,
    FILE_NOT_FOUND
  }

  /**
   * Initializes the Android Application Context in jOCD. It must be executed before any other jOCD
   * library call.
   *
   * @param context the android application context
   */
  public static void init(Object context) {
    AndroidApplicationContext.getInstance().init(context);
  }

  /**
   * Returns map with connected boards name and unique ID.   *
   * @return map with connected boards name and unique ID.
   */
  public static HashMap<String, String> getAllConnectedBoardsName() {
    HashMap<String, String> boardsName = new HashMap<String, String>();
    List<MbedBoard> boards = null;
    try {
      boards = MbedBoard.getAllConnectedBoards();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,
          "There was an error retrieving connected boards. Exception: " + e.toString());
      return boardsName;
    }
    for (MbedBoard board : boards) {
      boardsName.put(board.getUniqueId(), board.getName());
    }
    return boardsName;
  }

  /**
   * Flashes the specified file in the board specified by the provided unique ID.
   *
   * @param file filepath of the file that should be flashed
   * @param progressUpdate callback interface for reporting flashing progress update
   * @param uniqueId board unique ID where to flash file. If null it will search for the connected
   * devices and choose the first one.
   * @return map with connected boards name and unique ID.
   */
  public static ErrorCode flashBoard(String file, ProgressUpdateInterface progressUpdate,
      String uniqueId) {
    FlashTool tool = new FlashTool();
    return tool.flashBoard(file, progressUpdate, uniqueId);
  }
}
