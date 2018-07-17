/*                                                              
 *  HPE Face Extractor control script.
 *  
 */                                                             

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  d e p e n d e n c i e s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var http = require('http'),
    fs = require('fs'),
    path = require('path'),
    request = require('request'),
    logger = require('./logger.js'),
    opts = require('./options.js');
	copydir = require('copy-dir');
var getDuration = require('get-video-duration');
const uuidV4 = require('uuid/v4');
var mv = require('mv');

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  c o n f i g u r a t i o n
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var config = {
		log: {
			file: path.resolve("./logs/faceExtractionRunner.log"),
			maxSizeMB: 3,
			rotateIntervalSec: 60*5
		},
		checkProgressIntervalMSec: 1000*5 
	};

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  v a r i a b l e s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  f u n c t i o n s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
function xmlClean() {
    try {
        var xml = fs.readdirSync(opts.mediaserver.failedPath);
        for (var i=0; i<xml.length; i++) {
            fs.unlinkSync(opts.mediaserver.failedPath+path.sep+xml[i]);
        }
    } catch(e) {
        logger.message('xmlClean', "No old XML to clean.");
    }
}

function validateOptions() {
    
    // logger.message('validateOptions', JSON.stringify(opts, null, 2));
    
    // directories
	if(opts.dropboxDir=='' || false === fs.existsSync(opts.dropboxDir)){
		logger.message('dropboxDir Error','dropboxDir is "'+opts.dropboxDir+'"');
	}
	
	else{
    var params = ['faceDir', 'videoDir', 'subfileDir'],
        defaults = ['faces', 'video', 'subFiles'];
    
    for(var i=0; i<params.length; i++) {
        if('undefined' === typeof opts[params[i]]) {
            opts[params[i]] = defaults[i];
            logger.message('validateOptions', 'Setting default values for '+params[i]);
            
        } else {
            opts[params[i]] = path.resolve(opts[params[i]]);
            
            if(false === fs.existsSync(opts[params[i]])) {
				fs.mkdirSync(opts[params[i]]);
            }
        }
    }
    
    // video path
    if('undefined' === typeof opts.videoPath) {
        opts.videoPath = '';
        logger.message('validateOptions', 'Setting default values for videoPath');
        
    } else if('' !== opts.videoPath) {
        opts.videoPath = path.resolve(opts.videoPath);
    }
	
    // numeric parameters
    params = ['recognitionThreshold', 'minFaceWidth', 'sampleInterval', 'clipBuffer'];
    defaults = [80, 50, 200, 500];
    for(var i=0; i<params.length; i++) {
        if('undefined' === typeof opts[params[i]]) {
            opts[params[i]] = defaults[i];
            logger.message('validateOptions', 'Setting default values for '+params[i]);
            
        } else if('number' !== typeof opts[params[i]]) {
            opts[params[i]] = defaults[i];
            logger.message('validateOptions', 'Setting default values for '+params[i]);
            
        } else if(0 > opts[params[i]]) {
            opts[params[i]] = defaults[i];
            logger.message('validateOptions', 'Setting default values for '+params[i]);
        }
    }
    
    // boolean parameters
    params = ['createClip', 'bestFaceOnly'];
    defaults = [false, true];
    for(var i=0; i<params.length; i++) {
        if('undefined' === typeof opts[params[i]]) {
            opts[params[i]] = defaults[i];
            logger.message('validateOptions', 'Setting default values for '+params[i]);
            
        } else if('boolean' !== typeof opts[params[i]]) {
            opts[params[i]] = defaults[i];
            logger.message('validateOptions', 'Setting default values for '+params[i]);
        }
    }
    
    // video file types
    defaults = ['.mp4', '.avi'];
    if('object' !== typeof opts.videoTypes) {
        opts.videoTypes = defaults;
        logger.message('validateOptions', 'Setting default values for videoTypes');
    
    } else {
        for(var i=0; i<opts.videoTypes.length; i++) {
            if('string' === typeof opts.videoTypes[i]) {
                opts.videoTypes[i] = opts.videoTypes[i].toLowerCase();
                
                if('.' !== opts.videoTypes[i][0]) {
                    opts.videoTypes[i] = '.'+opts.videoTypes[i];
                }
            } else {
                opts.videoTypes[i] = defaults[0];
            }
        }
    }
    
    logger.message('validateOptions', JSON.stringify(opts, null, 2));
	}
}

