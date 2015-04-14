package flashcast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by Jake on 2015-02-06.
 */
public class KeyFunctions
{
    public static PublicKey stringToKey(String text)
    {
        PublicKey pub = null;
        try
        {
            String temp[] = text.split(":");

            BigInteger modulus = new BigInteger(temp[0]);
            BigInteger exponent = new BigInteger(temp[1]);

            RSAPublicKeySpec rsaPub = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            pub = fact.generatePublic(rsaPub);
            return pub;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return pub;
    }

    public static String keyToString(PublicKey key)
    {
        String temp = "";

        try
        {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            RSAPublicKeySpec pubSpec = keyFactory.getKeySpec(key, RSAPublicKeySpec.class);
            temp = pubSpec.getModulus() + ":";
            temp += pubSpec.getPublicExponent();
            return temp;
        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            return temp;
        }
    }

    public static PublicKey getPublicKey()
    {
        DataInputStream in = null;
        PublicKey pub = null;
        try
        {
            in = new DataInputStream(new FileInputStream(Constants.PUBLIC_KEY_FILE));
            byte[] data = new byte[in.available()];
            in.readFully(data);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(data);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            pub = kf.generatePublic(keySpec);
        } catch (FileNotFoundException e)
        {
            return null;
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return pub;
    }

    public static PrivateKey getPrivateKey()
    {
        FileInputStream fis = null;
        DataInputStream in = null;
        PrivateKey pri = null;
        try
        {

            in = new DataInputStream(new FileInputStream(Constants.PRIVATE_KEY_FILE));
            byte[] data = new byte[in.available()];
            in.readFully(data);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(data);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            pri = kf.generatePrivate(keySpec);

        } catch (FileNotFoundException e)
        {
            return null;
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return pri;
    }

    public static void storeKeys(PublicKey pub, PrivateKey pri)
    {
        DataOutputStream dos = null;

        try
        {
            dos = new DataOutputStream(new FileOutputStream(Constants.PUBLIC_KEY_FILE));
            dos.write(pub.getEncoded());
            dos.close();

            dos = new DataOutputStream(new FileOutputStream(Constants.PRIVATE_KEY_FILE));
            dos.write(pri.getEncoded());
            dos.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void generateKeys()
    {
        try
        {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(Constants.ENCRYPTSIZE);
            KeyPair pair = gen.generateKeyPair();
            PublicKey pub = pair.getPublic();
            PrivateKey pri = pair.getPrivate();
            storeKeys(pub, pri);
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }
}
