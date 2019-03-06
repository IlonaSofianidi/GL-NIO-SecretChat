package com.gl.homework.client;

class MessageInfo {
    String message;
    String messageId;
    int phase;
    int receiverUserId;

    public MessageInfo(String message, String messageId, int phase, int receiverUserId) {
        this.message = message;
        this.messageId = messageId;
        this.phase = phase;
        this.receiverUserId = receiverUserId;
    }
}
