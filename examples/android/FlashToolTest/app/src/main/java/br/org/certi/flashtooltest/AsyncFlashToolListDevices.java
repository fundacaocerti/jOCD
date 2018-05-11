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
package br.org.certi.flashtooltest;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import br.org.certi.jocd.board.MbedBoard;
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import br.org.certi.jocd.dapaccess.dapexceptions.InsufficientPermissions;
import br.org.certi.jocd.tools.AsyncResponse;
import br.org.certi.jocd.tools.FlashTool;
import br.org.certi.jocd.tools.ProgressUpdateInterface;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class AsyncFlashToolListDevices extends AsyncTask<String, String, List<MbedBoard>> implements
    ProgressUpdateInterface {

  // Logging
  private static final String TAG = "AsyncListDevices";

  // Callback to the UI activity.
  private AsyncResponse.ListBoards delegate = null;

  // Context (for USB Manager).
  private Context context = null;
  private boolean exceptionOccurred = false;

  /*
   * Constructor.
   */
  public AsyncFlashToolListDevices(Context context,
      AsyncResponse.ListBoards delegate) {
    this.context = context;
    this.delegate = delegate;
  }

  @Override
  protected List<MbedBoard> doInBackground(String... params) {
    String resp = "Devices: ";

    List<MbedBoard> boards = null;
    this.exceptionOccurred = false;

    publishProgress("Starting listing devices...");

    try {
      boards = MbedBoard.getAllConnectedBoards();
    } catch (Exception exception) {
      this.exceptionOccurred = true;
      onException(exception);
      return null;
    }

    publishProgress("Finished listing devices.");

    return boards;
  }

  @Override
  protected void onProgressUpdate(String... text) {
    if (this.exceptionOccurred) {
      return;
    }

    Log.d(TAG, text[0]);
    delegate.processAsyncTaskUpdate(text[0]);
  }

  @Override
  protected void onPostExecute(List<MbedBoard> boards) {
    if (this.exceptionOccurred) {
      return;
    }

    Log.d(TAG, "Number of boards: " + (boards == null ? 0 : boards.size()));
    delegate.processAsyncTaskFinish(boards);
  }

  protected void onException(Exception exception) {
    Log.d(TAG, "Exception " + exception.getMessage());
    delegate.processAsyncException(exception);
  }

  /*
   * Callback to receive the current percentage and
   * publish the progress.
   */
  public void progressUpdateCallback(int percentage) {
    publishProgress("Listing devices... " + percentage + "%");
  }
}
