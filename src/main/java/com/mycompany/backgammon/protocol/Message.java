package com.mycompany.backgammon.protocol;

import java.io.Serializable;

/**
 * Wire protocol message: a type tag plus an arbitrary serializable payload.
 * Using Java object serialization over ObjectInput/OutputStream keeps the
 * protocol simple and typed.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public final MessageType type;
    public final Object payload;

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public Message(MessageType type) {
        this(type, null);
    }

    @Override
    public String toString() {
        return "Message{" + type + (payload == null ? "" : ", " + payload) + "}";
    }
}
