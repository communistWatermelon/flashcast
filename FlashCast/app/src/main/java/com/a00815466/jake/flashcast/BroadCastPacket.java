package com.a00815466.jake.flashcast;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Jake on 2015-02-10.
 */
public class BroadCastPacket
{
    private int _SEQ;
    private byte _TYPE;
    private byte _CODE;
    private byte _DATA[] = new byte[Constants.BROADCASTDATASIZE];

    public BroadCastPacket(int s, byte t, byte c, byte[] d)
    {
        _SEQ = s;
        _TYPE = t;
        _CODE = c;
        _DATA = Arrays.copyOf(d, Constants.BROADCASTDATASIZE);
    }

    public BroadCastPacket(byte[] pack)
    {
        byte[] temp = Arrays.copyOf(pack, 4);
        _SEQ = ByteBuffer.wrap(temp).getInt();
        _TYPE = pack[4];
        _CODE = pack[5];
        _DATA = Arrays.copyOfRange(pack, 6, pack.length);
    }

    public byte[] getBytes()
    {
        byte temp[] = new byte[Constants.BROADCASTSIZE];
        Arrays.fill(temp, (byte) 0);
        ByteBuffer returnValue = ByteBuffer.wrap(temp);

        ByteBuffer integerValue = ByteBuffer.allocate(4);
        integerValue.putInt(_SEQ);

        returnValue.put(integerValue.array());
        returnValue.put(_TYPE);
        returnValue.put(_CODE);
        returnValue.put(_DATA);

        return temp;
    }

    public int getSEQ()
    {
        return _SEQ;
    }

    public int getTYPE()
    {
        return (char) _TYPE;
    }

    public int getCODE()
    {
        return (char) _CODE;
    }

    public String getDataString()
    {
        return new String(_DATA).trim();
    }
}