function checkConfigParams(defer){
	var params = ['dropboxDir','videoDir','subfileDir','keyViewBinDir','faceDir','exportDir','configFEA','configHKP','cfgFile','luaDir','xslDir'];
	var missingParamMessage='';
	for(var i=0; i<params.length; i++) {
		var flag=false;
		var logMessage='';
		if('undefined' === typeof opts[params[i]]) {
			logMessage+='Cannot find any '+params[i]+' parameter in config file which is exported.';
			logger.message(params[i]+' Param Missing', logMessage);
			missingParamMessage+=logMessage;
			flag=true;
		}
	
		if(!flag && false === fs.existsSync(opts[params[i]])) {
			if(opts[params[i]]==''){
				logMessage+=params[i]+' is declared without any value.';
				logger.message(params[i]+' Param Missing', logMessage);
				missingParamMessage+=logMessage;
			}
			else if(opts[params[i]].indexOf('\\')<=0 && opts[params[i]].indexOf('/')==-1){
				logMessage+=params[i]+' contains \\ ,Replace \\ with /';
				logger.message(params[i]+' Invalid Format', logMessage);
				missingParamMessage+=logMessage;
			}
			else{
				logMessage+=params[i]+' path does not exist.';
				logger.message(params[i]+' Param Missing', logMessage);
				missingParamMessage+=logMessage;
			}
        }
	}
	defer.resolve({"missingParamMessage":missingParamMessage});
}

function verifyUploadedConfig(defer){
	try{
	var targetPathToFEA = opts.configFEA + '/uploadedOptions.js';
	var targetPathToHKP = opts.configHKP + '/uploadedOptions.js';
	
	var FEAopts = require(targetPathToFEA);
	var HKPopts = require(targetPathToHKP);
	
	var params = ['dropboxDir','videoDir','subfileDir','keyViewBinDir','faceDir','exportDir','configFEA','configHKP','cfgFile','luaDir','xslDir'];
	var missingParamMessage='';
	
	for(var i=0; i<params.length; i++) {
		var flag=false;
		var logMessage='';
		if('undefined' === typeof HKPopts[params[i]]) {
			logMessage+='Cannot find any '+params[i]+' parameter in config file which is exported.';
			logger.message('Unable to update', logMessage);
			missingParamMessage+=logMessage;
			flag=true;
		}
		if(!flag && false === fs.existsSync(HKPopts[params[i]])) {
			if(HKPopts[params[i]]==''){
				logMessage+=params[i]+' is declared without any value.';
				logger.message('Unable to update', logMessage);
				missingParamMessage+=logMessage;
			}
			
			else if(HKPopts[params[i]].indexOf('\\')<=0 && HKPopts[params[i]].indexOf('/')==-1){
				logMessage+=params[i]+' contains \\ ,Replace \\ with /';
				logger.message(params[i]+' Invalid Format', logMessage);
				missingParamMessage+=logMessage;
			}
			else{
				logMessage+=params[i]+' path does not exist.';
				logger.message('Unable to update', logMessage);
				missingParamMessage+=logMessage;
			}
        }
	}
	
	var success = false;
	var successMessage = '';
	if(missingParamMessage.length==0){
		success = true;
		fs.createReadStream(targetPathToFEA).pipe(fs.createWriteStream(opts.configFEA + '/options.js'));
		fs.createReadStream(targetPathToHKP).pipe(fs.createWriteStream(opts.configHKP + '/options.js'));
		logger.message('Config Updated Successfully', 'Configuration file for face-extraction-app and HavenKitPlatform is updated successfully');
		successMessage = 'Config Updated Successfully';
	}
	
	/*
	// numeric parameters
    params = ['recognitionThreshold', 'minFaceWidth', 'sampleInterval', 'clipBuffer'];
    defaults = [80, 50, 200, 500];
	
	var numericParamsMessage = '';
	
	for(var i=0; i<params.length; i++){
		var logMessage = '';
		if('undefined' === typeof HKPopts[params[i]] || HKPopts[params[i]]=='') {
			logMessage+=params[i]+" is '"+ HKPopts[params[i]] +"' in the config file and default value is set to "+defaults[i];
			logger.message('Set To Default Value', logMessage);
			numericParamsMessage+=logMessage;
		}
	}
	*/
	
	var defaultValuesMessage = '';
	if(HKPopts['videoPath']=='' || 'undefined' === typeof HKPopts['videoPath']){
		var logMessage="videoPath is '"+HKPopts['videoPath']+"' in the config file and set to default value '"+HKPopts['videoDir']+"'.";
		if(!success){
			logMessage = logMessage.split('set to default value').join('default value is set to')
		}
		logger.message('Set To Default Value', logMessage);
		defaultValuesMessage+=logMessage
	}

	fs.unlinkSync(targetPathToFEA);
	fs.unlinkSync(targetPathToHKP);
	
	defer.resolve({"missingParamMessage":missingParamMessage,"defaultValuesMessage":defaultValuesMessage,"successMessage":successMessage});
	}
	catch(error){	
		if(error){
			var logPath = opts.configHKP+'/faceExtractionRunner.log';
			logPath = logPath.replace("module", "logs").replace(/\//g, "\\").replace("\\\\", "\\");
			//logPath = logPath.replace(/\//g, "\\");
			//logPath = logPath.replace("\\\\", "\\");
			logger.message('Syntax Error', error.stack);
			defer.resolve({"Error":error.message, "logFilePath": logPath});
		}
	}
}

function getAutnresponse(action, options, defer, callback) {

    http.get(options, function(response) {
        var body = '';
        response.on("data", function(data) {
            body += data;
        });
        response.on("end", function(data) {
            var json = JSON.parse(body);
            logger.message(action, json.autnresponse.response['$']);
            callback(json);
        });
    }).on('error', function(e) {
		logger.message(action, 'Cannot connect to HPE Media Server on '+opts.mediaserver.host+':'+opts.mediaserver.port);
		defer.resolve({"Error":1});
    });
}

function setupDbGetAutnresponse(action, options, callback) {

    http.get(options, function(response) {
        var body = '';
        response.on("data", function(data) {
            body += data;
        });
        response.on("end", function(data) {
            var json = JSON.parse(body);
            logger.message(action, json.autnresponse.response['$']);
            callback(json);
        });
    }).on('error', function(e) {
        logger.message(action, 'Cannot connect to HPE Media Server on '+opts.mediaserver.host+':'+opts.mediaserver.port);
    });
}

function manageDatabase(action, databaseName, callback){
	var options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&database="+databaseName+"&ResponseFormat=JSON&Synchronous=True"
        };
		
	http.get(options, function(response) {
        var body = '';
        response.on("data", function(data) {
            body += data;
        });
        response.on("end", function(data) {
            var json = JSON.parse(body);
            logger.message(action+" "+databaseName, json.autnresponse.response['$']);
            callback(json);
        });
    }).on('error', function(e) {
        logger.message(action, 'Cannot connect to HPE Media Server on '+opts.mediaserver.host+':'+opts.mediaserver.port);
    });
}

