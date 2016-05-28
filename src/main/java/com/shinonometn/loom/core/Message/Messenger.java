package com.shinonometn.loom.core.message;

import com.shinonometn.pupa.Pupa;
import com.shinonometn.pupa.tools.Dictionary;
import com.shinonometn.pupa.tools.HexTools;
import com.shinonometn.pupa.tools.Pronunciation;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by catten on 15/11/2.
 */
public class Messenger extends Thread{
    private static Logger logger = Logger.getLogger("messenger");
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
        logger.info("Initiating message Thread.");
        try {
            //创建监听Socket
            messageSocket = new DatagramSocket(4999,address);
            logger.info("Get socket for Messenger success.");
        } catch (SocketException e) {
            logger.error("Get socket for Messenger failed. ",e);
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_PORT_IN_USE,"get_message_socket_failed");
        }
    }

    public void close(){
        if(isRun){
            isRun = false;
            logger.warn("Messenger is closing...");
            if(!messageSocket.isClosed()){
                messageSocket.close();
                logger.warn("Messenger closed.");
            }else logger.warn("Messenger already closed.");
        }
    }

    public void run(){
        shuttleEvent.onMessage(ShuttleEvent.MESSAGE_START,"start");
        messagePacket = new DatagramPacket(buffer,buffer.length);
        while (isRun){
            try {
                //监听服务器信息
                logger.info("Listening Server message...");
                if(!messageSocket.isClosed()){
                    messageSocket.receive(messagePacket);
                    if(messagePacket.getData() != null){
                        bufferTemp = new byte[messagePacket.getLength()];
                        System.arraycopy(messagePacket.getData(), 0, bufferTemp, 0, bufferTemp.length);
                        logger.info(HexTools.byte2HexStr(bufferTemp));
                        messagePupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                        logger.info("Server send you a |" + Dictionary.actionNames(messagePupa.getAction()) + "| packet.");
                        //如果是下线包的话，通知为下线。如果是其他的包的话，直接把内容发送出去
                        if(messagePupa.getAction() == 0x9){
                            shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE, "offline");
                        }else{
                            shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE,messagePupa.toString(Pupa.INFO_ALL));
                        }
                    }
                }else
                    logger.warn("Messenger was closed. Can not recive messanges.");

            } catch (IOException e) {
                logger.error("An error accorded at Messenger.",e);
                isRun = false;
                if(!messageSocket.isClosed()){
                    //e.printStackTrace();
                    //dispose();
                    close();
                    shuttleEvent.onMessage(ShuttleEvent.MESSAGE_EXCEPTION,"message_thread_exception");
                }else{
                    //dispose();
                    close();
                    shuttleEvent.onMessage(ShuttleEvent.MESSAGE_CLOSE,"message_thread_closed");
                }
                //break;
            }
        }
    }
}
