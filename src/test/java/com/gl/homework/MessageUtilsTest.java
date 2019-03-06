package com.gl.homework;

import com.gl.homework.utils.MessageUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;


public class MessageUtilsTest {

    @Test
    public void extractSender() {
        String values = "From:3/To:1/:msg id:27b158f5-d356-41c8-8188-7c91bc9b7450/phase:3/msg:Hello";
        String actual = MessageUtils.extractSender(values);

        Assert.assertThat(actual, Matchers.is("3"));
    }

    @Test
    public void extractMsgId() {
        String values = "From:3/To:1/:msg id:27b158f5-d356-41c8-8188-7c91bc9b7450/phase:3/msg:Hello";
        String actual = MessageUtils.extractMsgId(values);

        Assert.assertThat(actual, Matchers.is("27b158f5-d356-41c8-8188-7c91bc9b7450"));
    }

    @Test
    public void extractUserId() {
        String values = "From:3/To:1/:msg id:27b158f5-d356-41c8-8188-7c91bc9b7450/phase:3/msg:Hello";
        int actual = MessageUtils.extractUserId(values);

        Assert.assertThat(actual, Matchers.is(1));
    }

    @Test
    public void extractMessage() {
        String values = "From:3/To:1/:msg id:27b158f5-d356-41c8-8188-7c91bc9b7450/phase:3/msg:Hello";
        String actual = MessageUtils.extractMessage(values);

        Assert.assertThat(actual, Matchers.is("Hello"));
    }

    @Test
    public void extractPhase() {
        String values = "From:3/To:1/:msg id:27b158f5-d356-41c8-8188-7c91bc9b7450/phase:3/msg:Hello";
        String actual = MessageUtils.extractPhase(values);

        Assert.assertThat(actual, Matchers.is("3"));
    }

}