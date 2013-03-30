//Ecoillini Car Dashboard Sketch
//Developed by: Corbin Souffrant

#include <Wire.h>
#include <Servo.h>
#include <Max3421e.h>
#include <Usb.h>

#include <AndroidAccessory.h>
#include <CapSense.h>

//TODO: I don't have my board with me, soooo no clue which pins I need.
//Set up the pin locations
//#define

//Identify the Accessory
AndroidAccessory a_device("Ecoillini",
                          "Car Dashboard",
                          "Car Dashboard Controller",
                          "0.1",
                          "https://github.com/corbins/EcoIllini-Car-Dashboard",
                          "0102030212345678"); //Serial?  I think I can just throw a random value here
                          
//TODO: Initialize the hardware
