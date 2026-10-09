package com.example.shm.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressLiteralTest {
    @ParameterizedTest
    @CsvSource({
            "0.0.0.0, 0.0.0.0", "255.255.255.255, 255.255.255.255", "192.0.2.9, 192.0.2.9",
            "::, 0:0:0:0:0:0:0:0", "::1, 0:0:0:0:0:0:0:1",
            "2001:DB8::1, 2001:db8:0:0:0:0:0:1", "1:2:3:4:5:6:7:8, 1:2:3:4:5:6:7:8",
            "1:2:3:4:5:6:7::, 1:2:3:4:5:6:7:0", "::ffff:192.0.2.1, 192.0.2.1",
            "0:0:0:0:0:FFFF:c000:201, 192.0.2.1", "2001:db8::192.0.2.1, 2001:db8:0:0:0:0:c000:201"
    })
    void canonicalizesValidLiterals(String input, String expected) {
        assertEquals(expected, IpAddressLiteral.canonical(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cafe", "face", "dead.beef", "127.1", "1", "2130706433", "0x7f000001",
            "1.2.3.4.", "1.2.3", "1.2.3.256", "01.2.3.4", "1.2.3.-1", "1.2.3.+1", "1.2.3.４",
            ":", ":::1", "1:::2", "1::2::3", "1:2:3:4:5:6:7", "1:2:3:4:5:6:7:8:9",
            "1:2:3:4:5:6:7:8::", "1:2:3:4:5:6:7::8", "12345::", "gg::", "[::1]",
            "fe80::1%3", "::ffff:127.1", "::ffff:192.168.01.1", "1:2:3:4:5:6:7:192.0.2.1"})
    void rejectsNonLiteralsAndMalformedAddresses(String input) {
        assertNull(IpAddressLiteral.canonical(input));
    }

    @Test
    void nullAndWhitespaceAreNotAddresses() {
        assertNull(IpAddressLiteral.canonical(null));
        assertNull(IpAddressLiteral.canonical(""));
        assertNull(IpAddressLiteral.canonical(" ::1 "));
    }
}
