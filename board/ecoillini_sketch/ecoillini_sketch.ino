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
