Whisper Demo Run on Android
===========================

## Introduction

Whisper Demo is an exemplary and easy-understand app to show what whisper framework can do over whisper network. It shows you can use apps on Android/iOS/Raspberry to control remote device (vice versa) based on P2P technology.

## Whisper network types

Two types of whisper network would be supported:

- Managed whisper network (or centralized whisper network)
- Decentralized whisper network

## Features:

The items for remote control currently includes:

- Turn on/off torch (or flashlight)
- Increase/Decrease screen backgroud light
- Turn on/off ringtone
- Increase/Decrease ringtone volume
- Turn on/off camera video

## Build from source

Run following command to get source code:

```shell
$ git clone https://github.com/stiartsly/WhisperDemo-Android.git
```
Then open this project with Android Studio to build app.

## Build dependencies

Before building whisper demo, you have to download and build the following dependencies:

- Whisper Android SDK (currently vanilla)
- ffmpeg 

As to dependency **Whisper Android SDK**, you need to get source from 

```
https://github.com/stiartsly/whisper-android.git
```
and build it, then copy it's ditribution **io.whisper-debug(release).aar** to **app/libs** directory.

And for dependency **ffmpeg**, please refer to **README.md** under **ffmpeg** directory.

## Deploy && Run

Run on Android Phone with android **API-21 or higher**.

## License

MIT