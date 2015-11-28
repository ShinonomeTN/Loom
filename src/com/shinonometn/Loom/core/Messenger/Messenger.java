package com.shinonometn.Loom.core.Messenger;

import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.Dictionary;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;
import com.sun.javafx.tools.packager.Log;

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

    public Messenger(ShuttleEvent feedBackObject,InetAddress address){
        shuttleEvent = feedBackObject;
        setDaemon(true);
        Logger.log("Initiating Message Thread.");
        try {
            //创建监听Socket
            messageSocket = new DatagramSocket(4999,address);
            Logger.log("Get socket for Messenger success.");
        } catch (SocketException e) {
            Logger.log("Get socket for Messenger failed. " + e.getMessage());
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_PORT_IN_USE,"get_message_socket_failed");
        }
    }

    public void dispose(){
        isRun = false;
        Logger.log("Messenger is closing...");
        if(!messageSocket.isClosed()){
            messageSocket.close();
            Logger.log("Messenger closed.");
        }else Logger.log("Messenger already closed.");
    }

    public void run(){
        shuttleEvent.onMessage(ShuttleEvent.MESSAGE_START,"start");
        messagePacket = new DatagramPacket(buffer,buffer.length);
        while (isRun){
            try {

                //监听服务器信息
                Logger.log("Listening Server message...");
                if(!messageSocket.isClosed()){
                    messageSocket.receive(messagePacket);
                    if(messagePacket.getData() != null){
                        bufferTemp = new byte[messagePacket.getLength()];
                        System.arraycopy(messagePacket.getData(), 0, bufferTemp, 0, bufferTemp.length);
                        Logger.log(HexTools.byte2HexStr(bufferTemp));
                        messagePupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                        Logger.log("Server send you a |" + Dictionary.actionNames(messagePupa.getAction()) + "| packet.");
                        //如果是下线包的话，通知为下线。如果是其他的包的话，直接把内容发送出去
                        if(messagePupa.getAction() == 0x9){
                            shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE, "offline");
                        }else{
                            shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE,Pupa.toPrintabelString(messagePupa));
                        }
                    }
                }else
                    Logger.log("Messenger was closed. Can not recive messanges.");

            } catch (IOException e) {
                Logger.error("An error accorded at Messenger." + e.getMessage());
                isRun = false;
                if(!messageSocket.isClosed()){
                    //e.printStackTrace();
                    dispose();
                    shuttleEvent.onMessage(ShuttleEvent.MESSAGE_EXCEPTION,"message_thread_exception");
                }else{
                    dispose();
                    shuttleEvent.onMessage(ShuttleEvent.MESSAGE_CLOSE,"message_thread_closed");
                }
                //break;
            }
        }
    }
}
