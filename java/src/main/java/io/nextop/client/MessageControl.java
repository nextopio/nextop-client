package io.nextop.client;

import io.nextop.Message;

/**
 * Created by brien on 1/10/15.
 */
class MessageControl {
    static enum Type {
        SEND_ACK, // follow up to send_nack (?)
        SEND_ERROR, // follow up to send_nack (?)
        SEND_NACK, // represents the case where the transport has committed the message to the remote but not received a response
        SEND,
        RECEIVE,
        RECEIVE_ACK
        // TODO receive nack?
    }


    Message message;

}
