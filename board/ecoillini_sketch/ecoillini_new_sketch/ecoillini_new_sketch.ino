/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "Arduino.h"
#include <ADK.h>

ADK L;

// ADK1 usb accessory strings
#define ACCESSORY_STRING_VENDOR "EcoIllini"
#define ACCESSORY_STRING_NAME   "Car Dashboard"
#define ACCESSORY_STRING_LONGNAME "Car Dashboard"
#define ACCESSORY_STRING_VERSION  "0.1"
#define ACCESSORY_STRING_URL    "https://github.com/corbins/EcoIllini-Car-Dashboard"
#define ACCESSORY_STRING_SERIAL "0102030212345678"

void adkPutchar(char c){Serial.write(c);}
extern "C" void dbgPrintf(const char *, ... );

void setup(void)
{
  Serial.begin(115200);

  L.adkSetPutchar(adkPutchar);
  L.adkInit();
  
  // set the old accessory strings
  L.usbSetAccessoryStringVendor(ACCESSORY_STRING_VENDOR);
  L.usbSetAccessoryStringName(ACCESSORY_STRING_NAME);
  L.usbSetAccessoryStringLongname(ACCESSORY_STRING_LONGNAME);
  L.usbSetAccessoryStringVersion(ACCESSORY_STRING_VERSION);
  L.usbSetAccessoryStringUrl(ACCESSORY_STRING_URL);
  L.usbSetAccessoryStringSerial(ACCESSORY_STRING_SERIAL);
  
  L.usbStart();
}

struct SendBuf {
  void Reset() { pos = 0; memset(buf, 0, sizeof(buf)); }
  void Append(int val) { buf[pos++] = val; }
  void Append(uint8_t val) { buf[pos++] = val; }
  void Append(uint16_t val) { buf[pos++] = val >> 8; buf[pos++] = val; }
  void Append(uint32_t val) { buf[pos++] = val >> 24; buf[pos++] = val >> 16; buf[pos++] = val >> 8; buf[pos++] = val; }

  int Send() { return L.accessorySend(buf, pos); }

  uint8_t buf[128];
  int pos;
};

void loop()
{
  static int last_report = 0;
  static uint8_t buttons = 0;

  int now = millis() / 100;
  
  // see if we need to report our current status
  if (now != last_report) {
    SendBuf buf;
    buf.Reset();

    // buttons
      buf.Append(0x01);
      buf.Append(0);
      buf.Append((uint8_t)(buttons & (1)));

    buf.Send();
  }

  // read from phone
  {
    uint8_t buf[64];

    int res = L.accessoryReceive(buf, sizeof(buf));
 
    int pos = 0;
    while (pos < res) {
      uint8_t op = buf[pos++];
  
      switch (op) {
        case 0x2: {
          
          break;
        }
        case 0x3: {
          
          break;
        }
        default: // assume 3 byte packet
          pos += 2;
          break;
      }    
    }
  }
  
  last_report = now;
  
  L.adkEventProcess(); //let the adk framework do its thing
}
