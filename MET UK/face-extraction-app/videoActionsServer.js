/*                                                              
 *  HPE Video Actions server.
 *  
 */                                                             

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  d e p e n d e n c i e s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var http = require('http'),
    url = require('url'),
    path = require('path'),
    childProcess = require('child_process'),
    logger = require('./modules/logger.js'),
    opts = require('./options.js');

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  c o n f i g u r a t i o n
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var serverName = "videoActionsServer",
	config = {
		files: {
			ffmpeg: path.resolve("ffmpeg.exe"),
			ffprobe: path.resolve("ffprobe.exe")
		},
		log: {
			file: path.resolve("./logs/"+serverName+".log"),
			maxSizeMB: 3,
			rotateIntervalSec: 60*5
		},
		queueDelay: { // milliseconds
			short: 200,
			long:  5000
		}
	};

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  v a r i a b l e s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var jobs = {
    createClip: []
};

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  f u n c t i o n s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
function getTotalFrames(sourceVideoPath, callback) {

    var totalFrames = -1,
		args = [
            "-v", "quiet",
            "-print_format", "json", "-show_streams", 
            sourceVideoPath
        ];
    /*
    childProcess.exec('C:/Windows/System32/whoami', function(error, stdout, stderr) {
        if (error !== null) {
            logger.message('getTotalFrames', 'whoami error: ' + error);
		} else {
            logger.message('getTotalFrames', 'whoami stdout: ' + stdout);
		}		
	});
*/	
    childProcess.exec(`${config.files.ffprobe} -v quiet -print_format json -show_streams "${sourceVideoPath}"`, function(error, stdout, stderr) {
        if (error !== null) {
            logger.message('getTotalFrames', 'exec error: ' + error);
            callback(1, totalFrames);

        } else {
            JSON.parse(stdout).streams.forEach(function(stream) {
                if(stream.codec_type === "video") {
					
					logger.message('getTotalFrames', JSON.stringify(stream, null, 2));
					
					if(typeof stream["nb_frames"] !== 'undefined') {
						totalFrames = stream["nb_frames"];
					
					} else {
						if(typeof stream["duration"] !== 'undefined' && typeof stream["avg_frame_rate"] !== 'undefined') {
							var duration = parseFloat(stream["duration"]),
								fps = parseFloat(stream["avg_frame_rate"].split("/")[0]);
								
							logger.message('getTotalFrames', 'duration: ' + duration);
							logger.message('getTotalFrames', 'fps: ' + fps);
								
							totalFrames = parseInt(duration * fps + 0.5);
						}
					}
				}
			});
			
            logger.message('getTotalFrames', 'Total frames: ' + totalFrames);
            callback(0, totalFrames);
        }
    });
}

function createClipLauncher(delay) {
    setTimeout(function() { createClipWorker(); }, delay);
}

function createClipWorker() {
    
    if(jobs.createClip.length > 0) {
        var job = jobs.createClip[0],
            args = [
                "-i", job.sourceVideoPath,
                "-ss", job.startSec,
                "-t", job.duration,
                job.clipPath
            ];
        
        // logger.message('createClipWorker', JSON.stringify(args, null, 2));
        
        childProcess.execFile(config.files.ffmpeg, args, function(error, stdout, stderr) {
            
			// logger.message('createClipWorker', 'stdout: ' + stdout);
            // logger.message('createClipWorker', 'stderr: ' + stderr);
            
			if (error !== null) {
                logger.message('createClipWorker', 'exec error: ' + error);
            } else {
				logger.message('createClipWorker', 'video clip created: '+args[6]);
			}
            
            jobs.createClip.shift();
            createClipLauncher(config.queueDelay.short);
        });
    } else {
        createClipLauncher(config.queueDelay.long);
    }
}

function processGetData(data, processCallback) {
    getTotalFrames(data.videoPath, processCallback);
}

function processPostData(data, processCallback) {
    
    // logger.message('processPostData', JSON.stringify(data, null, 2));
    
    try {
        jobs[data.action].push(data.job);
        processCallback(0);
    
    } catch(e) {
        processCallback(1);
    }
}

function parseUrlPathname(pathname) {
    var opts = {};
	
    // added begin by huarui, for handing unescaped char
	pathname = decodeURI(pathname);
	// added end
    pathname = pathname.substr(1, pathname.length);
    
    pathname.split('&').forEach(function(o) {
        var kvp = o.split('=');
        opts[kvp[0]] = kvp[1];
    });
    return opts;
}

function onRequest(request, response) {
	/*
	 * The server process handler.
	 */
    
    if (request.method == 'GET') {
        
        logger.message('onRequest', 'GET request received.');
        
        var data = parseUrlPathname(url.parse(request.url).pathname);
         logger.message('onRequest', JSON.stringify(data, null, 2));
        
        processGetData(data, function(err, totalFrames) {
            if(err) { 
                logger.message('onRequest', 'Processing failed.');
            } else {
                logger.message('onRequest', 'Processing complete.');
            }
            
			// console.log(totalFrames);
			
			response.writeHead(200, {'Content-Type': 'text/plain'});
            response.write(totalFrames.toString());
            response.end('\n');
        });
        
    } else if (request.method == 'POST') {
        
        logger.message('onRequest', 'POST request received.');
        
        var body = '';
        request.on('data', function(data) {
            body += data;
            if (body.length > 1e6)
                request.connection.destroy();
        });
        
        request.on('end', function() {
            
            processPostData(JSON.parse(body), function(err) {
                if(err) { 
                    logger.message('onRequest', 'Processing failed.');
                } else {
                    logger.message('onRequest', 'Processing complete.');
                }
                
                response.writeHead(200, {'Content-Type': 'text/plain'});
                response.end('\n');
            });
        });
    } else {
        response.writeHead(200, {'Content-Type': 'text/plain'});
        response.end('\n');
    }
}

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  m a i n
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
logger.init(config.log);

createClipLauncher(config.queueDelay.long);

var server = http.createServer(onRequest).listen(opts.videoActionsServer.port);
logger.message(serverName, serverName+" has started on port " + opts.videoActionsServer.port.toString() + ".");
