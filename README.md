# QR-Detector

This is the **QR-Detector** app to recognize and translate QR codes in real time,this app also recognize the movement direction of a code in order to know where it goes.


## Build the app
### Pre-requisites
1. #### Tell gradle to use the Azena SDK
   Set the following environment variables:
    ```
    export ANDROID_SDK_ROOT=<path_to_azena_sdk>
    export ANDROID_NDK_HOME=<path_to_azena_sdk>/ndk-bundle

2. #### Set up credentials for Azena's maven repository
   Create a file called `gradle.properties` and populate it with your credentials for [Azena's developer documentation](https://docs.azena.com/) as follows:
   ```
   sstUsername=John.Doe@example.com
   sstPassword=MySecurePassword
   ```
   Place this file in your home directory `.gradle` folder.
   - On Linux, put this file under `~/.gradle/gradle.properties`
   - On Windows, put this file under `%USERPROFILE%/.gradle/gradle.properties`.

3. #### Setup npm registry for Azena's npm packages
   In your home directory, create a file called `.npmrc` and populate it with your credentials for [Azena's developer documentation](https://docs.azena.com/) as follows:

   ```
   @azena:registry=https://artifacts.azena.com/repository/npm/
   //artifacts.azena.com/repository/npm/:username=John.Doe@example.com
   //artifacts.azena.com/repository/npm/:email=John.Doe@example.com
   # Password "MySecurePassword" must be base64-encoded before adding to this file.
   //artifacts.azena.com/repository/npm/:_password=TXlTZWN1cmVQYXNzd29yZA==
   ```
   ##### How to base64-encode your password
   * On Linux/Mac, run the following command:
     ```
     echo -n "MySecurePassword" | openssl base64
     ```
   * On Windows, run the following commands:
     ```
     echo|set/p="MySecurePassword" >temp.txt
     certutil -encodehex temp.txt out.txt 0x40000001
     type out.txt
     del temp.txt out.txt
     ```
           
To build the app, run:

    ./gradlew assembleDebug

### Install and run on Device

 1. Obtain the generated APK file from: 
 
        ./app/build/outputs/apk/debug/app-debug.apk
               
 2. Install the apk file on an Azena Device using ADB
      
        adb install -r -g ./app/build/outputs/apk/debug/app-debug.apk
        
 3. From a web browser, visit `https://<ip_address_of_camera>:8443`.
 4. From the menu on the left pane, select `Applications -> Overview`.
 5. From the apps listed on the right pane, find the app `HelloWorld` and select `App interface and configurations`.
 6. Verify that the video is being streamed.

### Generating a release APK

In order to generate the release APK, be sure to have your signing configuration setup as follows:

 1. The following environment variables must be defined, for example:

        export SIGNING_KEY_ALIAS=keyalias # Choose any name for the signing key
        export SIGNING_KEY_PASSWORD=signingkeypassword # Choose a password for the signing key
        export SIGNING_KEYSTORE_PATH=~/key_name.keystore # Choose a path to store your keystore
        export SIGNING_KEYSTORE_PASSWORD=keystorepassword # Choose a password for the keystore

 2. Run the following command to generate the keystore:

        keytool -genkey -v -keystore $SIGNING_KEYSTORE_PATH \
        -alias $SIGNING_KEY_ALIAS -keyalg RSA \-keysize 2048 \
        -validity 10000 -storepass $SIGNING_KEYSTORE_PASSWORD \
        -keypass $SIGNING_KEY_PASSWORD

    For more information on `keytool`, please refer to `man keytool` or [Oracle's keytool documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)

 3.  From the terminal run:

         ./gradlew assembleRelease