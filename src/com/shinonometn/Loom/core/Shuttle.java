package com.shinonometn.Loom.core;

import com.shinonometn.Loom.common.ConfigModule;
import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.core.Messenger.Messenger;
import com.shinonometn.Loom.core.Messenger.ShuttleEvent;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

import java.io.IOException;
import java.net.*;
import java.util.List;

/**
 * Created by catten on 15/11/2.
 */
public class Shuttle extends Thread{
    public boolean developerMode;
    //一些参数设置
    private final int defaultPacketSize = 1024;
    private final int defaultSocketTimeout = 10000;//默认超时时间10s，因为考虑到是内网环境所以时间设置得比较段

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
    private boolean[] state = new boolean[4];//存放状态用，0是敲门，1是认证成功，2是呼吸线程启动，3是信息线程启动
    private boolean logoutFlag = false;//提示程序下线
    private long sleepTime = 0;//呼吸等待时间
    private int serialNo = 0x01000003;//会话流水号，每次呼吸增加3

    Messenger messengerThread;//信息监听线程

    //用于发送通知的对象，一般是调用Shuttle的类，如果不设置的话就会使用默认的匿名类
    private ShuttleEvent shuttleEvent = new ShuttleEvent() {
        @Override
        public void onMessage(int messageType, String message) {
            Logger.log(String.format("[Message code %d]:%s",messageType,message));
        }
    };

    //为了资源重用，敲门、认证、呼吸这三个互斥的动作都用这个Socket
    private DatagramSocket datagramSocket;

