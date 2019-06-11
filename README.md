# HID remote control plug-in Sample for RICOH THETA
This sample plug-in does control THETA with HID(Human Interface Device).<br>
This project is based on [ricohapi/theta-plugin-sdk](https://github.com/ricohapi/theta-plugin-sdk).


## Usage
With this THETA plug-in, HID (Human Interface Device) connected to the USB port can be treated as an expanded operation button.
HID (Human Interface Device) has various tools including wired / wireless such as keyboard, mouse, game pad, finger presenter (slide control tool).

Regardless of the presence or absence of a USB-connected external operation button, the action corresponding to the operation button can be selected and set from the following 27 types.
This means that the role of the main unit control button can also be changed.


|No.|Name of action|Description|Simple View|Advanced View|
|---|---|---|---|---|
|0|NOP|do nothing(Used to eliminate assignment)|chooseable|chooseable|
|1|EXEC_SHUTTER|Image mode:Still image shooting, Video mode:Start/Stop|chooseable|chooseable|
|2|TGGLE_WLAN|Toggle the state of wlan (off->AP mode->CL mode->off...)|chooseable|chooseable|
|3|TGGLE_CAPTURE_MODE|Toggle the state of capture mode (iamge<->video)|chooseable|chooseable|
|4|TGGLE_EXP_DELAY|Toggle the state of exposure delay (off<->Final setting values other than off)|chooseable|chooseable|
|5|TGGLE_CAMERA_VOL|Toggle the state of camera volume (off->medium->loud->off...)|chooseable|chooseable|
|6|TGGLE_SPEECH_VOL|Toggle the state of speech volume (off->medium->loud->off...)|chooseable|chooseable|
|7|SET_EV_ZERO|Set the exposure compensation value to zero|chooseable|chooseable|
|8|SET_EV_PLUS |Set the exposure compensation value to +0.3 from the current value.|chooseable|chooseable|
|9|SET_EV_MINUS|Set the exposure compensation value to -0.3 from the current value.|chooseable|chooseable|
|10|SET_EXP_DELAY_OFF|Set exposure delay to off|chooseable|chooseable|
|11|SET_EXP_DELAY_1|Set exposure delay to 1 second|-|chooseable|
|12|SET_EXP_DELAY_2|Set exposure delay to 2 second|-|chooseable|
|13|SET_EXP_DELAY_3|Set exposure delay to 3 second|-|chooseable|
|14|SET_EXP_DELAY_4|Set exposure delay to 4 second|-|chooseable|
|15|SET_EXP_DELAY_5|Set exposure delay to 5 second|-|chooseable|
|16|SET_EXP_DELAY_6|Set exposure delay to 6 second|-|chooseable|
|17|SET_EXP_DELAY_7|Set exposure delay to 7 second|-|chooseable|
|18|SET_EXP_DELAY_8|Set exposure delay to 8 second|-|chooseable|
|19|SET_EXP_DELAY_9|Set exposure delay to 9 second|-|chooseable|
|20|SET_EXP_DELAY_10|Set exposure delay to 10 second|-|chooseable|
|21|SET_CAP_MODE_IMAGE|Set capture mode to iamge|-|chooseable|
|22|SET_CAP_MODE_VIDEO|Set capture mode to video|-|chooseable|
|23|SET_CAMERA_VOL_PLUS |Increase the camera volume by one level(10/100).|-|chooseable|
|24|SET_CAMERA_VOL_MINUS|Decrease the camera volume by one level(10/100).|-|chooseable|
|25|SET_SPEECH_VOL_PLUS |Increase the camera volume by one level(20/100).|-|chooseable|
|26|SET_SPEECH_VOL_MINUS|Decrease the camera volume by one level(20/100).|-|chooseable|

There are "Simple View" and "Advanced View" on the setting screen (webUI). In "Simple View", you can not select No. 11 or later (displayed if it is selected).


To link operation buttons and actions, use webUI.
When you first display the web UI, settings for demonstration have already been made.
Detailed usage of webUI is as follows.

|webUI Button name|Action|
|---|---|
|Save|Save the currently displayed setting|
|New History|Display the latest operation history|
|Clear History|Erase the operation history|
|Clear settings|Delete all settings|
|Factory settings|Set as a demonstration setting|
|Simple View/Advanced View|Switch between "Simple View" and "Advanced View"|


Points to note: Key code directly affecting the OS

Since the key codes summarized in the table below directly affect the Android OS, the following phenomena may occur.

 - If the plug-in side can not receive operation events. 
 - If THETA V goes to sleep. 
 - Inside THETA V, the dialog remains displayed. (Since there is no screen, it is impossible to answer both OK and NG, it seems that THETA V hung up.) 

Especially when the dialog is displayed, operations other than turning off the power will not be accepted. Please turn the power button OFF-> ON to restore the camera.

Some Android-compliant HID products generate these key events, so please be careful not to perform the operation itself when connecting to THETA V.
Before using with THETA V, we recommend that you connect to Android smartphone and check the operation.
Those who set the THETA V into developer mode, please check the operation while watching the Vysor screen and then perform only the safe key operation.

| KeyCode name | Reason for prohibiting use (operation) with this plugin |
|---|---|
|KEYCODE_HOME                     | The plug-in ends with the equivalent of the HOME button |
|KEYCODE_ENDCALL                  | THETA will sleep |
|KEYCODE_POWER                    | THETA will sleep |
|KEYCODE_SYM                      | The dialog "select keybord" will remain displayed |
|KEYCODE_ENVELOPE                 | E-Mail application initial setting dialog will remain displayed |
|KEYCODE_SYSRQ                    | screenshot is taken and onKeyDown event does not occur |
|KEYCODE_PICTSYMBOLS              | Emoji character input screen is displayed |
|KEYCODE_APP_SWITCH               | Multitask menu is displayed (It is equivalent to terminating plug-in) |
|KEYCODE_LANGUAGE_SWITCH          | The dialog "tggle keybord" will remain displayed |
|KEYCODE_CONTACTS                 | onKeyDown event does not occur |
|KEYCODE_CALENDAR                 | onKeyDown event does not occur |
|KEYCODE_MUSIC                    | MUSIC application initial setting dialog will remain displayed |
|KEYCODE_CALENDAR                 | onKeyDown event does not occur |
|KEYCODE_ASSIST                   | onKeyDown and onKeyUp events do not occur |
|KEYCODE_BRIGHTNESS_DOWN          | Plugin will end |
|KEYCODE_BRIGHTNESS_UP            | Plugin will end |
|KEYCODE_SLEEP                    | THETA will sleep |
|KEYCODE_WAKEUP                   | onKeyDown and onKeyUp events do not occur |
|KEYCODE_SOFT_SLEEP               | THETA will sleep |
|KEYCODE_SYSTEM_NAVIGATION_UP     | onKeyDown and onKeyUp events do not occur |
|KEYCODE_SYSTEM_NAVIGATION_DOWN   | onKeyDown and onKeyUp events do not occur |
|KEYCODE_SYSTEM_NAVIGATION_LEFT   | onKeyDown and onKeyUp events do not occur |
|KEYCODE_SYSTEM_NAVIGATION_RIGHT  | onKeyDown and onKeyUp events do not occur |




## Development Environment
### Camera
* RICOH THETA V Firmware ver.3.00.1 and above
* RICOH THETA Z1 Firmware ver.1.03.5 and above

### SDK/Library
* RICOH THETA Plug-in SDK ver.1.0.1

### Development Software
* Android Studio ver.3.1
* gradle ver.4.6
* arduino IDE ver.1.8.5

## About voice data licensing 
The sound data used in this program is created at the following site.

[https://note.cman.jp/other/voice/](https://note.cman.jp/other/voice/)

For the sound created at the above site, the following license notation is necessary.

[![Creative Commons Attribution (CC-BY) 3.0 license](http://mirrors.creativecommons.org/presskit/buttons/80x15/png/by.png)](https://creativecommons.org/licenses/by/3.0/deed.ja)

HTS Voice "Mei(Normal)" Copyright (c) 2009-2013 Nagoya Institute of Technology

For details of license data of audio data, please refer to [http://www.mmdagent.jp/](http://www.mmdagent.jp/).


## License

```
Copyright 2018 Ricoh Company, Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contact
![Contact](img/contact.png)

