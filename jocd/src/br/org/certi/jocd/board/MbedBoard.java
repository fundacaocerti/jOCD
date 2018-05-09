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
package br.org.certi.jocd.board;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import br.org.certi.jocd.dapaccess.DapAccessCmsisDap;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import br.org.certi.jocd.target.TargetFactory;
import br.org.certi.jocd.target.TargetFactory.targetEnum;
import java.util.ArrayList;
import java.util.List;

public class MbedBoard extends Board {

  // Logging
  private static final String TAG = "MbedBoard";

  public final String uniqueId;
  public final String boardId;
  public final String name;
  public String testBinary;

  /*
   * Constructor.
   */
  public MbedBoard(DapAccessCmsisDap link, TargetFactory.targetEnum targetOverride, int frequency)
      throws UnsupportedBoardException {
    super();

    this.uniqueId = link.getUniqueId();
    boardId = this.uniqueId.substring(0, 4);

    TargetFactory.targetEnum target = null;
    BoardInfo boardInfo = BoardInfo.getBoardInfo(boardId);
    if (boardInfo != null) {
      // Get the board name.
      this.name = boardInfo.name;
      // Get the native target.
      target = boardInfo.target;
      // Get the binary test.
      this.testBinary = boardInfo.binary;
    } else {
      this.name = "Unknown Board";
    }

    // Override the native target... (if supposed to)
    if (targetOverride != null) {
      target = targetOverride;
    }

    // Check if we have a valid target.
    if (target == null) {
      throw new UnsupportedBoardException(boardId);
      // TODO
      // pyOCD uses cotex_m here. Check if it makes sense.
      // (mved_board.py line 130)
    }

    super.setup(link, target, frequency);
  }

  /*
   * Overload for getAllConnectedBoards using default values.
   */
  public static List<MbedBoard> getAllConnectedBoards(Context context) throws DeviceError {
    return getAllConnectedBoards(context, false, true, null, 0);
  }

  /*
   * Return an array of all mbed boards connected.
   */
  public static List<MbedBoard> getAllConnectedBoards(Context context, boolean close,
      boolean blocking, targetEnum targetOverride, int frequency) throws DeviceError {
    List<MbedBoard> mbedList = new ArrayList<MbedBoard>();

    while (true) {
      List<DapAccessCmsisDap> connectedDaps = DapAccessCmsisDap.getConnectedDevices(context);

      // Loop for each device.
      for (DapAccessCmsisDap dapAccess : connectedDaps) {
        try {
          MbedBoard mbed = new MbedBoard(dapAccess, targetOverride, frequency);
          mbedList.add(mbed);
        } catch (UnsupportedBoardException e) {
          Log.w(TAG, "Board " + dapAccess.getUniqueId() + " is not supported yet.");
        }
      }

      if (close == false) {
        for (DapAccessCmsisDap dapAccess : connectedDaps) {
          dapAccess.open(context);
        }
      }

      // Non-blocking. Leave.
      if (blocking == false) {
        break;
      }
      // Blocking...  check que size of mbedList.
      else if (mbedList.size() > 0) {
        break;
      }
      // Blocking... mbedList empty, sleep 10 ms.
      else {
        // WARNING: Make sure to not call this from a UI thread.
        //          (SystemClock) shouldn't be called from an UI thread.
        SystemClock.sleep(10);
      }

      break;
    }

    return mbedList;
  }

  /*
   * Overwrite for chooseBoard using default values.
   */
  public static MbedBoard chooseBoard(Context context)
      throws NoBoardConnectedException, UniqueIDNotFoundException, UnspecifiedBoardIDException,
      DeviceError {
    return chooseBoard(context, true, false, null, null, 0, true);
  }

  /*
   * Return an array of all mbed boards connected.
   */
  public static MbedBoard chooseBoard(Context context, boolean blocking, boolean returnFirst,
      String boardId, String targetOverride, int frequency, boolean initBoard)
      throws NoBoardConnectedException, UniqueIDNotFoundException, UnspecifiedBoardIDException,
      DeviceError {

    // Get all connected boards.
    List<MbedBoard> allBoards = MbedBoard.getAllConnectedBoards(context);

    // If the board id (serial number) is specified, ignore all other boards.
    if (!TextUtils.isEmpty(boardId)) {

      // Create a new list that will replace the list of all boards.
      // This list will contain only the requested board.
      List<MbedBoard> selectedBoard = new ArrayList<MbedBoard>();

      // For each board in the list, look for the specified ID.
      for (MbedBoard board : allBoards) {

        // Check if this board id match with the specified.
        if (board.uniqueId.equals(boardId)) {
          // Yes, it match. This is the desired board.
          // Add this to the new list.
          selectedBoard.add(board);

          // Leave the loop.
          break;
        }

        // Replace the full list for the new one (containing only the selected board).
        allBoards = selectedBoard;
      }
    }

    // We should have at least one board. If we don't have, return.
    if (allBoards == null || allBoards.size() == 0) {
      // We don't have any board in the list. This might be
      // because we don't have any connected or because
      // the requested board isn't here (or the user typed the
      // wrong id).
      // Throw this info as exception as Java does't allow us
      // to return more than one object.
      if (TextUtils.isEmpty(boardId)) {
        throw new NoBoardConnectedException(allBoards);
      } else {
        throw new UniqueIDNotFoundException(allBoards);
      }
    }

    // Select first board?
    if (returnFirst) {
      List<MbedBoard> selectedBoard = new ArrayList<MbedBoard>();
      selectedBoard.add(allBoards.get(0));
      allBoards = selectedBoard;
    }

    // If we have more than one board left, we can't
    // continue. User has to select one (or specify to
    // return the first...).
    if (allBoards.size() > 1) {
      throw new UnspecifiedBoardIDException(allBoards);
    }

    // When we get here, means that the have one (and only one)
    // board on the allBoards list.
    MbedBoard board = allBoards.get(0);
    board.dapAccessLink.open(context);

    // Init the board (if we should...).
    if (initBoard) {
      try {
        board.init();
      } catch (Exception e) {
        board.dapAccessLink.close();
        // TODO handle this exception corretly
        throw e;
      }
    }

    return board;
  }

  public static class NoBoardConnectedException extends Exception {

    List<MbedBoard> boards;

    /*
     * Constructor
     */
    public NoBoardConnectedException(List<MbedBoard> boards) {
      this.boards = boards;
    }
  }

  public static class UniqueIDNotFoundException extends Exception {

    List<MbedBoard> boards;

    /*
     * Constructor
     */
    public UniqueIDNotFoundException(List<MbedBoard> boards) {
      this.boards = boards;
    }
  }

  public static class UnspecifiedBoardIDException extends Exception {

    List<MbedBoard> boards;

    /*
     * Constructor
     */
    public UnspecifiedBoardIDException(List<MbedBoard> boards) {
      this.boards = boards;
    }
  }

  public static class UnsupportedBoardException extends Exception {

    String boardId;

    /*
     * Constructor
     */
    public UnsupportedBoardException(String boardId) {
      this.boardId = boardId;
    }
  }
}
