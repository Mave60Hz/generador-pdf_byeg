package com.bizlinks.helpdesk.service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** JCE implementation compatible with the legacy NcCrypt 3DES URL-token format. */
final class LegacyNcCrypt {
    private static final byte[] CODE = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] IV = {(byte) 0xfe, (byte) 0xdc, (byte) 0xba, 0x21, (byte) 0x90, 0x43, (byte) 0x87, 0x65};
    private LegacyNcCrypt() { }

    static String encryptPassword(String value) throws Exception {
        byte[] seed = decodeKey("7c91de760ccdfeef".getBytes(StandardCharsets.US_ASCII));
        byte[] secondSeed = decodeKey(indexed(3, 11, 10, 3, 15, 1, 9, 5, 1, 13, 2, 11, 5, 13, 15, 3));
        byte[] thirdSeed = decodeKey(indexed(11, 1, 4, 8, 6, 15, 2, 11, 0, 13, 2, 11, 15, 10, 12, 0));
        byte[] key = new byte[24];
        System.arraycopy(seed, 0, key, 0, 8);
        System.arraycopy(secondSeed, 0, key, 8, 8);
        System.arraycopy(thirdSeed, 0, key, 16, 8);

        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DESede"), new IvParameterSpec(IV));
        byte[] encrypted = cipher.doFinal(legacyBytes(value));
        String base64 = Base64.getEncoder().encodeToString(encrypted);
        StringBuilder token = new StringBuilder("x'");
        for (int i = 0; i < base64.length(); i++) token.append(Integer.toHexString(base64.charAt(i)).toUpperCase());
        while (token.length() / 2 <= 32) token.append("20");
        return token.append('\'').toString();
    }

    private static byte[] indexed(int... indexes) {
        byte[] value = new byte[indexes.length];
        for (int i = 0; i < indexes.length; i++) value[i] = CODE[indexes[i]];
        return value;
    }

    private static byte[] decodeKey(byte[] encoded) {
        if ((encoded.length & 1) != 0) throw new IllegalArgumentException("Clave NcCrypt impar");
        byte[] decoded = new byte[encoded.length / 2];
        for (int i = 0; i < encoded.length; i += 2) {
            int high = indexOf(encoded[i]), low = indexOf(encoded[i + 1]);
            if (high < 0 || low < 0) throw new IllegalArgumentException("Clave NcCrypt inválida");
            decoded[i / 2] = (byte) ((high << 4) | low);
        }
        return decoded;
    }

    private static int indexOf(byte value) {
        for (int i = 0; i < CODE.length; i++) if (CODE[i] == value) return i;
        return -1;
    }

    private static byte[] legacyBytes(String value) {
        byte[] result = new byte[value.length()];
        for (int i = 0; i < value.length(); i++) result[i] = (byte) value.charAt(i);
        return result;
    }
}
