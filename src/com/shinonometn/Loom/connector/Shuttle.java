package com.shinonometn.Loom.connector;

import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.connector.Messanger.ShuttleEvent;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by catten on 15/11/2.
 */
public class Shuttle extends Thread{
    private final int defaultPacketSize = 1024;
    private final int defaultSocketTimeout = 10000;

    private NetworkInterface networkInterface;

    private String username;
    private String password;
    private byte[] init_session = new byte[]{0x00,0x00,0x00,0x01};
    private String session;

    private String ipAddress;
    private String serverIPAddress;
    private byte[] macAddress;
    private InetAddress serverInetAddress;
    private InetAddress localInetAddress;

    //private boolean isBreathing = false;
    Breathe breatheThread;
    Messenger messengerThread;

    private ShuttleEvent shuttleEvent;

    private DatagramSocket datagramSocket;

    public Shuttle(NetworkInterface networkInterface,ShuttleEvent feedBackObject){
        this.networkInterface = networkInterface;
        this.shuttleEvent = feedBackObject;
        setDaemon(true);
        List<InterfaceAddress> interfaceAddressList = networkInterface.getInterfaceAddresses();
        for(InterfaceAddress iA:interfaceAddressList){
            if(iA.toString().contains(".")) {
                localInetAddress = iA.getAddress();
                ipAddress = iA.getAddress().toString().replace("/", "");
                Logger.log("Shuttle change IP to " + ipAddress);
                try {
                    macAddress = networkInterface.getHardwareAddress();
                } catch (SocketException e) {
                    Logger.error(e.getMessage());
                }
            }
        }

        try {
            Logger.log("Try to get socket.");
            datagramSocket = new DatagramSocket(3848,localInetAddress);
            datagramSocket.setSoTimeout(defaultSocketTimeout);
        } catch (SocketException e) {
            Logger.error("Get socket Failed." + e.getMessage());
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_PORT_IN_USE, e.getMessage());
            return;
        }
        Logger.log("Get socket success.");
        shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_GET_SOCKET_SUCCESS, "get_socket_success");
    }

    //private Queue queue = new LinkedList<String>();

    public void run(){
        try{
            if(!knock()) return;
            this.wait();
        }catch (InterruptedException e){
            
        }
    }

    private boolean knock(){
        //if(datagramSocket == null) return;

        String fields = String.format(
                "session:%s|ip address:%s|mac address:%s",
                HexTools.byte2HexStr(init_session),
                HexTools.byte2HexStr(ipAddress.getBytes()),
                HexTools.byte2HexStr(macAddress)
        );
        byte[] data = Pronunciation.encrypt3848(new Pupa("login", fields).getData());
        Logger.log("[Fields]"+fields);
        DatagramPacket datagramPacket = null;
        try {
            datagramPacket = new DatagramPacket(data,data.length, InetAddress.getByName("1.1.1.8"),3850);
        } catch (UnknownHostException e) {
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_NOT_FOUNT, "knock_server_not_found");
            return false;
        }
        try {
            Logger.log("Knocking server.");
            datagramSocket.send(datagramPacket);
            datagramPacket.setData(new byte[defaultPacketSize]);
            datagramPacket.setLength(defaultPacketSize);
            //datagramSocket.setSoTimeout(defaultSocketTimeout);
            Logger.log("Waiting server response.");
            datagramSocket.receive(datagramPacket);
        } catch (SocketTimeoutException e) {
            Logger.log("Server no response.");
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_NO_RESPONSE, "knock_server_no_response");
            return false;
        } catch (IOException e) {
            Logger.log("Unknown Exception.");
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OTHER_EXCEPTION, "unknown_exception");
            return false;
        }
        Logger.log("Server response.");
        shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_RESPONSE, "server_response");
        data = new byte[datagramPacket.getLength()];
        System.arraycopy(Pronunciation.decrypt3848(datagramPacket.getData()), 0, data, 0, data.length);
        byte[] fieldBuffer = Pupa.fieldData(Pupa.findField(new Pupa(data), "server ip address"));
        serverIPAddress = String.format(
                "%d.%d.%d.%d",
                fieldBuffer[0] & 0xFF,
                fieldBuffer[1] & 0xFF,
                fieldBuffer[2] & 0xFF,
                fieldBuffer[3] & 0xFF
        );
        if(serverIPAddress != null){
            if(!serverIPAddress.equals("")){
                Logger.log("Server IP is : "+serverIPAddress);
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_RESPONSE,"Server IP is "+serverIPAddress);
                try {
                    serverInetAddress = InetAddress.getByName(serverIPAddress);
                } catch (UnknownHostException e) {
                    Logger.log("Server IP unavailable.");
                    shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_NOT_FOUNT, "server_ip_unavailable");
                }
            }else
                Logger.error("Get server IP failed: Field empty.");
        }else{
            Logger.error("Get server IP failed: No Server IP Data found.");
        }
        //datagramSocket.close();
        return true;
    }

    private boolean login(){
        if(datagramSocket == null) return false;
        Logger.log("Try to use account " + username + "to login...");
        String fields = String.format(
                "session:%s|username:%s|password:%s|ip address:%s|mac address:%s|access point:%s|version:%s|is dhcp enabled:%s",
                HexTools.byte2HexStr(init_session),
                HexTools.byte2HexStr(username.getBytes()),
                HexTools.byte2HexStr(password.getBytes()),
                HexTools.byte2HexStr(ipAddress.getBytes()),
                HexTools.byte2HexStr(macAddress),
                HexTools.byte2HexStr("internet".getBytes()),
                HexTools.byte2HexStr("3.6.9".getBytes()),
                "00"
        );
        Logger.log("[Fields]" + fields);
        byte[] data = Pronunciation.encrypt3848(new Pupa("login",fields).getData());
        DatagramPacket datagramPacket = new DatagramPacket(data,data.length,serverInetAddress,3848);
        try {
            Logger.log("Sending certify package...");
            datagramSocket.send(datagramPacket);
            datagramPacket.setData(new byte[defaultPacketSize]);
            datagramPacket.setLength(defaultPacketSize);
            datagramSocket.receive(datagramPacket);
            Logger.log("Server responsed.");
            data = new byte[datagramPacket.getLength()];
            Pupa pupa = new Pupa(Pronunciation.decrypt3848(data));
            byte[] fieldBuffer = Pupa.fieldData(Pupa.findField(pupa,"is success"));
            if(fieldBuffer != null){
                if(HexTools.toBool(fieldBuffer)){
                    Logger.log("Certify success!");
                    shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_CERTIFICATE_SUCCESS,"certificate_success");
                }
            }else{
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OTHER_EXCEPTION,"certificate_status_unsure");
            }
            fieldBuffer = Pupa.findField(pupa,"session");
            session = HexTools.toGB2312Str(Pupa.fieldData(fieldBuffer));
            fieldBuffer = Pupa.findField(pupa, "message");
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_MESSAGE,HexTools.toGB2312Str(fieldBuffer));
            breatheThread = new Breathe(datagramSocket,session,shuttleEvent,macAddress,ipAddress,serverInetAddress);
            messengerThread = new Messenger(shuttleEvent,serverInetAddress);
        } catch (IOException e) {
            if(e.getCause().getClass() == SocketTimeoutException.class){
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_NO_RESPONSE, "certificate_timeout");
                Logger.error("Server no response");
                return false;
            }
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OTHER_EXCEPTION, e.getMessage());
            Logger.error(e.getMessage());
            return false;
        }
        Logger.log("Login Success.");
        shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_CERTIFICATE_SUCCESS,"certificate_success");
        return true;
    }

    private boolean logoutFlag = false;
    public void Offline(){
        logoutFlag = true;
        this.interrupt();
    }
    private void logout(){
        logoutFlag = false;
    }

    public void dispose(){
        this.datagramSocket.close();
        Logger.log("Try to dispose Shuttle.");
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
