
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
  var animStep = 0;
  var animSleep = 100;

  function bounded(value, bounds) {
    if (value < 0) {
      value = (value % bounds) + bounds;
    }
    return value % bounds;
  }

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
      $elem = $('<div class="ladybug" id="' + id + '"></div>').addClass(ladybug.state.gender).appendTo("body");
    }

    var angle = bounded(parseInt(ladybug.state.directionAngle) * -1, 360);
    var degreesPerImageStep = 360 / 32;
    var rotateImageStep = parseInt((angle + (degreesPerImageStep / 2)) / degreesPerImageStep) % 32;
    var bgPosX = rotateImageStep * 40 * -1;
    if (bgPosX > 0) bgPosX -= 1280;
    //console.log("angle=" + angle + ", rotateImageStep=" + rotateImageStep + ", bgPosX=" + bgPosX + ", degreesPerImageStep=" + degreesPerImageStep);
    var bgPosY = (animStep % 4) * 40 * -1;

    $elem
      .css({
        "left": parseInt(ladybug.state.x) + "px",
        "top":  parseInt(ladybug.state.y) + "px",
        "background-position": bgPosX + "px " + bgPosY + "px"
      })
      /*.text(angle)*/;
  }

  function stepAnim() {
    animStep += 1;
    setTimeout(stepAnim, animSleep);
  }
  setTimeout(stepAnim, animSleep);

  return {
    updatePosition: updatePosition
  };
}());

$(function () {

  WS.connect("ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/");
  WS.listener(function (message) {
    //console.log("message=", message);
    var json = JSON.parse(message);
    //console.log("json:", json);
    LadybugHandler.updatePosition(json);
  });

  /*
  var _angle = 0;
  var f = function () {
    LadybugHandler.updatePosition({
      "self": "self",
      "state": {
        "x": 100,
        "y": 100,
        "directionAngle": _angle
      }
    });
    _angle += 1

    setTimeout(f, 500);
  };
  setTimeout(f, 500);
  */
});