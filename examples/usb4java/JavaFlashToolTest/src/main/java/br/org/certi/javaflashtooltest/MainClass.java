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
package br.org.certi.javaflashtooltest;

import br.org.certi.jocd.Jocd.ErrorCode;
import br.org.certi.jocd.board.MbedBoard;
import br.org.certi.jocd.tools.FlashTool;
import br.org.certi.jocd.tools.ProgressUpdateInterface;
import br.org.certi.jocdconnusb4java.JocdConnUsb4Java;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainClass implements ProgressUpdateInterface {

  public static void main(String[] args) {
    System.out.println("\nCopyright 2018 Fundação CERTI\n");
    System.out.println("Welcome to Java-FlashToolTest from jOCD.");
    System.out.println("Visit our official page for more information:");
    System.out.println("\thttps://www.github.com/fundacaocerti/jOCD\n");

    if (args.length < 1) {
      usage();
      return;
    }

    Logger rootLogger = LogManager.getLogManager().getLogger("");
    rootLogger.setLevel(Level.WARNING);
    for (Handler h : rootLogger.getHandlers()) {
      h.setLevel(Level.WARNING);
    }

    JocdConnUsb4Java.init();
    MainClass mainClass = new MainClass();

    int i = 0;
    while (i < args.length) {
      String arg = args[i];
      i++;

      switch (arg) {
        case "-h":
        case "--help":
          usage();
          return;

        case "--list":
        case "-l":
          mainClass.listDevices();
          return;

        case "--boardid":
        case "-bid":
          // We must have the board id after this argument.
          if (i >= args.length) {
            // Missing argument.
            usage();
          }
          String boardId = args[i++];

          // We must have the hex-file after the boardid.
          if (i >= args.length) {
            // Missing argument.
            usage();
          }
          String file = args[i++];
          mainClass.flashBoard(file, boardId);
          break;

        default:
          // Default: flash the first board, using the hex file passed as 
          // argument.
          mainClass.flashBoard(arg);
      }
    }
  }

  public static void usage() {
    System.out.println("Usage:\n"
        + "javaflasshtool [OPTIONS] [FILE]\n\n"
        + "\tOptions:\n"
        + "\t--help (-h): show this help message.\n"
        + "\t--list (-l): list all the CMSIS-DAP connected devices.\n"
        + "\t--boardid ID (-bid): flash the board with the chosen ID.\n");
  }

  public void flashBoard(String file) {
    FlashTool tool = new FlashTool();
    ErrorCode result = tool.flashBoard(file, this);
    if (result != ErrorCode.SUCCESS) {
      System.out.println("Unable to flash board. Error code: " + result.toString());
      return;
    }
    System.out.println("\nBoard flashed!");
  }

  public void flashBoard(String file, String boardId) {
    FlashTool tool = new FlashTool();
    ErrorCode result = tool.flashBoard(file, this, boardId);
    if (result != ErrorCode.SUCCESS) {
      System.out.println("Unable to flash board. Error code: " + result.toString());
      return;
    }
    System.out.println("\nBoard flashed!");
  }

  public void listDevices() {
    List<MbedBoard> boards = null;
    try {
      boards = MbedBoard.getAllConnectedBoards();
    } catch (Exception e) {
      System.out.println("Couldn't list devices. Exception: " + e.getMessage());
      return;
    }

    int i = 1;
    if (boards.size() < 1) {
        System.out.println("There are no compatible boards connected.");
        return;
    }
    for (MbedBoard board : boards) {
      System.out.println("[" + i++ + "]: " + board.getName() + " (" + board.getUniqueId() + ")");
    }
  }

  @Override
  public void progressUpdateCallback(int percentage) {
    printProgress(percentage);
  }

  private void printProgress(int percentage) {

    StringBuilder string = new StringBuilder(140);
    string
        .append('\r')
        .append(String.join("",
            Collections.nCopies(percentage == 0 ? 2 : 2 - (int) (Math.log10(percentage)), " ")))
        .append(String.format(" %d%% [", percentage))
        .append(String.join("", Collections.nCopies((percentage / 2), "=")))
        .append('>')
        .append(String.join("", Collections.nCopies((100 / 2) - (percentage / 2), " ")))
        .append(']');

    System.out.print(string);
  }
}
