package com.shinonometn.Loom.connector;

import com.shinonometn.Loom.connector.Message.FeedbackMessage;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.CHexConvert;
import com.shinonometn.Pupa.ToolBox.Cypherbook;
import com.shinonometn.Pupa.ToolBox.HACKTools;
import com.shinonometn.Pupa.ToolBox.HexTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;
import java.util.Queue;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by catten on 15/10/21.
 */
public class Shuttle implements IFeedBack{
    private int session;
    public final static int STATE_INITED = -1;
    public final static int STATE_SEARCH_SERVER = 0;
    public final static int STATE_SEARCH_FOUND = 1;
    public final static int STATE_SUPPLICAT_SHAKING = 2;
    //public final static int STATE_SUPPLICAT_SUCCESS = 3;
    public final static int STATE_SUPPLICAT_FAILED = 4;
    public final static int STATE_ONLINE_BREATHING = 5;
    public final static int STATE_OFFLINE_BREATH_FAILED = 6;
    public final static int STATE_OFFLINE_SERVER_REJECT = 7;
    public final static int STATE_OFFLINE_USER_OFFLINE = 8;
    public final static int STATE_OFFLINE_BREATH_STOPED = 9;

    public final static int ACTION_SEARCH = 10;
    public final static int ACTION_SUPPLICANT = 20;
    public final static int ACTION_BREATH = 50;
    public final static int ACTION_OFFLINE = 80;

    private Vector<String> mailBox = new Vector<>();
    //private Vector<Integer> planList = new Vector<>();
    //private Queue<Integer> planList = new SynchronousQueue<>();

    private int session_no;
    private int serial_no;

    private byte[] clientMAC;
    private String clientAddress;
    private InetAddress clientInetAddress;
    private String serverAddress;

    private int state;
    private int waitTime = 10000;
    private int lastBreathTime;

    public boolean DeveloperMode = true;

    private DatagramSocket datagramSocket_communicator;

    public Shuttle(NetworkInterface networkInterface) throws SocketException {
        clientMAC = networkInterface.getHardwareAddress();
        InetAddress address;
        Enumeration<InetAddress> addressEnumeration = networkInterface.getInetAddresses();
        while(addressEnumeration.hasMoreElements()){
            address = addressEnumeration.nextElement();
            if(address.toString().contains(".")){
                clientInetAddress = address;
                clientAddress = address.toString().replace("/","");
                break;
            }
        }
        state = STATE_INITED;
    }

    private Pupa pupa;

    public void log(String string){
        if(DeveloperMode) System.out.println(string);
    }

    public void feedBackPackage(Pupa pupa,FeedbackMessage feedbackMessage){
        this.pupa = pupa;
        for(int[] arr:pupa.getFields()){
            if("server ip address".equals(Cypherbook.actionNames(arr[0]))){
                byte[] buffer = new byte[arr.length - 2];
                for(int i = 0; i < buffer.length; i++){
                    buffer[i] = (byte)arr[i + 2];
                }
                serverAddress = String.format("%x.%x.%x.%x",arr[0],arr[1],arr[2],arr[3]);
            }
        }
        log(Pupa.toPrintabelString(pupa));
    }

    SearchServer searchServer;
    public void SearchServer(){
        searchServer = new SearchServer(clientInetAddress,clientMAC,this);
        searchServer.start();
    }

    private class SearchServer extends Thread{
        private Pupa pupa;
        int[] session = new int[]{0x00,0x00,0x00,0x01};
        String IP;

        IFeedBack iFeedBack;
        DatagramSocket datagramSocket;
        public SearchServer(InetAddress address,byte[] MAC,IFeedBack feedbackObject){

            iFeedBack = feedbackObject;
            this.IP = address.toString().replace("/","");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.format(
                            "session:%s|ip address:%s|mac address:%s",
                            HexTool.hexBinToHexStr(session),
                            HexTool.hexBinToHexStr(HexTool.byteArrToIntArr(IP.getBytes())),
                            HexTool.hexBinToHexStr(HexTool.byteArrToIntArr(MAC)))
            );
            log(stringBuilder.toString());
            pupa = new Pupa("get server",stringBuilder.toString());
            log(Pupa.toPrintabelString(pupa));
            try {
                datagramSocket = new DatagramSocket(3848,address);
            } catch (SocketException e) {
                if(e.getClass() == NoRouteToHostException.class){
                    feedBackPackage(null,new FeedbackMessage(FeedbackMessage.SERVER_NO_ROUTE));
                }
            }
        }

