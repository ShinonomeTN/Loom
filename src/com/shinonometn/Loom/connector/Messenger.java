package com.shinonometn.Loom.connector;

import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.connector.Messanger.ShuttleEvent;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.Dictionary;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by catten on 15/11/2.
 */
public class Messenger extends Thread{
    DatagramSocket messageSocket;
    DatagramPacket messagePacket;
    Pupa messagePupa;
    public boolean isRun = true;
    byte[] buffer = new byte[1024];
    byte[] bufferTemp;
    ShuttleEvent shuttleEvent;

    public Messenger(ShuttleEvent feedBackObject,InetAddress server){
        shuttleEvent = feedBackObject;
        try {
            messageSocket = new DatagramSocket(4999,server);
        } catch (SocketException e) {
            Logger.log("Get socket for Messenger failed.");
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_PORT_IN_USE,"get_message_socket_failed");
            return;
        }
    }

    public void run(){
        messagePacket = new DatagramPacket(buffer,buffer.length);
        while (isRun){
            try {
                sleep(10);
                System.out.println("Listening Server message...");
                messageSocket.receive(messagePacket);
                bufferTemp = new byte[messagePacket.getLength()];
                System.arraycopy(messagePacket.getData(), 0, bufferTemp, 0, bufferTemp.length);
                Logger.log(HexTools.byte2HexStr(bufferTemp));
                messagePupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                System.out.println("Server send you a " + Dictionary.actionNames(messagePupa.getAction()) + "packet.");
                if(messagePupa.getAction() == 0x9){
                    System.out.println("Server maybe wanna you go offline..........\nBut I will not stop myself till you close me ;P");
                }
                Logger.log(Pupa.toPrintabelString(messagePupa));
            } catch (IOException e) {
                e.printStackTrace();
                messageSocket.close();
                break;
            } catch (InterruptedException e){
                System.out.println("Message Thread Closing....");
                messageSocket.close();
                break;
            }
        }
    }
}
