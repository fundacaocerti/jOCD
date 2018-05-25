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

import android.os.AsyncTask;
import br.org.certi.jocd.Jocd;
import java.util.HashMap;

public class AsyncFlashToolListDevices extends AsyncTask<String, String, HashMap<String, String>> {

  // Callback to the UI activity.
  private AsyncResponse.ListBoards delegate = null;

  /*
   * Constructor.
   */
  public AsyncFlashToolListDevices(AsyncResponse.ListBoards delegate) {
    this.delegate = delegate;
  }

  @Override
  protected HashMap<String, String> doInBackground(String... params) {
    HashMap<String, String> boards = Jocd.getAllConnectedBoardsName();
    return boards;
  }

  @Override
  protected void onPostExecute(HashMap<String, String> boards) {
    delegate.processAsyncTaskFinish(boards);
  }
}