        public void run(){
            DatagramPacket datagramPacket;
            try {
                System.out.println("Calling server");
                datagramPacket = new DatagramPacket(
                        HexTool.intArrToByteArr(HACKTools.encrypt3848(HexTool.intArrToByteArr(pupa.getData()))),
                        pupa.getData().length,
                        InetAddress.getByName("1.1.1.8"),
                        3850
                );
                datagramSocket.send(datagramPacket);
                System.out.println("Waiting for response");
            } catch (IOException e) {
                //if((() e))
                e.printStackTrace();
            }
            byte[] buffer = new byte[1024];
            datagramPacket = new DatagramPacket(buffer,buffer.length);
            try {
                datagramSocket.setSoTimeout(waitTime);
                datagramSocket.receive(datagramPacket);
                System.out.println("Server responsed");
                byte[] buffer2 = new byte[datagramPacket.getLength()];
                System.arraycopy(buffer, 0, buffer2, 0, buffer2.length);
                feedBackPackage(new Pupa(HACKTools.decrypt3848(buffer2)),new FeedbackMessage(FeedbackMessage.SERVER_RESPONSE));
            } catch (IOException e) {
                log(e.toString());
                if(e.getClass() == SocketTimeoutException.class) feedBackPackage(null,new FeedbackMessage(FeedbackMessage.SERVER_TIMEOUT));
            }
        }
    }

    public static void main(String[] args){
        DatagramSocket datagramSocket = null;
        DatagramPacket datagramPacket;
        try{
            String ip;
            //Scanner scanner = new Scanner(System.in);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Input your IP address:");
            ip = bufferedReader.readLine();
            System.out.println("Getting Network Interface with "+ip);

            InetAddress inetAddress = InetAddress.getByName(ip);
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            byte[] macAddress = networkInterface.getHardwareAddress();
            if(macAddress == null){
                System.out.println("Network Interface Not Available.");
            }

            datagramSocket = new DatagramSocket(3848,inetAddress);
            byte[] session = new byte[]{0x00,0x00,0x00,0x01};

            String fields = String.format(
                    "session:%s|ip address:%s|mac address:%s",
                    HexTool.hexBinToHexStr(HexTool.byteArrToIntArr(session)),
                    HexTool.hexBinToHexStr(HexTool.byteArrToIntArr(ip.getBytes())),
                    HexTool.hexBinToHexStr(HexTool.byteArrToIntArr(macAddress))
            );
            Pupa pupa = new Pupa("get server",fields);
            System.out.println(String.format("Knocking Server...\n[%s]",fields));
            datagramPacket = new DatagramPacket(
                    HexTool.intArrToByteArr(HACKTools.encrypt3848(HexTool.intArrToByteArr(pupa.getData()))),
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
            pupa = new Pupa(HACKTools.decrypt3848(temp));
            System.out.println(Pupa.toPrintabelString(pupa));
            String serverIP = null;
            for(int[] arr:pupa.getFields()){
                if(arr[0] == Cypherbook.getKeyCode("server ip address")){
                    serverIP = String.format("%d.%d.%d.%d",arr[2],arr[3],arr[4],arr[5]);
                }
            }
            if(serverIP != null){
                System.out.println("Found that Server IP is "+serverIP);
                datagramPacket.setAddress(InetAddress.getByName(serverIP));
                datagramPacket.setPort(3848);
            }else return;
            String[] datas;
            while (true){
                System.out.println("input your package data:\nformat:\n\taction<data fields\n\n");
                fields = bufferedReader.readLine();
                if(fields.equals("exit")) break;
                datas = fields.split("\\<");
                pupa = new Pupa(datas[0],datas[1]);
                buffer = HexTool.intArrToByteArr(pupa.getData());
                datagramPacket.setData(HexTool.intArrToByteArr(HACKTools.encrypt3848(buffer)));
                datagramPacket.setLength(buffer.length);
                datagramSocket.send(datagramPacket);
                System.out.println("Sending data...");
                buffer = new byte[1024];
                datagramPacket.setData(buffer);
                datagramPacket.setLength(buffer.length);
                System.out.println("Wating for Server Response...");
                datagramSocket.receive(datagramPacket);
                temp = new byte[datagramPacket.getLength()];
                System.arraycopy(datagramPacket.getData(),0,temp,0,temp.length);
                pupa = new Pupa(HACKTools.decrypt3848(temp));
                System.out.println(Pupa.toPrintabelString(pupa));
            }
        }catch (Exception e){
            System.out.println(e.toString());
        }finally {
            if(datagramSocket != null) datagramSocket.close();
        }
    }
}
