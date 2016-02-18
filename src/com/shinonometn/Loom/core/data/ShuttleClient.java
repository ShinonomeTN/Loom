package com.shinonometn.Loom.core.data;

/**
 * Created by catten on 16/2/18.
 */
public interface ShuttleClient {
    String getSession();
    String getIPAddress();
    byte[] getMacAddress();
    String getUsername();
    String getPassword();
    String getVersion();
    int getSerialNo();
}
