/*
 *  HPE Face Extractor Server options.
 *
 */

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  v a r i a b l e s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var hpeDir = "C:/HewlettPackardEnterprise/",
	dropboxDir = "C:/HewlettPackardEnterprise/dropbox",
	mediaserverConfigDir = hpeDir+"/face-extraction-app/mediaserver"

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  o p t i o n s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
module.exports = {
	// server dependencies
	mediaserver: {
        host       : "localhost",
        port       : 14000,
        failedPath : hpeDir+"/IDOLServer-11.1.0/mediaserver/outputdata/failed"
    },
    faceExtractorServer: {
        host : "localhost",
        port : 14010
    },
    videoActionsServer: {
        host : "localhost",
        port : 14020
    },
	
	//Dropbox directory
	dropboxDir: dropboxDir,
	
	// clips
	createClip       : true,
	createClipFormat : ".mp4",
    clipBuffer       : 500, // milliseconds to record before and after tracked face

	// sources
	videoPath  : "", // optional path to a specific target video, if "" then looks in videoDir
    videoDir   : dropboxDir+"/videos",
    videoTypes : [".mp4", ".avi"],
	containerTypes : [".zip", ".rar", ".tar", ".tar.gz", ".tar.z", ".pptx", ".pdf"],
	
	//tempporary directory for video files that get extracted from container file
	subfileDir : dropboxDir+"/videos/subFiles",
	
	//Binary directory for keyview filter
	keyViewBinDir : "C:/HewlettPackardEnterprise/KeyviewFilterSDK-11.2.0/WINDOWS_X86_64/bin",

	// output
	faceDir : dropboxDir+"/faces",
	
	//export directory
	exportDir : 'C:/faces',
	
	//face-extraction-app config file Directory
	configFEA : hpeDir+"/face-extraction-app",
	
	//Haven Kit platform config file Directory
	configHKP : hpeDir+"/HavenKitPlatform/module",

	// processing
	cfgFile              : mediaserverConfigDir+"/faceExtractor.cfg",
    luaDir               : mediaserverConfigDir,
    xslDir               : mediaserverConfigDir,
    bestFaceOnly         : false, // if 'false', a sub-directory is written for each track, containing cropped face images from each frame
	faceOutputInterval   : 10, // interval within which to deduplicate tracked face output if bestFaceOnly is 'false'
    realTime             : true, // if 'true', frames may be dropped to keep up
    sampleInterval       : 1000, // milliseconds between analysed frames
	numParallel          : 1, // number of video frames to process in parallel.  Requires one CPU core for each.

	// face charactersistics
    minSize              : 40, // pixel width
	colorAnalysis        : false, // set to 'true' to activate, aiming to reduce false positive
	detectTilted         : true,
	faceDirection        : "any", // 'front' or 'any'
    orientation          : "any", // 'upright' or 'any'
	recognitionThreshold : 75 // %
}