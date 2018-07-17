var express = require('express');
var session = require('express-session');
var cookieSession = require('cookie-session');
var path = require('path');
var fs = require('fs');
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var jquery = require('jquery');

var routes = require('./routes/index');
var opts = require('./module/options.js');

var app = express();

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');

app.use(favicon(__dirname + '/app/images/favicon.ico'));
//app.use(favicon());
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(require('less-middleware')(path.join(__dirname, 'app')));
app.use(express.static(path.join(__dirname, 'app')));
app.use('jquery', express.static(__dirname + '/node_modules/jquery/dist/'));


//app.use('/', express.static(__dirname + '/public'));

//app.use('/', express.static('C:/HewlettPackardEnterprise/dropbox'));
if(opts.dropboxDir){
app.use('/', express.static(opts.dropboxDir));
}

// Using session
/*app.use(session({
    resave:false,
    saveUninitialized:true,
    secret:global.config["session-secret"],
    cookie:{
        maxAge: 60 * 1000,
        secure:true,
        httpOnly:false
    }
}));*/
// Login Interceptor
app.use('/', routes);

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error('Not Found');
  err.status = 404;
  next(err);
});

// error handlers

// development error handler
// will print stacktrace
if (app.get('env') === 'development') {
  app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
      message: err.message,
      error: err
    });
  });
}

module.exports = app;
