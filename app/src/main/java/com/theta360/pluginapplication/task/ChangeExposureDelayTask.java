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

    private int languageIndex;
    private final Integer[][] soundList = {
            {
                    R.raw.ed_off_jp,
                    R.raw.ed_1_jp,
                    R.raw.ed_2_jp,
                    R.raw.ed_3_jp,
                    R.raw.ed_4_jp,
                    R.raw.ed_5_jp,
                    R.raw.ed_6_jp,
                    R.raw.ed_7_jp,
                    R.raw.ed_8_jp,
                    R.raw.ed_9_jp,
                    R.raw.ed_10_jp
            },
            {
                    R.raw.ed_off_en,
                    R.raw.ed_1_en,
                    R.raw.ed_2_en,
                    R.raw.ed_3_en,
                    R.raw.ed_4_en,
                    R.raw.ed_5_en,
                    R.raw.ed_6_en,
                    R.raw.ed_7_en,
                    R.raw.ed_8_en,
                    R.raw.ed_9_en,
                    R.raw.ed_10_en
            }
    };


    private final Context context;
    private int speechVolume;
    private int setExposureDelay = 0;


    public ChangeExposureDelayTask(Context context, int inputDelay, int inSpeechVolume, int inLangIndex) {
        this.context = context;
        speechVolume = inSpeechVolume;
        if ( EXPOSURE_DELAY_OFF<=inputDelay && inputDelay<=EXPOSURE_DELAY_MAX ) {
            setExposureDelay = inputDelay ;
        } else {
            setExposureDelay = EXPOSURE_DELAY_TGGLE;
        }
        if ( SoundManagerTask.LANGUAGE_JP <= inLangIndex && inLangIndex <= SoundManagerTask.LANGUAGE_EN) {
            languageIndex = inLangIndex;
        } else {
            languageIndex = SoundManagerTask.LANGUAGE_EN;
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
            new SoundManagerTask(context, soundList[languageIndex][soundNo], speechVolume).execute();
        } else {
            //実行不可は無処理
        }
    }

}
