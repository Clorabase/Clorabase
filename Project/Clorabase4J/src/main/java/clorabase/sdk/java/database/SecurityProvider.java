package clorabase.sdk.java.database;

import java.security.Key;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityProvider {
    protected static Key key;


    public static void init(String password) throws Exception {
        key = generateKey(password);
    }
    protected static byte[] encrypt(String str) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE,key);
        return cipher.doFinal(str.getBytes());
    }

    private static SecretKey generateKey(String password) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), "abcdefgh".getBytes(), 786, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public static String decrypt(byte[] str) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,key);
            var bytes = cipher.doFinal(str);
            return new String(bytes);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