    public Shuttle(NetworkInterface networkInterface,ShuttleEvent feedBackObject){
        this.networkInterface = networkInterface;
        if(feedBackObject != null) this.shuttleEvent = feedBackObject;
        setDaemon(true);

        //获得IP地址
        List<InterfaceAddress> interfaceAddressList = networkInterface.getInterfaceAddresses();
        for(InterfaceAddress iA:interfaceAddressList){
            if(iA.toString().contains(".")) {
                localInetAddress = iA.getAddress();
                if(!ConfigModule.isFakeMode()){
                    ipAddress = iA.getAddress().toString().replace("/", "");
                    Logger.log("Shuttle change IP to " + ipAddress);
                    try {
                        macAddress = networkInterface.getHardwareAddress();
                    } catch (SocketException e) {
                        Logger.error(e.getMessage());
                    }
                }else{
                    ipAddress = ConfigModule.fakeIP;
                    Logger.log("Shuttle changed fake IP to " + ipAddress);
                    macAddress = HexTools.hexStr2Bytes(ConfigModule.fakeMac);
                    Logger.log("Shuttle change fake mac to " + ConfigModule.fakeMac);
                }
            }
        }

        try {

            //获得Socket用于通信
            Logger.log("Try to get socket.");
            datagramSocket = new DatagramSocket(3848,localInetAddress);
            datagramSocket.setSoTimeout(defaultSocketTimeout);
            Logger.log("Get socket success.");
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_GET_SUCCESS, "get_connection_socket_success");
        } catch (SocketException e) {
            Logger.error("Get socket Failed." + e.getMessage());
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_PORT_IN_USE, "get_connection_socket_failed");
        }
    }

    public void run(){
        byte[] data;
        DatagramPacket datagramPacket;
        String fields;

        //敲门
        try {

            //准备字段
            fields = String.format(
                    "session:%s|ip address:%s|mac address:%s",
                    HexTools.byte2HexStr(init_session),
                    HexTools.byte2HexStr(ipAddress.getBytes()),
                    HexTools.byte2HexStr(macAddress)
            );

            //利用Pupa和Pronunciation生成加密好的数据
            data = Pronunciation.encrypt3848(new Pupa("get server", fields).getData());
            Logger.log("[Fields]"+(developerMode?fields:"----Banned----"));

            //准备数据包
            datagramPacket = new DatagramPacket(data,data.length, InetAddress.getByName("1.1.1.8"),3850);

            //开始敲门
            Logger.log("Knocking server.");
            datagramSocket.send(datagramPacket);
            Logger.log("Knocking data package sent.");
            datagramPacket.setData(new byte[defaultPacketSize]);
            datagramPacket.setLength(defaultPacketSize);

            //等待服务器回应
            Logger.log("Waiting server response.");
            datagramSocket.receive(datagramPacket);
            Logger.log("Server response.");

            //取出数据包并利用Pupa取出认证服务器ip
            data = new byte[datagramPacket.getLength()];
            System.arraycopy(Pronunciation.decrypt3848(datagramPacket.getData()), 0, data, 0, data.length);
            byte[] fieldBuffer = Pupa.fieldData(Pupa.findField(new Pupa(data), "server ip address"));
            serverIPAddress = HexTools.toIPAddress(fieldBuffer);

            if(!serverIPAddress.equals("")){
                Logger.log("Server IP is : "+serverIPAddress);
                shuttleEvent.onMessage(ShuttleEvent.SERVER_RESPONSE_IPADDRESS,serverIPAddress);
                try {
                    serverInetAddress = InetAddress.getByName(serverIPAddress);
                    //敲门成功
                    state[0] = true;
                } catch (UnknownHostException e) {
                    Logger.error("Server IP unavailable.");
                    shuttleEvent.onMessage(ShuttleEvent.SOCKET_UNKNOWN_HOST_EXCEPTION, "server_ip_unavailable");
                    return;
                }
            }else {
                Logger.error("Get server IP failed: Field empty.");
                shuttleEvent.onMessage(ShuttleEvent.SERVER_NOT_FOUNT,"knock_server_not_found");
                return;
            }

        } catch (SocketTimeoutException e) {
            Logger.error("Server no response.");
            shuttleEvent.onMessage(ShuttleEvent.SERVER_NO_RESPONSE, "knock_server");
            return;
        } catch (UnknownHostException e) {
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_UNKNOWN_HOST_EXCEPTION, "knock_server");
            return;
        } catch (IOException e) {
            if(e.getMessage().equals("No route to host")){
                shuttleEvent.onMessage(ShuttleEvent.SOCKET_NO_ROUTE_TO_HOST, "no_route_to_host");
                Logger.error(e.getMessage());
            } else {
                Logger.error("Unknown Exception. cause:" + e.getMessage());
                shuttleEvent.onMessage(ShuttleEvent.SOCKET_OTHER_EXCEPTION, "knocking");
            }
            return;
        }

        //登录

        try {
            //先检查登录信息是否为空
            if(username == null || password == null) {
                shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_FAILED,"info_not_filled");
                Logger.error("No certification information.");
                return;
            }

            //准备认证用字段，这个认证版本是安朗的3.6.9版协议
            Logger.log("Try to use account " + (developerMode?username:"-not-shown-") + " to login...");
            fields = String.format(
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
            Logger.log("[Fields]"+(developerMode?fields:"----Banned----"));
            data = Pronunciation.encrypt3848(new Pupa("login",fields).getData());
            datagramPacket = new DatagramPacket(data,data.length,serverInetAddress,3848);

            //发送数据包
            Logger.log("Sending certify package...");
            datagramSocket.send(datagramPacket);
            Logger.log("Certification package sent.");
            datagramPacket.setData(new byte[defaultPacketSize]);
            datagramPacket.setLength(defaultPacketSize);

            //等待服务器回应
            Logger.log("Waiting for server response.");
            datagramSocket.receive(datagramPacket);
            Logger.log("Server response.");
            data = new byte[datagramPacket.getLength()];
            System.arraycopy(datagramPacket.getData(), 0, data, 0, data.length);
            Pupa pupa = new Pupa(Pronunciation.decrypt3848(data));

            //判断是否登陆成功
            byte[] fieldBuffer = Pupa.fieldData(Pupa.findField(pupa, "is success"));
            if (fieldBuffer != null) {
                if (HexTools.toBool(fieldBuffer)) {
                    //认证成功
                    state[1] = true;
                    Logger.error("Certify success!");
                    shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_SUCCESS, "success");
                } else {
                    String message = HexTools.toGB2312Str(Pupa.fieldData(Pupa.findField(pupa, "message")));
                    shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_FAILED,message);
                    Logger.error("Certify failed, Infomation: " + message);
                    return;
                }
            } else {
                Logger.error("Unknow certificate statue");
                shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_EXCEPTION, "status_unsure");
                return;
            }

            //提取会话号
            fieldBuffer = Pupa.findField(pupa, "session");
            if(fieldBuffer != null){
                session = HexTools.toGB2312Str(Pupa.fieldData(fieldBuffer));
                Logger.log("Get session number: " + session);
            }else Logger.log("No server session number found.");

            //提取服务器信息
            fieldBuffer = Pupa.findField(pupa, "message");
            if(fieldBuffer != null){
                String message = HexTools.toGB2312Str(fieldBuffer);
                shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE, message);
                Logger.log("Server leave a message: " + message);
            }else Logger.log("Server no message leave.");

        } catch (SocketTimeoutException e){//等待服务器回应的时候超时
            shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_EXCEPTION, "timeout");
            Logger.error("Server no response");
            return;
        } catch (IOException e) {//IO 错误
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_OTHER_EXCEPTION, e.getMessage());
            Logger.error(e.getMessage());
            return;
        }

        //启动消息监听线程
        messengerThread = new Messenger(this.shuttleEvent,localInetAddress);
        messengerThread.start();

        //呼吸
        Logger.log("Breathe started.");
        sleepTime = 20000; //20s
        Logger.log("Set breathe time as " + sleepTime + "ms.");
        Pupa breathePupa;
        boolean noSleep = false;
        while(!logoutFlag){
            //Breathing flag
            state[2] = true;
            try {
                //如果被要求跳过等待, 直接发送呼吸包
                if(!noSleep){
                    Logger.log("Sleep for " + sleepTime + "ms");
                    sleep(sleepTime);
                }else
                    noSleep = false;

                //发送呼吸包
                fields = String.format(
                        "session:%s|ip address:%s|serial no:0%x|mac address:%s",
                        HexTools.byte2HexStr(session.getBytes()),
                        HexTools.byte2HexStr(ipAddress.getBytes()),
                        serialNo,
                        HexTools.byte2HexStr(macAddress)
                );
                Logger.log("[Field]"+(developerMode?fields:"----Banned----"));
                data = Pronunciation.encrypt3848(new Pupa("breathe", fields).getData());
                datagramPacket = new DatagramPacket(data, data.length, serverInetAddress, 3848);
                Logger.log("Breathe...");
                if(!logoutFlag) datagramSocket.send(datagramPacket); else break;
                Logger.log("Breathe package sent");

                //准备接收服务器的回应
                data = new byte[1024];
                datagramPacket.setData(data);
                datagramPacket.setLength(data.length);
                Logger.log("Waiting Server Response...");
                datagramSocket.setSoTimeout(defaultSocketTimeout);
                if(!logoutFlag) datagramSocket.receive(datagramPacket); else break;
                Logger.log("Server response.");

                //解释数据包并提取有用的信息
                data = new byte[datagramPacket.getLength()];
                System.arraycopy(datagramPacket.getData(), 0, data, 0, data.length);
                breathePupa = new Pupa(Pronunciation.decrypt3848(data));

                //分析
                byte[] fieldBuffer = Pupa.findField(breathePupa,"is success");
                if(fieldBuffer != null) {
                    if(HexTools.toBool(fieldBuffer)){
                        serialNo += 0x03;
                        Logger.log("Breathed." + (developerMode ? String.format("Serial No. : 0x%x",serialNo):"----Banned----"));
                        shuttleEvent.onMessage(ShuttleEvent.BREATHE_SUCCESS,"success");
                    }else{
                        Logger.log("Server Rejected this Breathe.");
                        shuttleEvent.onMessage(ShuttleEvent.BREATHE_FAILED, "rejected");
                    }
                }else if(Pupa.findField(breathePupa,"serial no") != null){
                    serialNo = 0x01000003;
                    shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION,"time_clear");
                }else{
                    shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION,"exception");
                }
            } catch (InterruptedException e) {
                break;
            } catch (SocketTimeoutException e) {
                Logger.log("Breathe timeout.");
                noSleep = true;
                shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION, "timeout");
            } catch (IOException e){
                Logger.error(e.toString());
                shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION, "exception");
                dispose();
                return;
            }
        }
        Logger.log("Breathe thread Closeing....");
        state[2] = false;

        //通知消息线程
        messengerThread.dispose();
        //Messenger closed
        state[3] = false;

        try {
            //准备下线数据包
            fields = String.format(
                    "session:%s|ip address:%s|mac address:%s",
                    HexTools.byte2HexStr(session.getBytes()),
                    HexTools.byte2HexStr(ipAddress.getBytes()),
                    HexTools.byte2HexStr(macAddress)
            );
            Logger.log("[Field]" + (developerMode ? fields : "----Banned----"));
            //发送数据包
            Pupa pupa = new Pupa("logout", fields);
            datagramPacket.setData(Pronunciation.encrypt3848(pupa.getData()));
            datagramPacket.setLength(pupa.getData().length);
            Logger.log("Telling Server.....");
            datagramSocket.send(datagramPacket);
            Logger.log("Logout package sent");

            //接收服务器返回的数据包
            byte[] buffer = new byte[1024];
            datagramPacket.setData(buffer);
            datagramPacket.setLength(buffer.length);
            datagramSocket.setSoTimeout(defaultSocketTimeout);
            Logger.log("Waiting server response.");
            datagramSocket.receive(datagramPacket);
            Logger.log("Server response.");
            byte[] bufferTemp;
            bufferTemp = new byte[datagramPacket.getLength()];
            System.arraycopy(datagramPacket.getData(), 0, bufferTemp, 0, bufferTemp.length);
            pupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
            if (HexTools.toBool(Pupa.findField(pupa, "is success"))) {
                Logger.log("Server response.Now you are offline.");
                shuttleEvent.onMessage(ShuttleEvent.OFFLINE, "generally");
            }
            Logger.log("Offline politely...");
        }catch (SocketTimeoutException w){
            Logger.error("Logout Timeout...");
            shuttleEvent.onMessage(ShuttleEvent.OFFLINE,"timeout");
        } catch (IOException e) {
            Logger.error(e.toString());
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_OTHER_EXCEPTION, e.toString());
        }finally {
            if(!datagramSocket.isClosed()) {
                //datagramSocket.disconnect();
                datagramSocket.close();
            }
        }
    }

    public boolean isBreathing(){
        return state[2];
    }

    public boolean isMessageListening() { return state[3]; }

    //敲门

    public void offline(){
        logoutFlag = true;
        messengerThread.dispose();
        interrupt();
    }

    @Deprecated
    public void dispose(){
        logoutFlag = true;
        interrupt();
        if(messengerThread != null){
            messengerThread.dispose();
            messengerThread = null;
            state[3] = false;
        }

        if(datagramSocket != null) {
            //datagramSocket.disconnect();
            if(!datagramSocket.isClosed()) datagramSocket.close();
            state[2] = false;
        }
        Logger.log("Disposing Shuttle.");
        shuttleEvent.onMessage(ShuttleEvent.OFFLINE, "shuttle_closed");
        System.gc();
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

    public int getSerialNo(){
        return serialNo;
    }

    public String getSessionNo(){
        return session;
    }
}
