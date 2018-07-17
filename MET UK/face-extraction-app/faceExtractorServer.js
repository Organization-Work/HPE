/*                                                              
 *  HPE Face Extractor server.
 *  
 */                                                             

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  d e p e n d e n c i e s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var http = require('http'),
    url = require('url'),
    fs = require('fs'),
    path = require('path'),
	request = require('request'),
    xml2js = require('xml2js'),
    logger = require('./modules/logger.js'),
    opts = require('./options.js');

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  c o n f i g u r a t i o n
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var serverName = "faceExtractorServer",
	config = {
		log: {
			file: path.resolve("./logs/"+serverName+".log"),
			maxSizeMB: 3,
			rotateIntervalSec: 60*5
		}
	};

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  f u n c t i o n s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
function doNothing(d) {
    return 0;
}

function getMD5_Hash(videoName){
	var crypto = require("crypto");
	var databaseNamehashCode = crypto.createHash('md5').update(videoName).digest("hex");
	return databaseNamehashCode
}

function databaseName(imagePath){
	var faceDir = opts.faceDir;
		faceDir = faceDir.replace(/\//g, "\\")+"\\";
    var databaseName = imagePath.replace(faceDir,"");
		databaseName=databaseName.split("\\")[0];
	var hashCode = getMD5_Hash(databaseName);
	/*logger.message("At the faceExtractorServer part: ",databaseName+"  "+hashCode);
		databaseName=databaseName.replace(/[^a-zA-Z0-9]/g, "");
		databaseName=databaseName+"_db";*/
	return hashCode;
}



function getAutnresponse(action, options, callback) {

    // logger.message(config.files.log, JSON.stringify(options, null, 2));

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

function trainNewFace(identifier, imagePath, callback) {
	var database = databaseName(imagePath);
    var action = "trainFace",
		options = {
			host: opts.mediaserver.host,
			port: opts.mediaserver.port,
			path: "/action="+action+"&database="+database+
				"&imagepath="+imagePath+"&identifier="+identifier+"+&imagelabels="+identifier+"&ResponseFormat=JSON&Synchronous=False"
		};

	getAutnresponse(action, options, callback);
}

function addExtraFace(identifier, uuid, imagePath, callback) {
	var database = databaseName(imagePath);
    var action = "AddFaceImages",
		options = {
			host: opts.mediaserver.host,
			port: opts.mediaserver.port,
			path: "/action="+action+"&database="+database+
				"&imagepath="+imagePath+"&identifier="+identifier+"+&imagelabels="+uuid+"&ResponseFormat=JSON&Synchronous=False"
		};

	getAutnresponse(action, options, callback);
}

function buildFaces(database,callback) {

    var action = "BuildAllFaces",
        options = {
        host: opts.mediaserver.host,
        port: opts.mediaserver.port,
        path: "/action="+action+"&database="+database+
            "&ResponseFormat=JSON&Synchronous=False"
    };

    getAutnresponse(action, options, callback);
}

function syncFaces() {

    var action = "SyncFaces",
        options = {
        host: opts.mediaserver.host,
        port: opts.mediaserver.port,
        path: "/action="+action+
            "&ResponseFormat=JSON&Synchronous=False"
    };

    getAutnresponse(action, options, doNothing);
}

function parseUrlAction(pathname) {
    return pathname.substr(1, pathname.length);
}

function onRequest(req, res) {
    
    if (req.method == 'POST') {
        
        var action = parseUrlAction(url.parse(req.url).pathname);
		logger.message('onRequest', action+' request received from Media Server');
        
        var body = '';
        req.on('data', function(data) {
            body += data;
            if (body.length > 1e6)
                req.connection.destroy();
        });
        
        req.on('end', function() {
                
            xml2js.parseString(body, function(err, out) {
                
                processData(action, out, function(err) {
                    if(err) { 
                        logger.message('onRequest', 'Processing failed.');
                    } else {
                        logger.message('onRequest', 'Processing complete.');
                    }
                    
                    res.writeHead(200, {'Content-Type': 'text/plain'});
                    res.end('\n');
                });
            });
        });
    } else {
        res.writeHead(200, {'Content-Type': 'text/plain'});
        res.end('\n');
    }
}

function processData(action, data, processCallback) {
	if(action === "trainNewFace") {

		var proxyData = data.output.results[0].track[0].record[0].ProxyAndUUIDData[0],
			cropPath = proxyData.proxy[0]['$'].path,
			faceId = proxyData.id[0];
		trainNewFace(faceId, cropPath, function() {
			syncFaces();
		});
	
	} else if (action === "addExtraFace") {
		
		var proxyData = data.output.results[0].track[0].record[0].ProxyAndUUIDData[0],
			cropPath = proxyData.proxy[0]['$'].path,
			faceId = proxyData.id[0],
			_parts = cropPath.split('\\'),
			matchId = _parts[_parts.length-2];
		
		addExtraFace(matchId, faceId, cropPath, function() {
			var database = databaseName(cropPath);
            buildFaces(database,function() {
				syncFaces();
			});
        });

	} else if (action === "createClip") {
		
		var recordData = data.output.results[0].track[0].record[0],
			cropPath = recordData.ProxyAndUUIDData[0].proxy[0]['$'].path,
			clipPath = cropPath.split("_crop")[0]+opts.createClipFormat,
			startSec = recordData.timestamp[0].startTime[0]["_"]/1000000.0,
			duration = recordData.timestamp[0].duration[0]["_"]/1000000.0,
			sourceVideoPath = data.output.metadata[0].session[0].source[0];

		startSec = startSec - opts.clipBuffer/1000.0;
		duration = duration + opts.clipBuffer/500.0;
		if(startSec < 0) {
			duration = duration + startSec;
			startSec = 0;
		}

		try {
			request({
				method: 'POST',
				url: "http://"+opts.videoActionsServer.host+":"+opts.videoActionsServer.port+"/", 
				json: { 
					action: "createClip",
					job: {
						sourceVideoPath : sourceVideoPath,
						startSec : startSec,
						duration : duration,
						clipPath : clipPath
					}
				}
			});
			
		} catch(e) {
			logger.message('createClip', e);
			logger.message('createClip', "Could not communicate with video actions server.");
		}
        		
	} else if(action == "moveDataCrops") {
		
		// console.log(JSON.stringify(data.output.metadata, null, 2));
		
		var important = data.output.metadata[0].preXml[0].importantRecord[0],
			uuid = important.FaceRecognitionResult[0].id[0],
			identifier = important.FaceRecognitionResult[0].identity[0].identifier[0];
			
		data.output.results[0].track.forEach(function(t) {
			if(t["$"].name === "StoreAllTrackFaces.Proxy") {
				
				var sourcePath = '',
					targetPath = '';
								
				t.record.forEach(function(r) {
					if(uuid === r.ProxyAndUUIDData[0].id[0]) {
						
						sourcePath = r.ProxyAndUUIDData[0].proxy[0]['$'].path;
						targetPath = sourcePath.split(uuid).join(identifier).split('allFaces').join('allFaces_matched_'+uuid);
						
						try {
							//logger.message("moveDataCrops", '> making '+path.dirname(targetPath));
							fs.mkdirSync(path.dirname(targetPath));
							
						} catch(e) {
							// logger.message("moveDataCrops", e);
						}

						//logger.message("moveDataCrops", '> moving from '+sourcePath);
						fs.renameSync(sourcePath, targetPath);

					}
				});
				
				var sourceDir = path.dirname(sourcePath);
				if(fs.readdirSync(sourceDir).length === 0) {
					fs.rmdirSync(sourceDir);
					
					var parentDir = sourceDir.split('\\allFaces')[0];
					//console.log(parentDir);
					if(fs.readdirSync(parentDir).length === 0) {
						
						fs.rmdirSync(parentDir);
					}
				}
			}
		});
	}
	
    processCallback(0);
}


//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  m a i n
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
logger.init(config.log);

var server = http.createServer(onRequest).listen(opts.faceExtractorServer.port);
logger.message(serverName, serverName+" has started on port " + opts.faceExtractorServer.port.toString() + ".");
