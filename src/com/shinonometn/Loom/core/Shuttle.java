package com.shinonometn.loom.core;

import com.shinonometn.loom.Program;
import com.shinonometn.loom.common.ConfigModule;
import com.shinonometn.loom.core.message.Messenger;
import com.shinonometn.loom.core.message.ShuttleEvent;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;
import com.sun.javafx.tools.packager.Log;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Created by catten on 15/11/2.
 */
public class Shuttle extends Thread{

    private static Logger logger = Logger.getLogger("shuttle");

    //一些参数设置
    private static int defaultPacketSize = 1024;
    private static int defaultSocketTimeout = 10000;//默认超时时间10s，因为考虑到是内网环境所以时间设置得比较短

    private NetworkInterface networkInterface;

    //通信信息
    private String username;
    private String password;
    private byte[] init_session = new byte[]{0x00,0x00,0x00,0x01};//初始session
    private String session;//登陆后拿到的会话号保存在这里

    //一些经常用到的信息暂存在这里
    private String ipAddress;
    private String serverIPAddress;
    private byte[] macAddress;
    private InetAddress serverInetAddress;
    private InetAddress localInetAddress;

    public final static int STATE_INITIALIZED = 0x00;
    public final static int STATE_KNOCKING = 0x01;
    public final static int STATE_SUPPLICATING = 0x02;
    public final static int STATE_BREATHING = 0x04;
    public final static int STATE_MESSAGEON = 0x08;

    public int state = -1;

    //private boolean[] state = new boolean[4];//存放状态用，0是敲门，1是认证成功，2是呼吸线程启动，3是信息线程启动
    private boolean logoutFlag = false;//提示程序下线
    private long sleepTime = 0;//呼吸等待时间
    private int serialNo = 0x01000003;//会话流水号，每次呼吸增加3

    Messenger messengerThread;//信息监听线程

