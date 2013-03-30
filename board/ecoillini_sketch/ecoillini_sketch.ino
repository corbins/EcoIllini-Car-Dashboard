//Ecoillini Car Dashboard Sketch
//Test Board -- LED + Switch + potentiometer + Button
//Developed by: Corbin Souffrant


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
AndroidAccessory acc("Ecoillini",
                          "Car Dashboard",
                          "Car Dashboard Controller",
                          "0.1",
                          "https://github.com/corbins/EcoIllini-Car-Dashboard",
                          "0102030212345678"); //Serial?  I think I can just throw a random value here

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

//State variables for hardware
byte b1;
int pot1;

void setup() {
  Serial.begin(115200);
  Serial.print("\r\nStart");
  
  init_hardware();
  b1 = digitalRead(BUTTON1);
  acc.powerOn();
}

void loop() {
  byte err;
  byte idle;
  static byte count = 0;
  byte msg[3];
  byte potmsg[6];
  
  //Look for a connection and go, go, go!
  if(acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 1);
    int i;
    byte b;
    
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
    
    //Are we sending any data back? 0x1 is output command.
    msg[0] = 0x1;
    
    b = digitalRead(BUTTON1);
    if(b != b1) {
      msg[1] = 0;
      msg[2] = b ? 0 : 1;
      acc.write(msg, 3);
      b1 = b;
    }
    
    pot1 = analog_read(POTENTIOMETER1);
    potmsg[0] = 0x3; //0x3 is analog output command.
    potmsg[1] = 0x0;
    potmsg[2] = (byte) (pot1 >> 24);
    potmsg[3] = (byte) (pot1 >> 16);
    potmsg[4] = (byte) (pot1 >> 8);
    potmsg[5] = (byte) (pot1);
    acc.write(potmsg, 6);
  } else {
    //No connection.  Reset values to prevent bad things from happening.
    analogWrite(LED1_RED, 255);
    analogWrite(LED1_GREEN, 255);
    analogWrite(LED1_BLUE, 255);
  }
  
  delay(100);
}

//Referenced: http://jeffreysambells.com/2011/05/17/understanding-the-demokit-pde-arduino-sketch
//Referenced: Beginning Android ADK with Arduino by Mario Bohmer
