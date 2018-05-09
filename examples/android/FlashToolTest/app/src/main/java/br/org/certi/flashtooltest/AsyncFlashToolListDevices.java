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
import br.org.certi.jocd.dapaccess.dapexceptions.DeviceError;
import br.org.certi.jocd.tools.AsyncResponse;
import br.org.certi.jocd.tools.FlashTool;
import br.org.certi.jocd.tools.ProgressUpdateInterface;

public class AsyncFlashToolListDevices extends AsyncTask<String, String, String> implements
    ProgressUpdateInterface {

  // Logging
  private static final String TAG = "AsyncListDevices";

  // Callback to the UI activity.
  private AsyncResponse delegate = null;

  // Context (for USB Manager).
  private Context context = null;

  /*
   * Constructor.
   */
  public AsyncFlashToolListDevices(Context context,
      AsyncResponse delegate) {
    this.context = context;
    this.delegate = delegate;
  }

  @Override
  protected String doInBackground(String... params) {
    String resp = "Devices: ";

    FlashTool tool = new FlashTool(context);

    publishProgress("Starting listing devices...");

    try {
      resp = tool.listConnectedBoards();
    } catch (DeviceError exception) {
      Log.e(TAG, exception.getMessage());
    }

    publishProgress("Finished listing devices.");

    return resp;
  }

  @Override
  protected void onProgressUpdate(String... text) {
    Log.d(TAG, text[0]);
    delegate.processAsyncTaskUpdate(text[0]);
  }

  @Override
  protected void onPostExecute(String result) {
    Log.d(TAG, result);
    delegate.processAsyncTaskFinish(result);
  }

  /*
   * Callback to receive the current percentage and
   * publish the progress.
   */
  public void progressUpdateCallback(int percentage) {
    publishProgress("Listing devices... " + percentage + "%");
  }
}
