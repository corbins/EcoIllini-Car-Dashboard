/*
* ADK usb digitalRead
 *
 * TADA!
 *
 * (c) 2012 D. Cuartielles & A. Goransson
 * http://arduino.cc, http://1scale1.com
 *
 */

#include <AndroidAccessory.h>

// accessory descriptor. It's how Arduino identifies itself to Android
AndroidAccessory usb("Google, Inc.",
		     "DemoKit",
		     "DemoKit Arduino Board",
		     "1.0",
		     "http://www.android.com",
		     "0000000012345678");

// button variables
int buttonPin = A1;
int buttonState = 0;
char letter = 'a';

// counters
long timer = millis();

void setup() {
  // start the connection to the device over the USB host:
  usb.begin();
  Serial.begin(115200);
  Serial.print("\r\nStart");

  pinMode(buttonPin, INPUT);   
}

int b = 0;
int val = 0; //num of cycles
int time = 0; //current time in seconds
int speed_val = 10; //current speed in mph
int distance = 0; //current dist. in miles
  
void loop() {
  /* Read button state */
  buttonState = digitalRead(buttonPin);
  uint8_t buf[9];

  /* Print to usb */
  if(millis()-timer>100) { // sending 10 times per second
    if (usb.isConnected()) { // isConnected makes sure the USB connection is ope
    if(val>=100) {
      val = 0; //keep val from getting obnoxiously large, prevent chance of overflow
              //I can't send floats over I believe, so I'm converting it to an int and fixing it on androids end
      distance+=speed_val * 100000 / 3600;
    }
    
      if(val%10==0) {
        time++; //time is one second larger now! since ~10 cycles a second
      }
      val++;

      //buf[0] = command type, check android for reference
      //buf[1] and buf[2] = int storage for transfer
      buf[0] = 0x1;
      buf[1] = speed_val>>8;
      buf[2] = speed_val & 0xff;
      usb.write( buf, 3);
      buf[0] = 0x2;
      buf[1] = (distance)>>8;
      buf[2] = (distance) & 0xff;
      usb.write( buf, 3);
      buf[0] = 0x3;
      buf[1] = (time)>>8;
      buf[2] = (time) & 0xff;
      usb.write( buf, 3);
    }
    timer = millis();
  }
}