/*
function setGracefulExit() {
    process.on("SIGINT", doGracefulExit);
}

function doGracefulExit() {
	logger.message('gracefulExit', 'Sending stop signal...');
	stopMediaServer(function(d) {
		process.exit();
    });
}

function stopMediaServer(callback) {
    
    requestedStop = true;
    
    var action = "queueinfo",
        options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&queueaction=stop&queuename=process&token="+currentToken+"&ResponseFormat=JSON&Synchronous=True"
        };
        
    setupDbGetAutnresponse(action, options, callback);
}
*/

/*
function setupDb(action, callback) {
    
    var options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&database="+opts.mediaserver.db+"&ResponseFormat=JSON&Synchronous=True"
        };
        
    setupDbGetAutnresponse(action, options, callback);
}*/
/*
function clearDatabase(){
	action = "QueueInfo";
	QueueAction = "pause";
	QueueName = "Process"
	var options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&QueueAction="+QueueAction+"&QueueName="+QueueName+"&ResponseFormat=JSON"
        };
		
	setupDbGetAutnresponse(action, options, function(){
		logger.message(QueueAction+' Action','SUCCESS');
		setupDb("removeFaceDatabase", function() {
		action = "QueueInfo";
		QueueAction = "resume";
		QueueName = "Process"
		var options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&QueueAction="+QueueAction+"&QueueName="+QueueName+"&ResponseFormat=JSON"
        };
		setupDbGetAutnresponse(action, options, function(){
					logger.message(QueueAction+' Action','SUCCESS');
					setupDb("createFaceDatabase", function() {
				});
			});
		});
	});
}*/
function getVideoDuration(defer, inputDirectory){
		getDuration(inputDirectory).then(function(duration) {
			defer.resolve({"Status":"Success","duration":duration});
		}).catch(function(e){
			if(e.message=='spawn ffprobe ENOENT'){
				logger.message('Get Video Duration Error','ffprobe is not installed')
			}
			logger.message('Get Video Duration Error',e.stack);
			defer.resolve({"Status":"Error"});
		});
}

function priority(Token, defer, PriorityValue){
    var action = "queueinfo",
        options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&queueaction=changePriority&queuename=process&token="+Token+"&ResponseFormat=JSON&priority="+PriorityValue+""
        };
		
	getAutnresponse(action, options, defer, function(c) {
		defer.resolve({"currentStatus":"Queued","recordCount":0});
        logger.message("Extraction priority",'priority set to '+ PriorityValue);
    });
}

