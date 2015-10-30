package com.shinonometn.Loom.connector;

import com.shinonometn.Loom.connector.base.NetworkFeedback;
import com.shinonometn.Loom.connector.base.UDPRecive;
import com.shinonometn.Loom.connector.base.UDPSend;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.HexTool;

import java.net.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Created by catten on 15/10/21.
 */
public class Shuttle implements NetworkFeedback{
    private int session;

    public final static int STATE_NOT_INIT = -3;
    public final static int STATE_INITED_FAILED = -2;
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

    private UDPSend udpSend;
    private UDPRecive udpRecive;

    public boolean DeveloperMode = true;

    private DatagramSocket datagramSocket_communicator;
    //private ThreadPool threadPool;

    private void setState(int state){
        this.state = state;
        if(eventFeedbackObject != null){
            eventFeedbackObject.onStateChange(state);
        }
    }

    ShuttleEvent eventFeedbackObject;

    public void setFeedBackObject(ShuttleEvent shuttleEvent){
        this.eventFeedbackObject = shuttleEvent;
    }

    public Shuttle(NetworkInterface networkInterface){
        this();
        setNetworkInterface(networkInterface);
    }

    public Shuttle(){
        setState(STATE_NOT_INIT);
    }

    public boolean setNetworkInterface(NetworkInterface networkInterface){
        try {
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
            datagramSocket_communicator = new DatagramSocket(3848,clientInetAddress);
        } catch (SocketException e) {
            setState(STATE_INITED_FAILED);
            return false;
        }
        setState(STATE_INITED);

        udpRecive = new UDPRecive(datagramSocket_communicator);
        udpRecive.setFeedbackObject(this);
        udpSend = new UDPSend(datagramSocket_communicator);
        udpSend.setFeedBackObject(this);
        return true;
    }


    public void log(String string){
        if(DeveloperMode) System.out.println(string);
    }
/*
    private Pupa pupa;
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
//*/
    @Override
    public void onMessage(String message) {
        log("[Message]\t" + message);

        if(eventFeedbackObject != null) eventFeedbackObject.onMail(message);
    }

    @Override
    public void onException(Exception e) {
        log("[Exception]\t" + e.toString());
    }

    @Override
    public void onPackageRecived(DatagramPacket datagramPacket) {
        //THINGS BEFORE ACTION
        byte[] buffer = new byte[datagramPacket.getLength()];
        System.arraycopy(datagramPacket.getData(),0,buffer,0,buffer.length);
        Pupa pupa = new Pupa(HexTool.byteArrToIntArr(buffer));
        switch (pupa.getAction()){
            case 13:{
                for(int[] field :pupa.getFields()){
                    switch (field[0]){
                        case 0x0c:
                            serverAddress = String.format("%s.%s.%s.%s",field[2],field[3],field[4],field[5]);
                    }
                }
            }
            default:
                break;
        }
        if(eventFeedbackObject != null){
            eventFeedbackObject.onDatagramPackageArrive(datagramPacket);
        }
    }

    public void startSearchServer(){
        int[] session = new int[]{0x00,0x00,0x00,0x01};
        String targetIP = "1.1.1.8";

        DatagramPacket datagramPacket;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format(
                        "session:%s|ip address:%s|mac address:%s",
                        HexTool.toHexStr(session),
                        HexTool.toHexStr(HexTool.byteArrToIntArr(clientAddress.getBytes())),
                        HexTool.toHexStr(HexTool.byteArrToIntArr(clientMAC))
                )
        );
        log(stringBuilder.toString());
        Pupa pupa = new Pupa("get server",stringBuilder.toString());
        log(Pupa.toPrintabelString(pupa));
        try {
            datagramPacket = new DatagramPacket(
                    HexTool.intArrToByteArr(pupa.getData()),
                    pupa.getData().length,
                    InetAddress.getByName(targetIP),
                    3850
            );
            eventFeedbackObject.onAction(ACTION_SEARCH);
            eventFeedbackObject.onStateChange(STATE_SEARCH_SERVER);
            udpSend.send(datagramPacket);
        } catch (UnknownHostException e) {
            eventFeedbackObject.onMail(e.toString());
        }
    }

/*
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
                            HexTool.toHexStr(session),
                            HexTool.toHexStr(HexTool.byteArrToIntArr(IP.getBytes())),
                            HexTool.toHexStr(HexTool.byteArrToIntArr(MAC)))
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
    //*/
}
