package com.shinonometn.loom.core;

import com.shinonometn.loom.Program;
import com.shinonometn.loom.core.data.*;
import com.shinonometn.loom.core.message.Messenger;
import com.shinonometn.pupa.Pupa;
import com.shinonometn.pupa.tools.HexTools;
import com.shinonometn.pupa.tools.Pronunciation;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import static java.lang.Thread.sleep;

/**
 * Created by catten on 16/5/28.
 */
public class Shuttle implements IClient{
    private Logger logger;
    private DatagramSocket socket;

    private String session = new String(new byte[]{0x00, 0x00, 0x00, 0x01});
    private String ipAddress;
    private byte[] macAddress;
    private String username;
    private String password;
    private int serialNo;

    private PupaFactory factory;


    public Shuttle(DatagramSocket datagramSocket){
        socket = datagramSocket;
        factory = new PupaFactory(this);
    }

    public void run(){
        DatagramPacket datagramPacket;
        Pupa pupa;

        byte[] data;
        String fields;
        String serverIPAddress;

        //敲门
        InetAddress serverInetAddress;
        try {

            //准备字段
            pupa = factory.knockPupa();

            //利用Pupa和Pronunciation生成加密好的数据
            data = DataFactory.encrypt(pupa);
            logger.debug(pupa.toString());

            //准备数据包
            datagramPacket = new DatagramPacket(data,data.length, InetAddress.getByName("1.1.1.8"),3850);

            //开始敲门
            logger.info("Knocking server.");
            socket.send(datagramPacket);
            logger.info("Knocking data package sent.");
            datagramPacket.setData(new byte[1024]);

            //等待服务器回应
            logger.info("Waiting server response.");
            socket.receive(datagramPacket);
            logger.info("Server response.");

            //取出数据包并利用Pupa取出认证服务器ip
            pupa = PupaFactory.serverPupa(datagramPacket);
            serverIPAddress = HexTools.toIPAddress(pupa.findField("server ip address").getValue());

            if(!serverIPAddress.equals("")){
                try {
                    logger.info("Server IP is : "+serverIPAddress);
                    serverInetAddress = InetAddress.getByName(serverIPAddress);
                } catch (UnknownHostException e) {
                    logger.error("Server IP unavailable.",e);
                    return;
                }
            }else {
                logger.error("Get server IP failed: Return a empty field.");
                return;
            }

        } catch (SocketTimeoutException e) {
            logger.error("Server no response.",e);
            socket.close();
            return;
        } catch (UnknownHostException e) {
            logger.error("Host unknown",e);
            socket.close();
            return;
        } catch (IOException e) {
            logger.error("Network IO Exception",e);
            socket.close();
            return;
        }

        //登录

        try {
            //先检查登录信息是否为空
            if(username == null || password == null) {
                logger.fatal("No certification information.");
                socket.close();
                return;
            }

            //准备认证用字段，这个认证版本是安朗的3.6.9版协议
            logger.debug("Try to use account " + username + " to login...");
            pupa = factory.certificatePupa();

            //准备数据包
            logger.debug(pupa.toString());
            data = DataFactory.encrypt(pupa);
            datagramPacket = new DatagramPacket(data,data.length, serverInetAddress,3848);

            //发送数据包
            logger.info("Sending certify package...");
            socket.send(datagramPacket);
            logger.info("Certification package sent.");
            datagramPacket.setData(new byte[1024]);

            //等待服务器回应
            logger.info("Waiting for server response.");
            socket.receive(datagramPacket);
            logger.info("Server response.");
            pupa = PupaFactory.serverPupa(datagramPacket);

            //判断是否登陆成功
            if (pupa.findField("is success") != null) {
                if (HexTools.toBool(pupa.findField("is success").getValue())) {
                    //认证成功
                    logger.info("Certify success!");
                    if(Program.isDeveloperMode()){
                        //提取会话号
                        if(pupa.findField("session") != null){
                            session = HexTools.toGB2312Str(pupa.findField("session").getData());
                            logger.info("Get session number: " + session);
                        }else logger.warn("No server session number found.");
                    }
                } else {
                    String message = HexTools.toGB2312Str(pupa.findField("message").getValue());
                    logger.error("Certify failed, Information: " + message);
                    socket.close();
                    return;
                }
            } else {
                logger.error("Unknown certificate statue");
                socket.close();
                return;
            }

            //提取服务器信息
            if(pupa.findField("message") != null){
                String message = HexTools.toGB2312Str(pupa.findField("message").getData());
                logger.info("Server: " + message);
            }else logger.info("Server leave no message.");

        } catch (SocketTimeoutException e){//等待服务器回应的时候超时
            logger.error("Server no response",e);
            socket.close();
            return;
        } catch (IOException e) {//IO 错误
            logger.error("Network IO Error",e);
            socket.close();
            return;
        }

        //启动消息监听线程
        messengerThread = new Messenger(this.shuttleEvent,localInetAddress);
        messengerThread.start();

        //呼吸
        logger.info("Breathe started.");
        int sleepTime = 20000; //20s
        logger.info("Set breathe time as " + sleepTime + "ms.");
        boolean noSleep = false;
        //If should logout
        boolean logoutFlag = false;
        //Error count
        int breathError = 0;
        while(!logoutFlag){

            try {
                socket.setSoTimeout(10000);
                //如果被要求跳过等待, 直接发送呼吸包
                if(!noSleep){
                    logger.info("Sleep for " + sleepTime + "ms");
                    sleep(sleepTime);
                }else
                    noSleep = false;

                //发送呼吸包
                pupa = factory.breathPupa();
                logger.debug(pupa.toString());
                data = DataFactory.encrypt(pupa);
                logger.info("Breathe...");
                socket.send(datagramPacket);
                logger.info("Breathe package sent");

                //准备接收服务器的回应
                data = new byte[1024];
                datagramPacket.setData(data);
                datagramPacket.setLength(data.length);
                logger.info("Waiting Server Response...");
                socket.receive(datagramPacket);
                logger.info("Server response.");

                //解释数据包并提取有用的信息
                pupa = PupaFactory.serverPupa(datagramPacket);

                //分析
                if(pupa.findField("is success") != null) {
                    if(HexTools.toBool(pupa.findField("is success").getValue())){
                        serialNo += 0x03;
                        logger.info("Breathed.");
                        logger.debug(String.format("Serial No : 0x%x",serialNo));
                        breathError = 0;
                    }else{
                        breathError++;
                        logger.info("Server Rejected this Breathe.");
                        if(breathError <= 10){
                            logger.info("Breath was retried 10 times. Abandon.");
                            return;
                        }
                    }
                }else if(pupa.findField("serial no") != null){
                    serialNo = 0x01000003;
                }
            } catch (InterruptedException e) {
                logoutFlag = true;
                break;
            } catch (SocketTimeoutException e) {
                logger.warn("Breathe timeout.",e);
                noSleep = true;
            } catch (IOException e){
                logger.error("Unknown Exception",e);
                return;
            }
        }
        logger.warn("Breathe Closing....");
        //state[2] = false;

        //通知消息线程
        messengerThread.close();
        //Messenger closed
        //state[3] = false;

        try {
            logger.info("Offline politely...");
            //准备下线数据包
            pupa = factory.logoutPupa();
            logger.debug(pupa.toString());
            //发送数据包
            datagramPacket.setData(DataFactory.encrypt(pupa));
            logger.info("Telling server logout.....");
            socket.send(datagramPacket);
            logger.info("Logout package sent");

            //接收服务器返回的数据包
            datagramPacket.setData(new byte[1024]);
            socket.setSoTimeout(10000);
            logger.info("Waiting server response.");
            socket.receive(datagramPacket);
            logger.info("Server response.");
            pupa = PupaFactory.serverPupa(datagramPacket);
            if (HexTools.toBool(pupa.findField("is success").getValue())) {
                logger.info("Server response. Now you are offline.");
            }
        }catch (SocketTimeoutException w){
            logger.warn("Logout Timeout...");
        } catch (IOException e) {
            logger.error("Network IO Exception",e);
        }finally {
            socket.close();
        }
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

            shuttle = new Shuttle(new DatagramSocket(3848,inetAddress));
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

    @Override
    public String getSession() {
        return session;
    }

    @Override
    public String getIPAddress() {
        return ipAddress;
    }

    @Override
    public byte[] getMacAddress() {
        return macAddress;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return username;
    }

    @Override
    public String getVersion() {
        return "3.6.9";
    }

    @Override
    public int getSerialNo() {
        return serialNo;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setMacAddress(byte[] macAddress) {
        this.macAddress = macAddress;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
