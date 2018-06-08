# jOCD

jOCD is Java port of the pyOCD (https://github.com/mbedmicro/pyOCD) project. This is a JAVA library for programming micro:bit board using CMSIS-DAP. Currently, only Android is supported. Windows, Linux and OSX might become supported.

This library is licensed under Apache V 2.0.

## Instructions:

For now we only support list devices through an Android application. The only device implemented is the micro:bit board.
Make sure your device supports USB OTG (https://en.wikipedia.org/wiki/USB_On-The-Go)

### Compiling Instructions

To compile it for Android, open the build project located on "build/android/BuildJocdAndroid/"" with Android Studio and build jocd-conn-android. This should generate the following files:
* build/android/BuildJocdAndroid/java-intelhex-parser/build/libs/java-intelhex-parser.jar
* build/android/BuildJocdAndroid/jocd/build/libs/jocd.jar 
* build/android/BuildJocdAndroid/jocd-conn-android/build/outputs/aar/jocd-conn-android-release.aar

## Example applications:

1. JocdAndroidTestApp
<br />Path: examples/android/JocdAndroidTestApp
<br />Description: Simple Android app to list all connected devices and program using a selected hex file using jOCD API.
<br />Instructions: To run this project you must first compile the library as [described in the compiling section above](#compiling-instructions). After compiling, the application will link the project with the compiled files (remember to keep the folder structure or adjust your project dependencies at your app build.gradle).

2. FlashToolTest
<br />Path: examples/android/FlashToolTest
<br />Description: Simple Android app to list all connected devices and program using a selected hex file. 
<br />Instructions: This example doesn't use the compiled library and use the sources instead (remember to keep the folder sctructure or adjust your project sourceSets at your app build.gradle).