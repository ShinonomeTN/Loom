package com.shinonometn.Loom.core.Message;

import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.core.data.DataFactory;
import com.shinonometn.Loom.core.data.PupaFactory;
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
    public boolean isRun = true;
    ShuttleEvent shuttleEvent;

    public Messenger(ShuttleEvent feedBackObject,InetAddress address){
        shuttleEvent = feedBackObject;
        setDaemon(true);
        try {
            //创建监听Socket
            messageSocket = new DatagramSocket(4999,address);
        } catch (SocketException e) {
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_PORT_IN_USE,"get_message_socket_failed");
        }
    }

    public void close(){
        if(isRun){
            isRun = false;
            Logger.log("Messenger is closing...");
            if(!messageSocket.isClosed()){
                messageSocket.disconnect();
                messageSocket.close();
            }
        }
    }

    public void run(){
        shuttleEvent.onMessage(ShuttleEvent.MESSAGE_START,"start");

        DatagramPacket messagePacket = new DatagramPacket(new byte[1024],1024);
        Pupa messagePupa;
        while (isRun){
            try {
                //监听服务器信息
                if(!messageSocket.isClosed()){
                    messageSocket.receive(messagePacket);
                    if(messagePacket.getData() != null){
                        messagePupa = PupaFactory.serverPupa(messagePacket);
                        //如果是下线包的话，通知为下线。如果是其他的包的话，直接把内容发送出去
                        if(messagePupa.getAction() == 0x9){
                            shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE, "offline");
                        }else{
                            shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE,messagePupa.toString(Pupa.INFO_ALL));
                        }
                    }
                }
            } catch (IOException e) {
                isRun = false;
                if(!messageSocket.isClosed()){
                    close();
                    shuttleEvent.onMessage(ShuttleEvent.MESSAGE_EXCEPTION,"message_thread_exception");
                }else{
                    close();
                    shuttleEvent.onMessage(ShuttleEvent.MESSAGE_CLOSE,"message_thread_closed");
                }
                //break;
            }
        }
    }
}
