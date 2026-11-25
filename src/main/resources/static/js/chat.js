// Chat STOMP client for the chat UI. Externalized (CSP script-src has no 'unsafe-inline' —
// inline <script> bodies are blocked by the browser); served from /js/** (same-origin,
// CSP-permitted). Reads the current user's id from a data- attribute on the #chat-app
// container instead of Thymeleaf-inlining it into the script body.
(function () {
    var app = document.getElementById('chat-app');
    var currentUserId = app ? parseInt(app.getAttribute('data-current-user-id'), 10) : 0;

    var socket = null;
    var otherUserId = null;

    function conversationKey(a, b) {
        return a <= b ? (a + '-' + b) : (b + '-' + a);
    }

    // Minimal STOMP 1.2 text-frame encode/decode — no external JS libraries, so this stays
    // inside the existing strict CSP with zero new network dependencies. The backend endpoint
    // (WebSocketConfig) is registered with SockJS for broader client compatibility, but a modern
    // browser's native WebSocket can speak STOMP directly against it.
    function encodeFrame(command, headers, body) {
        var frame = command + '\n';
        for (var key in headers) {
            if (Object.prototype.hasOwnProperty.call(headers, key)) {
                frame += key + ':' + headers[key] + '\n';
            }
        }
        frame += '\n' + (body || '') + ' ';
        return frame;
    }

    function decodeFrame(raw) {
        var text = raw.charAt(raw.length - 1) === ' ' ? raw.slice(0, -1) : raw;
        var split = text.indexOf('\n\n');
        var head = split === -1 ? text : text.substring(0, split);
        var body = split === -1 ? '' : text.substring(split + 2);
        var lines = head.split('\n');
        var command = lines[0];
        var headers = {};
        for (var i = 1; i < lines.length; i++) {
            var idx = lines[i].indexOf(':');
            if (idx > -1) {
                headers[lines[i].substring(0, idx)] = lines[i].substring(idx + 1);
            }
        }
        return { command: command, headers: headers, body: body };
    }

    function appendMessage(msg) {
        var li = document.createElement('li');
        li.className = (msg.senderId === currentUserId) ? 'ck-chat-mine' : 'ck-chat-theirs';
        li.textContent = msg.body;
        var list = document.getElementById('chat-messages');
        list.appendChild(li);
        li.scrollIntoView({ block: 'nearest' });
    }

    function loadHistory(otherId) {
        fetch('/chat/' + otherId + '/messages', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.json(); })
            .then(function (messages) {
                document.getElementById('chat-messages').innerHTML = '';
                messages.forEach(appendMessage);
            });
    }

    function connectAndSubscribe(otherId) {
        var proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        socket = new WebSocket(proto + '//' + window.location.host + '/ws');
        socket.onopen = function () {
            socket.send(encodeFrame('CONNECT', { 'accept-version': '1.2', host: window.location.host }));
        };
        socket.onmessage = function (event) {
            var frame = decodeFrame(event.data);
            if (frame.command === 'CONNECTED') {
                var key = conversationKey(currentUserId, otherId);
                socket.send(encodeFrame('SUBSCRIBE', { id: 'sub-chat', destination: '/topic/chat.' + key }));
            } else if (frame.command === 'MESSAGE' && frame.body) {
                appendMessage(JSON.parse(frame.body));
            }
        };
    }

    document.getElementById('chat-open-form').addEventListener('submit', function (e) {
        e.preventDefault();
        var value = parseInt(document.getElementById('other-user-id').value, 10);
        if (!value) { return; }
        otherUserId = value;
        document.getElementById('chat-window').hidden = false;
        loadHistory(otherUserId);
        if (socket) { socket.close(); }
        connectAndSubscribe(otherUserId);
    });

    document.getElementById('chat-send-form').addEventListener('submit', function (e) {
        e.preventDefault();
        var input = document.getElementById('chat-body');
        var body = input.value.trim();
        if (!body || !otherUserId || !socket || socket.readyState !== WebSocket.OPEN) { return; }
        socket.send(encodeFrame('SEND', { destination: '/app/chat.send', 'content-type': 'application/json' },
            JSON.stringify({ recipientId: otherUserId, body: body })));
        input.value = '';
    });
})();
