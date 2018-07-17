var Service = require('node-windows').Service;
var path = require('path');
 
// Create a new service object 
var svc = new Service({
  name:'C1',
  description: 'The nodejs.org example web server.',
  script: 'C:\\HewlettPackardEnterprise\\face-extraction-app\\videoActionsServer.js',
  env: [{
    name: "HOME",
    value: process.env["USERPROFILE"] // service is now able to access the user who created its' home directory 
  },
  {
    name: "TEMP",
    value: path.join(process.env["USERPROFILE"],"/temp") // use a temp directory in user's home directory 
  }]
});
 
// Listen for the "install" event, which indicates the 
// process is available as a service. 
svc.on('install',function(){
  svc.start();
});
 
svc.install();