# jOCD

jOCD is multiplatform Java port of the pyOCD (https://github.com/mbedmicro/pyOCD) project. This is a JAVA library for programming micro:bit board using CMSIS-DAP. Currently, only Android is supported. Windows, Linux and OSX might become supported.

This library is licensed under Apache V 2.0.

## Instructions:

For now we only support list devices through an Android application. The only device implemented is the micro:bit board.
Make sure your device supports USB OTG (https://en.wikipedia.org/wiki/USB_On-The-Go)

### Compiling Instructions

#### Compiling for Android 

To compile it for Android, open the build project located on "build/android/BuildJocdAndroid/" with Android Studio and build jocd-conn-android. This should generate the following files:
* build/android/BuildJocdAndroid/java-intelhex-parser/build/libs/java-intelhex-parser.jar
* build/android/BuildJocdAndroid/jocd/build/libs/jocd.jar 
* build/android/BuildJocdAndroid/jocd-conn-android/build/outputs/aar/jocd-conn-android-release.aar

#### Compiling for Windows, Linux, Mac OS X

To compile it for those platforms, usb4java (http://usb4java.org/) is used as dependency.
Using Maven (https://maven.apache.org) do the following steps to install the dependencies (jOCD and jocd-conn-usb4java) into your local maven repository:

```bash
~/jOCD$ cd jocd
~/jOCD$ mvn install
~/jOCD$ cd ../jocd-conn-usb4java
~/jOCD$ mvn install
```

Now, you are ready to create your using jocd and jocd-conn-usb4java as dependency:

pom.xml
```xml
<project>
	...
    <dependencies>
       <dependency>
          <groupId>br.org.certi</groupId>
          <artifactId>jOCD</artifactId>
          <version>1.0.0</version>
        </dependency>
       <dependency>
          <groupId>br.org.certi</groupId>
          <artifactId>jOCD-conn-usb4java</artifactId>
          <version>1.0.0</version>
        </dependency>
       <dependency>
          <groupId>org.usb4java</groupId>
          <artifactId>usb4java-javax</artifactId>
          <version>1.2.0</version>
        </dependency>
    </dependencies>
</project>
```

## Example applications:

### Example applications for Android

1. JocdAndroidTestApp
<br />Path: examples/android/JocdAndroidTestApp
<br />Description: Simple Android app to list all connected devices and program using a selected hex file using jOCD API.
<br />Instructions: To run this project you must first compile the library as [described in the compiling section above](#compiling-for-Android). After compiling, the application will link the project with the compiled files (remember to keep the folder structure or adjust your project dependencies at your app build.gradle).

2. FlashToolTest
<br />Path: examples/android/FlashToolTest
<br />Description: Simple Android app to list all connected devices and program using a selected hex file. 
<br />Instructions: This example doesn't use the compiled library and use the sources instead (remember to keep the folder sctructure or adjust your project sourceSets at your app build.gradle).

### Example applications for Windows, Linux, Mac OS X

1. JocdUsb4JavaTestCli
<br />Path: examples/usb4java/JocdUsb4JavaTestCli
<br />Description: Simple Java cli application to list all connected devices and program using a selected hex file using jOCD API.
<br />Instructions: To run this project you must first compile the library as [described in the compiling section above](#compiling-for-Windows,-Linux,-Mac-OS-X). 

1. JavaFlashToolTestCli
<br />Path: examples/usb4java/JavaFlashToolTestCli
<br />Description: Simple Java cli application to list all connected devices and program using a selected hex file. 
<br />Instructions: This example doesn't use the compiled library and use the sources instead (remember to keep the folder sctructure or adjust your pom.xml).
