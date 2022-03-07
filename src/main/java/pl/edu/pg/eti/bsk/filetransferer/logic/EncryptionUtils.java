package pl.edu.pg.eti.bsk.filetransferer.logic;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

public class EncryptionUtils {

    public static byte[] encryptAesCbc(
            byte[] plaintext, SecretKey key, IvParameterSpec iv
    ) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(plaintext);
    }

    public static byte[] decryptAesCbc(
            byte[] ciphertext, SecretKey key, IvParameterSpec iv
    ) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(ciphertext);
    }

    public static byte[] encryptAesEcb(
            byte[] plaintext, SecretKey key
    ) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plaintext);
    }

    public static byte[] decryptAesEcb(
            byte[] ciphertext, SecretKey key
    ) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(ciphertext);
    }

    public static byte[] encryptSessionKey(SecretKey sessionKey, PublicKey publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        Cipher encryptor = Cipher.getInstance("RSA");
        encryptor.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] sessionKeyBytes = sessionKey.getEncoded();
        return encryptor.doFinal(sessionKeyBytes);
    }

    public static SecretKey decryptSessionKey(byte[] ciphertext, PrivateKey privateKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        Cipher decryptor = Cipher.getInstance("RSA");
        decryptor.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] sessionKeyBytes = decryptor.doFinal(ciphertext);
        return new SecretKeySpec(sessionKeyBytes, 0, sessionKeyBytes.length, "AES");
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public static SecretKey generateSecretKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        return keyGenerator.generateKey();
    }

    public static KeyPair generateRsaKeyPair(int n) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(n);
        return keyPairGenerator.generateKeyPair();
    }

}
