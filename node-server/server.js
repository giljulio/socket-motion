var http = require('http')
  , express = require('express')
  , app = express()
  , port = process.env.PORT || 5000;


//http server to output static page
app.use(express.static(__dirname + '/'));

var server = http.createServer(app);
var io = require('socket.io').listen(server);
server.listen(port);


console.log('http server listening on %d', port);

console.log('socket.io server created');

var users = {};

io.sockets.on('connection', function(socket) {
    console.log('websocket connection open');

    var code = undefined;

    /**
     * Simple, redirects update stream back to the browser
     * @param any data, doesn't get validated
     */
    socket.on('update', function(motion){
      if(users[code] != undefined && users[code].browser != undefined){
        users[code].browser.emit('update', motion);
      }
    });

    /**
     * Creates a new session
     *
     **/
    socket.on('create', function(){
      code = Math.random().toString(36).slice(2, 5);
      users[code] = {
        phone: socket
      };
      socket.emit('createCallback', {
        code: code
      });
    });

    /**
     * Joins a session with an code
     * @param code of the session
     */
    socket.on('join', function(data){
      var user = users[data.code];
      if(user != undefined){
        code = data.code;
        user["browser"] = socket;
      }
    })


    /**
     * Session closed
     * Deletes session data to stop memory leaks
     */
    socket.on('close', function() {
      var user = users[code];
      if(user != undefined && user.browser === socket){
        user.browser = undefined;
      } else if(user != undefined){
        users[code] = undefined; 
      }
      code = undefined;
      console.log('websocket connection close');
    });
});