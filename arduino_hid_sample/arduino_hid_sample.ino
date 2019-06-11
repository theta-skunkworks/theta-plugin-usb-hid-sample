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

#include "Keyboard.h"

#define BUTTON1    3

void setup() {
  Keyboard.begin();
  pinMode(BUTTON1, INPUT_PULLUP);
}

void loop() {
  if(digitalRead(BUTTON1) == LOW){
    Keyboard.press(KEY_RETURN);
    delay(100);
    while(digitalRead(BUTTON1) == LOW);
    Keyboard.release(KEY_RETURN);
  }
  
  delay(100);
}


