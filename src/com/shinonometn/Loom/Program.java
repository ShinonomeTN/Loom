package com.shinonometn.Loom;

import com.shinonometn.Loom.common.Networks;
import com.shinonometn.Loom.ui.MainForm;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.Dictionary;
import com.shinonometn.Pupa.ToolBox.Pronunciation;
import com.shinonometn.Pupa.ToolBox.HexTools;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * Created by catten on 15/10/20.
 */
public class Program {
    private static boolean developerMode = false;

    private static void log(String loginfo){
        if(developerMode){
            System.out.println(String.format("[%s][LOG]%s", new Date().toString(),loginfo));
        }
    }

    public static void main(String[] args){
        if(args.length >= 2){
            if(args[1] != null && "-developerMode".toLowerCase().equals(args[1].toLowerCase())){
                developerMode = true;
            }
        }

        if(args.length > 0 && "-consoleMode".toLowerCase().equals(args[0].toLowerCase())){
            Loomv02();
        }else if(args.length == 0 || "-graphicMode".toLowerCase().equals(args[0].toLowerCase())){
            try{
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }catch (Exception e){
                e.printStackTrace();
            }
            MainForm lnkToMainform = new MainForm();

            Vector<NetworkInterface> nf = Networks.getNetworkInterfaces(false);
            if(nf != null) System.out.println("Network Interfaces found:");
            for(NetworkInterface n : nf){
                try {
                    System.out.printf("[%s]%n", n.getDisplayName());
                    List<InterfaceAddress> list = n.getInterfaceAddresses();
                    for(InterfaceAddress ia:list){
                        try{
                            System.out.println(ia.getAddress());
                        }catch (Exception e){
                            System.out.println("null");
                        }
                    }
                    System.out.println(HexTools.byte2HexStr(n.getHardwareAddress()));
                    //System.out.println();
                }catch (Exception e){
                    System.out.print("Null");
                }
                System.out.println("------------------------");
            }
        /*
        try {
            Shuttle shuttle = new Shuttle(nf.get(0));
            shuttle.SearchServer();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        //*/
        }
    }

