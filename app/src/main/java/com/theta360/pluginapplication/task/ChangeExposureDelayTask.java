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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import skunkworks.hid.R;
import com.theta360.pluginapplication.network.HttpConnector;

import org.json.JSONException;
import org.json.JSONObject;


public class ChangeExposureDelayTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "HIDREMOTE";

    public static final int EXPOSURE_DELAY_TGGLE = -1;
    public static final int EXPOSURE_DELAY_OFF = 0;
    public static final int EXPOSURE_DELAY_MAX = 10;

    private final Integer[] soundList = {
            R.raw.ed_off,
            R.raw.ed_01,
            R.raw.ed_02,
            R.raw.ed_03,
            R.raw.ed_04,
            R.raw.ed_05,
            R.raw.ed_06,
            R.raw.ed_07,
            R.raw.ed_08,
            R.raw.ed_09,
            R.raw.ed_10
    };

    private final Context context;
    private int speechVolume;
    private int setExposureDelay = 0;


    public ChangeExposureDelayTask(Context context, int inputDelay, int inSpeechVolume) {
        this.context = context;
        speechVolume = inSpeechVolume;
        if ( EXPOSURE_DELAY_OFF<=inputDelay && inputDelay<=EXPOSURE_DELAY_MAX ) {
            setExposureDelay = inputDelay ;
        } else {
            setExposureDelay = EXPOSURE_DELAY_TGGLE;
        }
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected Integer doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";

        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_STAT, "");
        try {
            JSONObject output2 = new JSONObject(strResult);
            JSONObject state = output2.getJSONObject("state");
            String captureStatus = state.getString("_captureStatus");
            int recordedTime = state.getInt("_recordedTime");

            if ( captureStatus.equals("idle") && (recordedTime==0) ) {

                if ( setExposureDelay == EXPOSURE_DELAY_TGGLE ) {
                    String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"exposureDelay\", \"_latestEnabledExposureDelayTime\"] } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);

                    JSONObject output = new JSONObject(strResult);
                    JSONObject results = output.getJSONObject("results");
                    JSONObject options = results.getJSONObject("options");
                    int curExposureDelay = options.getInt("exposureDelay");
                    int latestEnabledExposureDelayTime = options.getInt("_latestEnabledExposureDelayTime");

                    if ( curExposureDelay == EXPOSURE_DELAY_OFF ) {
                        setExposureDelay = latestEnabledExposureDelayTime;
                    } else {
                        setExposureDelay = EXPOSURE_DELAY_OFF;
                    }
                }
                String strJsonSetCaptureMode = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"exposureDelay\":\"" + String.valueOf(setExposureDelay) +"\"} } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetCaptureMode);
                Log.d(TAG, "ChangeExposureDelayTask: Set exposureDelay=" + String.valueOf(setExposureDelay));

            } else {
                //実行不可
                setExposureDelay = -1;
            }

        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return setExposureDelay;
    }

    @Override
    protected void onPostExecute(Integer result) {
        //resultにあった音声を再生
        Integer soundNo = result;
        if ( (EXPOSURE_DELAY_OFF <= soundNo) && (soundNo <= EXPOSURE_DELAY_MAX) ) {
            new SoundManagerTask(context, soundList[soundNo], speechVolume).execute();
        } else {
            //実行不可は無処理
        }
    }

}
