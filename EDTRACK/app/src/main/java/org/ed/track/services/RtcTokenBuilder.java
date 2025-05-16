package org.ed.track.services;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Random;

public class RtcTokenBuilder {
    public enum Role {
        Role_Publisher(1),
        Role_Subscriber(2);

        private int value;

        Role(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public String buildTokenWithUid(String appId, String appCertificate, String channelName, int uid, Role role, int privilegeTs) {
        return buildTokenWithUserAccount(appId, appCertificate, channelName, String.valueOf(uid), role, privilegeTs);
    }

    public String buildTokenWithUserAccount(String appId, String appCertificate, String channelName, String account, Role role, int privilegeTs) {
        String version = "007";
        String nonce = generateRandomString(32);
        int ts = (int) (System.currentTimeMillis() / 1000);
        byte[] signature = generateSignature(appId, appCertificate, channelName, account, ts, privilegeTs, nonce);

        StringBuilder token = new StringBuilder();
        token.append(version).append(appId)
                .append(byteArrayToHex(signature))
                .append(padInt(ts))
                .append(padInt(privilegeTs))
                .append(padString(account))
                .append(padString(nonce));
        return token.toString();
    }

    private byte[] generateSignature(String appId, String appCertificate, String channelName, String uid, int ts, int privilegeTs, String nonce) {
        try {
            String message = appId + uid + channelName + ts + privilegeTs + nonce;
            SecretKeySpec keySpec = new SecretKeySpec(appCertificate.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(message.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private String padInt(int value) {
        return String.format("%010d", value);
    }

    private String padString(String str) {
        return String.format("%03d%s", str.length(), str);
    }

    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random rnd = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

