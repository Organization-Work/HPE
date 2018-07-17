# face-extraction-app

All commands as entered in [git bash](https://git-scm.com/downloads) on Windows.

## Setup
Put me under C:/HewlettPackardEnterprise:
```
$ mkdir C:/HewlettPackardEnterprise
$ cd C:/HewlettPackardEnterprise
$ git clone https://github.hpe.com/bdp-rich-media-apps/face-extraction-app.git
```

Install [node.js](https://nodejs.org/en/download/) and install this package's dependencies with:
```
$ cd C:/HewlettPackardEnterprise/face-extraction-app
$ npm install
```

Install [ffmpeg](https://ffmpeg.zeranoe.com/builds/), i.e. unzip a static build, and copy the following two executables in this directory:
```
$ cp C:/Program\ Files/ffmpeg/bin/ffmpeg.exe C:/HewlettPackardEnterprise/face-extraction-app/
$ cp C:/Program\ Files/ffmpeg/bin/ffprobe.exe C:/HewlettPackardEnterprise/face-extraction-app/
```

Install License Server and Media Server from the IDOL 11.1.0 installer with default options.

Overwrite Media Server's config file with the one from this package:
```
$ cp C:/HewlettPackardEnterprise/face-extraction-app/mediaserver/mediaserver.cfg C:/HewlettPackardEnterprise/IDOLServer-11.1.0/mediaserver/
```

Install HPE and project services with the included `setup/install_services.bat` file (run as administrator).

## Run
Start the services with the included `setup/start_services.bat` file.

Look at, validate, change the run-time options in `options.js`.

Put a(some) video(s) in the dropbox directory to process.

Launch the processing:
```
$ node faceExtractionRunner.js
```

## Stop
`Ctrl-C`
