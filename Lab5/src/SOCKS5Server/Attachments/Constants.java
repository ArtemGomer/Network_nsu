package SOCKS5Server.Attachments;

public class Constants {
    public static final int BUF_SIZE = 1024;
    /*ERRORS*/
    public static byte SUCCESS = 0x00;
    public static byte SERVER_ERR = 0x01;
    public static byte RESTRICTED = 0x02;
    public static byte NOT_REACHABLE_NET = 0x03;
    public static byte NOT_REACHABLE_HOST = 0x04;
    public static byte DENIED_CONNECTION = 0x05;
    public static byte TTL_EXPIRED = 0x06;
    public static byte NOT_SUPPORTED_CMD = 0x07;
    public static byte NOT_SUPPORTED_TYPE = 0x08;
    /*AUTH*/
    public static final byte NO_AUTH = 0x00;
    public static final byte AUTH_NOT_FOUND = (byte) 0xFF;
    /*OTHER*/
    public static final byte VERSION = 0x05;
    public static final byte IPV4 = 0x01;
    public static final byte DOMAIN_NAME = 0x03;
    public static final byte TCP_CONNECT = 0x01;
    public static final byte RESERVED_BYTE = 0x00;
}