    public static void Loomv02(){
        DatagramSocket datagramSocket = null;
        DatagramPacket datagramPacket;
        try{
            System.out.println("Welcome to use Loom v0.3!\n");
            String ip;
            String username;
            String password;
            byte[] session = new byte[]{0x00,0x00,0x00,0x01};
            String str_session;

            Thread breatheThread;
            Thread serverMessageThread;

            //StringBuilder sB_fields = new StringBuilder();

            //Scanner scanner = new Scanner(System.in);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Input your IP address:");
            ip = bufferedReader.readLine();
            System.out.println("Getting Network Interface with "+ip);
            InetAddress inetAddress = InetAddress.getByName(ip);
            InetAddress serverInetAddress;
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            byte[] macAddress = networkInterface.getHardwareAddress();
            if(macAddress == null){
                System.out.println("Network Interface Not Available.");
            }

            datagramSocket = new DatagramSocket(3848,inetAddress);

            String fields = String.format(
                    "session:%s|ip address:%s|mac address:%s",
                    HexTools.byte2HexStr(session),
                    HexTools.byte2HexStr(ip.getBytes()),
                    HexTools.byte2HexStr(macAddress)
            );
            Pupa pupa = new Pupa("get server",fields);
            System.out.println("Knocking Server...");
            log("Fields:" + fields);
            datagramPacket = new DatagramPacket(
                    Pronunciation.encrypt3848(pupa.getData()),
                    pupa.getData().length,
                    InetAddress.getByName("1.1.1.8"),
                    3850
            );
            datagramSocket.send(datagramPacket);
            byte[] buffer = new byte[1024];
            byte[] temp;
            datagramPacket.setData(buffer);
            datagramPacket.setLength(buffer.length);
            System.out.println("Listening form Server...");
            datagramSocket.setSoTimeout(5000);
            datagramSocket.receive(datagramPacket);
            System.out.println("Server responsed.");
            temp = new byte[datagramPacket.getLength()];
            System.arraycopy(datagramPacket.getData(), 0, temp, 0, temp.length);
            pupa = new Pupa(Pronunciation.decrypt3848(temp));
            log(Pupa.toPrintabelString(pupa));
            String serverIP = null;
            for(byte[] arr:pupa.getFields()){
                if(arr[0] == Dictionary.getByteKeyCode("server ip address")){
                    byte[] fieldbuffer = Pupa.fieldData(arr);
                    serverIP = String.format("%d.%d.%d.%d",fieldbuffer[0]&0xFF,fieldbuffer[1]&0xFF,fieldbuffer[2]&0xFF,fieldbuffer[3]&0xFF);
                }
            }
            if(serverIP != null){
                System.out.println("Found that Server IP is "+serverIP);
                serverInetAddress = InetAddress.getByName(serverIP);
                datagramPacket.setAddress(serverInetAddress);
                datagramPacket.setPort(3848);
            }else return;

            System.out.println("Username for supplicant:");
            username = bufferedReader.readLine();
            System.out.println("Password:");
            password = bufferedReader.readLine();

            fields = String.format(
                    "session:%s|username:%s|password:%s|ip address:%s|mac address:%s|access point:%s|version:%s|is dhcp enabled:%s",
                    HexTools.byte2HexStr(session),
                    HexTools.byte2HexStr(username.getBytes()),
                    HexTools.byte2HexStr(password.getBytes()),
                    HexTools.byte2HexStr(ip.getBytes()),
                    HexTools.byte2HexStr(macAddress),
                    HexTools.byte2HexStr("internet".getBytes()),
                    HexTools.byte2HexStr("3.6.9".getBytes()),
                    "00"
            );

            pupa = new Pupa("login",fields);
            log("Fields: " + fields);
            datagramPacket.setData(Pronunciation.encrypt3848(pupa.getData()));
            datagramPacket.setLength(pupa.getData().length);
            System.out.println("Sending certify package...");
            datagramSocket.send(datagramPacket);
            buffer = new byte[1024];
            datagramPacket.setData(buffer);
            datagramPacket.setLength(1024);
            System.out.println("Waiting Server Response...");
            datagramSocket.receive(datagramPacket);
            temp = new byte[datagramPacket.getLength()];
            System.arraycopy(datagramPacket.getData(),0,temp,0,temp.length);
            pupa = new Pupa(Pronunciation.decrypt3848(temp));
            byte[] fieldBuffer = Pupa.findField(pupa,"is success");
            if(!HexTools.toBool(fieldBuffer)){
                System.out.println("Certification failed.");
                log(Pupa.toPrintabelString(pupa));
                fieldBuffer = Pupa.findField(pupa,"message");
                byte fieldTemp[] = new byte[fieldBuffer.length - 2];
                System.arraycopy(fieldBuffer,0,fieldTemp,0,fieldTemp.length);
                System.out.println(HexTools.toGB2312Str(fieldTemp));
            }else if(HexTools.toBool(fieldBuffer)){
                System.out.println("Certification success.");
                log(Pupa.toPrintabelString(pupa));
                fieldBuffer = Pupa.findField(pupa, "session");
                str_session = HexTools.toGB2312Str(Pupa.fieldData(fieldBuffer));
                fieldBuffer = Pupa.findField(pupa, "message");
                System.out.println("|Server Administrator Message|");
                System.out.println(HexTools.toGB2312Str(Pupa.fieldData(fieldBuffer)));
                datagramSocket.close();
                System.out.println("Starting Breathe Thread.");
                breatheThread = new Thread(){
                    Pupa breathePupa;
                    public boolean isRun = true;
                    String tempFiled;
                    int serialNo = 0x01000003;
                    byte[] buffer;
                    byte[] bufferTemp;
                    DatagramPacket breathePacket;
                    DatagramSocket breathSocket = new DatagramSocket(3848,inetAddress);

                    public void run(){
                        System.out.println("Set breathe time as 20s.");
                        while(isRun){
                            try {
                                System.out.println("Sleep for 20s...");
                                sleep(20000);
                                System.out.println("Breathe...");
                                tempFiled = String.format(
                                        "session:%s|ip address:%s|serial no:0%x|mac address:%s",
                                        HexTools.byte2HexStr(str_session.getBytes()),
                                        HexTools.byte2HexStr(ip.getBytes()),
                                        serialNo,
                                        HexTools.byte2HexStr(macAddress)
                                );
                                breathePupa = new Pupa("breathe",tempFiled);
                                log(Pupa.toPrintabelString(breathePupa));
                                breathePacket = new DatagramPacket(
                                        Pronunciation.encrypt3848(breathePupa.getData()),
                                        breathePupa.getData().length,
                                        serverInetAddress,
                                        3848
                                );
                                breathSocket.send(breathePacket);
                                buffer = new byte[1024];
                                breathePacket.setData(buffer);
                                breathePacket.setLength(buffer.length);
                                System.out.println("Waiting Server Response...");
                                breathSocket.setSoTimeout(10000);
                                try{
                                    breathSocket.receive(breathePacket);
                                }catch (SocketTimeoutException e){
                                    try {
                                        System.out.println("Breathe timeout...Try again....");
                                        breathePacket.setData(Pronunciation.encrypt3848(breathePupa.getData()));
                                        breathePacket.setLength(breathePupa.getData().length);
                                        breathSocket.send(breathePacket);
                                        breathePacket.setData(buffer);
                                        breathePacket.setLength(buffer.length);
                                        breathSocket.receive(breathePacket);
                                    }catch (SocketTimeoutException ee){
                                        System.out.println("Breathe failed...Server no response.");
                                        throw new InterruptedException("I killed myself");
                                    }
                                }
                                bufferTemp = new byte[breathePacket.getLength()];
                                System.arraycopy(breathePacket.getData(),0,bufferTemp,0,bufferTemp.length);
                                breathePupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                                log(Pupa.toPrintabelString(breathePupa));
                                byte[] TfiledBuffer = Pupa.findField(breathePupa,"is success");
                                if(TfiledBuffer != null && HexTools.toBool(TfiledBuffer)) {
                                    serialNo += 0x03;
                                    System.out.println("Breathed.");
                                }else if(TfiledBuffer == null && Pupa.findField(breathePupa,"serial no") != null){
                                    if(serialNo == 0x01000003){
                                        serialNo = 0x0100000;
                                    }
                                    serialNo += 0x06;
                                }else{
                                    System.out.println("Server Rejected this Breathe.");
                                    //break;
                                }
                            } catch (InterruptedException e) {
                                System.out.println("Breathe thread Closeing....");
                                breathSocket.close();
                                break;
                            } catch (IOException e){
                                e.printStackTrace();
                                breathSocket.close();
                            }
                        }
                    }
                };
                breatheThread.setDaemon(true);
                breatheThread.start();
                System.out.println("Starting Server Message Thread.");
                serverMessageThread = new Thread(){
                    DatagramSocket messageSocket = new DatagramSocket(4999,inetAddress);
                    DatagramPacket messagePacket;
                    Pupa messagePupa;
                    public boolean isRun = true;
                    byte[] buffer = new byte[1024];
                    byte[] bufferTemp;

                    public void run(){
                        messagePacket = new DatagramPacket(buffer,buffer.length);
                        while (isRun){
                            try {
                                sleep(10);
                                System.out.println("Listening Server message...");
                                messageSocket.receive(messagePacket);
                                bufferTemp = new byte[messagePacket.getLength()];
                                System.arraycopy(messagePacket.getData(), 0, bufferTemp, 0, bufferTemp.length);
                                log(HexTools.byte2HexStr(bufferTemp));
                                messagePupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                                System.out.println("Server send you a " + Dictionary.actionNames(messagePupa.getAction()) + "packet.");
                                if(messagePupa.getAction() == 0x9){
                                    System.out.println("Server maybe wanna you go offline..........\nBut I will not stop myself till you close me ;P");
                                }
                                log(Pupa.toPrintabelString(messagePupa));
                            } catch (IOException e) {
                                e.printStackTrace();
                                messageSocket.close();
                                break;
                            } catch (InterruptedException e){
                                System.out.println("Message Thread Closing....");
                                messageSocket.close();
                                break;
                            }
                        }
                    }
                };
                serverMessageThread.setDaemon(true);
                serverMessageThread.start();
                System.out.println("All works are finished. if you want to go offline, please input \"exit\"");
                while(!"exit".equals(bufferedReader.readLine().toLowerCase()));
                System.out.println("Offline politely...");
                breatheThread.interrupt();
                serverMessageThread.interrupt();
                while (breatheThread.isAlive() || breatheThread.isAlive());
                datagramSocket = new DatagramSocket(3848,inetAddress);
                fields = String.format(
                        "session:%s|ip address:%s|mac address:%s",
                        HexTools.byte2HexStr(str_session.getBytes()),
                        HexTools.byte2HexStr(ip.getBytes()),
                        HexTools.byte2HexStr(macAddress)
                );
                log(fields);
                System.out.println("Telling Server.....");
                pupa = new Pupa("logout",fields);
                datagramPacket.setData(Pronunciation.encrypt3848(pupa.getData()));
                datagramPacket.setLength(pupa.getData().length);
                datagramSocket.send(datagramPacket);
                buffer = new byte[1024];
                datagramPacket.setData(buffer);
                datagramPacket.setLength(buffer.length);
                datagramSocket.setSoTimeout(10000);
                try {
                    datagramSocket.receive(datagramPacket);
                    byte[] bufferTemp;
                    bufferTemp = new byte[datagramPacket.getLength()];
                    System.arraycopy(datagramPacket.getData(),0,bufferTemp,0,bufferTemp.length);
                    pupa = new Pupa(Pronunciation.decrypt3848(bufferTemp));
                    log(Pupa.toPrintabelString(pupa));
                    if(HexTools.toBool(Pupa.findField(pupa, "is success"))){
                        System.out.println("Server response.Now you are offline.");
                    }
                }catch (SocketTimeoutException w){
                    System.out.println("Timeout...");
                }
                datagramSocket.close();
            }
        }catch (Exception e){
            System.out.println(e.toString());
        }finally {
            if(datagramSocket != null) datagramSocket.close();
        }
    }
}
