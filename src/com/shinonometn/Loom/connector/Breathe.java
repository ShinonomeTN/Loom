package com.shinonometn.Loom.connector;

import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.connector.Messanger.ShuttleEvent;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * Created by catten on 15/11/2.
 */
public class Breathe extends Thread {
    private String session;
    private ShuttleEvent shuttleEvent;
    DatagramSocket datagramSocket;
    private boolean stopFlag = false;
    public int sleepTime = 20000;
    private int serialNo = 0x01000003;
    byte[] buffer;
    byte[] bufferTemp;
    DatagramPacket breathePacket;
    String tempFiled;
    byte[] macAddress;
    String ipAddress;
    private Pupa breathePupa;
    private InetAddress serverInetAddress;

    public Breathe(DatagramSocket socket,String sessionNo,ShuttleEvent feedbackObject,byte[] mac,String ip,InetAddress server){
        shuttleEvent = feedbackObject;
        session = sessionNo;
        datagramSocket = socket;
        macAddress = mac;
        ipAddress = ip;
    }

    public void stopBreath(){
        stopFlag = true;
    }
    /*
    public void run(){
        try{
            //
            sleep(sleepTime);
        }catch (InterruptedException e){
            if(stopFlag){
                //
            }
        }
    }//*/
    public void run(){
        Logger.log("Set breathe time as "+sleepTime+"s.");
        boolean noSleep = true;
        while(!stopFlag){
            try {
                if(!noSleep){
                    noSleep = true;
                    Logger.log("Sleep for " + sleepTime);
                    sleep(sleepTime);
                }
                Logger.log("Breathe...");
                tempFiled = String.format(
                        "session:%s|ip address:%s|serial no:0%x|mac address:%s",
                        HexTools.byte2HexStr(session.getBytes()),
                        HexTools.byte2HexStr(ipAddress.getBytes()),
                        serialNo,
                        HexTools.byte2HexStr(macAddress)
                );
                breathePupa = new Pupa("breathe",tempFiled);
                Logger.log("[Field]" + Pupa.toPrintabelString(breathePupa));
                breathePacket = new DatagramPacket(
                        Pronunciation.encrypt3848(breathePupa.getData()),
                        breathePupa.getData().length,
                        serverInetAddress,
                        3848
                );
                datagramSocket.send(breathePacket);
                buffer = new byte[1024];
                breathePacket.setData(buffer);
                breathePacket.setLength(buffer.length);
                Logger.log("Waiting Server Response...");
                datagramSocket.setSoTimeout(10000);
                datagramSocket.receive(breathePacket);
                /*
                try{
                    datagramSocket.receive(breathePacket);
                }catch (SocketTimeoutException e){
                    try {
                        Logger.log("Breathe timeout...Try again....");
                        breathePacket.setData(Pronunciation.encrypt3848(breathePupa.getData()));
                        breathePacket.setLength(breathePupa.getData().length);
                        datagramSocket.send(breathePacket);
                        breathePacket.setData(buffer);
                        breathePacket.setLength(buffer.length);
                        datagramSocket.receive(breathePacket);
                    }catch (SocketTimeoutException ee){
                        Logger.log("Breathe failed...Server no response.");
                        throw new InterruptedException("I killed myself");
                    }
                }
                //*/
                bufferTemp = new byte[breathePacket.getLength()];
                System.arraycopy(breathePacket.getData(),0,bufferTemp,0,bufferTemp.length);
                breathePupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                Logger.log(Pupa.toPrintabelString(breathePupa));
                byte[] TfiledBuffer = Pupa.findField(breathePupa,"is success");
                if(TfiledBuffer != null && HexTools.toBool(TfiledBuffer)) {
                    serialNo += 0x03;
                    Logger.log("Breathed.");
                }else if(TfiledBuffer == null && Pupa.findField(breathePupa,"serial no") != null){
                    if(serialNo == 0x01000003){
                        serialNo = 0x0100000;
                    }
                    serialNo += 0x06;
                }else{
                    Logger.log("Server Rejected this Breathe.");
                    //break;
                }
            } catch (InterruptedException e) {
                Logger.log("Breathe thread Closeing....");
                datagramSocket.close();
                break;
            } catch (SocketTimeoutException e){
                Logger.log("Breathe timeout.");
                noSleep = true;
            } catch (IOException e){
                e.printStackTrace();
                datagramSocket.close();
            }
        }
    }
}
