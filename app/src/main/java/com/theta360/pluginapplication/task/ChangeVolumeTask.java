/**
 * Copyright 2018 Ricoh Company, Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.pluginapplication.task;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.theta360.pluginapplication.network.HttpConnector;
import com.theta360.pluginlibrary.values.LedTarget;

import org.json.JSONException;
import org.json.JSONObject;


public class ChangeVolumeTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "HIDREMOTE";

    public static final int VOLUME_TGGLE =0;
    public static final int VLLUME_STEP_MIN = -100;
    public static final int VLLUME_STEP_MAX = 100;

    private final Context context;
    private int changeStep;

    public ChangeVolumeTask(Context context, int val) {
        this.context = context;
        if ( VOLUME_TGGLE < val && val <= VLLUME_STEP_MAX ) {
            changeStep = val;
        } else if ( VLLUME_STEP_MIN <= val && val < VOLUME_TGGLE ) {
            changeStep = val;
        } else {
            changeStep = VOLUME_TGGLE; //Auto Change Off->50->100->0ff ...
        }
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected Integer doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";
        Integer setVolume = 0;

        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_STAT, "");
        try {
            JSONObject output2 = new JSONObject(strResult);
            JSONObject state = output2.getJSONObject("state");
            String captureStatus = state.getString("_captureStatus");
            int recordedTime = state.getInt("_recordedTime");

            if ( captureStatus.equals("idle") && (recordedTime==0) ) {

                String strJsonGetVolume = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\":[\"_shutterVolume\"] } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetVolume);

                JSONObject output = new JSONObject(strResult);
                JSONObject results = output.getJSONObject("results");
                JSONObject options = results.getJSONObject("options");
                int volume = options.getInt("_shutterVolume");

                if ( changeStep == 0 ) {
                    if ( 0<=volume && volume< 30 ) {
                        volume = 60;    // Off to Mid
                    } else if (30<=volume && volume< 80) {
                        volume = 100;   // Mid to Max
                    } else { // 70 to 100
                        volume = 0;     // Max to Off
                    }
                } else {
                    volume += changeStep ;
                    if ( 100 < volume  ) {
                        volume = 100;
                    }
                    if ( volume < 0 ) {
                        volume = 0;
                    }
                }

                String strJsonSetVolume = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"_shutterVolume\":" + String.valueOf(volume) + "} } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetVolume);
                Log.d(TAG, "ChangeVolumeTask: Set volume=" + String.valueOf(volume));
                setVolume = volume;
            } else {
                //実行不可
                setVolume = -1;
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return setVolume;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result>=0) {
            //LED6点灯
            Intent intentLedShow = new Intent("com.theta360.plugin.ACTION_LED_SHOW");
            intentLedShow.putExtra("target", LedTarget.LED6.toString());
            context.sendBroadcast(intentLedShow);

            //サンプル音を鳴らす
            context.sendBroadcast(new Intent("com.theta360.plugin.ACTION_AUDIO_MOVSTART"));

            //LED点灯が見えるようにwaitする
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //LED6消灯
            Intent intentLedHide = new Intent("com.theta360.plugin.ACTION_LED_HIDE");
            intentLedHide.putExtra("target", LedTarget.LED6.toString());
            context.sendBroadcast(intentLedHide);
        } else {
            //実行不可は無処理
        }
    }
}
