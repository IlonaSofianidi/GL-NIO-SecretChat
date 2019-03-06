package com.gl.homework.utils;

import lombok.extern.java.Log;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Log
public class Encryption {
    private static final char[] encryptionArr = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    public String encrypt(String messageString, String key) {
        if (messageString.length() != key.length()) {
            return null;
        }
        BigInteger message = convertStringToBigInteger(messageString);
        BigInteger key1 = convertStringToBigInteger(key);
        BigInteger cipherText = message.xor(key1);
        return convertBigIntegerToString(cipherText);
    }

    public String decrypt(String message, String key) {
        BigInteger key1 = convertStringToBigInteger(key);
        BigInteger message1 = convertStringToBigInteger(message);
        BigInteger receivedMessage = message1.xor(key1);

        return convertBigIntegerToString(receivedMessage);
    }

    private String convertBigIntegerToString(BigInteger b) {
        String s = new String();
        while (b.compareTo(BigInteger.ZERO) == 1) {
            BigInteger c = new BigInteger("11111111", 2);
            int cb = (b.and(c)).intValue();
            Character cv = new Character((char) cb);
            s = (cv.toString()).concat(s);
            b = b.shiftRight(8);
        }
        return s;
    }

    private BigInteger convertStringToBigInteger(String s) {
        BigInteger b = BigInteger.valueOf(0);
        for (int i = 0; i < s.length(); i++) {
            Integer code = (int) s.charAt(i);
            BigInteger c = new BigInteger(code.toString());
            b = b.shiftLeft(8);
            b = b.or(c);
        }
        return b;
    }

    public String generateKey(int length) {
        if (length <= 0) {
            return null;
        }
        String key = "";
        SecureRandom secureRandom = new SecureRandom();
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            log.info("Key generation fail " + e);
        }
        //Builds the key.
        for (int i = 0; i < length; i++) {
            int randomValue = secureRandom.nextInt(26);
            key += encryptionArr[randomValue];
        }
        return key;
    }
}
