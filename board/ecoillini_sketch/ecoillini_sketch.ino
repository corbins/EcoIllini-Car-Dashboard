//Ecoillini Car Dashboard Sketch
//Test Board -- LED + Switch + potentiometer + Button
//Developed by: Corbin Souffrant
//Referenced: http://jeffreysambells.com/2011/05/17/understanding-the-demokit-pde-arduino-sketch

#include <Wire.h>
#include <Servo.h>
#include <Max3421e.h>
#include <Usb.h>

#include <AndroidAccessory.h>
#include <CapSense.h>

//TODO: I don't have my board with me, soooo no clue which pins. These are filler.
//Set up the pin locations
#define BUTTON1 A0
#define POTENTIOMETER1 A1
#define SWITCH1 1
#define LED1_RED 2
#define LED1_GREEN 3
#define LED1_BLUE 4

//Identify the Accessory
AndroidAccessory a_device("Ecoillini",
                          "Car Dashboard",
                          "Car Dashboard Controller",
                          "0.1",
                          "https://github.com/corbins/EcoIllini-Car-Dashboard",
                          "0102030212345678"); //Serial?  I think I can just throw a random value here
                          
//TODO: Initialize the hardware

//Initial board setup.
void setup();

//IO loop
void loop();

//TODO: How to setup switch and potentiometer?
void init_hardware() {
  pinMode(BUTTON1, INPUT);
  digitalWrite(BUTTON1, HIGH);
  
  digitalWrite(LED1_RED, 1);
  digitalWrite(LED1_GREEN, 1);
  digitalWrite(LED1_BLUE, 1);
  pinMode(LED1_RED, OUTPUT);
  pinMode(LED1_GREEN, OUTPUT);
  pinMode(LED1_BLUE, OUTPUT);
}

//Button state variable
byte b1;

void setup() {
  Serial.begin(115200); //TODO: wtf is this?
  Serial.print("\r\nStart");
  
  init_hardware();
  
  //TODO: I think I have to set up the analog stuff here... probably...
  b1 = digitalRead(BUTTON1);
  
  a_device.powerOn();
}

void loop() {
  //There might be a lot of junk vars I don't need for my board, I'll come back to it.
  byte err;
  byte idle;
  static byte count = 0;
  byte msg[3];
  long touchcount;
  
  //Look for a connection and go, go, go!
  if(a_device.isConnected()) {
    int len = a_device.read(msg, sizeof(msg), 1);
    int i;
    byte b;
    uint16_t val;
    int x, y;
    
    //Is there a message?
    if(len > 0) {
      
      //msg = [command, target, (byte) value]
      if(msg[0] == 0x2) {
        if(msg[1] == 0x0)
          analogWrite(LED1_RED, 255 - msg[2]);
        if(msg[1] == 0x1)
          analogWrite(LED1_GREEN, 255 - msg[2]);
        if(msg[1] == 0x2)
          analogWrite(LED1_BLUE, 255 - msg[2]);
      }
    }
    
    //Are we sending any data back?
    
  }
}
