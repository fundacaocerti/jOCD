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
package certi.org.br.jocdandroidtestapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import br.org.certi.jocd.Jocd;
import br.org.certi.jocd.Jocd.ErrorCode;
import br.org.certi.jocd.tools.ProgressUpdateInterface;

public class AsyncFlashToolFlashBoard extends AsyncTask<String, Integer, String> implements
    ProgressUpdateInterface {

  // Logging
  private static final String TAG = "AsyncFlashBoard";

  // Callback to the UI activity.
  private AsyncResponse.FlashBoard delegate = null;

  // Path to file.
  private String flashFilePath;

  // Board uniqueId
  private String uniqueId;

  // Context (for USB Manager).
  private Context context = null;
  private boolean exceptionOccurred = false;

  /*
   * Constructor.
   */
  public AsyncFlashToolFlashBoard(Context context, AsyncResponse.FlashBoard delegate,
      String flashFilePath, String uniqueId) {
    this.context = context;
    this.delegate = delegate;
    this.flashFilePath = flashFilePath;
    this.uniqueId = uniqueId;
  }

  @Override
  protected String doInBackground(String... params) {
    if (this.flashFilePath == null) {
      this.exceptionOccurred = true;
      onException(new Exception("Can't flash without a file."));
      return "Unable to flash board. Can't flash without a file.";
    }

    this.exceptionOccurred = false;
    ErrorCode flashBoardError = Jocd.flashBoard(this.flashFilePath, this, this.uniqueId);
    if (flashBoardError != ErrorCode.SUCCESS) {
      this.exceptionOccurred = true;
      onException(new Exception("Failed with error code: " + flashBoardError.toString()));
      return "Unable to flash board. Error code: " + flashBoardError.toString();
    }
    return "Board flashed";
  }

  @Override
  protected void onProgressUpdate(Integer... percentage) {
    if (this.exceptionOccurred) {
      return;
    }

    Log.d(TAG, "Flashing device... " + percentage[0] + "%");
    delegate.processAsyncTaskUpdate("Flashing device... " + percentage[0] + "%");
  }

  @Override
  protected void onPostExecute(String result) {
    Log.d(TAG, result);
    delegate.processAsyncTaskFinish(result);
  }

  protected void onException(Exception exception) {
    Log.e(TAG, "Exception caught: " + exception.getMessage());
  }

  /*
   * Callback to receive the current percentage and
   * publish the progress.
   */
  public void progressUpdateCallback(int percentage) {
    publishProgress(percentage);
  }
}
