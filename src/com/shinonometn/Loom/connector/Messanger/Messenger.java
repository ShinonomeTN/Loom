package com.shinonometn.Loom.connector.Messanger;

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

    public Messenger(ShuttleEvent feedBackObject,InetAddress address){
        shuttleEvent = feedBackObject;
        setDaemon(true);
        Logger.log("Initiating Message Thread.");
        try {
            //创建监听Socket
            messageSocket = new DatagramSocket(4999,address);
        } catch (SocketException e) {
            Logger.log("Get socket for Messenger failed. " + e.getMessage());
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_PORT_IN_USE,"get_message_socket_failed");
        }
    }

    public void run(){
        messagePacket = new DatagramPacket(buffer,buffer.length);
        while (isRun){
            try {
                sleep(1);

                //监听服务器信息
                Logger.log("Listening Server message...");
                messageSocket.receive(messagePacket);
                bufferTemp = new byte[messagePacket.getLength()];
                System.arraycopy(messagePacket.getData(), 0, bufferTemp, 0, bufferTemp.length);
                Logger.log(HexTools.byte2HexStr(bufferTemp));
                messagePupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                Logger.log("Server send you a |" + Dictionary.actionNames(messagePupa.getAction()) + "| packet.");
                //如果是下线包的话，通知为下线。如果是其他的包的话，直接把内容发送出去
                if(messagePupa.getAction() == 0x9){
                    shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_MESSAGE, "offline");
                }else{
                    shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_MESSAGE,Pupa.toPrintabelString(messagePupa));
                }
                //Logger.log(Pupa.toPrintabelString(messagePupa));
            } catch (IOException e) {
                e.printStackTrace();
                messageSocket.close();
                isRun = false;
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OTHER_EXCEPTION,"message_thread_exception");
                //break;
            } catch (InterruptedException e){
                //一旦中断就退出
                isRun = false;
                System.out.println("Message Thread Closing....");
                messageSocket.close();
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_MSGTHREAD_CLOSE,"message_thread_closed");
                //break;
            }
        }
    }
}
