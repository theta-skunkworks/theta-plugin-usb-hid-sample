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


public class ChangeEvTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "HIDREMOTE";

    public static final int EV_MINUS = -2;
    public static final int EV_PLUS = -1;
    public static final int EV_M20 = 0;
    public static final int EV_M17 = 1;
    public static final int EV_M13 = 2;
    public static final int EV_M10 = 3;
    public static final int EV_M07 = 4;
    public static final int EV_M03 = 5;
    public static final int EV_ZERO = 6;
    public static final int EV_P03 = 7;
    public static final int EV_P07 = 8;
    public static final int EV_P10 = 9;
    public static final int EV_P13 = 10;
    public static final int EV_P17 = 11;
    public static final int EV_P20 = 12;
    public static final int EV_MIN = EV_M20;
    public static final int EV_MAX = EV_P20;
    public static final int EV_ERR = 13;

    public static final String evList[] = {
            "-2.0",
            "-1.7",
            "-1.3",
            "-1.0",
            "-0.7",
            "-0.3",
            "0.0",
            "0.3",
            "0.7",
            "1.0",
            "1.3",
            "1.7",
            "2.0"
    };

    /*
    private final Integer[] soundList = {
            R.raw.ev_m20_en,
            R.raw.ev_m17_en,
            R.raw.ev_m13_en,
            R.raw.ev_m10_en,
            R.raw.ev_m07_en,
            R.raw.ev_m03_en,
            R.raw.ev_0_en,
            R.raw.ev_p03_en,
            R.raw.ev_p07_en,
            R.raw.ev_p10_en,
            R.raw.ev_p13_en,
            R.raw.ev_p17_en,
            R.raw.ev_p20_en
    };
     */
    private int languageIndex;
    private final Integer[][] soundList = {
            {
                    R.raw.ev_m20_jp,
                    R.raw.ev_m17_jp,
                    R.raw.ev_m13_jp,
                    R.raw.ev_m10_jp,
                    R.raw.ev_m07_jp,
                    R.raw.ev_m03_jp,
                    R.raw.ev_0_jp,
                    R.raw.ev_p03_jp,
                    R.raw.ev_p07_jp,
                    R.raw.ev_p10_jp,
                    R.raw.ev_p13_jp,
                    R.raw.ev_p17_jp,
                    R.raw.ev_p20_jp,
                    R.raw.ev_error_jp
            },
            {
                    R.raw.ev_m20_en,
                    R.raw.ev_m17_en,
                    R.raw.ev_m13_en,
                    R.raw.ev_m10_en,
                    R.raw.ev_m07_en,
                    R.raw.ev_m03_en,
                    R.raw.ev_0_en,
                    R.raw.ev_p03_en,
                    R.raw.ev_p07_en,
                    R.raw.ev_p10_en,
                    R.raw.ev_p13_en,
                    R.raw.ev_p17_en,
                    R.raw.ev_p20_en,
                    R.raw.ev_error_en
            }
    };




    private final Context context;
    private int speechVolume;
    private int setEv = EV_ZERO;

    public ChangeEvTask(Context context, int inputEv, int inSpeechVolume, int inLangIndex) {
        this.context = context;
        speechVolume = inSpeechVolume;
        if ( (EV_MINUS<=inputEv) && (inputEv<=EV_MAX) ) {
            setEv = inputEv;
        } else {
            setEv = EV_ZERO;
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

                String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"exposureCompensation\", \"exposureProgram\"] } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);

                JSONObject output = new JSONObject(strResult);
                JSONObject results = output.getJSONObject("results");
                JSONObject options = results.getJSONObject("options");
                String curExposureCompensation = options.getString("exposureCompensation");
                int curExposureProgram = options.getInt("exposureProgram");

                if ( curExposureProgram != 1 ) {    //not MANUAL
                    if ( setEv < EV_MIN ) {
                        int curEv;
                        for ( curEv=EV_MIN; curEv<=EV_MAX; curEv++) {
                            if ( curExposureCompensation.equals(evList[curEv]) ) {
                                break;
                            }
                        }
                        if (curEv>EV_MAX){
                            setEv = EV_ZERO;
                        } else {
                            if ( setEv == EV_PLUS ) {
                                setEv = curEv + 1;
                                if (setEv>EV_MAX) {
                                    setEv = EV_MAX;
                                }
                            } else {
                                setEv = curEv - 1;
                                if (setEv<EV_MIN) {
                                    setEv = EV_MIN;
                                }
                            }
                        }
                    }
                    String strJsonSetCaptureMode = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"exposureCompensation\":\"" + evList[setEv] +"\"} } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetCaptureMode);
                    Log.d(TAG, "ChangeEvTask: Set exposureCompensation=" + evList[setEv]);
                } else {
                    //MANUALであることを通知
                    setEv = -2;
                }
            } else {
                //実行不可
                setEv = -1;
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return setEv;
    }

    @Override
    protected void onPostExecute(Integer result) {
        //resultにあった音声を再生
        if ( (EV_MIN <= result) && (result <= EV_MAX) ) {
            new SoundManagerTask(context, soundList[languageIndex][result], speechVolume).execute();
        } else if (result==-2) {
            new SoundManagerTask(context, soundList[languageIndex][EV_ERR], speechVolume).execute();
        } else {
            //実行不可は無処理
        }
    }

}