/*below functions are added for working with UI*/
function startAnalysis4UI(videoName, defer) {

    var currentVideoTotalFrames = 0;
    var currentToken = "testtoken";

    logger.message('startAnalysis', 'Processing '+videoName);
    //-----------------------------------
    // SESSION CONFIG TEMPLATE
    var cfg = fs.readFileSync(opts.cfgFile, "utf8");

    //  I N G E S T
    if (false === opts.realTime) {
        cfg = cfg.split("^REALTIME^").join("");
    } else {
        cfg = cfg.split("^REALTIME^").join("//");
    }
	
	var databaseName = videoName.split(".")[0];
	var hashCode = getMD5_Hash(databaseName);
	
    //  A N A L Y S I S
    cfg = cfg
        .split("^SAMPLEINTERVAL^").join(opts.sampleInterval.toString())
        .split("^NUMPARALLEL^").join(opts.numParallel.toString())
        .split("^MINSIZE^").join(opts.minSize.toString())
        .split("^COLORANALYSIS^").join(opts.colorAnalysis)
        .split("^DETECTTITLED^").join(opts.detectTilted)
        .split("^FACEDIRECTION^").join(opts.faceDirection)
        .split("^ORIENTATION^").join(opts.orientation)
        .split("^FACEDB^").join(hashCode)
        .split("^RECOGNITIONTHRESHOLD^").join(opts.recognitionThreshold.toString());

    //  E V E N T P R O C E S S I N G
    cfg = cfg
        .split("^LUADIR^").join(opts.luaDir)
        .split("^FACEOUTPUTINTERVAL^").join(opts.faceOutputInterval);

    //  E N C O D I N G
    if (false === opts.bestFaceOnly) {
        cfg = cfg.split("^ALLDATA^").join("");
    } else {
        cfg = cfg.split("^ALLDATA^").join("//");
    }

    cfg = cfg
        .split("^FACEPATH^")
        .join(opts.faceDir+"/"+path.basename(videoName).split('.')[0]);

    //  O U T P U T
    var createClipAndAllData = (true === opts.createClip) && (false === opts.bestFaceOnly),
        createClipOnly = (false === createClipAndAllData) && (true === opts.createClip),
        allDataOnly = (false === createClipAndAllData) && (false === opts.bestFaceOnly);

    if (true === createClipAndAllData) {
        cfg = cfg.split("^CREATECLIPANDALLDATA^").join("");
    } else {
        cfg = cfg.split("^CREATECLIPANDALLDATA^").join("//");
    }

    if (true === createClipOnly) {
        cfg = cfg.split("^CREATECLIPONLY^").join("");
    } else {
        cfg = cfg.split("^CREATECLIPONLY^").join("//");
    }

    if (true === allDataOnly) {
        cfg = cfg.split("^ALLDATAONLY^").join("");
    } else {
        cfg = cfg.split("^ALLDATAONLY^").join("//");
    }

    cfg = cfg
        .split("^FAILEDPATH^").join(opts.mediaserver.failedPath)
        .split("^XSLPATH^").join(opts.xslDir)
        .split("^HOST^").join(opts.faceExtractorServer.host)
        .split("^PORT^").join(opts.faceExtractorServer.port.toString());

    logger.message('startAnalysis', cfg);
    //-----------------------------------

    // inquire video server for currentVideoTotalFrames
    var options = {
        host: opts.videoActionsServer.host,
        port: opts.videoActionsServer.port,
        path: "/videoPath=" + opts.videoDir + "/" +encodeURI(videoName)
    };

    http.get(options, function(response) {
        var body = '';
        response.on("data", function(data) {
            body += data;
        });
        response.on("end", function(data) {

            var nFrames = parseInt(body);

            logger.message('startAnalysis', 'Video frames to process: '+nFrames);
            //currentVideoTotalFrames = +nFrames;
            currentVideoTotalFrames = nFrames;

            // videoActionsServer returns some error
            if (-1 == currentVideoTotalFrames)
            {
                logger.message('startAnalysis', 'video server return -1.');
                defer.resolve({"currentVideoTotalFrames":currentVideoTotalFrames,"currentToken":currentToken});
                return;
            }

            var bsf = new Buffer(cfg).toString('base64'),
                action = "process",
                options = {
                    host: opts.mediaserver.host,
                    port: opts.mediaserver.port,
                    path: "/action="+action+"&source="+opts.videoDir + "/" + encodeURI(videoName)+"&config="+bsf+"&ResponseFormat=JSON"
                };

            //console.log(bsf);

            getAutnresponse(action, options, defer, function(d) {
                currentToken = d.autnresponse.responsedata.token["$"];
                defer.resolve({"currentVideoTotalFrames":currentVideoTotalFrames,"currentToken":currentToken});
            });
        });
    }).on('error', function(e) {
        logger.message('startAnalysis', 'Cannot connect to video actions server.');
        defer.resolve({"currentVideoTotalFrames":-1,"currentToken":currentToken});
    });
}

