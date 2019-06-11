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

import org.json.JSONException;
import org.json.JSONObject;


public class ChangeCaptureModeTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "HIDREMOTE";

    public static final String CAPMODE_TGGLE = "";
    public static final String CAPMODE_IMAGE = "image";
    public static final String CAPMODE_VIDEO = "video";

    private String strSetStr = "";

    public ChangeCaptureModeTask(String inputSetStr) {
        if (inputSetStr.equals(CAPMODE_IMAGE)) {
            strSetStr = CAPMODE_IMAGE ;
        } else if (inputSetStr.equals(CAPMODE_VIDEO)) {
            strSetStr = CAPMODE_VIDEO ;
        } else {
            strSetStr = CAPMODE_TGGLE ;
        }
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected String doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";

        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_STAT, "");
        try {
            JSONObject output2 = new JSONObject(strResult);
            JSONObject state = output2.getJSONObject("state");
            String captureStatus = state.getString("_captureStatus");
            int recordedTime = state.getInt("_recordedTime");

            if ( captureStatus.equals("idle") && (recordedTime==0) ) {

                if ( strSetStr.equals(CAPMODE_TGGLE) ) {
                    String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\":[\"captureMode\"] } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);
                    try {
                        JSONObject output = new JSONObject(strResult);
                        JSONObject results = output.getJSONObject("results");
                        JSONObject options = results.getJSONObject("options");
                        String captureMode = options.getString("captureMode");
                        if ( captureMode.equals(CAPMODE_IMAGE) ) {
                            strSetStr = CAPMODE_VIDEO;
                        } else {
                            strSetStr = CAPMODE_IMAGE;
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
                String strJsonSetCaptureMode = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"captureMode\":\"" + strSetStr +"\"} } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetCaptureMode);
                Log.d(TAG, "ChangeCaptureModeTask: Set CaptureMode=" + strSetStr);
                strResult = strSetStr;
            } else {
                //実行不可
                strResult = "";
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return strResult;
    }

    @Override
    protected void onPostExecute(String result) {

    }

}
