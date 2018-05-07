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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import br.org.certi.jocd.tools.AsyncResponse;

public class MainActivity extends AppCompatActivity implements
        AsyncResponse {

    // Logging
    private static final String TAG = "MainActivity";

    TextView textViewConnectedBoards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewConnectedBoards = (TextView)findViewById(R.id.textViewConnectedBoards);
        textViewConnectedBoards.setText("Devices....");
    }

    public void onClickListDevices(View view) {
        Log.d("CLICK", "Button list devices clicked");

        // Create a new async task to list all connected devices.
        AsyncFlashToolListDevices asyncListDevices = new AsyncFlashToolListDevices(this,this);
        asyncListDevices.execute();
    }

    public void onClickFlashDevice(View view) {
        Log.d("CLICK", "Button flash device clicked");

        // Create a new async task to list all connected devices.
        AsyncFlashToolFlashBoard asyncFlashDevice = new AsyncFlashToolFlashBoard(this,this);
        asyncFlashDevice.execute();
    }

    /*
     * Callback to onProgressUpdate (from AsyncTask).
     */
    public void processAsyncTaskUpdate(String status) {
        textViewConnectedBoards.setText(status);
    }

    /*
     * Callback to onPostExecute (from AsyncTask).
     */
    public void processAsyncTaskFinish(String result) {
        textViewConnectedBoards.setText(result);
    }
}
