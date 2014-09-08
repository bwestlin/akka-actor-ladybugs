
var WS = (function () {

  var ws, isOpen = false;
  var pendingMessages = [];
  var listenerFun;

  function onOpen(e) {
    console.log("WS.onOpen");
    isOpen = true;
    for (i in pendingMessages) ws.send(pendingMessages[i]);
  }

  function onMessage(e) {
    var data = e.data;
    //console.log("WS.onMessage data:", data);
    if (listenerFun) listenerFun(e.data);
  }
  function connect(url) {
    ws = new WebSocket(url);
    ws.onmessage = onMessage;
    ws.onopen = onOpen;
  }
  function disconnect() {
    console.log("WS.disconnect");
    ws.close();
    ws = null;
  }
  function send(message) {
    if (isOpen) ws.send(message);
    else pendingMessages.push(message);
  }

  function listener(f) {
    listenerFun = f;
  }

  return {
    connect: connect,
    disconnect: disconnect,
    send: send,
    listener: listener
  };
}());

var LadybugHandler = (function () {

  var positions = [];

  function updatePosition(ladybug) {
    //console.log("updatePosition(" + ladybug + ")");
    var idx = _.findIndex(positions, function (obj) {
      return obj.self === ladybug.self;
    });

    if (idx >= 0) {
      positions[idx] = ladybug;
    }
    else {
      idx = positions.length;
      positions.push(ladybug);
    }

    var id = "pos" + idx;
    var $elem = $("#" + id);
    if ($elem.length == 0) {
      $elem = $('<div class="ladybug" id="' + id + '"></div>').appendTo("body");
    }
    $elem
      .css({
        "left": ladybug.x + "px",
        "top": ladybug.y + "px"
      })
      .text(parseInt(ladybug.angle));
  }

  return {
    updatePosition: updatePosition
  };
}());

$(function () {

  WS.connect("ws://localhost:8080/");
  WS.listener(function (message) {
    //console.log("message=", message);
    var json = JSON.parse(message);
    //console.log("json:", json);
    LadybugHandler.updatePosition(json);
  });
});