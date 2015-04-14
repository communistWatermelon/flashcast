package flashcast;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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
    }

    public int dataLength()
    {
        return _DATA.length;
    }

    public String toString(boolean packet)
    {
        String temp = "";
        if (packet)
        {
//                int size = _DATA.length;
//                if (size < Constants.DATASIZE)
//                {
//                    byte tempArray[] = new byte[Constants.DATASIZE];
//                    Arrays.fill(tempArray, (byte) 0);
//                    System.arraycopy(_DATA, 0, tempArray, 0, size);
//                    _DATA = tempArray;
//                }
//                temp = (Integer.toString(_SEQ) + ((char) _TYPE) + ((char) _CODE) + new String(_DATA, "UTF-8"));
            temp = (Integer.toString(_SEQ) + _TYPE + _CODE + new String(_DATA));
        } else
        {
            temp = (Integer.toString(_SEQ) + _TYPE + _CODE + new String(_DATA).trim());
        }
        return temp;
    }

    public Packet(byte[] pack)
    {
        byte[] temp = Arrays.copyOf(pack, 4);
        _SEQ = ByteBuffer.wrap(temp).getInt();
        _TYPE = pack[4];
        _CODE = pack[5];
        _DATA = Arrays.copyOfRange(pack, 6, pack.length);
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

    public byte[] getData()
    {
        return _DATA;
    }

    public int getSEQ()
    {
        return _SEQ;
    }

    public void incrementSEQ()
    {
        _SEQ++;
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
