package com.example.shm.common.ratelimit;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Strict IPv4/IPv6 literals only. Parsing never invokes a name resolver. */
final class IpAddressLiteral {
    private IpAddressLiteral() {
    }

    static String canonical(String value) {
        if (value == null || value.isEmpty() || value.length() > 45) {
            return null;
        }
        byte[] address = value.indexOf(':') >= 0 ? ipv6(value) : ipv4(value);
        if (address == null) {
            return null;
        }
        try {
            // getByAddress accepts bytes, performs no DNS, and normalizes IPv4-mapped IPv6.
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (UnknownHostException impossibleLength) {
            throw new IllegalStateException(impossibleLength);
        }
    }

    private static byte[] ipv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] result = new byte[4];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 3 || (part.length() > 1 && part.charAt(0) == '0')) {
                return null;
            }
            int number = 0;
            for (int j = 0; j < part.length(); j++) {
                char digit = part.charAt(j);
                if (digit < '0' || digit > '9') {
                    return null;
                }
                number = number * 10 + digit - '0';
            }
            if (number > 255) {
                return null;
            }
            result[i] = (byte) number;
        }
        return result;
    }

    private static byte[] ipv6(String value) {
        if (value.indexOf('.') >= 0) {
            int colon = value.lastIndexOf(':');
            byte[] tail = ipv4(value.substring(colon + 1));
            if (tail == null) {
                return null;
            }
            value = value.substring(0, colon + 1)
                    + Integer.toHexString((tail[0] & 255) * 256 + (tail[1] & 255)) + ":"
                    + Integer.toHexString((tail[2] & 255) * 256 + (tail[3] & 255));
        }
        int compression = value.indexOf("::");
        if (compression >= 0 && value.indexOf("::", compression + 2) >= 0) {
            return null;
        }
        String leftText = compression < 0 ? value : value.substring(0, compression);
        String rightText = compression < 0 ? "" : value.substring(compression + 2);
        String[] left = leftText.isEmpty() ? new String[0] : leftText.split(":", -1);
        String[] right = rightText.isEmpty() ? new String[0] : rightText.split(":", -1);
        int groups = left.length + right.length;
        if (compression < 0 ? groups != 8 : groups >= 8) {
            return null;
        }
        byte[] result = new byte[16];
        if (!writeGroups(left, result, 0) || !writeGroups(right, result, 8 - right.length)) {
            return null;
        }
        return result;
    }

    private static boolean writeGroups(String[] groups, byte[] result, int offset) {
        for (String group : groups) {
            if (group.isEmpty() || group.length() > 4) {
                return false;
            }
            int number = 0;
            for (int i = 0; i < group.length(); i++) {
                char digit = group.charAt(i);
                int hex = digit >= '0' && digit <= '9' ? digit - '0'
                        : digit >= 'a' && digit <= 'f' ? digit - 'a' + 10
                        : digit >= 'A' && digit <= 'F' ? digit - 'A' + 10 : -1;
                if (hex < 0) {
                    return false;
                }
                number = number * 16 + hex;
            }
            result[offset * 2] = (byte) (number >>> 8);
            result[offset * 2 + 1] = (byte) number;
            offset++;
        }
        return true;
    }
}
