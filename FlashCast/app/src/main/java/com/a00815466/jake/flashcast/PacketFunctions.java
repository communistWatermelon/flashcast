package com.a00815466.jake.flashcast;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Jake on 2015-02-06.
 */
public class PacketFunctions
{
    public byte[] buildFileSendRequestPacket(String clientPubKey, String data, long size)
    {
        Log.d("","");
        String temp = data + ":" + Long.toString(size);
        Packet p = new Packet(0, Constants.DIRECTION, Constants.SENDINGREQ, temp.getBytes());
        return encryptPacket(p, clientPubKey);
    }

    public byte[] buildSendRequestPacket(String clientPubKey, long size)
    {
        String temp = Long.toString(size);
        Packet p = new Packet(0, Constants.DIRECTION, Constants.SENDINGREQ, temp.getBytes());
//        Packet p = new Packet(0, Constants.DIRECTION, Constants.SENDINGREQ, "");
        return encryptPacket(p, clientPubKey);
    }

    public byte[] buildTransferResponsePacket(String clientPubKey, boolean accept, int counter)
    {
        Packet p;
        if (accept)
        {
            p = new Packet(counter%10, Constants.TRANSFER, Constants.ACCEPTREQ, new byte[1]);
        } else
        {
            p = new Packet(counter%10, Constants.TRANSFER, Constants.DENIEDREQ, new byte[1]);
        }

        return encryptPacket(p, clientPubKey);
    }

    public byte[] buildSendFinishedPacket(String clientPubKey, int counter)
    {
        Packet p = new Packet(counter%10, Constants.DIRECTION, Constants.FINISHED, new byte[1]);
        return encryptPacket(p, clientPubKey);
    }

    public byte[] buildTransferPacket(byte[] data, byte type, String clientPubKey, int counter)
    {
        Packet p = new Packet(counter%10, Constants.TRANSFER, type, data);
        return encryptPacket(p, clientPubKey);
    }

    public byte[] buildSendResponsePacket(String clientPubKey, boolean accept)
    {
        Packet p;

        if (accept)
        {
            p = new Packet(1, Constants.DIRECTION, Constants.ACCEPTREQ, new byte[1]);
        } else
        {
            p = new Packet(1, Constants.DIRECTION, Constants.DENIEDREQ, new byte[1]);
        }

        return encryptPacket(p, clientPubKey);
    }

    public byte[] buildHelloPacket(Context context)
    {
        PublicKey temp = KeyFunctions.getPublicKey(context);
        String key = KeyFunctions.keyToString(temp);
        BroadCastPacket p = new BroadCastPacket(0, Constants.REGISTER, Constants.HELLO_E, key.getBytes());
        return p.getBytes();
    }

    public byte[] buildResponsePacket(Context context)
    {
        String key = KeyFunctions.keyToString(KeyFunctions.getPublicKey(context));
        BroadCastPacket p = new BroadCastPacket(1, Constants.REGISTER, Constants.HELLO_E, key.getBytes());
        return p.getBytes();
    }

    private byte[] encryptPacket(Packet packet, String pubKey)
    {
        PublicKey clientPub = KeyFunctions.stringToKey(pubKey);
        byte[] aesEncryptedData;
        byte[] rsaEncryptedData;
        byte[] temp = null;

        try
        {
            // AES
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            final SecretKeySpec secretKey = new SecretKeySpec(Constants.KEY.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte aesTempArray[] = cipher.doFinal(packet.getBytes());
            aesEncryptedData = Base64.encode(aesTempArray, Base64.NO_PADDING | Base64.NO_WRAP);   //base64 the aes

            // RSA
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, clientPub);
            rsaEncryptedData = c.doFinal(aesEncryptedData);
            temp = Base64.encode(rsaEncryptedData, Base64.NO_PADDING | Base64.NO_WRAP);  // base 64 the rsa
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return temp;
    }

    public Packet decryptPacket(byte[] encryptedData, Context context)
    {
        // get the keys
        PrivateKey pri = KeyFunctions.getPrivateKey(context);
        Packet p = null;
        byte[] aesDecryptedData;
        byte[] rsaDecryptedData;


        if (encryptedData.length >= 256)
        {
            encryptedData = Arrays.copyOf(encryptedData, new String(encryptedData).indexOf(0)-1);
        }

        try
        {
            //RSA
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.DECRYPT_MODE, pri);
            byte[] rsaTempArray = Base64.decode(encryptedData, Base64.NO_PADDING | Base64.NO_WRAP);
            rsaDecryptedData = c.doFinal(rsaTempArray);

            // AES
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            final SecretKeySpec secretKey = new SecretKeySpec(Constants.KEY.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] aesTempArray = Base64.decode(rsaDecryptedData, Base64.NO_PADDING | Base64.NO_WRAP);
            aesDecryptedData = cipher.doFinal(aesTempArray);
            p = new Packet(aesDecryptedData);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return p;
    }

    public String transferPacketParser(byte[] packets, int numberOfPackets, Context context)
    {
        // number of packets
        String data = "";
        for (int i = 0; i < numberOfPackets; i++)
        {
            byte[] temp = Arrays.copyOfRange(packets, (i*Constants.CIPHERSIZE), (i+1)*Constants.CIPHERSIZE);
            data += parsePacket(temp, context).getDataString();
        }
        return data;
    }

    public Packet parsePacket(byte[] packet, Context context)
    {
        Packet temp;
        temp = decryptPacket(packet, context);

        if (temp == null)
        {
            try
            {
                throw new Exception();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return temp;
    }

    public BroadCastPacket parseBroadcastPacket(byte[] packet)
    {
        return new BroadCastPacket(packet);
    }

}
