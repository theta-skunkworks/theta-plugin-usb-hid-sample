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

//package com.theta360.pluginapplication;
package skunkworks.hid;

import android.content.Context;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;
import android.util.Log;

import com.theta360.pluginapplication.task.CheckApListTask;
import com.theta360.pluginapplication.task.SoundManagerTask;
import com.theta360.pluginapplication.task.ShutterButtonTask;
import com.theta360.pluginapplication.task.ChangeCaptureModeTask;
import com.theta360.pluginapplication.task.ChangeVolumeTask;
import com.theta360.pluginapplication.task.ChangeExposureDelayTask;
import com.theta360.pluginapplication.task.ChangeEvTask;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;

import android.content.Intent;



public class MainActivity extends PluginActivity {
    private static final String TAG = "HIDREMOTE";

    private static final String ACTION_OLED_DISPLAY_SET = "com.theta360.plugin.ACTION_OLED_DISPLAY_SET";

    private static final int WLAN_MODE_OFF = 0;
    private static final int WLAN_MODE_AP = 1;
    private static final int WLAN_MODE_CL = 2;
    private int wlanMode = WLAN_MODE_AP;
    private boolean wlanClList = false;

    private boolean firstModeButtonUp = false;

    private static final int SPEECH_VOLUME_STEP = 20;
    private static final int SPEECH_VOLUME_MAX = 100;
    private static final int SPEECH_VOLUME_MIN = 0;
    private int speechVolume = SPEECH_VOLUME_MIN;
    private int defaultSpeechVolume = SPEECH_VOLUME_MAX;


