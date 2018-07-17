var express = require('express');
var fs = require('fs');
var multipart = require('connect-multiparty');
var options = require('../module/options');
var logger = require('../module/logger');
var FER = require('../module/faceExtractionRunner');
var Q = require('q')
var ffmpeg = require('fluent-ffmpeg');
var moment = require('moment');
var getDuration = require('get-video-duration');
var rimraf = require('rimraf');

var opts = require('../module/options.js');

var router = express.Router();
var path = require('path');

// ---------------------------------------------
// Get All files
// ---------------------------------------------
/*function listFiles(){
    var files = fs.readdirSync('C:/Users/Lang/Dev/path');
    var retFiles = [];
    for(var idx = 0; idx < files.length; idx++ ){
        retFiles.push(files[idx]);
    }
    return retFiles;
}*/

/** Main Page **/
router.get('/', function (req, res, next) {
       return res.render('idol', {
           title:"Face Extraction Runner"
       });
});

/** Settings Page **/
router.get('/settings', function (req, res, next) {
       return res.render('settings', {
           title:"Face Extraction Runner"
       });
});

// upload route
router.post('/upload',multipart(),function(req,res){
    try {
        // get the filename of uploading
        var filename = req.files.file.originalFilename || path.basename(req.files.file.path);
        //console.log('videro dir:'+req.files.file.path);
        var targetPath = options.videoDir + '/' + filename;
        // create files on server host
        fs.createReadStream(req.files.file.path).pipe(fs.createWriteStream(targetPath));
        // flag to OK
        res.write(JSON.stringify({ OK: 1 }));
        res.end();
    }
	catch(error){
        res.write(JSON.stringify({ OK: 0 }));
        res.end();
    }
});

// upload Configuration route
router.post('/uploadConfiguration',multipart(),function(req,res){
    try {
        // get the filename of uploading
        var filename = req.files.file.originalFilename || path.basename(req.files.file.path);
     
        var targetPathToFEA = options.configFEA + '/uploadedOptions.js';
		var targetPathToHKP = options.configHKP + '/uploadedOptions.js';
		
        // create files on server host
        fs.createReadStream(req.files.file.path).pipe(fs.createWriteStream(targetPathToFEA));
		fs.createReadStream(req.files.file.path).pipe(fs.createWriteStream(targetPathToHKP));
        // flag to OK
        res.write(JSON.stringify({ OK: 1 }));
        res.end();
    }
	catch(error){
        res.write(JSON.stringify({ OK: 0 }));
        res.end();
    }
});

// Verify the params of uploaded config
router.get('/verifyUploadedConfig',function(req,res,next){
	res.set({
        "Content-Type":"text/json"
    });

    var defer = Q.defer();
    FER.verifyUploadedConfig(defer);
    defer.promise.then(function (data) {
        console.log(data);
        res.write(JSON.stringify(data));
        res.end();
    })
});

// upload route
router.get('/checkConfigParams',function(req,res,next){
	res.set({
        "Content-Type":"text/json"
    });

    var defer = Q.defer();
    FER.checkConfigParams(defer);
    defer.promise.then(function (data) {
        console.log(data);
        res.write(JSON.stringify(data));
        res.end();
    })
});

//create database for each video route
router.get('/createVideoDatabases', function(req, res, next){
	res.set({
		"Content-Type":"text/json"
	});
	
	var defer = Q.defer();
	FER.createVideoDatabases(req.query.listDatabases, defer);
    defer.promise.then(function (data) {
        console.log(data);
        res.write(JSON.stringify(data));
        res.end();
    })
});

//create database for each video route
router.get('/removeAllDatabases', function(req, res, next){
	res.set({
		"Content-Type":"text/json"
	});
	
	var defer = Q.defer();
	FER.removeAllDatabases(defer);
    defer.promise.then(function (data) {
        console.log(data);
        res.write(JSON.stringify(data));
        res.end();
    })
});

// analyze route
 router.get('/analyze', function (req, res, next) {
     res.set({
     "Content-Type":"text/json"
     });

     var defer = Q.defer();
	 
	 console.log("Sumit:::::::"+req.query.videoName);
     FER.startAnalysis4UI(req.query.videoName, defer);
     defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
 });

// checkProgress route
router.get('/checkProgress', function (req, res, next) {
    res.set({
        "Content-Type":"text/json"
    });

    var defer = Q.defer();
    FER.checkProgress4UI(req.query.Token, req.query.currentVideoTotalFrames, defer);
    defer.promise.then(function (data) {
        console.log(data);
        res.write(JSON.stringify(data));
        res.end();
    })
});

//stop route
router.get('/stop', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });

     var defer = Q.defer();
     FER.stopAnalysis4UI(req.query.Token, defer);
     defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});

router.get('/faces', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });

     var defer = Q.defer();
     FER.getPathFaces(req.query.Token, defer);
     defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});

router.get('/allFaces', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });

     var defer = Q.defer();
     FER.getPathAllFaces(req.query.Token, req.query.recordId, defer);
     defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});


router.get('/displayMetaData', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });

     var defer = Q.defer();
     FER.getPathMeataData(req.query.Token, req.query.recordId, defer);
     defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});

//For priority
router.get('/givePriority', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });

     var defer = Q.defer();
	 
     FER.priority(req.query.Token, defer, req.query.PriorityValue);
     defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});
//For End Time of Actual Video
router.get('/getEndTime', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });
	 
	 var defer = Q.defer();
	 var inputDirectory = opts.videoDir+'/'+req.query.videoName;
	 
	 FER.getVideoDuration(defer, inputDirectory);
	 
	 defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});
