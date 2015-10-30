package com.shinonometn.Loom.connector.base;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Created by catten on 15/10/27.
 */
public class UDPSend {
    private DatagramSocket datagramSocket;
    private NetworkFeedback networkFeedback;
    public boolean runAsDeamon = true;
    //private InetSocketAddress socketAddress;
    //private DatagramPacket datagramPacket;

    public UDPSend(DatagramSocket datagramSocket){
        this.datagramSocket = datagramSocket;
    }

    public void excuteOnMessage(String Message){
        if(networkFeedback != null){
            networkFeedback.onMessage(Message);
        }
    }

    public void excuteOnException(Exception e){
        if(networkFeedback != null){
            networkFeedback.onException(e);
        }
    }

    public void setFeedBackObject(NetworkFeedback networkFeedback){
        this.networkFeedback = networkFeedback;
    }

    public void send(DatagramPacket datagramPacket){
        Thread thread = new Thread(){
            public void run(){
                try {
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    //excuteOnMessage("exception");
                    excuteOnException(e);
                }
            }
        };
        thread.setDaemon(runAsDeamon);
        thread.start();
    }

}
