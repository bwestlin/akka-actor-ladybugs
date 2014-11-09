
var WS = (function () {

  var ws, isOpen = false;
  var pendingMessages = [];
  var listeners = [];

  function onOpen(e) {
    console.log("WS.onOpen");
    isOpen = true;
    for (i in pendingMessages) ws.send(pendingMessages[i]);
  }

  function onMessage(e) {
    var data = e.data;
    //console.log("WS.onMessage data:", data);
    for (var i in listeners) listeners[i](e.data);
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
    listeners.push(f);
  }

  return {
    connect: connect,
    disconnect: disconnect,
    send: send,
    listener: listener
  };
}());

var InvokeCounter = (function () {

  function counterFunction(intervalMillisec, callback) {
    var counter = 0;

    function intervalEnded() {
      callback(counter);
      counter = 0;
      setTimeout(intervalEnded, intervalMillisec);
    }
    setTimeout(intervalEnded, intervalMillisec);

    return function () {
      counter++;
    }
  }

  return {
    counterFunction: counterFunction
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

    var stageInfo = {
      egg:   { width: 20, numImages: 1 , numFrames: 1 },
      child: { width: 30, numImages: 32, numFrames: 4 },
      adult: { width: 40, numImages: 32, numFrames: 4 },
      old:   { width: 40, numImages: 32, numFrames: 4, animDevisor: 3 },
      dead:  { width: 40, numImages: 32, numFrames: 1 }
    }[ladybug.state.stage];

    var angle = bounded(parseInt(ladybug.state.directionAngle) * -1, 360);
    var degreesPerImageStep = 360 / stageInfo.numImages;
    var rotateImageStep = parseInt((angle + (degreesPerImageStep / 2)) / degreesPerImageStep) % stageInfo.numImages;
    var bgPosX = rotateImageStep * stageInfo.width * -1;
    if (bgPosX > 0) bgPosX -= (stageInfo.width * stageInfo.numImages);
    //console.log("angle=" + angle + ", rotateImageStep=" + rotateImageStep + ", bgPosX=" + bgPosX + ", degreesPerImageStep=" + degreesPerImageStep);
    var animStepDivisor = stageInfo.animDevisor || 1;
    var bgPosY = (parseInt(animStep / animStepDivisor) % stageInfo.numFrames) * stageInfo.width * -1;

    $elem
      .css({
        "left": parseInt(ladybug.position.pos.x) + "px",
        "top":  parseInt(ladybug.position.pos.y) + "px",
        "background-position": bgPosX + "px " + bgPosY + "px"
      })
      .removeClass("egg child adult old dead")
      .addClass(ladybug.state.stage)
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

  WS.listener(InvokeCounter.counterFunction(1000, function (count) {
    $("#msgsPerSec").text(count + " msgs/s");
  }));

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