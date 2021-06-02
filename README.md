# DataHop Device-to-Device Wifi-Direct connectivity

This library implements a Device-to-Device (D2D) connectivity solution for Android devices used by the DataHop Network platform.
[DataHop](https://datahop.network) is a new platform aimed at exploring the potential of a user-operated, smartphone-centric content distribution model for user applications in contrast with the traditional client-server model, that is used by the majority of mobile data transfers today.
In particular, we propose that some nodes, either fetching the content via Internet from the content provider (e.g., BBC, CNN), or via sharing user-generated content, could directly update and share content with other nodes in the vicinity in a D2D manner without requiring Internet connectivity. We leverage on sophisticated information-aware and application-centric connectivity techniques to distribute content between mobile devices in densely-populated urban environments or rural areas where Internet connectivity is poor.

### Information-Centric and Application-Aware Connectivity

WiFi Direct provides all the features required to provide smart connectivity between users and transfer content without infrastructure participation. 
However current Android WiFi Direct Android implementation still has one drawback: it requires user participation to accept every connection. 
In order to require any user participation allowing DataHop to run in the background, seamlessly to users, this library uses a hybrid mode according to which source devices create a WiFi Direct network using the previously described WiFi
Direct Autonomous Mode and destination devices connect to it as a normal WiFi connection (legacy connection).

This library implements the native library for Android of the WiFi Direct specification to exchange application content updates. 
This library is compatible with Go and the IPFS-lite library created in [https://github.com/datahop/ipfs-lite](https://github.com/datahop/ipfs-lite).


## Objectives

* [x] User devices must automatically connect between them without requiring user participation when required (e.g. when discovered content of interest in other devices nearby).
* [x] Connections must be secure, encrypted and allow only traffic coming from the specified app.
* [x] Connectivity should be transparent (and run as a background process) to the user and work when the device is in standby mode.
* [x] Connectivity must take into account power consumption and must implement mechanisms to avoid battery depletion.

# Installation

The library can be built manually using the following command:

```
$ ./gradlew wifidirect:assembleRelease
```

and copy the generated .aar library into the `app/libs` folder of your app.

Note: It requires [Datahop Ipfs-lite](https://github.com/datahop/ipfs-lite]) aar in the libs folder to compile.

To add the library in your Android project first add the libs folder in your repositories list of the project `build.gradle` file,

```
allprojects {
    repositories {
    ....
      flatDir {
          dirs 'libs'
      }
    }
}
```

and then add the library as a depencency of your project in the app `build.gradle` file.

```
dependencies {
    ....
    implementation(name: 'wifidirect-release', ext: 'aar')

}
```
The library can be also automatically imported via gradle: TBC


# Usage

The library is basically implementing two functionalities, the `WifiDirectHotspot` that will create the Wifi-Direct group and will accept incoming connections from other users, and the `WifiLink`, that will connect to other users that previously created a Wifi-Direct group. The two can be created and used following the next instructions:

* From Android:

```
  //Both instances must be created
  WifiDirectHotSpot hotspot = WifiDirectHotSpot.getInstance(getApplicationContext());
  WifiLink connection = WifiLink.getInstance(getApplicationContext());
  
  //A notifier should be passed into the instance. These notifiers can be implemented in Go and must implement WifiConnection and WifiHotspot interfaces defined in github.com/datahop/mobile
  hotspot.setNotifier(hsnotifier);
  connection.setNotifier(cnnotifier);
  
```

`WifiDirectHotspot` and `WifiDirectHotspot` can be direclty handled from Go passing it to the Go app like the following

```
Datahop.init(getApplicationContext().getCacheDir() + "/" + root, this, bleDiscoveryDriver, bleAdvertisingDriver, (WifiHotspot)hotspot,(WifiConnection)connection);
```


* From Go:

Start and stop hotspot

```
hop.hotspot.Start()
hop.hotspot.Stop()

```


Wifi link connect and disconnect

```
hop.wifiCon.Connect(network,pass,address)
hop.wifiCon.Disconnect()
```

# Docs

[Code documentation](https://datahop.github.io/p2p-connection-wifidirect)


# Demo  application

[https://github.com/datahop/datahop-android-demo](https://github.com/datahop/datahop-android-demo)

# How to make contributions
Please read and follow the steps in [CONTRIBUTING.md](/CONTRIBUTING.md)

# License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

# Acknowledgment

This software is part of the NGI Pointer project "Incentivised Content Dissemination at the Network Edge" that has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 871528

<p align="center"><img  alt="ngi logo" src="./Logo_Pointer.png" width=40%> <img  alt="eu logo" src="./eu.png" width=25%></p>

