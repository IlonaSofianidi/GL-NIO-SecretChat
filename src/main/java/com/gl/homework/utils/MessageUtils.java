package com.gl.homework.utils;

import lombok.extern.java.Log;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class MessageUtils {
    public static String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    public static String extractSender(String lineToParse) {
        String userId = null;
        if (lineToParse.contains("From:")) {
            Pattern p = Pattern.compile("From:([0-9]+)/");
            Matcher m = p.matcher(lineToParse);
            while (m.find()) {
                userId = (m.group(1));
            }
        }
        return userId;
    }

    public static String extractMsgId(String lineToParse) {
        String message = null;
        if (lineToParse.contains("msg id:")) {
            Pattern p = Pattern.compile("msg id:(.*?)/");
            Matcher m = p.matcher(lineToParse);
            while (m.find()) {
                message = (m.group(1));
            }
        }
        return message;
    }

    public static int extractUserId(String lineToParse) {
        int userId = 0;
        if (lineToParse.contains("To:")) {
            try {
                Pattern p = Pattern.compile("To:([0-9]+)/");
                Matcher m = p.matcher(lineToParse);
                while (m.find()) {
                    userId = Integer.parseInt(m.group(1));
                }
            } catch (NumberFormatException ex) {
                log.info("Invalid user id " + ex);
            }
        }
        return userId;
    }

    public static String extractMessage(String lineToParse) {
        String message = null;
        if (lineToParse.contains("msg:")) {
            Pattern p = Pattern.compile("msg:(.*)");
            Matcher m = p.matcher(lineToParse);
            while (m.find()) {
                message = (m.group(1));
            }
        }
        return message;
    }

    public static String extractPhase(String lineToParse) {
        String message = null;
        if (lineToParse.contains("phase:")) {
            Pattern p = Pattern.compile("phase:([0-9]+)/");
            Matcher m = p.matcher(lineToParse);
            while (m.find()) {
                message = (m.group(1));
            }
        }
        return message;
    }
}