function stopAnalysis4UI(Token, defer){
	
    var action = "queueinfo",
        options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&queueaction=stop&queuename=process&token="+Token+"&ResponseFormat=JSON&Synchronous=True"
        };
		
	getAutnresponse(action, options, defer, function(c) {
		defer.resolve({"currentStatus":"stoped","recordCount":-1});
        logger.message("Extraction Process",'Extraction Process Stoped.');
    });	
}

function getVideoSize(defer, videoName){
	var stats = fs.statSync(opts.videoDir+'/trim/'+'Trimmed_'+videoName);
	var size = stats["size"];
	// convert it to humanly readable format.
	var i = Math.floor( Math.log(size) / Math.log(1024) );
	var fileSize = ( size / Math.pow(1024, i) ).toFixed(2) * 1 + ' ' + ['B', 'KB', 'MB', 'GB', 'TB'][i];
	defer.resolve({"Size":fileSize});
}

function getPathAllFaces(Token, recordID, defer){
	var action = "queueinfo",
        options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&queueAction=getStatus&queueName=process&token="+Token+"&ResponseFormat=JSON"
        };
	getAutnresponse(action, options, defer, function(c) {
			var pathArray = [];
			var dropBoxDirPath= opts.dropboxDir;	
		
			c.autnresponse.responsedata.actions.action.output.record.forEach(function(t) 
			{	
				if(t.trackname["$"] === "StoreAllTrackFaces.Proxy")
				{
					if(t.ProxyAndUUIDData.id["$"] === recordID){
					var pathface = t.ProxyAndUUIDData.proxy["@path"]; 
						logger.message('Display allFaces', 'Face Path: '+pathface);
						pathArray.push(pathface);
					}
				}
				/*
			    if(t.trackname["$"] === "StoreUnRecognisedCroppedFace.Proxy")
				{
					logger.message('checkProgress TrackName For Cropface ='+t.trackname);
					pathCropFace = t.ProxyAndUUIDData.proxy["@path"]; 
					logger.message('Get Crop Faces Path  = '+pathCropFace);
				}
				
				if(t.trackname["$"] === "StoreUnRecognisedFullFrame.Proxy")
				{
					logger.message('checkProgress TrackName For FullFrames ='+t.trackname);
					pathFullFrames = t.ProxyAndUUIDData.proxy["@path"]; 
					logger.message('Get Crop Faces Path  = '+pathFullFrames);
				}*/
			});	
			//logger.message('checkProgress Array length = '+pathArray.length);
			defer.resolve({"Path":pathArray,"dropBoxPath":dropBoxDirPath});
    });
}

function getPathMeataData(Token, recordID, defer){
	var action = "queueinfo",
        options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&queueAction=getStatus&queueName=process&token="+Token+"&ResponseFormat=JSON"
        };
    getAutnresponse(action, options, defer, function(c) {
            //var pathCropFace;
            //var pathFullFrames;
			
			var pathCropFace = [];
			var pathFullFrame = [];
			var recordIdArray = [];
			var dropBoxDirPath= opts.dropboxDir;	
        
            c.autnresponse.responsedata.actions.action.output.record.forEach(function(t) 
            {   
                if(t.trackname["$"] === "StoreUnRecognisedCroppedFace.Proxy")
                {
					if(t.ProxyAndUUIDData.id["$"] === recordID){
					logger.message('checkProgress TrackName For Cropface ='+t.trackname);
                    //pathCropFace = t.ProxyAndUUIDData.proxy["@path"]; 
					pathCropFace.push(t.ProxyAndUUIDData.proxy["@path"])
					recordIdArray.push(recordID);
                    //logger.message('Get Crop Faces Path  = '+pathCropFace);	
					}
                }
                
                if(t.trackname["$"] === "StoreUnRecognisedFullFrame.Proxy")
                {
					if(t.ProxyAndUUIDData.id["$"] === recordID){
                    logger.message('checkProgress TrackName For FullFrames ='+t.trackname);
                    //pathFullFrames = t.ProxyAndUUIDData.proxy["@path"]; 
					pathFullFrame.push(t.ProxyAndUUIDData.proxy["@path"]);
                    //logger.message('Get Crop Faces Path  = '+pathFullFrames);
					}
                }
            }); 
            
			c.autnresponse.responsedata.actions.action.output.record.forEach(function(t) 
            {   
                if(t.trackname["$"] === "StoreRecognisedCroppedFace.Proxy")
                {
					var pathMatchedCrop = t.ProxyAndUUIDData.proxy["@path"];
					var checkMatchedCrop = pathMatchedCrop.includes(recordID);
					if(checkMatchedCrop){
						pathCropFace.push(pathMatchedCrop);
						recordIdArray.push(t.ProxyAndUUIDData.id["$"]);
					}
                }
				
                if(t.trackname["$"] === "StoreRecognisedFullFrame.Proxy")
                {
					var pathMatchedFullFrame = t.ProxyAndUUIDData.proxy["@path"];
					var checkFullFrame = pathMatchedFullFrame.includes(recordID);
					if(checkFullFrame){
						pathFullFrame.push(pathMatchedFullFrame);
					}
                }
            }); 
            defer.resolve({"cropPath": pathCropFace, "fullFrame": pathFullFrame, "recordId": recordIdArray,"dropBoxPath":dropBoxDirPath});
    });
}

