package com.gl.homework;

import com.gl.homework.utils.Encryption;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class EncryptionTest {
    private Encryption unit;

    @Before
    public void setUpObjects() {
        unit = new Encryption();
    }

    @Test
    public void encrypt() {
        String messageActual = "Hello";
        String keyActual1 = "ADORN";
        String messageActual2 = "How are you today?";
        String keyActual2 = "TBPGVMGZVOQWEYPMJB";
        String encryptActual = unit.encrypt(messageActual, keyActual1);
        String encryptActal2 = unit.encrypt(messageActual2, keyActual2);
        Assert.assertThat(encryptActual, Matchers.is("	!#>!"));
        Assert.assertThat(encryptActal2, Matchers.is("\u001C-'g7?\"z/ $w164,3}"));
    }

    @Test
    public void whenKeyLengthNotEqualsMessageLengthReturnNull() {
        String messageActual = "Hello";
        String keyActual1 = "ADORNDMNU";
        String encryptActual = unit.encrypt(messageActual, keyActual1);
        Assert.assertThat(encryptActual, Matchers.nullValue());
    }

    @Test
    public void decrypt() {
        String messageActual = ">64j>-9.:6#e*9x:=(((s";
        String keyActual = "KQYPJIHXZRSQECJXNRLIQR";
        String decryptActual = unit.decrypt(messageActual, keyActual);
        Assert.assertThat(decryptActual, Matchers.is("Good weather is today!"));
    }

    @Test
    public void generateKey() {
        int keyLength = 10;
        String keyActual = unit.generateKey(keyLength);
        Assert.assertThat(keyActual.length(), Matchers.is(keyLength));
    }

    @Test
    public void whenGenerateKeyInputIsLessOrEqualsZeroReturnNull() {
        int keyLength = 0;
        int keyLength2 = -4;
        String keyActual = unit.generateKey(keyLength);
        String keyActual2 = unit.generateKey(keyLength2);
        Assert.assertThat(keyActual, Matchers.nullValue());
        Assert.assertThat(keyActual2, Matchers.nullValue());
    }
}