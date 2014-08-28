
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
    console.log("WS.onMessage data:", data);
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

$(function () {
  console.log("hellllo");
  var pings = 0;

  WS.connect("ws://localhost:8080/");
  WS.send("helloo");
  WS.listener(function (message) {
    if (message == "ping") {
      $("#pings").text(++pings);
    }
  });

  setTimeout(function () {
    WS.send("helloo2");
  }, 100);
});