//for displaying all crop faces
function getPathFaces(Token, defer){
	
	var action = "queueinfo",
        options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&queueAction=getStatus&queueName=process&token="+Token+"&ResponseFormat=JSON"
        };
	getAutnresponse(action, options, defer, function(c) {
			var pathArray = [];
			var recordID = [];
			var dropBoxDirPath= opts.dropboxDir;
			if(c.autnresponse.responsedata.actions.action.output!==undefined){
			c.autnresponse.responsedata.actions.action.output.record.forEach(function(t) 
			{	
				if(t.trackname["$"] === "StoreUnRecognisedCroppedFace.Proxy")
				{
					var pathface = t.ProxyAndUUIDData.proxy["@path"];
					logger.message('Display Faces', 'Face Path: '+pathface);
					var record_id = t.ProxyAndUUIDData.id["$"];
					pathArray.push(pathface);
					recordID.push(record_id);
				}
				/*
			    if(t.trackname["$"] === "StoreUnRecognisedCroppedFace.Proxy")
				{
					logger.message('checkProgress TrackName For Cropface ='+t.trackname);
					pathCropFace = t.ProxyAndUUIDData.proxy["@path"]; 
					logger.message('Get Crop Faces Path  = '+pathCropFace);
				}
				
				if(t.trackname["$"] === "StoreUnRecognisedFullFrame.Proxy")
				{
					logger.message('checkProgress TrackName For FullFrames ='+t.trackname);
					pathFullFrames = t.ProxyAndUUIDData.proxy["@path"]; 
					logger.message('Get Crop Faces Path  = '+pathFullFrames);
				}*/
				
			});	
			
			}
			else{
				logger.message('Cropped Face','No face detected');
			}
			//logger.message('checkProgress Array length = '+pathArray.length);
			defer.resolve({"Path":pathArray, "recordId":recordID, "dropBoxPath":dropBoxDirPath});
    });
}
function getExtractedFileName(defer){
	var subFilesArray = [];
	var subFilesSize = [];
	var fileIdArray = [];
	const fs = require('fs');
	
	var files = fs.readdirSync(opts.subfileDir);
	
	files.forEach(file => {
		var fileExtension = path.extname(file);
		if(fileExtension=='.mp4' || fileExtension=='.avi' || fileExtension=='.mov' || fileExtension=='.mts'){
			fileIdArray.push(uuidV4());
			subFilesArray.push(file);
			
			var stats = fs.statSync(opts.subfileDir+'/'+file);
			var size = stats["size"];
			// convert it to humanly readable format.
			var i = Math.floor( Math.log(size) / Math.log(1024) );
			var fileSize = ( size / Math.pow(1024, i) ).toFixed(2) * 1 + ' ' + ['B', 'KB', 'MB', 'GB', 'TB'][i];
			subFilesSize.push(fileSize);
			
			var source = fs.createReadStream(opts.subfileDir+'/'+file);
			var dest = fs.createWriteStream(opts.videoDir+'/'+file);
			source.pipe(dest);
			
		}
		else{
			fs.unlinkSync(opts.subfileDir+'/'+file);
		}
	})
	defer.resolve({"subFiles":subFilesArray, "subFileID":fileIdArray, "filesSize":subFilesSize});
}

