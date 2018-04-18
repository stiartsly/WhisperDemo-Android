Whisper Demo Run on Android
===========================

Whisper Demo is an easy-understand application to show what Whisper framework can do over whisper network. It shows you can use Apps on Android/iOS/Raspberry to control each other with p2p technology.

## Whisper network types

Two types of whisper network would be supprted:

- Managed whisper network
- Decentralzied whisper network

## Feaures:

The items for remote control currently includes:

- Turn on/off torch (or light)
- Turn on/off ringtone
- Increase/Decrease ringtone volume
- Turn on/off camera video

## Build from source

You should get source code from the following repository on github.com:

```
https://github.com/stiartsly/WhisperDemo-Android.git
```
Then open this android project with Android studio to build it.

## Build dependencies

Before buiding whisper demo, you have to download and build the following dependencies:

- whisper android framework (vanilla)
- ffmpeg 

As to whisper android sdk, you need to get source from 

```
https://github.com/stiartsly/whisper-android.git
```
and after building, copy it's ditributions 'io.whisper-debug.aar' to 'app/libs' directory.

And for dependency 'ffmpeg', please refer to README.md under 'ffmpeg' directory.

## Deploy && Run

Run on android phone with android API-21 or higher.

## License

Whisper Demo project source code files are made available under the MIT License, located in the LICENSE file.