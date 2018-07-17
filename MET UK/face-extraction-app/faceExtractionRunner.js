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
    logger = require('./modules/logger.js'),
    opts = require('./options.js');

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
var currentToken = "",
    currentVideoTotalFrames = 1,
    requestedStop = false;

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
    var params = ['faceDir', 'videoDir'],
        defaults = ['faces', 'video'];
    
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

function getAutnresponse(action, options, callback) {

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
        
    getAutnresponse(action, options, callback);
}

function setupDb(action, callback) {
    
    var options = {
            host: opts.mediaserver.host,
            port: opts.mediaserver.port,
            path: "/action="+action+"&database="+opts.mediaserver.db+"&ResponseFormat=JSON&Synchronous=True"
        };
        
    getAutnresponse(action, options, callback);
}

function getVideos(callback) {
    
    var videos = [],
        videoPath = "";
        
    if("" !== opts.videoPath) {
        videos.push({
            videoPath: opts.videoPath
        });
    
    } else {
        var files = fs.readdirSync(opts.videoDir);
        for(var i=0; i<files.length; i++) {
            if(-1 < opts.videoTypes.indexOf(path.extname(files[i]).toLowerCase())) {
                videos.push({
                    videoPath: opts.videoDir+path.sep+files[i]
                });
            }
        }
    }
    
    logger.message('getVideos', JSON.stringify(videos, null, 2));
    callback(videos);
}

function startAnalysis(videos) {
    
    if(requestedStop) {
        logger.message('startAnalysis', 'Process stopped.');
    
    } else if (0 === videos.length) {
        logger.message('startAnalysis', 'No videos to process.');
		doGracefulExit();
    
    } else {
        logger.message('startAnalysis', 'Processing '+videos[0].videoPath);
        
		//-----------------------------------
		// SESSION CONFIG TEMPLATE
        var cfg = fs.readFileSync(opts.cfgFile, "utf8");
		
		//  I N G E S T
		if (false === opts.realTime) {
            cfg = cfg.split("^REALTIME^").join("");
        } else {
            cfg = cfg.split("^REALTIME^").join("//");
        }

		//  A N A L Y S I S
        cfg = cfg
			.split("^SAMPLEINTERVAL^").join(opts.sampleInterval.toString())
			.split("^NUMPARALLEL^").join(opts.numParallel.toString())
            .split("^MINSIZE^").join(opts.minSize.toString())
            .split("^COLORANALYSIS^").join(opts.colorAnalysis)
            .split("^DETECTTITLED^").join(opts.detectTilted)
            .split("^FACEDIRECTION^").join(opts.faceDirection)
            .split("^ORIENTATION^").join(opts.orientation)
            .split("^FACEDB^").join(opts.mediaserver.db)
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
			.join(opts.faceDir+"/"+path.basename(videos[0].videoPath).split('.')[0]);
			
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

        var options = {
			host: opts.videoActionsServer.host,
			port: opts.videoActionsServer.port,
			path: "/videoPath="+videos[0].videoPath
		};
        
        http.get(options, function(response) {
            var body = '';
            response.on("data", function(data) {
                body += data;
            });
            response.on("end", function(data) {

				var nFrames = parseInt(body);
				
                logger.message('startAnalysis', 'Video frames to process: '+nFrames);
                currentVideoTotalFrames = +nFrames;
                
				var bsf = new Buffer(cfg).toString('base64'),
                    action = "process",
                    options = {
                        host: opts.mediaserver.host,
                        port: opts.mediaserver.port,
                        path: "/action="+action+"&source="+videos[0].videoPath+"&config="+bsf+"&ResponseFormat=JSON"
                    };
                
                //console.log(bsf);
				
                videos.shift();
                getAutnresponse(action, options, function(d) {
                    currentToken = d.autnresponse.responsedata.token["$"];
                    checkProgress(videos);
                });

            });
            
        }).on('error', function(e) {
            logger.message('startAnalysis', 'Cannot connect to video actions server.');
        });
    }
}

function checkProgress(videos) {
    
    setTimeout(function(){ 
        var action = "queueinfo",
            options = {
                host: opts.mediaserver.host,
                port: opts.mediaserver.port,
                path: "/action="+action+"&queueaction=getstatus&queuename=process&token="+currentToken+"&ResponseFormat=JSON"
        };
        
        getAutnresponse(action, options, function(c) {
            var currentStatus = c.autnresponse.responsedata.actions.action.status["$"];
            
            if('Finished' === currentStatus) {
                startAnalysis(videos);
            
            } else if('Processing' === currentStatus || 'Queued' === currentStatus) {
                checkProgress(videos);
                
                var action = "getstatus",
                    options = {
                        host: opts.mediaserver.host,
                        port: opts.mediaserver.port,
                        path: "/action="+action+"&showtracksstatistics&ResponseFormat=JSON"
                    };
                
                getAutnresponse(action, options, function(c) {
                    var sessions = c.autnresponse.responsedata.statistics.sessionStatistics,
                        recordCount = 0,
						trackedFaceCount = 0; // TODO - count discovered faces
                    
                    if(false === Array.isArray(sessions)) {
                        sessions = [sessions];
                    }
                    
                    // logger.message('checkProgress', JSON.stringify(sessions, null, 2));
                    try {
						sessions.forEach(function(session) {
							if(session['@token'] === currentToken) {
								
								// logger.message('checkProgress', JSON.stringify(session.tracks, null, 2));
								session.tracks.track.forEach(function(track) {
									if(track['@name'] === "Image_1") {
										recordCount = +track['@recordCount'];
									}
								});
							}
						});
					} catch(e) {
						logger.message('checkProgress', e);
					}
                    
                    logger.message('checkProgress', 'Ingested '+recordCount+" / "+currentVideoTotalFrames+" frames ["+(100*recordCount/currentVideoTotalFrames).toFixed(0).toString()+"%]");
                });
            } else {
                logger.message('checkProgress', 'process status: '+currentStatus+". Check Media Server logs.");
            }
        }); 
    
    }, config.checkProgressIntervalMSec);    
}

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  m a i n
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
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
