/**
 * Клиентский класс для работы с WebSocket.
 * Управляет соединением с сервером, отправкой и приёмом сообщений.
 */

class MindClashWebSocket {
    constructor() {
        this.socket = null;
        this.connected = false;
        this.userId = null;
        this.currentRoom = null;
        this.messageHandlers = {};
    }

    connect() {
        this.socket = new WebSocket('ws://localhost:8081');

        this.socket.onopen = () => {
            console.log('WebSocket подключен');
            this.connected = true;
            if (this.messageHandlers['open']) {
                this.messageHandlers['open']();
            }
        };

        this.socket.onmessage = (event) => {
            console.log('Получено:', event.data);
            try {
                const data = JSON.parse(event.data);
                if (this.messageHandlers[data.type]) {
                    this.messageHandlers[data.type](data);
                }
                if (this.messageHandlers['*']) {
                    this.messageHandlers['*'](data);
                }
            } catch (e) {
                console.log('Ошибка парсинга:', e);
            }
        };

        this.socket.onclose = () => {
            console.log('WebSocket отключен');
            this.connected = false;
            if (this.messageHandlers['close']) {
                this.messageHandlers['close']();
            }
        };

        this.socket.onerror = (error) => {
            console.log('Ошибка WebSocket:', error);
            if (this.messageHandlers['error']) {
                this.messageHandlers['error'](error);
            }
        };
    }

    send(type, data = {}) {
        if (!this.connected || !this.socket) {
            console.log('WebSocket не подключен');
            return false;
        }
        const message = JSON.stringify({
            type: type,
            ...data
        });
        this.socket.send(message);
        return true;
    }

    login(username, password) {
        console.log('Отправка login:', username);
        return this.send('login', { username, password });
    }

    invite(toUsername, difficulty) {
        return this.send('invite', { toUsername, difficulty });
    }

    acceptInvite(inviteId) {
        return this.send('accept_invite', { inviteId });
    }

    rejectInvite() {
        return this.send('reject_invite', {});
    }

    submitAnswer(roomId, answer) {
        return this.send('answer', { roomId, answer });
    }

    createRoom(difficulty) {
        return this.send('create_room', { difficulty });
    }

    joinRoom(roomId) {
        return this.send('join_room', { roomId });
    }

    ping() {
        return this.send('ping');
    }

    on(event, callback) {
        this.messageHandlers[event] = callback;
    }

    disconnect() {
        if (this.socket) {
            this.socket.close();
        }
    }
}

const wsClient = new MindClashWebSocket();