    private CheckApListTask.Callback mCheckApListCallback = new CheckApListTask.Callback() {
        @Override
        public void onCheckApList(int listNum) {
            if (listNum == 0) {
                wlanClList = false;
            } else {
                wlanClList = true;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set OLED Display mode for THETA Z1
        Intent oledIntentSet = new Intent(ACTION_OLED_DISPLAY_SET);
        oledIntentSet.putExtra("display", "basic");
        sendBroadcast(oledIntentSet);

        // Set enable to close by pluginlibrary, If you set false, please call close() after finishing your end processing.
        setAutoClose(true);
        // Set a callback when a button operation event is acquired.
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                Log.d(TAG, "onKeyDown(): keyCode=" + keyCode + ", keyEvent=" + event);
                TextView viewKeyCode = (TextView) findViewById(R.id.viewKeyDown);
                String strKeyCode = String.valueOf(keyCode);
                viewKeyCode.setText(strKeyCode + "[" + event.keyCodeToString(keyCode) + "]");

                if ((keyCode != KeyReceiver.KEYCODE_MEDIA_RECORD) && (keyCode != KeyReceiver.KEYCODE_WLAN_ON_OFF)) {
                    lastOperationCode = keyCode;
                    execKeyProcess(keyCode2KeyProcess(keyCode));
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
                /**
                 * You can control the LED of the camera.
                 * It is possible to change the way of lighting, the cycle of blinking, the color of light emission.
                 * Light emitting color can be changed only LD3.
                 */
                Log.d(TAG, "onKeyUp(): keyCode=" + keyCode + ", keyEvent=" + event);
                TextView viewKeyCode = (TextView) findViewById(R.id.viewKeyUp);
                String strKeyCode = String.valueOf(keyCode);
                viewKeyCode.setText(strKeyCode + "[" + event.keyCodeToString(keyCode) + "]");

                switch (keyCode) {
                    case KeyReceiver.KEYCODE_WLAN_ON_OFF:
                        if (wlanLongPress) {
                            wlanLongPress = false;
                        } else {
                            lastOperationCode = keyCode;
                            execKeyProcess(keyCode2KeyProcess(keyCode));
                        }
                        break;
                    case KeyReceiver.KEYCODE_MEDIA_RECORD:
                        if (firstModeButtonUp) {
                            lastOperationCode = keyCode;
                            execKeyProcess(keyCode2KeyProcess(keyCode));
                        } else {
                            firstModeButtonUp = true;
                        }
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                Log.d(TAG, "onKeyLongPress(): keyCode=" + keyCode + ", keyEvent=" + event);
                TextView viewKeyCode = (TextView) findViewById(R.id.viewKeyLongPress);
                String strKeyCode = String.valueOf(keyCode);
                viewKeyCode.setText(strKeyCode + "[" + event.keyCodeToString(keyCode) + "]");

                switch (keyCode) {
                    case KeyReceiver.KEYCODE_WLAN_ON_OFF:
                        lastOperationCode = KEYCODE_WLAN_LONGPRESS;

                        wlanLongPress = true;
                        execKeyProcess(wlanLongPress2Process);

                        //debug code
                        dumpKeyCode2ProcessList();
                        break;
                    default:
                        break;
                }
            }

        });

        this.context = getApplicationContext();
        this.webServer = new WebServer(this.context);
        try {
            this.webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        //前回起動から時引き継ぐべき状態を読み込む
        readPluginSetting();

        //キー設定を読み込む
        readInitalList();

        // LEDを正しく点灯させるため、わざと動画モードにしてから静止画モードにする。
        new ChangeCaptureModeTask("video").execute();
        new ChangeCaptureModeTask("image").execute();

        //WlanをAPモードから開始する
        notificationWlanAp();   // プラグイン起動前からAPモードでVysorのWLANデバッグしていても影響なし
        wlanMode = WLAN_MODE_AP;

        //Wlan CLモード用のリスト有無チェック -> リストなしは Off<->AP 、ありは Off->AP->CL->Off->...
        new CheckApListTask(mCheckApListCallback).execute();

    }

    @Override
    protected void onPause() {
        // Do end processing
        //close();

        //次回起動時引き継ぐべき状態を保存
        savePluginSetting();

        //キー設定を保存する
        //saveInitalList(); //webUIからsaveしたときだけ保存したいのでコメントアウト

        super.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Log.d(TAG, "onTouchEvent(): MotionEvent=" + event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "onTouchEvent(): MotionEvent=" + event);

            TextView viewAdButtonState = (TextView) findViewById(R.id.viewAdButtonState);
            int buttonState = event.getButtonState();
            int toolType = event.getToolType(0);
            String strAdButtonState = String.valueOf(buttonState);
            viewAdButtonState.setText(strAdButtonState + ", toolType=" + String.valueOf(toolType));

            int motionIndex = -1;
            switch (buttonState) {
                case MotionEvent.BUTTON_PRIMARY:    //Mouse left button click
                    motionIndex = MOTION_INDEX_PRIMARY;
                    break;
                case MotionEvent.BUTTON_SECONDARY:  //Mouse right button click
                    motionIndex = MOTION_INDEX_SECONDARY;
                    break;
                case MotionEvent.BUTTON_TERTIARY:   //Mouse wheel click
                    motionIndex = MOTION_INDEX_TERTIARY;
                    break;
                case MotionEvent.BUTTON_BACK:
                    motionIndex = MOTION_INDEX_BACK;
                    break;
                case MotionEvent.BUTTON_FORWARD:
                    motionIndex = MOTION_INDEX_FORWARD;
                    break;
                case MotionEvent.BUTTON_STYLUS_PRIMARY:
                    motionIndex = MOTION_INDEX_STYLUS_PRIMARY;
                    break;
                case MotionEvent.BUTTON_STYLUS_SECONDARY:
                    motionIndex = MOTION_INDEX_STYLUS_SECONDARY;
                    break;
                default:
                    //未定義
                    return false;
            }
            lastOperationCode = motionIndex + KEYCODE_LIST_MAX_NUM;
            execKeyProcess(listMotionEvent2Process[motionIndex]);
        }

        return true;
    }

    //=====================================================
    //<<< KeyProcess >>>
    //=====================================================
    boolean advancedOption = false;

    String processName[] = {
            "NOP",
            "EXEC_SHUTTER",
            "TGGLE_WLAN",
            "TGGLE_CAPTURE_MODE",
            "TGGLE_EXP_DELAY",
            "TGGLE_CAMERA_VOL",
            "TGGLE_SPEECH_VOL",
            "SET_EV_ZERO",
            "SET_EV_PLUS",
            "SET_EV_MINUS",
            "SET_EXP_DELAY_OFF",
            "SET_EXP_DELAY_1",
            "SET_EXP_DELAY_2",
            "SET_EXP_DELAY_3",
            "SET_EXP_DELAY_4",
            "SET_EXP_DELAY_5",
            "SET_EXP_DELAY_6",
            "SET_EXP_DELAY_7",
            "SET_EXP_DELAY_8",
            "SET_EXP_DELAY_9",
            "SET_EXP_DELAY_10",
            "SET_CAP_MODE_IMAGE",
            "SET_CAP_MODE_VIDEO",
            "SET_CAMERA_VOL_PLUS",
            "SET_CAMERA_VOL_MINUS",
            "SET_SPEECH_VOL_PLUS",
            "SET_SPEECH_VOL_MINUS",
    };
    public static final int WITH_INPUT = -1;  //use key operation history
    public static final int NO_PROCESS = 0;
    public static final int EXEC_SHUTTER = 1;
    public static final int TGGLE_WLAN = 2;
    public static final int TGGLE_CAPTURE_MODE = 3;
    public static final int TGGLE_EXP_DELAY = 4;
    public static final int TGGLE_CAMERA_VOL = 5;
    public static final int TGGLE_SPEECH_VOL = 6;
    public static final int SET_EV_ZERO = 7;
    public static final int SET_EV_PLUS = 8;
    public static final int SET_EV_MINUS = 9;
    public static final int SET_EXP_DELAY_OFF = 10;
    public static final int SET_EXP_DELAY_1 = 11;
    public static final int SET_EXP_DELAY_2 = 12;
    public static final int SET_EXP_DELAY_3 = 13;
    public static final int SET_EXP_DELAY_4 = 14;
    public static final int SET_EXP_DELAY_5 = 15;
    public static final int SET_EXP_DELAY_6 = 16;
    public static final int SET_EXP_DELAY_7 = 17;
    public static final int SET_EXP_DELAY_8 = 18;
    public static final int SET_EXP_DELAY_9 = 19;
    public static final int SET_EXP_DELAY_10 = 20;
    public static final int SET_CAP_MODE_IMAGE = 21;
    public static final int SET_CAP_MODE_VIDEO = 22;
    public static final int SET_CAMERA_VOL_PLUS = 23;
    public static final int SET_CAMERA_VOL_MINUS = 24;
    public static final int SET_SPEECH_VOL_PLUS = 25;
    public static final int SET_SPEECH_VOL_MINUS = 26;
    public static final int PROCESS_CODE_MAX_NUM = 27;//Last Code + 1
    public static final int PROCESS_CODE_SIMPLE_NUM = SET_EXP_DELAY_OFF + 1;

    private void execKeyProcess(int processCode) {

        switch (processCode) {
            case EXEC_SHUTTER:
                new ShutterButtonTask().execute();
                break;
            case TGGLE_WLAN:
                changeWlanMode();
                break;
            case TGGLE_CAPTURE_MODE:
                new ChangeCaptureModeTask(ChangeCaptureModeTask.CAPMODE_TGGLE).execute();
                break;
            case TGGLE_EXP_DELAY:
                new ChangeExposureDelayTask(getApplicationContext(), ChangeExposureDelayTask.EXPOSURE_DELAY_TGGLE, speechVolume).execute();
                break;
            case TGGLE_CAMERA_VOL:
                new ChangeVolumeTask(getApplicationContext(), ChangeVolumeTask.VOLUME_TGGLE).execute();
                break;
            case TGGLE_SPEECH_VOL:
                changeSpeechVolume();
                break;
            case SET_EV_ZERO:
                new ChangeEvTask(getApplicationContext(), ChangeEvTask.EV_ZERO, speechVolume).execute();
                break;
            case SET_EV_PLUS:
                new ChangeEvTask(getApplicationContext(), ChangeEvTask.EV_PLUS, speechVolume).execute();
                break;
            case SET_EV_MINUS:
                new ChangeEvTask(getApplicationContext(), ChangeEvTask.EV_MINUS, speechVolume).execute();
                break;
            case SET_EXP_DELAY_OFF:
                new ChangeExposureDelayTask(getApplicationContext(), ChangeExposureDelayTask.EXPOSURE_DELAY_OFF, speechVolume).execute();
                break;
            case SET_EXP_DELAY_1:
            case SET_EXP_DELAY_2:
            case SET_EXP_DELAY_3:
            case SET_EXP_DELAY_4:
            case SET_EXP_DELAY_5:
            case SET_EXP_DELAY_6:
            case SET_EXP_DELAY_7:
            case SET_EXP_DELAY_8:
            case SET_EXP_DELAY_9:
            case SET_EXP_DELAY_10:
                new ChangeExposureDelayTask(getApplicationContext(), (processCode - SET_EXP_DELAY_OFF), speechVolume).execute();
                break;
            case SET_CAP_MODE_IMAGE:
                new ChangeCaptureModeTask(ChangeCaptureModeTask.CAPMODE_IMAGE).execute();
                break;
            case SET_CAP_MODE_VIDEO:
                new ChangeCaptureModeTask(ChangeCaptureModeTask.CAPMODE_VIDEO).execute();
                break;
            case SET_CAMERA_VOL_PLUS:
                new ChangeVolumeTask(getApplicationContext(), 10).execute();
                break;
            case SET_CAMERA_VOL_MINUS:
                new ChangeVolumeTask(getApplicationContext(), -10).execute();
                break;
            case SET_SPEECH_VOL_PLUS:
                speechVolume += SPEECH_VOLUME_STEP;
                if (speechVolume > SPEECH_VOLUME_MAX) {
                    speechVolume = SPEECH_VOLUME_MAX;
                }
                new SoundManagerTask(getApplicationContext(), R.raw.speech_volume, speechVolume).execute();
                break;
            case SET_SPEECH_VOL_MINUS:
                speechVolume -= SPEECH_VOLUME_STEP;
                if (speechVolume < SPEECH_VOLUME_MIN) {
                    speechVolume = SPEECH_VOLUME_MIN;
                }
                new SoundManagerTask(getApplicationContext(), R.raw.speech_volume, speechVolume).execute();
                break;
        }

        return;
    }


    //=====================================================
    //<<< Motion Event >>>
    //=====================================================
    public static final int MOTION_INDEX_PRIMARY = 0;
    public static final int MOTION_INDEX_SECONDARY = 1;
    public static final int MOTION_INDEX_TERTIARY = 2;
    public static final int MOTION_INDEX_BACK = 3;
    public static final int MOTION_INDEX_FORWARD = 4;
    public static final int MOTION_INDEX_STYLUS_PRIMARY = 5;
    public static final int MOTION_INDEX_STYLUS_SECONDARY = 6;
    public static final int MOTION_INDEX_NUM = 7;

    private int listDefaultMotionEvent[] = {
            EXEC_SHUTTER,           // BUTTON_PRIMARY
            TGGLE_CAPTURE_MODE,     // BUTTON_SECONDARY
            TGGLE_CAMERA_VOL,       // BUTTON_TERTIARY
            NO_PROCESS,             // BUTTON_BACK
            NO_PROCESS,             // BUTTON_FORWARD
            NO_PROCESS,             // BUTTON_STYLUS_PRIMARY
            NO_PROCESS             // BUTTON_STYLUS_SECONDARY
    };

    private int listMotionEvent2Process[] = {
            NO_PROCESS,             // BUTTON_PRIMARY
            NO_PROCESS,             // BUTTON_SECONDARY
            NO_PROCESS,             // BUTTON_TERTIARY
            NO_PROCESS,             // BUTTON_BACK
            NO_PROCESS,             // BUTTON_FORWARD
            NO_PROCESS,             // BUTTON_STYLUS_PRIMARY
            NO_PROCESS             // BUTTON_STYLUS_SECONDARY
    };

    //=====================================================
    //<<< Key Event >>>
    //=====================================================
    private int lastOperationCode = -1;

    private int wlanLongPress2Process = 0;
    private int defaultwlanLongPress2Process = SET_EV_ZERO;
    private boolean wlanLongPress = false;
    private static final int KEYCODE_WLAN_LONGPRESS = -1 * KeyReceiver.KEYCODE_WLAN_ON_OFF;


    private static final int KEYCODE_LIST_MAX_NUM = 300;
    private ArrayList<Integer> keyCode2ProcessList = new ArrayList<Integer>();

    //Initial value for shipment
    private int defaultKeyProcess[][] = {
            //- THETA V body button -
            {KeyReceiver.KEYCODE_CAMERA, EXEC_SHUTTER},
            //{KeyReceiver.KEYCODE_WLAN_ON_OFF, TGGLE_WLAN},
            //{KeyReceiver.KEYCODE_MEDIA_RECORD, TGGLE_CAPTURE_MODE},
            {KeyReceiver.KEYCODE_WLAN_ON_OFF, SET_EV_PLUS},
            {KeyReceiver.KEYCODE_MEDIA_RECORD, SET_EV_MINUS},
            //{KeyReceiver.KEYCODE_WLAN_ON_OFF, SET_SPEECH_VOL_PLUS},
            //{KeyReceiver.KEYCODE_MEDIA_RECORD, SET_SPEECH_VOL_MINUS},

            //- Finger Presenter - [Shared setting prioritizing "Inateck WP 2002"]
            {KeyEvent.KEYCODE_PAGE_DOWN, SET_EV_PLUS},   // KOKUYO ELA-FP1(center key), Inateck WP2002(center key)
            {KeyEvent.KEYCODE_PAGE_UP, SET_EV_MINUS},    // KOKUYO ELA-FP1(right key), Inateck WP2002(under key)
            {KeyEvent.KEYCODE_B, SET_EV_ZERO},           // KOKUYO ELA-FP1(left key), Inateck WP2002(center key long push)
            {KeyEvent.KEYCODE_F5, EXEC_SHUTTER},          // KOKUYO ELA-FP1(center key long push), Inateck WP2002(under key long push = F5+MUSIC)
            {KeyEvent.KEYCODE_TAB, EXEC_SHUTTER},         // Inateck WP2002 Only (top key)
            {KeyEvent.KEYCODE_ENTER, TGGLE_EXP_DELAY},   // Inateck WP2002 Only (top key double clic)
            //{KeyEvent.KEYCODE_ALT_LEFT, TGGLE_CAMERA_VOL},//Inateck WP2002 (top key long push = ALT_LEFT+PICTSYMBOLS)
            {KeyEvent.KEYCODE_ESCAPE, TGGLE_EXP_DELAY},  // KOKUYO ELA-FP1 Only (right key long push)
            //{KeyEvent.KEYCODE_BACK, NO_PROCESS},          // KOKUYO ELA-FP1 Only (right key long push)

            //- BUFFALO GAMEPAD BSGP815GY -
            {KeyEvent.KEYCODE_DPAD_UP, SET_CAMERA_VOL_PLUS},      // "Up"
            {KeyEvent.KEYCODE_DPAD_DOWN, SET_CAMERA_VOL_MINUS},   // "Down"
            {KeyEvent.KEYCODE_DPAD_LEFT, SET_SPEECH_VOL_MINUS},   // "Left"
            {KeyEvent.KEYCODE_DPAD_RIGHT, SET_SPEECH_VOL_PLUS},   // "Right"
            {KeyEvent.KEYCODE_BUTTON_A, SET_EV_PLUS},              // "A" (Multiple Output)
            {KeyEvent.KEYCODE_BUTTON_B, SET_EV_MINUS},             // "B" (Multiple Output)
            {KeyEvent.KEYCODE_BUTTON_X, SET_CAP_MODE_IMAGE},      // "X" (Multiple Output)
            {KeyEvent.KEYCODE_BUTTON_Y, SET_CAP_MODE_VIDEO},      // "Y" (Multiple Output)
            {KeyEvent.KEYCODE_BUTTON_R2, SET_EXP_DELAY_OFF},      // "R"
            {KeyEvent.KEYCODE_BUTTON_L2, SET_EXP_DELAY_5},        // "L"
            {KeyEvent.KEYCODE_BUTTON_R1, EXEC_SHUTTER},           // "START"
            {KeyEvent.KEYCODE_BUTTON_L1, TGGLE_CAPTURE_MODE},    // "SELECT"
            {KeyEvent.KEYCODE_DPAD_CENTER, NO_PROCESS},           // "A" (Multiple Output)
            {KeyEvent.KEYCODE_BACK, NO_PROCESS},                   // "B" (Multiple Output)
            {KeyEvent.KEYCODE_DEL, NO_PROCESS},                    // "X" (Multiple Output)
            {KeyEvent.KEYCODE_SPACE, NO_PROCESS},                  // "Y" (Multiple Output)

            //- Other keyboard operation which seems to be useful -
            {KeyEvent.KEYCODE_VOLUME_UP, EXEC_SHUTTER},
            {KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, EXEC_SHUTTER},
            {KeyEvent.KEYCODE_MEDIA_STOP, SET_EV_ZERO},
            {KeyEvent.KEYCODE_MEDIA_NEXT, SET_EV_PLUS},
            {KeyEvent.KEYCODE_MEDIA_PREVIOUS, SET_EV_MINUS},
            {KeyEvent.KEYCODE_GRAVE, TGGLE_SPEECH_VOL},
            {KeyEvent.KEYCODE_FUNCTION, TGGLE_EXP_DELAY},
            {KeyEvent.KEYCODE_0, SET_EXP_DELAY_10},
            {KeyEvent.KEYCODE_1, SET_EXP_DELAY_1},
            {KeyEvent.KEYCODE_2, SET_EXP_DELAY_2},
            {KeyEvent.KEYCODE_3, SET_EXP_DELAY_3},
            {KeyEvent.KEYCODE_4, SET_EXP_DELAY_4},
            {KeyEvent.KEYCODE_5, SET_EXP_DELAY_5},
            {KeyEvent.KEYCODE_6, SET_EXP_DELAY_6},
            {KeyEvent.KEYCODE_7, SET_EXP_DELAY_7},
            {KeyEvent.KEYCODE_8, SET_EXP_DELAY_8},
            {KeyEvent.KEYCODE_9, SET_EXP_DELAY_9},

            {-1, NO_PROCESS}
    };

    //Ban key code list (Because the android system is using, assignment prohibited)
    private int listBanKeyCode[] = {
            KeyEvent.KEYCODE_HOME,           // The plug-in ends with the equivalent of the HOME button
            KeyEvent.KEYCODE_ENDCALL,        // THETA will sleep
            //KeyEvent.KEYCODE_VOLUME_UP,    //Vol up : The system works but there is no problem
            //KeyEvent.KEYCODE_VOLUME_DOWN,  //Vol down : When the ringing volume is set to 0, it can not be restored from the application.
            KeyEvent.KEYCODE_POWER,          // THETA will sleep
            KeyEvent.KEYCODE_SYM,            // The dialog "select keybord" will remain displayed
            KeyEvent.KEYCODE_ENVELOPE,       // E-Mail application initial setting dialog will remain displayed
            KeyEvent.KEYCODE_SYSRQ,          // screenshot is taken and onKeyDown event does not occur
            //KeyEvent.KEYCODE_VOLUME_MUTE,  //mute on/off : -> The system works but there is no problem
            KeyEvent.KEYCODE_PICTSYMBOLS,    // Emoji character input screen is displayed
            KeyEvent.KEYCODE_APP_SWITCH,     // Multitask menu is displayed (It is equivalent to terminating plug-in)
            KeyEvent.KEYCODE_LANGUAGE_SWITCH,// The dialog "tggle keybord" will remain displayed
            KeyEvent.KEYCODE_CONTACTS,       // onKeyDown event does not occur
            KeyEvent.KEYCODE_CALENDAR,       // onKeyDown event does not occur
            KeyEvent.KEYCODE_MUSIC,          // MUSIC application initial setting dialog will remain displayed
            KeyEvent.KEYCODE_CALENDAR,       // onKeyDown event does not occur
            KeyEvent.KEYCODE_ASSIST,         // onKeyDown and onKeyUp events do not occur
            KeyEvent.KEYCODE_BRIGHTNESS_DOWN,// Plugin will end
            KeyEvent.KEYCODE_BRIGHTNESS_UP,  // Plugin will end
            KeyEvent.KEYCODE_SLEEP,          // THETA will sleep
            KeyEvent.KEYCODE_WAKEUP,         // onKeyDown and onKeyUp events do not occur
            KeyEvent.KEYCODE_SOFT_SLEEP,     // THETA will sleep
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,   //onKeyDown and onKeyUp events do not occur
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN, //onKeyDown and onKeyUp events do not occur
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT, //onKeyDown and onKeyUp events do not occur
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,//onKeyDown and onKeyUp events do not occur
            -1
    };
    // THETA body button code list (use web UI)
    private int listThetaBodyButton[] = {
            KeyReceiver.KEYCODE_CAMERA,         //Shutter Button
            KeyReceiver.KEYCODE_WLAN_ON_OFF,    // WLAN Button
            KeyReceiver.KEYCODE_MEDIA_RECORD,   // Mode Button
            -1
    };

    private ArrayList<ArrayList<Integer>> InitalList = new ArrayList<ArrayList<Integer>>();

    private void setFactorySettings() {
        //webUI 表示オプション
        advancedOption = false;

        //発話音量
        speechVolume = defaultSpeechVolume;

        //WLAN LongPressは特別
        wlanLongPress2Process = defaultwlanLongPress2Process;

        //ポインティングデバイスも特別
        for (int i = 0; i < MOTION_INDEX_NUM; i++) {
            listMotionEvent2Process[i] = listDefaultMotionEvent[i];
        }

        //その他キー
        int index = 0;
        while (true) {
            if (defaultKeyProcess[index][0] == -1) {
                break;
            } else {
                ArrayList<Integer> element = new ArrayList<Integer>();
                element.add(defaultKeyProcess[index][0]);
                element.add(defaultKeyProcess[index][1]);
                InitalList.add(element);
            }
            index++;
        }
        setKeyCode2KeyProcessList();
    }

    private static final String pluginSettingFileName = "plugin.txt";
    private void readPluginSetting() {
        InputStream in;
        try {
            FileInputStream fileInputStream = openFileInput(pluginSettingFileName);
            BufferedReader buffReader = new BufferedReader( new InputStreamReader(fileInputStream, "UTF-8") );
            //ファイルがある場合
            Log.d(TAG, "open file :" + pluginSettingFileName);

            String line = "";
            while( (line = buffReader.readLine()) != null ){
                //Log.d(TAG, line);

                String[] element = line.split("=");
                Log.d(TAG, "[0]=" + element[0] + ", [1]=" + element[1]);
                if (element[0].equals("advancedOption")) {
                    if ( element[1].equals("true") ) {
                        advancedOption = true;
                    } else {
                        advancedOption = false;
                    }
                } else if (element[0].equals("speechVolume")) {
                    if ( this.isNumber(element[1]) ) {
                        speechVolume = Integer.parseInt(element[1]);
                    } else {
                        speechVolume = 100;
                    }
                } else {
                    //無処理
                }

            }
            buffReader.close();

        } catch (IOException e) {
            //e.printStackTrace();

            //ファイルがない場合
            Log.d(TAG, "Can't open file :" + pluginSettingFileName);

            //webUI 表示オプション
            advancedOption = false;
            //発話音量
            speechVolume = defaultSpeechVolume;
        }

    }
    private void savePluginSetting() {
        //ファイルに保存する
        FileOutputStream  out;

        try {
            out = openFileOutput(pluginSettingFileName, MODE_PRIVATE);

            //webUI 表示オプション
            String outStrAO = "advancedOption=" + String.valueOf(advancedOption) + "\n";
            byte[] buffAO = outStrAO.getBytes();
            out.write(buffAO,0, outStrAO.length());

            //発話音量
            String outStrSV = "speechVolume=" + String.valueOf(speechVolume) + "\n";
            byte[] buffSV = outStrSV.getBytes();
            out.write(buffSV,0, outStrSV.length());

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static final String initialFileName = "initKey.txt";
    private void readInitalList() {
        InputStream in;
        try {
            FileInputStream fileInputStream = openFileInput(initialFileName);
            BufferedReader buffReader = new BufferedReader( new InputStreamReader(fileInputStream, "UTF-8") );
            //ファイルがある場合
            Log.d(TAG, "open file :" + initialFileName);

            String line = "";
            while( (line = buffReader.readLine()) != null ){
                //Log.d(TAG, line);

                String[] element = line.split("=");
                Log.d(TAG, "[0]=" + element[0] + ", [1]=" + element[1]);
                if (element[0].equals("wlanLongPress2Process")) {
                    if ( this.isNumber(element[1]) ) {
                        wlanLongPress2Process = Integer.parseInt(element[1]);
                    } else {
                        wlanLongPress2Process = NO_PROCESS;
                    }
                } else if (element[0].matches("(listMotionEvent2Process\\[)([0-9]+)(\\])")) {
                    if ( this.isNumber(element[1]) ) {
                        String strIndex = element[0].replaceAll("(listMotionEvent2Process\\[)([0-9]+)(\\])","$2");
                        int index = Integer.parseInt(strIndex);
                        if ( 0<=index && index < MOTION_INDEX_NUM ) {
                            listMotionEvent2Process[index] = Integer.parseInt(element[1]);
                        }
                    }
                } else if ( this.isNumber(element[0]) ) {
                    if ( this.isNumber(element[1]) ) {
                        ArrayList<Integer> addElement = new ArrayList<Integer>();
                        addElement.add(Integer.parseInt(element[0]));
                        addElement.add(Integer.parseInt(element[1]));
                        InitalList.add(addElement);
                    }
                } else {
                    //無処理
                }

            }
            buffReader.close();
            setKeyCode2KeyProcessList();

        } catch (IOException e) {
            //e.printStackTrace();

            //ファイルがない場合
            Log.d(TAG, "Can't open file :" + initialFileName);
            setFactorySettings();
        }
    }
    public static boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private void saveInitalList() {
        //ファイルに保存する
        FileOutputStream  out;

        try {
            out = openFileOutput(initialFileName, MODE_PRIVATE);

            //WLAN LogPressは特別
            String outStrWlanLongPress = "wlanLongPress2Process=" + String.valueOf(wlanLongPress2Process) + "\n";
            byte[] buffWlanLongPress = outStrWlanLongPress.getBytes();
            out.write(buffWlanLongPress,0, outStrWlanLongPress.length());

            //ポインティングデバイスも特別
            for (int i=0; i<listMotionEvent2Process.length; i++) {
                String outMotionDevice =  "listMotionEvent2Process[" + String.valueOf(i) + "]=" + String.valueOf(listMotionEvent2Process[i]) + "\n";
                byte[] buffMotionDevice = outMotionDevice.getBytes();
                out.write(buffMotionDevice,0, outMotionDevice.length());
            }

            //その他キー
            for (int i = 0; i < keyCode2ProcessList.size(); i++) {
                int processCode = keyCode2ProcessList.get(i);
                if ( ((0 < processCode) && (processCode < PROCESS_CODE_MAX_NUM)) || (processCode==WITH_INPUT) ) {
                    // 定義済 or 操作履歴のあるものだけ保存する
                    String outKeyCode2Process = String.valueOf(i) + "=" + String.valueOf(processCode) + "\n";
                    byte[] buffKeyCode2Process = outKeyCode2Process.getBytes();
                    out.write(buffKeyCode2Process,0, outKeyCode2Process.length());
                }
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearInitalList() {
        //WLAN LongPressは特別
        wlanLongPress2Process = NO_PROCESS;

        //ポインティングデバイスも特別
        for (int i = 0; i < MOTION_INDEX_NUM; i++) {
            listMotionEvent2Process[i] = NO_PROCESS;
        }

        //その他キー
        clearKeyCode2KeyProcessList();
    }

    //履歴消し
    private void clearHistory() {
        lastOperationCode = -1;

        //WLAN LogPressは特別
        if (wlanLongPress2Process == WITH_INPUT) {
            wlanLongPress2Process = NO_PROCESS;
        }

        //ポインティングデバイスも特別
        for (int i = 0; i < MOTION_INDEX_NUM; i++) {
            if (listMotionEvent2Process[i] == WITH_INPUT) {
                listMotionEvent2Process[i] = NO_PROCESS;
            }
        }

        //その他キー
        for (int i = 0; i < keyCode2ProcessList.size(); i++) {
            int processCode = keyCode2ProcessList.get(i);
            if (processCode == WITH_INPUT) {
                keyCode2ProcessList.set(i, NO_PROCESS);
            }
        }
    }


    private void setKeyCode2KeyProcessList() {
        int result = 0;

        // Reset keyCode2ProcessList
        keyCode2ProcessList.clear();

        // Make keyCode2ProcessList
        for (int i=0; i<KEYCODE_LIST_MAX_NUM; i++) {
            //Used when not exhaustive
            //if (InitalList.size()==0){
            //    break;
            //}

            boolean flag = false;
            for(int j=0; j<InitalList.size();j++) {
                if ( i == InitalList.get(j).get(0) ) {
                    keyCode2ProcessList.add(InitalList.get(j).get(1));
                    InitalList.remove(j);
                    flag = true;
                    break;
                }
            }
            if (flag==false) {
                keyCode2ProcessList.add(NO_PROCESS);
            }
        }
        Log.d(TAG, "keyCode2ProcessList.size()=" + String.valueOf(keyCode2ProcessList.size()));

        return ;
    }
    private void clearKeyCode2KeyProcessList() {
        // Reset keyCode2ProcessList
        keyCode2ProcessList.clear();
        // Make keyCode2ProcessList
        for (int i=0; i<KEYCODE_LIST_MAX_NUM; i++) {
            keyCode2ProcessList.add(NO_PROCESS);
        }
    }

    private int keyCode2KeyProcess(int inKeyCode) {
        int result = NO_PROCESS;

        try {
            Integer listDat = keyCode2ProcessList.get(inKeyCode);

            if ( listDat == NO_PROCESS ) {
                // update key history
                keyCode2ProcessList.set(inKeyCode, WITH_INPUT);
            } else if ( listDat == WITH_INPUT ) {
                result = NO_PROCESS;
            } else {
                result = listDat;
            }

        } catch (IndexOutOfBoundsException  e) {
            Log.d(TAG, "keyCode2KeyProcess() : Undefined KeyCode=" + String.valueOf(inKeyCode) + ", " + e.getMessage() );
            result = NO_PROCESS;
        }

        //Log.d(TAG, "keyCode2KeyProcess() : result=" + processName[result] );
        return result;
    }

    // for debug
    private void dumpKeyCode2ProcessList() {

        for (int i=0; i<keyCode2ProcessList.size(); i++) {
            int processCode = keyCode2ProcessList.get(i);

            String dumpStr;
            if ( 0<=processCode && processCode < PROCESS_CODE_MAX_NUM ) {
                dumpStr = processName[processCode];
            } else if ( processCode == WITH_INPUT ){
                dumpStr = "WITH_INPUT";
            } else {
                dumpStr = "Undefined Process Code : " + String.valueOf(processCode);
            }
            //Log.d("DUMP", "keyCode2ProcessList[" + String.valueOf(i) + "] = " + dumpStr );
            Log.d("DUMP", "keyCode2ProcessList[" + KeyEvent.keyCodeToString (i) + "] = " + dumpStr );
        }
    }



    //=====================================================
    //<<< Non-task processings >>>
    //=====================================================

    private void changeWlanMode() {
        switch (wlanMode) {
            case WLAN_MODE_OFF :
                notificationWlanAp();
                wlanMode = WLAN_MODE_AP;
                break ;
            case WLAN_MODE_AP :
                if (wlanClList == true) {
                    notificationWlanCl();
                    wlanMode = WLAN_MODE_CL;
                } else {
                    notificationWlanOff();
                    wlanMode = WLAN_MODE_OFF;
                }
                break ;
            case WLAN_MODE_CL :
                notificationWlanOff();
                wlanMode = WLAN_MODE_OFF;
                break ;
        }
    }

    private void changeSpeechVolume() {

        if ( 0<=speechVolume && speechVolume< 30 ) {
            speechVolume = 50;    // Off to Mid
        } else if (30<=speechVolume && speechVolume< 80) {
            speechVolume = 100;   // Mid to Max
        } else { // 70 to 100
            speechVolume = 0;     // Max to Off
        }
        new SoundManagerTask(getApplicationContext(), R.raw.speech_volume , speechVolume).execute();
    }




    //=====================================================
    //<<< web server processings >>>
    //=====================================================

    private Context context;
    private WebServer webServer;

    protected void onDestroy() {
        super.onDestroy();
        if (this.webServer != null) {
            this.webServer.stop();
        }
    }
    private class WebServer extends NanoHTTPD {

        private static final int PORT = 8888;
        private Context context;

        public WebServer(Context context) {
            super(PORT);
            this.context = context;
        }

        @Override
        public Response serve(IHTTPSession session) {
            Method method = session.getMethod();
            String uri = session.getUri();

            switch (method) {
                case GET:
                    return this.serveHtml(uri);
                case POST:
                    Map<String, List<String>> parameters = this.parseBodyParameters(session);
                    Log.d(TAG, "parameters=" + parameters.toString() );
                    execButtonAction(parameters);
                    return this.serveHtml(uri);
                default:
                    return newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, "text/plain",
                            "Method [" + method + "] is not allowed.");
            }
        }

        private Map<String, List<String>> parseBodyParameters(IHTTPSession session) {
            Map<String, String> tmpRequestFile = new HashMap<>();
            try {
                session.parseBody(tmpRequestFile);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ResponseException e) {
                e.printStackTrace();
            }
            return session.getParameters();
        }

        private Response serveHtml(String uri) {
            String html="";

            switch (uri) {
                case "/":
                    html = editHtml();
                    return newFixedLengthResponse(Status.OK, "text/html", html);
                default:
                    html = "URI [" + uri + "] is not found.";
                    return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", html);
            }
        }

        public static final String buttonName1 = "Save" ;
        public static final String buttonName2 = "New History" ;
        public static final String buttonName3 = "Clear History" ;
        public static final String buttonName4 = "Clear settings" ;
        public static final String buttonName5 = "Factory settings" ;
        public static final String buttonName6 = "Simple View" ;
        public static final String buttonName7 = "Advanced View" ;

        private void execButtonAction( Map<String, List<String>> inParameters ) {
            if (inParameters.containsKey("button")) {
                List<String> button = inParameters.get("button");
                Log.d(TAG, "button=" + button.toString() );

                if ( button.get(0).equals(buttonName1) ) {         //Save
                    Log.d(TAG, "exec 1" );
                    updateSettings(inParameters);
                    saveInitalList();
                    readInitalList();
                } else if ( button.get(0).equals(buttonName2) ) {  //New History
                    Log.d(TAG, "exec 2" );
                    Log.d(TAG, " bef KEYCODE_CAMERA=" +  keyCode2KeyProcess(KeyReceiver.KEYCODE_CAMERA));
                    Log.d(TAG, " bef KEYCODE_WLAN_ON_OFF=" +  keyCode2KeyProcess(KeyReceiver.KEYCODE_WLAN_ON_OFF));
                    Log.d(TAG, " bef KEYCODE_MEDIA_RECORD=" +  keyCode2KeyProcess(KeyReceiver.KEYCODE_MEDIA_RECORD));
                    updateSettings(inParameters);
                    Log.d(TAG, " aft KEYCODE_CAMERA=" +  keyCode2KeyProcess(KeyReceiver.KEYCODE_CAMERA));
                    Log.d(TAG, " aft KEYCODE_WLAN_ON_OFF=" +  keyCode2KeyProcess(KeyReceiver.KEYCODE_WLAN_ON_OFF));
                    Log.d(TAG, " aft KEYCODE_MEDIA_RECORD=" +  keyCode2KeyProcess(KeyReceiver.KEYCODE_MEDIA_RECORD));
                } else if ( button.get(0).equals(buttonName3) ) {  //Clear History
                    Log.d(TAG, "exec 3" );
                    updateSettings(inParameters);
                    clearHistory();
                } else if ( button.get(0).equals(buttonName4) ) {  //Clear settings
                    Log.d(TAG, "exec 4" );
                    clearInitalList();
                } else if ( button.get(0).equals(buttonName5) ) {  //Factory settings
                    Log.d(TAG, "exec 5" );
                    setFactorySettings();
                } else if ( button.get(0).equals(buttonName6) ) {  //Simple View
                    Log.d(TAG, "exec 6" );
                    updateSettings(inParameters);
                    advancedOption = false;
                } else if ( button.get(0).equals(buttonName7) ) {  //Advanced View
                    Log.d(TAG, "exec 7" );
                    updateSettings(inParameters);
                    advancedOption = true;
                }
            }
            return;
        }

        private void updateSettings( Map<String, List<String>> inParameters ) {
            //List<String> button = inParameters.get("button"); //save
            if (inParameters.containsKey("button")) {
                inParameters.remove("button");
            }

            //WLAN LogPressは特別
            String keyWlanLongPress = String.valueOf(KEYCODE_WLAN_LONGPRESS);
            if (inParameters.containsKey(keyWlanLongPress)) {
                List<String> wlanLongPress = inParameters.get(keyWlanLongPress);
                inParameters.remove(keyWlanLongPress);
                int setData = Integer.parseInt( wlanLongPress.get(0) );
                if ( (0<setData) && (setData<PROCESS_CODE_MAX_NUM) ) {
                    Log.d(TAG, "wlanLongPress2Process: cur=" + String.valueOf(wlanLongPress2Process) +", set=" + String.valueOf(setData) );
                    wlanLongPress2Process = setData;
                }
            }

            //ポインティングデバイスも特別
            if (advancedOption) {
                for (int i = 0; i < MOTION_INDEX_NUM; i++) {
                    String key = String.valueOf(i+KEYCODE_LIST_MAX_NUM);
                    if (inParameters.containsKey(key)) {
                        List<String> curStrValue = inParameters.get(key);
                        inParameters.remove(key);
                        int curIntValue = Integer.parseInt( curStrValue.get(0) );
                        if ( (0<curIntValue) && (curIntValue<PROCESS_CODE_MAX_NUM) ) {
                            Log.d(TAG, "MotionEvent" + String.valueOf(i) + ": cur=" + String.valueOf(listMotionEvent2Process[i]) +", set=" + String.valueOf(curIntValue) );
                            listMotionEvent2Process[i] = curIntValue;
                        }
                    }
                }
            }

            //その他キー
            Iterator<Map.Entry<String, List<String>>>  iterator = inParameters.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<String>> entryData = iterator.next();
                String entryKeyStr = entryData.getKey();
                List<String> entryVal = entryData.getValue();

                int entryKeyCode = Integer.parseInt(entryKeyStr);
                int entryProcessCode = Integer.parseInt(entryVal.get(0));
                if ( (0<entryKeyCode) && (entryKeyCode<KEYCODE_LIST_MAX_NUM) ) {
                    if ( (0<=entryProcessCode) && (entryProcessCode<PROCESS_CODE_MAX_NUM) ) {
                        if (entryProcessCode==0) {
                            boolean bodyButton=false;
                            for (int i=0; listThetaBodyButton[i]!=-1; i++) {
                                if ( entryKeyCode == listThetaBodyButton[i] ) {
                                    bodyButton=true;
                                    break;
                                }
                            }
                            if (bodyButton) {
                                //常に表示されるので更新不要
                            } else {
                                entryProcessCode = WITH_INPUT;
                                Log.d(TAG, "entryKeyCode[" + String.valueOf(entryKeyCode) + "]: cur=" + String.valueOf(keyCode2ProcessList.get(entryKeyCode)) +", set=" + String.valueOf(entryProcessCode) );
                                keyCode2ProcessList.set(entryKeyCode, entryProcessCode);
                            }
                        } else {
                            Log.d(TAG, "entryKeyCode[" + String.valueOf(entryKeyCode) + "]: cur=" + String.valueOf(keyCode2ProcessList.get(entryKeyCode)) +", set=" + String.valueOf(entryProcessCode) );
                            keyCode2ProcessList.set(entryKeyCode, entryProcessCode);
                        }
                    }
                }
            }
        }

        private String editHtml() {
            String html="";
            html += "<html>";
            html += "<head>";
            //html += "  <meta name='viewport' content='width=device-width,initial-scale=1'>";
            html += "  <meta name='viewport' content='width=480,initial-scale=0.7'>";
            html += "  <title>HID Remote : Key Setting</title>";
            html += "  <script type='text/javascript'>";
            html += "  </script>";
            html += "</head>";
            html += "<body>";
            html += "";
            html += "<form action='/' method='post' name='SettingForm'>";
            html += "  <hr>";
            html += "  <h2>[HID Remote : Key Setting]</h2>";
            html += "  <table>";
            html += "    <tr>";
            html += "      <td><input type='submit' name='button' value='" + buttonName1 + "'></td>";
            html += "      <td> </td>";
            html += "      <td> </td>";
            html += "      <td> </td>";
            html += "      <td> </td>";
            html += "      <td><input type='submit' name='button' value='" + buttonName2 + "'></td>";
            html += "      <td><input type='submit' name='button' value='" + buttonName3 + "'></td>";
            html += "      <td> </td>";
            html += "      <td> </td>";
            html += "      <td> </td>";
            html += "      <td> </td>";
            html += "      <td><input type='submit' name='button' value='" + buttonName4 + "'></td>";
            html += "      <td><input type='submit' name='button' value='" + buttonName5 + "'></td>";
            html += "      <td> </td>";
            if (advancedOption) {
                html += "      <td><input type='submit' name='button' value='" + buttonName6 + "'></td>";
            } else {
                html += "      <td><input type='submit' name='button' value='" + buttonName7 + "'></td>";
            }
            html += "    </tr>";
            html += "  </table>";
            html += "  <hr>";
            html += "  <h3>THETA Body Key's</h3>";
            html += "  ";
            html += "  <table>";

            html += "    <tr>";
            html += "      <td style='background:#CCCCCC'>Key operation</td>";
            html += "      <td style='background:#CCCCCC'>Linked action</td>";
            html += "    </tr>";
            html += "    <tr>";
            html += "      <td " + editCellColor(KeyReceiver.KEYCODE_CAMERA) + ">Shutter Button</td>";
            html += "      <td>";
            html += editSelectOption(KeyReceiver.KEYCODE_CAMERA);
            html += "      </td>";
            html += "    </tr>";

            html += "    <tr>";
            html += "      <td " + editCellColor(KeyReceiver.KEYCODE_WLAN_ON_OFF) + ">WLAN Button Short Press</td>";
            html += "      <td>";
            html += editSelectOption(KeyReceiver.KEYCODE_WLAN_ON_OFF);
            html += "      </td>";
            html += "    </tr>";

            html += "    <tr>";
            html += "      <td " + editCellColor(KEYCODE_WLAN_LONGPRESS) + ">WLAN Button Long Press</td>";
            html += "      <td>";
            html += editSelectOption(KEYCODE_WLAN_LONGPRESS);
            html += "      </td>";
            html += "    </tr>";

            html += "    <tr>";
            html += "      <td " + editCellColor(KeyReceiver.KEYCODE_MEDIA_RECORD) + ">Mode Button</td>";
            html += "      <td>";
            html += editSelectOption(KeyReceiver.KEYCODE_MEDIA_RECORD);
            html += "      </td>";
            html += "    </tr>";

            html += "  </table>";
            html += "  <br>";
            html += "";
            html += "  <hr>";
            html += "  <h3>External Input Devices (Keybord/GamePad/etc)</h3>";

            html += "  <table>";
            html += "    <tr>";
            html += "      <td style='background:#CCCCCC'>Keycode name</td>";
            html += "      <td style='background:#CCCCCC'>Linked action</td>";
            html += "    </tr>";
            html += editExternalInputDevices();
            html += "  </table>";
            html += "  <br>";

            if (advancedOption) {
                html += "  <hr>";
                html += "  <h3>Pointing Devices(Mouse/StylusPen/etc)</h3>";
                html += "  <table>";
                html += "";
                html += "    <tr>";
                html += "      <td style='background:#CCCCCC'>Keycode name</td>";
                html += "      <td style='background:#CCCCCC'>Linked action</td>";
                html += "    </tr>";
                html += "    <tr>";
                html += "      <td " + editCellColor(MOTION_INDEX_PRIMARY+KEYCODE_LIST_MAX_NUM) + ">BUTTON_PRIMARY</td>";
                html += "      <td>";
                html += editSelectOption(MOTION_INDEX_PRIMARY+KEYCODE_LIST_MAX_NUM);
                html += "      </td>";
                html += "    </tr>";
                html += "";
                html += "    <tr>";
                html += "      <td " + editCellColor(MOTION_INDEX_SECONDARY+KEYCODE_LIST_MAX_NUM) + ">BUTTON_SECONDARY</td>";
                html += "      <td>";
                html += editSelectOption(MOTION_INDEX_SECONDARY+KEYCODE_LIST_MAX_NUM);
                html += "      </td>";
                html += "    </tr>";
                html += "";
                html += "    <tr>";
                html += "      <td " + editCellColor(MOTION_INDEX_TERTIARY+KEYCODE_LIST_MAX_NUM) + ">BUTTON_TERTIARY</td>";
                html += "      <td>";
                html += editSelectOption(MOTION_INDEX_TERTIARY+KEYCODE_LIST_MAX_NUM);
                html += "      </td>";
                html += "    </tr>";
                html += "";
                html += "    <tr>";
                html += "      <td " + editCellColor(MOTION_INDEX_BACK+KEYCODE_LIST_MAX_NUM) + ">BUTTON_BACK</td>";
                html += "      <td>";
                html += editSelectOption(MOTION_INDEX_BACK+KEYCODE_LIST_MAX_NUM);
                html += "      </td>";
                html += "    </tr>";
                html += "";
                html += "    <tr>";
                html += "      <td " + editCellColor(MOTION_INDEX_FORWARD+KEYCODE_LIST_MAX_NUM) + ">BUTTON_FORWARD</td>";
                html += "      <td>";
                html += editSelectOption(MOTION_INDEX_FORWARD+KEYCODE_LIST_MAX_NUM);
                html += "      </td>";
                html += "    </tr>";
                html += "";
                html += "    <tr>";
                html += "      <td " + editCellColor(MOTION_INDEX_STYLUS_PRIMARY+KEYCODE_LIST_MAX_NUM) + ">BUTTON_STYLUS_PRIMARY</td>";
                html += "      <td>";
                html += editSelectOption(MOTION_INDEX_STYLUS_PRIMARY+KEYCODE_LIST_MAX_NUM);
                html += "      </td>";
                html += "    </tr>";
                html += "";
                html += "    <tr>";
                html += "      <td " + editCellColor(MOTION_INDEX_STYLUS_SECONDARY+KEYCODE_LIST_MAX_NUM) + ">BUTTON_STYLUS_SECONDARY</td>";
                html += "      <td>";
                html += editSelectOption(MOTION_INDEX_STYLUS_SECONDARY+KEYCODE_LIST_MAX_NUM);
                html += "      </td>";
                html += "    </tr>";
                html += "";
                html += "  </table>";
                html += "  <br>";
            }

            html += "  <br>";
            html += "  <hr>";

            html += "  <h3>About voice data licensing</h3>";
            html += "  The sound data used in this program is created at the following site.<br>";
            html += "  <a href='https://note.cman.jp/other/voice/'>https://note.cman.jp/other/voice/</a><br>";
            html += "  <br>";
            html += "  For the sound created at the above site, the following license notation is necessary.<br>";
            html += "  <br>";
            html += "  <a href='https://creativecommons.org/licenses/by/3.0/deed.ja'><img src='http://mirrors.creativecommons.org/presskit/buttons/80x15/png/by.png' alt='Creative Commons Attribution (CC-BY) 3.0 licens'></a><br>";
            html += "  <br>";
            html += "  HTS Voice \"Mei(Normal)\" Copyright (c) 2009-2013 Nagoya Institute of Technology<br>";
            html += "  <br>";
            html += "  For details of license data of audio data, please refer to <a href='http://www.mmdagent.jp/'>http://www.mmdagent.jp/</a>.<br>";

            html += "</form>";
            html += "";
            html += "</body>";
            html += "</html>";

            return html;
        }

        private String editSelectOption(int keyCode) {
            String result = "";
            int curProcessCode = 0;

            if ( (0<=keyCode) && (keyCode<KEYCODE_LIST_MAX_NUM) ) {
                curProcessCode = keyCode2KeyProcess(keyCode);
            } else if ( (KEYCODE_LIST_MAX_NUM <= keyCode) && (keyCode<(KEYCODE_LIST_MAX_NUM+MOTION_INDEX_NUM)) ) {
                curProcessCode = listMotionEvent2Process[(keyCode-KEYCODE_LIST_MAX_NUM)];
            } else if ( keyCode == KEYCODE_WLAN_LONGPRESS ) {
                curProcessCode = wlanLongPress2Process;
            } else {
                //未定義
                return result ;
            }

            int selectDispNum;
            if (advancedOption) {
                selectDispNum = PROCESS_CODE_MAX_NUM;
            } else {
                if ( curProcessCode > (PROCESS_CODE_SIMPLE_NUM-1) ) {
                    selectDispNum = PROCESS_CODE_MAX_NUM;
                } else {
                    selectDispNum = PROCESS_CODE_SIMPLE_NUM;
                }
            }

            result += "<select name='" + String.valueOf(keyCode) + "'>";
            for (int i=0; i<selectDispNum; i++) {
                result += "<option value='" + String.valueOf(i) + "'";
                if (i==curProcessCode)  {
                    result += " selected";
                }
                result += ">" + processName[i] + "</option>";
            }
            result += "</select>";

            return result ;
        }

        private String editCellColor(int keyCode) {
            String result = "";
            if (keyCode == lastOperationCode) {
                result = " style='background:#FF5555'";
            }
            return result ;
        }

        private String editExternalInputDevices() {
            String result = "";

            int dispLineCnt = 0;
            for (int i=0; i<keyCode2ProcessList.size(); i++) {
                //check Linked
                int processCode = keyCode2ProcessList.get(i);
                if ( processCode == NO_PROCESS ) {
                    continue;
                }

                boolean disp = true;
                //check ban list
                for (int j=0; listBanKeyCode[j]!=-1; j++) {
                    if (i==listBanKeyCode[j]) {
                        disp = false;
                        break;
                    }
                }
                if (disp==false) {
                    continue;
                }
                //check body key
                for (int j=0; listThetaBodyButton[j]!=-1; j++) {
                    if (i==listThetaBodyButton[j]) {
                        disp = false;
                        break;
                    }
                }

                if (disp) {
                    result += "    <tr>";
                    result += "      <td " + editCellColor(i) + ">" + KeyEvent.keyCodeToString (i) + "</td>";
                    result += "      <td>";
                    result += editSelectOption(i);
                    result += "      </td>";
                    result += "    </tr>";

                    dispLineCnt++;
                }
            }
            if ( dispLineCnt == 0 ) {
                result += "    <tr>";
                result += "      <td colspan='2'>There is neither operation history nor external input setting.</td>";
                result += "    </tr>";
            }

            return result ;
        }
    }


}
