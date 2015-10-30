package com.shinonometn.Loom.connector.base;

import java.net.DatagramPacket;

/**
 * Created by catten on 15/10/27.
 */
public interface NetworkFeedback {
    void onMessage(String message);
    void onException(Exception e);
    void onPackageRecived(DatagramPacket datagramPacket);
}
