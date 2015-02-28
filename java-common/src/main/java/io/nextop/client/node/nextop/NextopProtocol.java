package io.nextop.client.node.nextop;

public class NextopProtocol {

    /** [id] */
    public static final byte F_START_MESSAGE = 0x01;
    /** [int length][data] */
    public static final byte F_MESSAGE_CHUNK = 0x02;
    /** [md5] */
    public static final byte F_MESSAGE_END = 0x03;
    // CANCEL [id]
    /** [id] */
//    static final byte F_ACK = 0x04;


}
