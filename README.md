# jOCD

jOCD is a port to JAVA/Android from pyOCD (https://github.com/mbedmicro/pyOCD) project. This is a JAVA library for programming and debugging ARM Cortex-M microcontrollers using CMSIS-DAP. Currently, only Android is supported. Windows, Linux and OSX might become supported.

This library is licensed under Apache V 2.0.

## Instructions:

For now we only support list devices through an Android application. The only device implemented is the Microbit board.
Make sure your device supports USB OTG (https://en.wikipedia.org/wiki/USB_On-The-Go)


## Example application:

1. FlashToolTest
<br />Path: examples/android/FlashToolTest
<br />Description: Simple Android app to list all connected devices. 