router.get('/videoSize', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });
	 var defer = Q.defer();
	 FER.getVideoSize(defer, req.query.videoName);
	 
	 defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});

//For trim
router.get('/trimVideo', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });
	 
	 var createTrimDir = opts.videoDir+'/trim';

	 if (!fs.existsSync(createTrimDir)){
		fs.mkdirSync(createTrimDir);
	 }
	 
	 var startInputTime = req.query.StartTrimTime;
	 var endInputTime = req.query.EndTrimTime;
	 
	 var startTime = moment(startInputTime, 'hh:mm:ss');
	 var endTime = moment(endInputTime, 'hh:mm:ss');
	 
	 var secondsDiff = moment(endTime).diff(startTime, 'seconds');
	 
	 try{
	 var inputDirectory = opts.videoDir+'\\'+req.query.videoName;
	 var outputDirectory = opts.videoDir+'\\trim\\'+'Trimmed_'+req.query.videoName;
	 ffmpeg(inputDirectory)
		.format('mp4')
		.seekInput(startInputTime)
		.duration(secondsDiff)
		.on('start', function(cmd){
			logger.message('Trimming', 'executing ffmpeg trimming command: '+cmd);
		})
		.on('error', function(err){
			logger.message('Trimming Error',err.message);
			res.end();
		})
		.output(outputDirectory)
		.on('end',function(){
			fs.createReadStream(outputDirectory).pipe(fs.createWriteStream(opts.videoDir+'/'+req.query.videoName));
			logger.message('Trimmed Successfully',req.query.videoName+' trimmed successfully from '+startInputTime+' to '+endInputTime);
			res.end('{"success" : "Trimmed Successfully", "status" : 200}');
		})
		.run();
	}
	 catch(error){
		logger.message('Trimming Error',error.msg);
        res.end('{"error" : "Trim UnSuccessful", "status" : 0}');
    }
});

//subfile Extraction Route
router.get('/extractSubfile', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });
	 var defer = Q.defer();
	 FER.extractSubfile(defer, req.query.containerName);
	 
	 defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});

//After Subfile Extraction
router.get('/getSubFiles', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });
	 
	 var defer = Q.defer();
	 FER.getExtractedFileName(defer);
	 
	 defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});

//Delete Subfiles from temporary directory
router.get('/deleteSubfiles', function(req, res, next){
	try {
		var subFiles = fs.readdirSync(opts.subfileDir);
	
		subFiles.forEach(subFile => {
			fs.unlinkSync(opts.subfileDir+'/'+subFile);
		})
        // flag to OK
        res.write(JSON.stringify({ OK: 1 }));
        res.end();
    }
	catch(error){
		logger.message('Delete subfile Error', error.message);
        res.write(JSON.stringify({ OK: 0 }));
        res.end();
    }
});

//For deleting allfaces image
router.get('/deleteFaces', function(req, res, next){
	try {
		rimraf(req.query.fullFacePath, function () { 
		res.write(JSON.stringify({ OK: 1 }));
        res.end();
		});
    }
	catch(error){
		logger.message('Deletion Error', error.stack);
        res.write(JSON.stringify({ OK: 0 }));
        res.end();
    }
}); 

router.get('/checkFileExtension', function(req, res, next){
	try {
		var countVideo = 0;
		var countContainer = 0;
		for(var i=0; i<opts.videoTypes.length; i++) {
			userVideoExtension = '.'+req.query.fileExtension;
			if(options.videoTypes[i].toLowerCase()==userVideoExtension.toLowerCase()){
				countVideo++;
			}
		}
		for(var i=0; i<opts.containerTypes.length; i++) {
			userVideoExtension = '.'+req.query.fileExtension;
			if(options.containerTypes[i].toLowerCase()==userVideoExtension.toLowerCase()){
				countContainer++;
			}
		}
        if(countVideo==1){
			res.write(JSON.stringify({ OK: 1 }));
			res.end();
		}
		else if(countContainer){
			res.write(JSON.stringify({ OK: 2 }));
			res.end();
		}
		else{
			res.write(JSON.stringify({ OK: 3 }));
			res.end();
		}
    }
	catch(error){
        res.write(JSON.stringify({ OK: 0 }));
        res.end();
    }
});

//open file Explorer
router.get('/openFileExplorer', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });
	 var defer = Q.defer();
	 FER.fileExplorer(defer, req.query.folderPath);
	 
	 defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
});

//Export Face
router.get('/exportFaces', function(req, res, next){
	res.set({
     "Content-Type":"text/json"
     });
	 var defer = Q.defer();
	 FER.exportFaces(defer, req.query.facePath, req.query.folderName, req.query.checkWindow);
	 
	 defer.promise.then(function (data) {
         console.log(data);
         res.write(JSON.stringify(data));
         res.end();
     })
	 /*
	 try {
		targetPath=opts.exportDir;
		var createExportDir=targetPath+'/'+req.query.folderName;
		fs.mkdirSync(createExportDir);
		
		facePathList=req.query.facePath;
		faceNameList=req.query.faceName;
		for(var i=0; i<facePathList.length; i++)
			fs.createReadStream(facePathList[i]).pipe(fs.createWriteStream(path.join(createExportDir,faceNameList[i])));
		
		res.write(JSON.stringify({ OK: 1 }));
        res.end();
    }
	catch(error){
        res.write(JSON.stringify({ OK: 0 }));
        res.end();
    }*/
});  
module.exports = router;
