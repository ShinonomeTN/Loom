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
    //一些参数设置
    private final int defaultPacketSize = 1024;
    private final int defaultSocketTimeout = 10000;

    private NetworkInterface networkInterface;
    //通信信息
    private String username;
    private String password;
    //初始session
    private byte[] init_session = new byte[]{0x00,0x00,0x00,0x01};
    //登陆后拿到的会话号保存在这里
    private String session;

    //一些经常用到的信息暂存在这里
    private String ipAddress;
    private String serverIPAddress;
    private byte[] macAddress;
    private InetAddress serverInetAddress;
    private InetAddress localInetAddress;

    //private boolean isBreathing = false;
    Breathe breatheThread;//呼吸线程
    Messenger messengerThread;//信息监听线程

    private ShuttleEvent shuttleEvent;//用于发送通知的对象，一般是调用Shuttle的类

    private DatagramSocket datagramSocket;//为了资源重用，敲门、认证、呼吸这三个互斥的动作都用这个Socket

    public Shuttle(NetworkInterface networkInterface,ShuttleEvent feedBackObject){
        this.networkInterface = networkInterface;
        this.shuttleEvent = feedBackObject;
        setDaemon(true);
        //获得IP地址
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
            //获得Socket用于通信
            Logger.log("Try to get socket.");
            datagramSocket = new DatagramSocket(3848,localInetAddress);
            datagramSocket.setSoTimeout(defaultSocketTimeout);
            Logger.log("Get socket success.");
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_GET_SOCKET_SUCCESS, "get_socket_success");
        } catch (SocketException e) {
            Logger.error("Get socket Failed." + e.getMessage());
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_PORT_IN_USE, e.getMessage());
        }
    }

    //private Queue queue = new LinkedList<String>();

    public void run(){
        try{
            //敲门
            if(!knock()) return;
            //登陆
            if(!login()) return;
            //创建呼吸和信息线程
            breatheThread = new Breathe(datagramSocket,session,shuttleEvent,macAddress,ipAddress,serverInetAddress);
            messengerThread = new Messenger(shuttleEvent,serverInetAddress);
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_CERTIFICATE_SUCCESS,"login_success");
            //挂起，等待
            wait();
        }catch (InterruptedException e){
            //下线
            Offline();
            dispose();
        }
    }

    public boolean isOnline(){
        return breatheThread != null && breatheThread.isAlive();
    }

    //敲门
    private boolean knock(){
        //if(datagramSocket == null) return;
        //准备好字段
        String fields = String.format(
                "session:%s|ip address:%s|mac address:%s",
                HexTools.byte2HexStr(init_session),
                HexTools.byte2HexStr(ipAddress.getBytes()),
                HexTools.byte2HexStr(macAddress)
        );
        //利用Pupa和Pronunciation生成加密好的数据
        byte[] data = Pronunciation.encrypt3848(new Pupa("login", fields).getData());
        Logger.log("[Fields]"+fields);
        DatagramPacket datagramPacket = null;
        try {
            //准备数据包
            datagramPacket = new DatagramPacket(data,data.length, InetAddress.getByName("1.1.1.8"),3850);
            try {
                //开始敲门
                Logger.log("Knocking server.");
                datagramSocket.send(datagramPacket);
                datagramPacket.setData(new byte[defaultPacketSize]);
                datagramPacket.setLength(defaultPacketSize);
                //datagramSocket.setSoTimeout(defaultSocketTimeout);
                //等待服务器回应
                Logger.log("Waiting server response.");
                datagramSocket.receive(datagramPacket);
                Logger.log("Server response.");
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_RESPONSE, "server_response");
                //取出数据包并利用Pupa取出认证服务器ip
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
                        shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_RESPONSE,serverIPAddress);
                        try {
                            serverInetAddress = InetAddress.getByName(serverIPAddress);
                        } catch (UnknownHostException e) {
                            Logger.log("Server IP unavailable.");
                            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_NOT_FOUNT, "server_ip_unavailable");
                        }
                    }else Logger.error("Get server IP failed: Field empty.");
                }else{
                    Logger.error("Get server IP failed: No Server IP Data found.");
                }
            } catch (SocketTimeoutException e) {
                Logger.log("Server no response.");
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_NO_RESPONSE, "knock_server_no_response");
                return false;
            } catch (IOException e) {
                Logger.log("Unknown Exception.");
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OTHER_EXCEPTION, "unknown_exception");
                return false;
            }
        } catch (UnknownHostException e) {
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_NOT_FOUNT, "knock_server_not_found");
            return false;
        }
        return true;
    }

    //登陆
    private boolean login(){
        if(datagramSocket == null) {
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OTHER_EXCEPTION,"socket_not_init");
            return false;
        }

        if(username == null || password == null) {
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OTHER_EXCEPTION,"certificate_info_not_filled");
            return false;
        }
        //准备认证用字段，这个认证版本是安朗的3.6.9版协议
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
        //准备数据包
        Logger.log("[Fields]" + fields);
        byte[] data = Pronunciation.encrypt3848(new Pupa("login",fields).getData());
        DatagramPacket datagramPacket = new DatagramPacket(data,data.length,serverInetAddress,3848);
        try {
            //发送数据包
            Logger.log("Sending certify package...");
            datagramSocket.send(datagramPacket);
            datagramPacket.setData(new byte[defaultPacketSize]);
            datagramPacket.setLength(defaultPacketSize);
            //等待服务器回应
            datagramSocket.receive(datagramPacket);
            Logger.log("Server responsed.");
            data = new byte[datagramPacket.getLength()];
            Pupa pupa = new Pupa(Pronunciation.decrypt3848(data));
            //判断是否登陆成功
            byte[] fieldBuffer = Pupa.fieldData(Pupa.findField(pupa,"is success"));
            if(fieldBuffer != null){
                if(HexTools.toBool(fieldBuffer)){
                    Logger.log("Certify success!");
                    shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_CERTIFICATE_SUCCESS,"certificate_success");
                }else{
                    shuttleEvent.onMessage(
                            ShuttleEvent.SHUTTLE_CERTIFICATE_FAILED,
                            HexTools.toGB2312Str(
                                    Pupa.fieldData(Pupa.findField(pupa,"message"))
                            ));
                }
            }else{
                shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OTHER_EXCEPTION,"certificate_status_unsure");
            }
            //提取会话号
            fieldBuffer = Pupa.findField(pupa,"session");
            session = HexTools.toGB2312Str(Pupa.fieldData(fieldBuffer));
            //提取服务器信息
            fieldBuffer = Pupa.findField(pupa, "message");
            shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_SERVER_MESSAGE,HexTools.toGB2312Str(fieldBuffer));
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
        //Logger.log("Login Success.");
        //shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_CERTIFICATE_SUCCESS, "certificate_success");
        return true;
    }

    private boolean logoutFlag = false;
    public void Offline(){
        logoutFlag = true;
        this.interrupt();
    }

    private void logout(){
        logoutFlag = false;
        Logger.log("Offline politely...");
        //通知呼吸和消息线程要下线了
        breatheThread.interrupt();
        messengerThread.interrupt();
        //等待两个线程关闭
        while (breatheThread.isAlive() || breatheThread.isAlive());
        //准备下线数据包
        String fields = String.format(
                "session:%s|ip address:%s|mac address:%s",
                HexTools.byte2HexStr(session.getBytes()),
                HexTools.byte2HexStr(ipAddress.getBytes()),
                HexTools.byte2HexStr(macAddress)
        );
        Logger.log(fields);
        Logger.log("Telling Server.....");
        //发送数据包
        Pupa pupa = new Pupa("logout",fields);
        DatagramPacket datagramPacket = new DatagramPacket(null,0,serverInetAddress,3848);
        datagramPacket.setData(Pronunciation.encrypt3848(pupa.getData()));
        datagramPacket.setLength(pupa.getData().length);
        try {
            datagramSocket.send(datagramPacket);
            //接收服务器返回的数据包
            byte[] buffer = new byte[1024];
            datagramPacket.setData(buffer);
            datagramPacket.setLength(buffer.length);
            datagramSocket.setSoTimeout(10000);
            try {
                datagramSocket.receive(datagramPacket);
                byte[] bufferTemp;
                bufferTemp = new byte[datagramPacket.getLength()];
                System.arraycopy(datagramPacket.getData(),0,bufferTemp,0,bufferTemp.length);
                pupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                Logger.log(Pupa.toPrintabelString(pupa));
                if(HexTools.toBool(Pupa.findField(pupa, "is success"))){
                    System.out.println("Server response.Now you are offline.");
                }
            }catch (SocketTimeoutException w){
                System.out.println("Timeout...");
            }
            datagramSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dispose(){
        breatheThread.interrupt();
        messengerThread.interrupt();
        this.datagramSocket.close();
        Logger.log("Try to dispose Shuttle.");
        shuttleEvent.onMessage(ShuttleEvent.SHUTTLE_OFFLINE,"shuttle_closed");
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
