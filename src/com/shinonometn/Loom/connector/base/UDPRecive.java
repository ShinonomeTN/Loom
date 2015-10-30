package com.shinonometn.Loom.connector.base;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.rmi.server.ExportException;

/**
 * Created by catten on 15/10/27.
 */
public class UDPRecive{
    private DatagramSocket datagramSocket;
    private NetworkFeedback networkFeedbackObject;
    private DatagramPacket mailBox;
    private int datagramPackSize = 1024;
    public boolean runAsDeamon = true;

    public UDPRecive(DatagramSocket datagramSocket){
        this.datagramSocket = datagramSocket;
    }

    public void setFeedbackObject(NetworkFeedback feedbackObject){
        this.networkFeedbackObject = feedbackObject;
    }

    private void excuteOnMessage(String message){
        if(networkFeedbackObject != null){
            networkFeedbackObject.onMessage(message);
        }
    }

    private void excuteOnException(Exception e){
        if (networkFeedbackObject != null){
            networkFeedbackObject.onException(e);
        }
    }

    private void excuteOnTransmit(DatagramPacket datagramPacket){
        if (networkFeedbackObject != null){
            networkFeedbackObject.onPackageRecived(datagramPacket);
        }
    }

    public DatagramPacket getDatagramPack(){
        return mailBox;
    }


    public void listen(){
        Thread thread = new Thread(){
            public void run(){
                try{
                    byte[] buffer = new byte[datagramPackSize];
                    mailBox = new DatagramPacket(buffer,datagramPackSize);
                    excuteOnMessage("waiting");
                    datagramSocket.receive(mailBox);
                    excuteOnMessage("recived");
                    excuteOnTransmit(mailBox);
                }catch (Exception e){
                    excuteOnException(e);
                }
            }
        };
        thread.setDaemon(runAsDeamon);
        thread.start();
    }
}
