package com.a00815466.jake.flashcast;

import android.util.Log;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Jake on 2015-02-06.
 */
public class Packet
{
    private int _SEQ;
    private byte _TYPE;
    private byte _CODE;
    private byte _DATA[];

    public Packet(int s, byte t, byte c, byte[] d)
    {
        _SEQ = s;
        _TYPE = t;
        _CODE = c;
        _DATA = Arrays.copyOf(d, d.length);
//        _DATA = d.toCharArray();
        Log.d("","");
    }

    public Packet(byte[] pack)
    {
        byte[] temp = Arrays.copyOf(pack, 4);
        _SEQ = ByteBuffer.wrap(temp).getInt();
        _TYPE = pack[4];
        _CODE = pack[5];
//        _DATA = (pack.substring(3)).toCharArray();
        _DATA = Arrays.copyOfRange(pack, 6, pack.length);
        Log.d("","");
    }

    public byte[] getBytes()
    {
        byte temp[] = new byte[_DATA.length+6];
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

    public byte getTYPE()
    {
        return _TYPE;
    }

    public byte getCODE()
    {
        return _CODE;
    }

    public String getDataString()
    {
        return new String(_DATA).trim();
    }
}
