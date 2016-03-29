package com.shinonometn.Loom.core;

import com.shinonometn.Loom.Program;
import com.shinonometn.Loom.common.ConfigModule;
import com.shinonometn.Loom.common.Networks;
import com.shinonometn.Loom.core.Message.DefaultShuttleEvent;
import com.shinonometn.Loom.core.Message.Messenger;
import com.shinonometn.Loom.core.Message.ShuttleEvent;
import com.shinonometn.Loom.core.data.DataFactory;
import com.shinonometn.Loom.core.data.PupaFactory;
import com.shinonometn.Loom.core.data.ShuttleClient;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.HexTools;

import java.io.IOException;
import java.net.*;

/**
 * Created by catten on 15/11/2.
 */
public class Shuttle extends Thread implements ShuttleClient {

    //一些参数设置
    private static int defaultPacketSize = 1024;
    private static int defaultSocketTimeout = 10000;//默认超时时间10s，因为考虑到是内网环境所以时间设置得比较短

    //通信信息
    private String session = new String(new byte[]{0x00, 0x00, 0x00, 0x01});//登陆后拿到的会话号保存在这里
    private String username;
    private String password;
    private String ipAddress;
    private byte[] macAddress;

    private InetAddress serverInetAddress;
    private InetAddress localInetAddress;

    PupaFactory factory = new PupaFactory(this);

    private boolean logoutFlag = false;//提示程序下线

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    private long sleepTime = 0;//呼吸等待时间
    private int serialNo = 0x01000003;//会话流水号，每次呼吸增加3

    Messenger messengerThread;//信息监听线程

    //用于发送通知的对象，一般是调用Shuttle的类，如果不设置的话就会使用默认的
    private ShuttleEvent shuttleEvent;

    //为了资源重用，敲门、认证、呼吸这三个互斥的动作都用这个Socket
    private DatagramSocket datagramSocket;

    public Shuttle(DatagramSocket socket) throws SocketException {
        this(socket, new DefaultShuttleEvent());
    }

    public Shuttle(DatagramSocket socket, ShuttleEvent feedBackObject) throws SocketException {
        this.shuttleEvent = feedBackObject;
        setDaemon(true);

        //获得IP地址
        localInetAddress = socket.getInetAddress();
        //TODO 判断是否使用作弊资料应该交还给视图层做
        /*
        if (!ConfigModule.isFakeMode()) {
            ipAddress = localInetAddress.toString().replace("/", "");
            //macAddress = networkInterface.getHardwareAddress();
        } else {
            ipAddress = ConfigModule.fakeIP;
            macAddress = HexTools.hexStr2Bytes(ConfigModule.fakeMac);
        }
        */
        this.datagramSocket = socket;
    }

