package com.meituan.data.jmxtools.conf;

import org.junit.Test;

import static org.junit.Assert.*;

public class EndpointTest {

    @Test
    public void testValueOf() throws Exception {
        Endpoint local = Endpoint.valueOf("local:.*HiveMetaStore.*");
        assertFalse(local.isRemote());
        assertEquals(".*HiveMetaStore.*", local.getName());

        Endpoint remote = Endpoint.valueOf("remote:localhost:1234");
        assertTrue(remote.isRemote());
        assertEquals("localhost", remote.getRemoteHost());
        assertEquals(1234, remote.getRemotePort());

        try {
            Endpoint.valueOf("localhost:1234");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("invalid tagged format", e.getMessage());
        }

        try {
            Endpoint.valueOf("remote:localhost");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("remote name should be host:port, but was localhost", e.getMessage());
        }
    }
}