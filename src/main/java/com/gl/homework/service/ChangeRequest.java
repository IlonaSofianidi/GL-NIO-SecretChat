package com.gl.homework.service;

import java.nio.channels.SocketChannel;

class ChangeRequest {
    static final int CHANGEOPS = 2;

    SocketChannel socket;
    int type;
    int operationSelector;

    ChangeRequest(SocketChannel socket, int type, int operationSelector) {
        this.socket = socket;
        this.type = type;
        this.operationSelector = operationSelector;
    }
}