    //备胎
    private ShuttleEvent spareEventObject = new ShuttleEvent() {
        @Override
        public void onMessage(int messageType, String message) {
            logger.info(String.format("[Message code %d]:%s",messageType,message));
        }

        @Override
        public void onNetworkError(int errorType, String message) {
            logger.error(String.format("[Error code %d]:%s",errorType,message));
        }


    };
    //用于发送通知的对象，一般是调用Shuttle的类，如果不设置的话就会使用默认的
    private ShuttleEvent shuttleEvent = spareEventObject;

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
                    logger.info("Shuttle change IP to " + ipAddress);
                    try {
                        macAddress = networkInterface.getHardwareAddress();
                    } catch (SocketException e) {
                        logger.error(e);
                    }
                }else{
                    ipAddress = ConfigModule.fakeIP;
                    logger.info("Shuttle changed fake IP to " + ipAddress);
                    macAddress = HexTools.hexStr2Bytes(ConfigModule.fakeMac);
                    logger.info("Shuttle change fake mac to " + ConfigModule.fakeMac);
                }
            }
        }

        try {

            //获得Socket用于通信
            logger.info("Try to get socket.");
            datagramSocket = new DatagramSocket(3848,localInetAddress);
            datagramSocket.setSoTimeout(defaultSocketTimeout);
            logger.info("Get socket success.");
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_GET_SUCCESS, "get_connection_socket_success");
            state = STATE_INITIALIZED;
        } catch (SocketException e) {
            logger.error("Get socket Failed.",e);
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
            logger.debug(fields);

            //准备数据包
            datagramPacket = new DatagramPacket(data,data.length, InetAddress.getByName("1.1.1.8"),3850);

            //开始敲门
            logger.info("Knocking server.");
            datagramSocket.send(datagramPacket);
            logger.info("Knocking data package sent.");
            datagramPacket.setData(new byte[defaultPacketSize]);
            datagramPacket.setLength(defaultPacketSize);

            //等待服务器回应
            logger.info("Waiting server response.");
            datagramSocket.receive(datagramPacket);
            logger.info("Server response.");

            //取出数据包并利用Pupa取出认证服务器ip
            data = new byte[datagramPacket.getLength()];
            System.arraycopy(Pronunciation.decrypt3848(datagramPacket.getData()), 0, data, 0, data.length);
            //byte[] fieldBuffer = Pupa.(Pupa.findField(new Pupa(data), "server ip address"));
            byte[] fieldBuffer = new Pupa(data).findField("server ip address").getValue();
            serverIPAddress = HexTools.toIPAddress(fieldBuffer);

            if(!serverIPAddress.equals("")){
                logger.info("Server IP is : "+serverIPAddress);
                shuttleEvent.onMessage(ShuttleEvent.SERVER_RESPONSE_IPADDRESS,serverIPAddress);
                try {
                    serverInetAddress = InetAddress.getByName(serverIPAddress);
                    //敲门成功
                    //state[0] = true;
                } catch (UnknownHostException e) {
                    logger.error("Server IP unavailable.",e);
                    shuttleEvent.onMessage(ShuttleEvent.SOCKET_UNKNOWN_HOST_EXCEPTION, "server_ip_unavailable");
                    return;
                }
            }else {
                logger.error("Get server IP failed: Field empty.");
                shuttleEvent.onMessage(ShuttleEvent.SERVER_NOT_FOUNT,"knock_server_not_found");
                return;
            }

        } catch (SocketTimeoutException e) {
            logger.error("Server no response.",e);
            shuttleEvent.onMessage(ShuttleEvent.SERVER_NO_RESPONSE, "knock_server");
            datagramSocket.close();
            return;
        } catch (UnknownHostException e) {
            logger.error("Host unknown",e);
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_UNKNOWN_HOST_EXCEPTION, "knock_server");
            datagramSocket.close();
            return;
        } catch (IOException e) {
            if(e.getMessage().equals("No route to host")){
                shuttleEvent.onMessage(ShuttleEvent.SOCKET_NO_ROUTE_TO_HOST, "no_route_to_host");
                logger.error("No route to host", e);
            } else {
                logger.error("Unknown Exception.",e);
                shuttleEvent.onMessage(ShuttleEvent.SOCKET_OTHER_EXCEPTION, "knocking");
            }
            datagramSocket.close();
            return;
        }

        //登录

        try {
            //先检查登录信息是否为空
            if(username == null || password == null) {
                shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_FAILED,"info_not_filled");
                logger.fatal("No certification information.");
                datagramSocket.close();
                return;
            }

            //准备认证用字段，这个认证版本是安朗的3.6.9版协议
            logger.debug("Try to use account " + username + " to login...");
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
            logger.debug(fields);
            data = Pronunciation.encrypt3848(new Pupa("login",fields).getData());
            datagramPacket = new DatagramPacket(data,data.length,serverInetAddress,3848);

            //发送数据包
            logger.info("Sending certify package...");
            datagramSocket.send(datagramPacket);
            logger.info("Certification package sent.");
            datagramPacket.setData(new byte[defaultPacketSize]);
            datagramPacket.setLength(defaultPacketSize);

            //等待服务器回应
            logger.info("Waiting for server response.");
            datagramSocket.receive(datagramPacket);
            logger.info("Server response.");
            data = new byte[datagramPacket.getLength()];
            System.arraycopy(datagramPacket.getData(), 0, data, 0, data.length);
            Pupa pupa = new Pupa(Pronunciation.decrypt3848(data));

            //判断是否登陆成功
            //byte[] fieldBuffer = Pupa.fieldData(Pupa.findField(pupa, "is success"));
            byte[] fieldBuffer = pupa.findField("is success").getValue();
            if (fieldBuffer != null) {
                if (HexTools.toBool(fieldBuffer)) {
                    //认证成功
                    //state[1] = true;
                    logger.info("Certify success!");
                    if(Program.isDeveloperMode()){
                        //提取会话号
                        fieldBuffer = pupa.findField("session").getValue();
                        if(fieldBuffer != null){
                            session = HexTools.toGB2312Str(fieldBuffer);
                            logger.info("Get session number: " + session);
                        }else logger.warn("No server session number found.");
                    }
                    shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_SUCCESS, "success");
                } else {
                    String message = HexTools.toGB2312Str(pupa.findField("message").getValue());
                    shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_FAILED,message);
                    logger.error("Certify failed, Infomation: " + message);
                    datagramSocket.close();
                    return;
                }
            } else {
                logger.error("Unknow certificate statue");
                shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_EXCEPTION, "status_unsure");
                datagramSocket.close();
                return;
            }

            //提取会话号
            if(!Program.isDeveloperMode()){
                //fieldBuffer = Pupa.findField(pupa, "session");
                fieldBuffer = pupa.findField("session").getValue();
                if(fieldBuffer != null){
                    session = HexTools.toGB2312Str(fieldBuffer);
                    logger.info("Get session number: " + session);
                }else logger.info("No server session number found.");
            }

            //提取服务器信息
            fieldBuffer = pupa.findField("message").getValue();
            if(fieldBuffer != null){
                String message = HexTools.toGB2312Str(fieldBuffer);
                shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE, message);
                logger.info("Server leave a message: " + message);
            }else logger.info("Server no message leave.");

        } catch (SocketTimeoutException e){//等待服务器回应的时候超时
            shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_EXCEPTION, "timeout");
            logger.error("Server no response",e);
            datagramSocket.close();
            return;
        } catch (IOException e) {//IO 错误
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_OTHER_EXCEPTION, e.getMessage());
            logger.error("Unknown Error",e);
            datagramSocket.close();
            return;
        }

        //启动消息监听线程
        messengerThread = new Messenger(this.shuttleEvent,localInetAddress);
        messengerThread.start();

        //呼吸
        logger.info("Breathe started.");
        sleepTime = 20000; //20s
        logger.info("Set breathe time as " + sleepTime + "ms.");
        Pupa breathePupa;
        boolean noSleep = false;
        while(!logoutFlag){
            //Breathing flag
            //state[2] = true;
            try {
                //如果被要求跳过等待, 直接发送呼吸包
                if(!noSleep){
                    logger.info("Sleep for " + sleepTime + "ms");
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
                logger.debug(fields);
                data = Pronunciation.encrypt3848(new Pupa("breathe", fields).getData());
                datagramPacket = new DatagramPacket(data, data.length, serverInetAddress, 3848);
                logger.info("Breathe...");
                if(!logoutFlag) datagramSocket.send(datagramPacket); else break;
                logger.info("Breathe package sent");

                //准备接收服务器的回应
                data = new byte[1024];
                datagramPacket.setData(data);
                datagramPacket.setLength(data.length);
                logger.info("Waiting Server Response...");
                datagramSocket.setSoTimeout(defaultSocketTimeout);
                if(!logoutFlag) datagramSocket.receive(datagramPacket); else break;
                logger.info("Server response.");

                //解释数据包并提取有用的信息
                data = new byte[datagramPacket.getLength()];
                System.arraycopy(datagramPacket.getData(), 0, data, 0, data.length);
                breathePupa = new Pupa(Pronunciation.decrypt3848(data));

                //分析
                byte[] fieldBuffer = breathePupa.findField("is success").getValue();
                if(fieldBuffer != null) {
                    if(HexTools.toBool(fieldBuffer)){
                        serialNo += 0x03;
                        logger.info("Breathed." + (Program.isDeveloperMode() ? String.format("Serial No. : 0x%x",serialNo):"----Banned----"));
                        shuttleEvent.onMessage(ShuttleEvent.BREATHE_SUCCESS,"success");
                    }else{
                        logger.info("Server Rejected this Breathe.");
                        shuttleEvent.onMessage(ShuttleEvent.BREATHE_FAILED, "rejected");
                    }
                }else if(breathePupa.findField("serial no") != null){
                    serialNo = 0x01000003;
                    shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION,"time_clear");
                }else{
                    shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION,"exception");
                }
            } catch (InterruptedException e) {
                logoutFlag = true;
                break;
            } catch (SocketTimeoutException e) {
                logger.warn("Breathe timeout.",e);
                noSleep = true;
                shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION, "timeout");
            } catch (IOException e){
                logger.error("Unknown Exception",e);
                shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION, e.getMessage());
                offline();
                return;
            }
        }
        logger.warn("Breathe thread Closeing....");
        //state[2] = false;

        //通知消息线程
        messengerThread.close();
        //Messenger closed
        //state[3] = false;

        try {
            //准备下线数据包
            fields = String.format(
                    "session:%s|ip address:%s|mac address:%s",
                    HexTools.byte2HexStr(session.getBytes()),
                    HexTools.byte2HexStr(ipAddress.getBytes()),
                    HexTools.byte2HexStr(macAddress)
            );
            logger.debug(fields);
            //发送数据包
            Pupa pupa = new Pupa("logout", fields);
            datagramPacket.setData(Pronunciation.encrypt3848(pupa.getData()));
            datagramPacket.setLength(pupa.getData().length);
            logger.info("Telling Server.....");
            datagramSocket.send(datagramPacket);
            logger.info("Logout package sent");

            //接收服务器返回的数据包
            byte[] buffer = new byte[1024];
            datagramPacket.setData(buffer);
            datagramPacket.setLength(buffer.length);
            datagramSocket.setSoTimeout(defaultSocketTimeout);
            logger.info("Waiting server response.");
            datagramSocket.receive(datagramPacket);
            logger.info("Server response.");
            byte[] bufferTemp;
            bufferTemp = new byte[datagramPacket.getLength()];
            System.arraycopy(datagramPacket.getData(), 0, bufferTemp, 0, bufferTemp.length);
            pupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
            if (HexTools.toBool(pupa.findField("is success").getValue())) {
                logger.info("Server response.Now you are offline.");
                shuttleEvent.onMessage(ShuttleEvent.OFFLINE, "generally");
            }
            logger.info("Offline politely...");
        }catch (SocketTimeoutException w){
            logger.warn("Logout Timeout...");
            shuttleEvent.onMessage(ShuttleEvent.OFFLINE,"timeout");
        } catch (IOException e) {
            logger.error("Unknown Exception",e);
            shuttleEvent.onMessage(ShuttleEvent.SOCKET_OTHER_EXCEPTION, e.toString());
        }finally {
            datagramSocket.close();
        }
    }

    public boolean isBreathing(){
        return currentThread().isAlive();
    }

    public boolean isMessageListening(){
        return messengerThread.isAlive();
    }

    //敲门

    public void offline(){
        logoutFlag = true;
        interrupt();
        if(!this.isBreathing()){
            datagramSocket.close();
        }
        //if(!datagramSocket.isClosed()) datagramSocket.close();
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

    public static void LoomConsole(String args){
        try {
            System.out.println("Welcome to use Loom v2.2 Console!\n");

            String ip;
            final String username;
            final String password;

            final Shuttle shuttle;

            Scanner scanner = new Scanner(System.in);

            if(args != null){
                System.out.print("Loom now running under pre-fix mode.");
                ip = args;
                username = null;
                password = null;
            }else{
                System.out.println("Input your IP address:");
                ip = scanner.next();
                System.out.println("Please input your account");
                username = scanner.next();
                System.out.println("PIN Code?(Password)");
                password = scanner.next();
            }

            System.out.println("Getting Network Interface with " + ip);
            InetAddress inetAddress = InetAddress.getByName(ip);
            final NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            byte[] macAddress = networkInterface.getHardwareAddress();
            if (macAddress == null) {
                System.out.println("Network Interface Not Available...Exit.");
                return;
            }

            shuttle = new Shuttle(networkInterface, null);
            if(ConfigModule.isFakeMode()) logger.info("Please remember that fake mode on.");
            if(ConfigModule.allowAutoMode()) logger.info("Please remember that auto-mode on.");

            System.out.println("Prepared to login.");

            if(!ConfigModule.allowAutoMode()){
                if(args == null){
                    shuttle.setUsername(username);
                    shuttle.setPassword(password);
                }else{
                    shuttle.setUsername(ConfigModule.username);
                    shuttle.setPassword(ConfigModule.password);
                }
                System.out.println("Loom Start.");
                shuttle.start();

                //scan for exit
                do{
                    System.out.println("If you want to get offline or exit program, Please input \"exit\"");
                    if (!scanner.next().toLowerCase().equals("exit")){
                        if(scanner.next().equals("about")){
                            Program.aboutMe();
                        }else System.out.println("If you want to get offline or exit program, Please input \"exit\"");
                    }else{
                        shuttle.offline();
                        while(shuttle.isBreathing() || shuttle.isMessageListening());
                        return;
                    }
                }while(shuttle.isAlive());

                if(!Program.isDeveloperMode()){
                    System.out.println(
                            "Some error accorded. Please restart program, or enable developer mode to know more."
                    );
                }

            }else{
                shuttle.datagramSocket.close();
                shuttle.offline();
                Thread thread = new Thread(){
                    boolean alertFlag = false;
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                    boolean runflag = true;
                    Shuttle shuttle1;
                    public void run(){
                        logger.info("Loom running under auto-mode.");
                        while(runflag){
                            String date = simpleDateFormat.format(new Date());
                            logger.debug("Time check. " + date);
                            if(shuttle1 == null){
                                if(ConfigModule.autoOnlineMode.equals("both") || ConfigModule.autoOnlineMode.equals("online")){
                                    if(date.equals(ConfigModule.autoOnlineTime)){
                                        if(!alertFlag){
                                            shuttle1 = new Shuttle(networkInterface,null);
                                            shuttle1.setUsername(ConfigModule.username);
                                            shuttle1.setPassword(ConfigModule.password);
                                            shuttle1.start();
                                            alertFlag = true;
                                            logger.info("Auto online because reach the online time point.");
                                        }
                                    }else alertFlag = false;
                                }
                            }else{
                                if(ConfigModule.autoOnlineMode.equals("both") || ConfigModule.autoOfflineTime.equals("offline")){
                                    if(date.equals(ConfigModule.autoOfflineTime)){
                                        if(!alertFlag){
                                            if(shuttle1 != null) shuttle1.offline();
                                            shuttle1 = null;
                                            alertFlag = true;
                                            logger.info("Auto offline because reach the offline time point.");
                                        }
                                    }else alertFlag = false;
                                }
                            }
                            try {
                                sleep(10000);
                            } catch (InterruptedException e) {
                                runflag = false;
                            }
                        }
                        shuttle1.offline();
                    }
                };
                thread.setDaemon(true);
                thread.start();
                do{
                    System.out.println("If you want to exit. please input \"exit\"");
                }
                while(!scanner.next().equals("exit"));
                thread.interrupt();
            }
        }catch (SocketException | UnknownHostException e){
            logger.error("Unknown exception",e);
        }
    }
}
