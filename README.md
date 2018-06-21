<!---
 Copyright 2018 Fundação CERTI

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the license for the specific language governing permissions and
 limitations under the license.
-->
# jOCD
[![GitHub release](https://img.shields.io/github/release/fundacaocerti/jocd.svg)](https://github.com/fundacaocerti/jOCD)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/fundacaocerti/jOCD.svg?branch=master)](https://travis-ci.org/fundacaocerti/jOCD) <a href="https://scan.coverity.com/projects/fundacaocerti-jocd">
  <img alt="Coverity Scan Build Status"
       src="https://scan.coverity.com/projects/16003/badge.svg"/>
</a>

jOCD is multiplatform Java port of the pyOCD (https://github.com/mbedmicro/pyOCD) project. This is a Java library for programming micro:bit board using CMSIS-DAP. Currently the following platforms are supported:

* Android (API >= 21 - Lollipop 5.0)
* Linux (Ubuntu 18.04 LTS)

This library is licensed under Apache V 2.0.

## Instructions:

For now the only device implemented is the micro:bit board.
If you are running in an Android device, make sure your device supports USB OTG (https://en.wikipedia.org/wiki/USB_On-The-Go).

### Compiling Instructions

In order to use the jOCD library, you will need:

* jocd.jar: The jOCD library;
* jocd-conn [jocd-conn-android/jocd-conn-usb4java]: An USB interface for the desired platform (Android or Linux);
* usb4java: a Java library to access USB devices (http://usb4java.org/ - not used on Android - dependency of jocd-conn-usb4java);
* IntelHex Parser: a parser for IntelHex files (https://github.com/j123b567/java-intelhex-parser).

You can compile each desired component by running "./gradlew build", or "./gradle buildAll" to compile itself with all dependencies.

#### Compiling for Android 

To compile it for Android, on the project root, run:
```bash
~/jOCD/jOCD-conn-usb4java$ ./gradlew android
```
This will compile "jocd-conn-android" and its dependencies, generating:

* jocd-conn-android-release.aar
* jocd.jar 
* java-intelhex-parser.jar

Now, you are ready to create your application using jocd and jocd-conn-android as dependency:

build.gradle:
```groovy
dependencies {
    implementation 'cz.jaybee:intelhexparser:1.0.0'
    implementation 'br.org.certi:jocd:1.0.0'
    implementation files('../../../../jocd-conn-android/build/outputs/aar/jocd-conn-android-release.aar')
}
```

#### Compiling for Linux (usb4java)

To compile it for Linux, on the project root, run: 
```bash
~/jOCD/jOCD-conn-usb4java$ ./gradlew usb4java
```
This will compile "jocd-conn-usb4java" and its dependencies, generating:

* jocd-conn-usb4java.jar
* jocd.jar 
* java-intelhex-parser.jar

Now, you are ready to create your application using jocd and jocd-conn-usb4java as dependency:

build.gradle:
```groovy
dependencies {
    implementation 'br.org.certi:jocd-conn-usb4java:1.0.0'
}
```

or

pom.xml:
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

You can compile all dependencies from any of the following examples by running:
```bash
~/jOCD/jOCD-conn-usb4java$ ./gradlew buildAll
```
If you run this, you won't need to follow the [compiling section](#compiling).
You need to run this only once, to setup your dependencies.

### Example applications for Android

1. JocdAndroidTestApp
<br />Path: examples/android/JocdAndroidTestApp
<br />Description: Simple Android app to list all connected devices and program using a selected hex file using jOCD API.
<br />Instructions: To run this project you must first compile the library as [described in the compiling section above](#compiling-for-Android). After compiling, the application will link the project with the compiled files (remember to keep the folder structure or adjust your project dependencies at your app build.gradle).

2. FlashToolTest
<br />Path: examples/android/FlashToolTest
<br />Description: Simple Android app to list all connected devices and program using a selected hex file. 
<br />Instructions: This example doesn't use the compiled library and use the sources instead (remember to keep the folder sctructure or adjust your project sourceSets at your app build.gradle).

### Example applications for Linux

1. JocdUsb4JavaTestCli
<br />Path: examples/usb4java/JocdUsb4JavaTestCli
<br />Description: Simple Java cli application to list all connected devices and program using a selected hex file using jOCD API.
<br />Instructions: To run this project you must first compile the library as [described in the compiling section above](#compiling-for-Windows,-Linux,-Mac-OS-X). 

1. JavaFlashToolTestCli
<br />Path: examples/usb4java/JavaFlashToolTestCli
<br />Description: Simple Java cli application to list all connected devices and program using a selected hex file. 
<br />Instructions: This example doesn't use the compiled library and use the sources instead (remember to keep the folder sctructure or adjust your pom.xml).
