/*                                                              
 *  Shared methods for logging.
 *  
 */                                                             

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  d e p e n d e n c i e s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var fs = require('fs'),
    path = require('path'),
    logrotate = require("logrotator");

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  v a r i a b l e s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
var rotator = logrotate.rotator,
	config = {};

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  i n t e r n a l
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
function archiveLogSuffix(idx) {
    /*
     * <filename>.log.2016-06-16_20.13.34.gz
     */
    var d = new Date(),
	/*
        dateString = d.toISOString() // '2016-08-01T15:08:43.527Z'
            .split('.')[0]
            .split('T').join('_')
            .split(':').join('.');*/
		dateString = d.toLocaleString() // '10/28/2017, 3:07:31 PM'
			.split(', ').join('_')
			.split(' ').join('-');
    //return config.file + '.' + dateString + '.zip';
    return dateString;
}

function init(_config) {

	config = JSON.parse(JSON.stringify(_config));

    // create log directory
    try {
        fs.mkdirSync(path.parse(config.file).dir);
    } catch(e) {
        try {
            fs.statSync(path.parse(config.file).dir);
        } catch(e) {
            console.log('Failed to create log directory with error: ' + e);
        }
    }

    // create log file if doesn't exist
    try {
        fs.statSync(config.file);
    } catch(e) {
        try {
            fs.writeFileSync(config.file, "");
        } catch (e) {
            console.log('Failed to create log file with error: ' + e);
        }
    }

    // configure log rotation
    rotator.register(config.file, {
        schedule: config.rotateIntervalSec.toString() + 's',
        size: config.maxSizeMB.toString() + 'm',
        compress: true,
        count: 12,
        format: archiveLogSuffix
    });

    rotator.on('error', function(e) {
        console.log('Failed to rotate log file with error: ' + e);
    });

    rotator.on('rotate', function(file) {
        console.log('Log file ' + file + ' was rotated.');
    });

}

function message(identifier, msg) {
    var d = new Date();
    //msg = d.toISOString().split('T').join(' ').split('Z').join('') +
	msg = d.toLocaleString().split(', ').join('_').split(' ').join('.'+d.getMilliseconds()+' ').split('_').join(', ') +
        " [" + identifier + "] " + msg + "\r\n";

    try {
        console.log(msg);
        fs.appendFileSync(config.file, msg);
    } catch(e) {
        console.log('Failed to write to log file with error: ' + e);
    }
}

//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//  f u n c t i o n s
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
module.exports = {
	init: init,
    message: message
};
