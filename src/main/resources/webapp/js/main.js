
var WS = (function () {

  var ws, isOpen = false;
  var pendingMessages = [];
  var listeners = [];

  function onOpen(e) {
    console.log("WS.onOpen", e);
    isOpen = true;
    for (i in pendingMessages) ws.send(pendingMessages[i]);
  }

  function onMessage(e) {
    var data = e.data;
    //console.log("WS.onMessage data:", data);
    for (var i in listeners) listeners[i](e.data);
  }

  function onClose(e) {
    console.log("WS.onClose", e);
  }
  function connect(url) {
    ws = new WebSocket(url);
    ws.onmessage = onMessage;
    ws.onopen = onOpen;
    ws.onclose = onClose;
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

  var selectedLadybugId;

  function hasSelectedLadybug() {
    return !!selectedLadybugId;
  }

  function selectLadybug(id) {
    var $arena = $("#arena");
    var $infoPopup = $("#infoPopup");
    if (selectedLadybugId) {
      $arena.find("#" + selectedLadybugId).removeClass("selected");
      selectedLadybugId = null;
      $infoPopup.hide();
    }

    if (id) {
      if ($infoPopup.length == 0) {
        $infoPopup = $('<div id="infoPopup"></div>');
      }

      selectedLadybugId = id;
      $arena.find("#" + selectedLadybugId).addClass("selected").append($infoPopup);
      $infoPopup.show();
    }
  }

  function updateSelectedInfo(ladybug) {
    if (selectedLadybugId === ladybug.id) {
      var $infoPopup = $("#infoPopup");

      var json = JSON.stringify(ladybug, null, ' ')
      $infoPopup.html("<pre>" + json + "</pre>");
    }
  }

  function bounded(value, bounds) {
    if (value < 0) {
      value = (value % bounds) + bounds;
    }
    return value % bounds;
  }

  function updatePosition(ladybug) {
    var idx = _.findIndex(positions, function (obj) {
      return obj.id === ladybug.id;
    });

    if (idx >= 0) {
      positions[idx] = ladybug;
    }
    else {
      idx = positions.length;
      positions.push(ladybug);
    }

    var id = ladybug.id;
    var $elem = $("#" + id);
    if ($elem.length == 0) {
      $elem = $('<div class="ladybug" id="' + id + '"></div>').addClass(ladybug.gender).appendTo("#arena");
    }

    if (ladybug.stage == "annihilated") {
      $elem.addClass(ladybug.stage);
      setTimeout(function () {
        $elem.remove();
      }, 2000);
      positions.splice(idx, 1);
      return;
    }

    var stageInfo = {
      egg:   { width: 20, numImages: 1 , numFrames: 1 },
      child: { width: 30, numImages: 32, numFrames: 4 },
      adult: { width: 40, numImages: 32, numFrames: 4 },
      old:   { width: 40, numImages: 32, numFrames: 4, animDevisor: 3 },
      dead:  { width: 40, numImages: 32, numFrames: 1 }
    }[ladybug.stage];

    var angle = bounded(parseInt(ladybug.dir) * -1, 360);
    var degreesPerImageStep = 360 / stageInfo.numImages;
    var rotateImageStep = parseInt((angle + (degreesPerImageStep / 2)) / degreesPerImageStep) % stageInfo.numImages;
    var bgPosX = rotateImageStep * stageInfo.width * -1;
    if (bgPosX > 0) bgPosX -= (stageInfo.width * stageInfo.numImages);
    var animStepDivisor = stageInfo.animDevisor || 1;
    var bgPosY = (parseInt(animStep / animStepDivisor) % stageInfo.numFrames) * stageInfo.width * -1;

    $elem
      .css({
        "left": parseInt(ladybug.pos[0]) + "px",
        "top":  parseInt(ladybug.pos[1]) + "px",
        "background-position": bgPosX + "px " + bgPosY + "px"
      })
      .removeClass("egg child adult old dead")
      .addClass(ladybug.stage);

    updateSelectedInfo(ladybug);
  }

  function stepAnim() {
    animStep += 1;
  }
  setInterval(stepAnim, animSleep);

  return {
    updatePosition: updatePosition,
    hasSelectedLadybug: hasSelectedLadybug,
    selectLadybug: selectLadybug
  };
}());

var ArenaHandler = (function () {

  var currentStones = [];

  $(function () {
    var $arena = $("#arena");
    $arena.on("click", "div.ladybug", function (e) {
      if (e.shiftKey) {
        WS.send(JSON.stringify({
          "kill": {
            "id": $(this).attr("id")
          }
        }));
        e.stopPropagation();
        return false;
      }
      else if (!e.ctrlKey) {
        LadybugHandler.selectLadybug($(this).attr("id"));
        e.stopPropagation();
        return false;
      }
    });
    $arena.on("click", "div.stone", function (e) {
      var idSplit = $(this).attr("id").split("_");
      WS.send(JSON.stringify({
        "removeStone": {
          pos: [parseInt(idSplit[1]), parseInt(idSplit[2])]
        }
      }));
      e.stopPropagation();
      return false;
    });
    $arena.on("click", function (e) {
      var hadSelectedLadyBug = LadybugHandler.hasSelectedLadybug();
      LadybugHandler.selectLadybug();
      if (hadSelectedLadyBug) return;

      if (e.ctrlKey) {
        WS.send(JSON.stringify({
          "spawn": {
            position: [e.offsetX - 4, e.offsetY - 4]
          }
        }));
      }
      else {
        WS.send(JSON.stringify({
          "putStone": {
            pos: [e.offsetX - 4, e.offsetY - 4]
          }
        }));
      }
    });
  });


  function updateArena(updates) {
    var movements = updates.movements || [];
    _.each(movements, LadybugHandler.updatePosition);
    InfoHandler.updateNumLadybugs(movements.length);
    var stones = updates.stones || [];
    updateStones(stones);
    InfoHandler.updateNumStones(stones.length);
  }

  function updateStones(stones) {
    _.each(stones, function (stone) {
      var $stone = $("#" + stoneId(stone));
      if ($stone.length == 0) {
        $stone = $('<div class="stone" id="' + stoneId(stone) + '"></div>').appendTo("#arena");
      }

      $stone
        .css({
          "left": parseInt(stone.pos[0]) + "px",
          "top":  parseInt(stone.pos[1]) + "px"
        });
    });

    // Remove no longer existing stones
    var stonesToRemove = _.remove(currentStones, function (stone) {
      return !_.find(stones, function (uStone) { return stone.pos[0] == uStone.pos[0] && stone.pos[1] == uStone.pos[1]; });
    });
    _.each(stonesToRemove, function (stone) {
      $("#" + stoneId(stone)).remove();
    });
    currentStones = stones;
  }

  function stoneId(stone) {
    return "stone_" + stone.pos[0] + "_"  + stone.pos[1];
  }

  return {
    updateArena: updateArena
  };
}());

var InfoHandler = (function () {

  var msgsPerSec = 0;
  var numLadybugs = 0;
  var numStones = 0;
  var serverStats = {};

  var nextUpdate;

  function updateMsgsPerSec(m) {
    msgsPerSec = m;
  }

  function updateNumLadybugs(num) {
    numLadybugs = num;
  }

  function updateNumStones(num) {
    numStones = num;
  }

  function updateServerStats(ss) {
    serverStats = ss;
  }

  function trigger(f) {
    return function () {
      f.apply(this, arguments);
      if (!nextUpdate) nextUpdate = setTimeout(update, 0);
    };
  }

  function update() {
    var $info = $("#info");
    $info.html(
      '<span><b>' + numLadybugs + '</b> ladybugs</span>' +
      '<span><b>' + numStones + '</b> stones</span>' +
      '<span><b>' + msgsPerSec + '</b> msgs/s</span>' +
      '<span><b>' + ((serverStats.numConnections || 1) - 1) + '</b> other ws connections</span>'
    );
    nextUpdate = undefined;
  }

  return {
    updateMsgsPerSec: trigger(updateMsgsPerSec),
    updateNumLadybugs: trigger(updateNumLadybugs),
    updateNumStones: trigger(updateNumStones),
    updateServerStats: trigger(updateServerStats)
  };
}());


$(function () {

  var messageHandlers = {
    "arena": ArenaHandler.updateArena,
    "stats": InfoHandler.updateServerStats
  };

  WS.connect("ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/");
  WS.listener(function (rawMessage) {
    var message = JSON.parse(rawMessage);

    var handler = messageHandlers[message.type];
    if (handler) handler(message.payload);
    else console.error("Unknown WS message:", message);
  });

  WS.listener(InvokeCounter.counterFunction(1000, function (count) {
    //$("#msgsPerSec").text(count + " msgs/s");
    InfoHandler.updateMsgsPerSec(count);
  }));

  /*
  var _angle = 0;
  var f = function () {
    var stage = ["child", "adult", "old", "dead", "annihilated"][((_angle / 360) | 0) % 5];
    LadybugHandler.updatePosition({
      "id": "self",
      "pos": [100, 100],
      "stage": stage,
      "gender": "male",
      "dir": _angle
    });
    if (stage == "annihilated") return;
    _angle += 10;
    setTimeout(f, 100);
  };
  setTimeout(f, 100);
  */
});

$(function () {
  function getParameterByName(name) {
      name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
      var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
          results = regex.exec(location.search);
      return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
  }

  if (getParameterByName("inline") === "true") {
    $("h1, p").hide();
    $("#container").addClass("inline");
  }
  $("body").show();
});