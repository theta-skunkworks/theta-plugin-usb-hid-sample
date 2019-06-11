/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.pluginapplication.task;

import android.os.AsyncTask;
import android.util.Log;

import com.theta360.pluginapplication.network.HttpConnector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CheckApListTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "HIDREMOTE";

    private Callback mCallback;

    public CheckApListTask(CheckApListTask.Callback callback) {
        this.mCallback = callback;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected String doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";
        int listNum = 0;

        String strJsonGetCaptureMode = "{\"name\": \"camera._listAccessPoints\"}";
        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);

        try {
            JSONObject output = new JSONObject(strResult);
            JSONObject results = output.getJSONObject("results");
            JSONArray accessPoints = results.getJSONArray("accessPoints");
            listNum = accessPoints.length();
            mCallback.onCheckApList(listNum);
            Log.d(TAG, "CheckApListTask: listNum=" + String.valueOf(listNum));
        } catch (JSONException e1) {
            e1.printStackTrace();
            strResult = e1.getMessage();
        }

        return strResult;
    }

    @Override
    protected void onPostExecute(String result) {
    }

    public interface Callback {
        void onCheckApList(int listNum);
    }

}