function extractSubfile(defer, containerName){
	
	var createSubfileDir = opts.subfileDir;

	 if (!fs.existsSync(createSubfileDir)){
		fs.mkdirSync(createSubfileDir);
	 }
	
	 try{
		 var inputFile=' "'+opts.videoDir+'/'+containerName+'"';
		 var outputFile=' "'+opts.subfileDir+'"'
		 var keyviewBin = '"'+opts.keyViewBinDir+'"'
		 var commandString = 'java -Djava.library.path=' +keyviewBin+ ' ExtractFilter -ext-nodir ' +keyviewBin;
		 var finalString = commandString + inputFile + outputFile;
		 
		 const exec = require('child_process').exec;
		 //const child = exec('java -Djava.library.path="C:\HewlettPackardEnterprise\KeyviewFilterSDK-11.2.0\WINDOWS_X86_64\bin" ExtractFilter "C:\HewlettPackardEnterprise\KeyviewFilterSDK-11.2.0\WINDOWS_X86_64\bin" "C:\HewlettPackardEnterprise\ListVideos.zip" "C:\HewlettPackardEnterprise\ListVideos1"',
		 const child = exec(finalString,
		(error, stdout, stderr) => {
			var subFiles = fs.readdirSync(opts.subfileDir);
			var countSubfile = 0;
			subFiles.forEach(subFile => {
				var fileExtension = path.extname(subFile);
				if(fileExtension=='.mp4' || fileExtension=='.avi' || fileExtension=='.mov' || fileExtension=='.mts'){
				countSubfile++;
				}
			})
			if(!error){
				logger.message('Subfile Extraction Success',`${stdout}`);
				defer.resolve({"SubfileCount":countSubfile,"ContainerName":containerName});
			}
			if (error !== null) {
				logger.message('Subfile Extraction Error',error.message);
				defer.resolve({"Error":"Subfile Extraction UnSuccessful","Status":0});
			}
			
		})
	 }
	 
	 catch(error){
		 logger.message('Subfile Extraction Error',error.message);
		 defer.resolve({"Error":"Subfile Extraction UnSuccessful","Status":0});
		 //res.end('{"error" : "Subfile Extraction UnSuccessfull", "status" : 0}');
	 }
	
}

function fileExplorer(defer, folderPath){
	try{
		var exec = require('child_process').exec;
		var cmdString = 'start '+folderPath;
		
		logger.message('Test USER PROFILE: '+ process.env.USERPROFILE);

		exec(cmdString,{env: process.env},function(err,stdout,stderr) {
			if (err) {
				logger.message('OpenFileExplorer','Child process exited with error code'+err.code);
				return
			}
			logger.message('OpenFileExplorer',folderPath);
			defer.resolve({"Status":1});
		});
	}
	catch(error){
		logger.message('File explorer Error',error.message);
		defer.resolve({"Status":0});
	}
}

function exportFaces(defer, facePath, folderName, checkWindow){
	var targetPath=opts.exportDir;
	var createExportDir=targetPath+'/'+folderName;
	fs.mkdirSync(createExportDir);
	
	var countExport=0;
	console.log(facePath);
	facePath.forEach( item => {
		if(item.lastIndexOf('/')!=(item.length-1)){
			faceNameTest=item.substr(item.lastIndexOf('/')+1,item.length-1);
			fs.createReadStream(item).pipe(fs.createWriteStream(path.join(createExportDir ,faceNameTest)));
			
			// for exporting text file default, if user export vdo, fullFrame, crop.
			if(checkWindow==1 || checkWindow==2){
				if(faceNameTest.indexOf('matched') !== -1){
					metaFileName = faceNameTest.substr(0,53)+'.txt';
				}
				else{
					metaFileName = faceNameTest.substr(0,45)+'.txt';
				}
				metaFilePath = item.substr(0,item.lastIndexOf('/'))+'/'+metaFileName;
				fs.createReadStream(metaFilePath).pipe(fs.createWriteStream(path.join(createExportDir ,metaFileName)));
			}
			
		}
		else{
			sourceFolder = item + 'allFaces';
			destFolder = createExportDir + '/allFaces';
			
			if (!fs.existsSync(destFolder)){
				fs.mkdirSync(destFolder);
			}
			copydir.sync(sourceFolder,destFolder);
		}		
		countExport++;
		})
		if(countExport>0){
			logger.message('Exported Successfully','Export Directory: '+createExportDir);
			defer.resolve({"exportDir":createExportDir});
		}
		else{
			logger.message('Export Error','Error while Exporting');
		}
}
function getMD5_Hash(videoName){
	var crypto = require("crypto");
	var databaseNamehashCode = crypto.createHash('md5').update(videoName).digest("hex");
	return databaseNamehashCode
}

function createVideoDatabases(listDatabases,defer){
	listDatabases.forEach( databaseName => {
		var action = "createFaceDatabase";
		var hashCode = getMD5_Hash(databaseName);
		logger.message(databaseName, hashCode);
		manageDatabase(action,hashCode, function(){
		});
	});
	logger.message("Create Databases","Create database for each video");
	defer.resolve({"Status":1});
}