    private boolean knock() throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024, InetAddress.getByName("1.1.1.8"), 3850);

        //准备数据包
        datagramPacket.setData(DataFactory.encrypt(factory.knockPupa()));

        //开始敲门
        datagramSocket.send(datagramPacket);

        //等待服务器回应
        datagramSocket.receive(datagramPacket);


        //取出数据包并利用Pupa取出认证服务器ip
        String serverIPAddress = HexTools.toIPAddress(PupaFactory
                .serverPupa(datagramPacket)
                .findField("server ip address")
                .getValue()
        );

        if (!"".equals(serverIPAddress)) {
            shuttleEvent.onMessage(ShuttleEvent.SERVER_RESPONSE_IPADDRESS, serverIPAddress);
            serverInetAddress = InetAddress.getByName(serverIPAddress);
        } else {
            shuttleEvent.onMessage(ShuttleEvent.SERVER_NOT_FOUNT, "knock_server_not_found");
            return false;
        }

        return true;
    }

    private boolean certificate() throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024, serverInetAddress, 3848);

        //先检查登录信息是否为空
        if (username == null || password == null) {
            shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_FAILED, "info_not_filled");
            return false;
        }

        //准备认证用字段，这个认证版本是安朗的3.6.9版协议
        //准备数据包
        datagramPacket.setData(DataFactory.encrypt(factory.certificatePupa()));

        //发送数据包
        datagramSocket.send(datagramPacket);

        //等待服务器回应
        datagramSocket.receive(datagramPacket);
        Pupa receivedPupa = PupaFactory.serverPupa(datagramPacket);

        //判断是否登陆成功
        if (HexTools.toBool(receivedPupa.findField("is success").getData())) {
            shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_SUCCESS, "success");
        } else {
            String message = HexTools.toGB2312Str(receivedPupa.findField("message").getValue());
            shuttleEvent.onMessage(ShuttleEvent.CERTIFICATE_FAILED, message);
            return false;
        }

        //提取会话号
        session = HexTools.toGB2312Str(receivedPupa.findField("session").getValue());

        //提取服务器信息
        byte[] _field = receivedPupa.findField("message").getValue();
        if (_field != null) {
            shuttleEvent.onMessage(ShuttleEvent.SERVER_MESSAGE, HexTools.toGB2312Str(_field));
        }

        return true;
    }

    private boolean breathe() throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024, serverInetAddress, 3848);

        //发送呼吸包
        datagramPacket.setData(DataFactory.encrypt(factory.breathPupa()));
        datagramSocket.send(datagramPacket);

        //准备接收服务器的回应
        datagramSocket.setSoTimeout(defaultSocketTimeout);
        datagramSocket.receive(datagramPacket);

        //解释数据包并提取有用的信息
        Pupa receivedPupa = PupaFactory.serverPupa(datagramPacket);

        //分析
        byte[] _field = receivedPupa.findField("is success").getValue();
        if (_field != null) {
            if (HexTools.toBool(_field)) {
                serialNo += 0x03;
                shuttleEvent.onMessage(ShuttleEvent.BREATHE_SUCCESS, "success");
            } else {
                shuttleEvent.onMessage(ShuttleEvent.BREATHE_FAILED, "rejected");
            }
        } else if (receivedPupa.findField("serial no") != null) {
            serialNo = 0x01000003;
            shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION, "time_clear");
        } else {
            shuttleEvent.onMessage(ShuttleEvent.BREATHE_EXCEPTION, "exception");
        }

        return true;
    }

    private boolean logout() throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024, serverInetAddress, 3848);

        //准备下线数据包
        //发送数据包
        datagramPacket.setData(DataFactory.encrypt(factory.logoutPupa()));

        datagramSocket.send(datagramPacket);

        //接收服务器返回的数据包
        datagramSocket.setSoTimeout(defaultSocketTimeout);
        datagramSocket.receive(datagramPacket);

        Pupa receivedPupa = PupaFactory.serverPupa(datagramPacket);
        if (HexTools.toBool(receivedPupa.findField("is success").getValue())) {
            shuttleEvent.onMessage(ShuttleEvent.OFFLINE, "generally");
            return true;
        }

        return false;
    }

    public void run() {
        try {
            if (knock()) { //敲门
                if (certificate()) { //登录
                    //启动消息监听线程
                    messengerThread = new Messenger(this.shuttleEvent, localInetAddress);
                    messengerThread.start();
                    //呼吸
                    sleepTime = 20000; //20s
                    boolean noSleep = false;
                    while (!logoutFlag) {
                        try {
                            //如果被要求跳过等待, 直接发送呼吸包
                            if (noSleep) {
                                sleep(sleepTime);
                            }

                            noSleep = !breathe();

                        } catch (InterruptedException e) {
                            logoutFlag = true;
                            break;
                        }
                    }
                    //通知消息线程
                    messengerThread.close();
                    logout();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            shuttleEvent.onMessage(ShuttleEvent.SERVER_NOT_FOUNT,"knock_server");
        } finally {
            datagramSocket.close();
        }
    }

    public boolean isBreathing() {
        return currentThread().isAlive();
    }

    public boolean isMessageListening() {
        return messengerThread.isAlive();
    }

    //敲门

    public void offline() {
        logoutFlag = true;
        interrupt();
        if (!this.isBreathing()) {
            datagramSocket.close();
        }

    }

    //Shuttle Client 实现
    @Override
    public String getSession() {
        return session;
    }

    @Override
    public String getIPAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress){
        this.ipAddress = ipAddress;
    }

    @Override
    public byte[] getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(byte[] macAddress){
        this.macAddress = macAddress;
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

    @Override
    public String getVersion() {
        return "3.6.9";
    }

    @Override
    public int getSerialNo() {
        return serialNo;
    }

    public static void main(String[] args) {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName("en0");
            InetAddress inetAddress = Networks.getInetAddress(networkInterface);
            Shuttle shuttle = new Shuttle(new DatagramSocket(3848,inetAddress));
            shuttle.setIpAddress("192.168.0.100");
            shuttle.setMacAddress(networkInterface.getHardwareAddress());
            shuttle.setPassword("25803748");
            shuttle.setUsername("14601120234");
            shuttle.setDaemon(false);
            shuttle.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

}
