package com.shinonometn.loom.core.data;

/**
 * Created by catten on 16/5/28.
 */
public interface Client {
    String getSession();

    String getIPAddress();

    byte[] getMacAddress();

    String getUsername();

    String getPassword();

    String getVersion();

    int getSerialNo();
}