function removeAllDatabases(defer){
	var action = "ListFaceDatabases";
	var options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action=ListFaceDatabases&ResponseFormat=JSON"
        };
		
	setupDbGetAutnresponse(action, options, function(c){
		var databases = c.autnresponse.responsedata.database;
		var action = "removeFaceDatabase";
		if(databases==undefined){
			//do nothing
			logger.message("Remove database","No database to remove")
		}
		else{
			if(databases.length==undefined){
				var databaseName = databases.database["$"];
				/*var options = {
					host: opts.mediaserver.host,
					port: opts.mediaserver.port,
					path: "/action="+action+"&database="+databaseName+"&ResponseFormat=JSON&Synchronous=True"
				};
				setupDbGetAutnresponse(action, options, function(t){
					logger.message("Database Name",databaseName);
				});*/
				manageDatabase(action, databaseName, function(){
					
				});
			}
			else{
				for(var i=0; i<databases.length; i++){
					var databaseName = databases[i].database["$"];
					/*var options = {
						host: opts.mediaserver.host,
						port: opts.mediaserver.port,
						path: "/action="+action+"&database="+databaseName+"&ResponseFormat=JSON&Synchronous=True"
					};
					setupDbGetAutnresponse(action, options, function(t){
						logger.message("Database Name",databaseName);
					});*/
					manageDatabase(action, databaseName, function(){
					
					});
				}
			}
			logger.message("Remove Databases","Remove all databases");
		}	
	});
	defer.resolve({"Status":1});
}
function checkProgress4UI(Token, currentVideoTotalFrames, defer) {

    var action = "queueinfo",
        options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&queueaction=getstatus&queuename=process&token="+Token+"&ResponseFormat=JSON"
        };

    getAutnresponse(action, options, defer, function(c) {
        var currentStatus = c.autnresponse.responsedata.actions.action.status["$"];

        if('Finished' === currentStatus) {
			//clearDatabase();
            defer.resolve({"currentStatus":"Finished","recordCount":0});
        }
        else if ('Queued' === currentStatus) {
            defer.resolve({"currentStatus":"Queued","recordCount":0});
        }
        else if('Processing' === currentStatus) {
            var action = "getstatus",
                options = {
                    host: opts.mediaserver.host,
                    port: opts.mediaserver.port,
                    path: "/action="+action+"&showtracksstatistics&ResponseFormat=JSON"
                };

            getAutnresponse(action, options, defer, function(c) {
                var sessions = c.autnresponse.responsedata.statistics.sessionStatistics,
                    recordCount = 0,
                    trackedFaceCount = 0; // TODO - count discovered faces

                if(false === Array.isArray(sessions)) {
                    sessions = [sessions];
                }

                // logger.message('checkProgress', JSON.stringify(sessions, null, 2));
                try {
                    sessions.forEach(function(session) {
                        if(session['@token'] === Token) {

                            // logger.message('checkProgress', JSON.stringify(session.tracks, null, 2));
                            session.tracks.track.forEach(function(track) {
                                if(track['@name'] === "Image_1") {
                                    recordCount = +track['@recordCount'];
                                }
                            });
                        }
                    });
					
					//finished condition
					/*if(100<=(100*recordCount/currentVideoTotalFrames).toFixed(0)){
						clearDatabase();
					}*/
                } catch(e) {
                    logger.message('checkProgress', e);
                }
                defer.resolve({"currentStatus":"Processing","recordCount":recordCount});
                //logger.message('checkProgress', 'Ingested '+recordCount+" / "+currentVideoTotalFrames+" frames ["+(100*recordCount/currentVideoTotalFrames).toFixed(0).toString()+"%]");
            });
        } else {
            logger.message('checkProgress', 'process status: '+currentStatus+". Check Media Server logs.");
            defer.resolve({"currentStatus":currentStatus,"recordCount":0});
        }
    });
}


module.exports = {startAnalysis4UI:startAnalysis4UI, checkProgress4UI:checkProgress4UI, stopAnalysis4UI:stopAnalysis4UI, priority:priority, getVideoDuration:getVideoDuration, getPathFaces:getPathFaces, getExtractedFileName:getExtractedFileName, getPathMeataData:getPathMeataData, getPathAllFaces:getPathAllFaces, extractSubfile:extractSubfile, getVideoSize:getVideoSize, fileExplorer:fileExplorer, exportFaces:exportFaces, checkConfigParams:checkConfigParams, verifyUploadedConfig:verifyUploadedConfig, createVideoDatabases:createVideoDatabases, removeAllDatabases:removeAllDatabases};

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  m a i n
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

// application initialization
logger.init(config.log);
xmlClean();
validateOptions();
logger.message("Main",'Application started.');
/*
setupDb("removeFaceDatabase", function() {
    setupDb("createFaceDatabase", function() {
        logger.message("Main",'Application started.');
    });
});*/

/*
setGracefulExit();
logger.init(config.log);
xmlClean();
validateOptions();

setupDb("removeFaceDatabase", function() {
    setupDb("createFaceDatabase", function() {
        
        getVideos(function(videos) {
            startAnalysis(videos);
        });
    });
});
*/