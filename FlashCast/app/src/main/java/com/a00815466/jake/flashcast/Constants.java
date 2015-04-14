package com.a00815466.jake.flashcast;

/**
 * Created by Jake on 2015-02-06.
 */
public class Constants
{
    // Types of messages
    public final static byte REGISTER = 0;
    public final static byte DIRECTION = 1;
    public final static byte TRANSFER = 2;

    // Codes for Registration
    public final static byte HELLO_E = 0;
    public final static byte HELLO_R = 1;
    public final static byte GOODBYE = 2;

    // Codes for Direction
    public final static byte SENDINGREQ = 0;
    public final static byte FINISHED = 1;
    public final static byte ACCEPTREQ = 2;
    public final static byte DENIEDREQ = 3;

    // Codes for Transfer
    public final static byte LINK = 0;
    public final static byte FILE = 1;
    public final static byte RAW = 2;

    public final static String PUBLIC_KEY_FILE = "public_key";
    public final static String PRIVATE_KEY_FILE = "private_key";


    public final static int PACKETSIZE = 256;
    public final static int DATASIZE = 250;
    public final static int TIMEOUT = 10000;

    public final static int BROADCASTSIZE = PACKETSIZE * 4;
    public final static int BROADCASTDATASIZE = DATASIZE * 4;

    public final static int TCPPORT = 1516;
    public final static int BROADCASTPORT = 1515;

    public final static int ENCRYPTSIZE = 512;
    public final static int MAXSIZE = 32;

    public final static int CIPHERSIZE = 86;
    public final static String KEY = "LAMECODEISSUPERL";

    public static final int FOREGROUND_SERVICE = 101;
    public static final String STOPSERVICE = "com.a00815466.jake.flashcast.action.stop";
    public static final String MAIN_ACTION = "com.a00815466.jake.flashcast.action.main";

    public static final String WHITELISTMODE = "1";
    public static final String BLACKLISTMODE = "2";
    public static final String NOLISTMODE = "3";
}
