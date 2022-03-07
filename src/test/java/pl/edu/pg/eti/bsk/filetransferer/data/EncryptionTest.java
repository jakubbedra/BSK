package pl.edu.pg.eti.bsk.filetransferer.data;

import org.junit.Test;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class EncryptionTest {

    @Test
    public void encryptionSample()
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        String input = "sample text";
        IvParameterSpec iv = EncryptionUtils.generateIv();
        SecretKey key = EncryptionUtils.generateSecretKey(128);

        byte[] ciphertext = EncryptionUtils.encryptAesCbc(
                input.getBytes(StandardCharsets.UTF_8), key, iv
        );
        byte[] plaintext = EncryptionUtils.decryptAesCbc(
                ciphertext, key, iv
        );
        System.out.println(new String(ciphertext));
        System.out.println(new String(plaintext));
    }

}
