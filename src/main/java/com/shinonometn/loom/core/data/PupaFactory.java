package com.shinonometn.loom.core.data;

import com.shinonometn.pupa.Pupa;
import com.shinonometn.pupa.tools.HexTools;
import com.shinonometn.pupa.tools.Pronunciation;

import java.net.DatagramPacket;

/**
 * Created by catten on 16/2/18.
 */
public class PupaFactory {
    public IClient getIClient() {
        return IClient;
    }

    public void setIClient(IClient IClient) {
        this.IClient = IClient;
    }

    private IClient IClient;

    public PupaFactory(IClient IClient) {
        this.IClient = IClient;
    }

    public Pupa knockPupa(){
        return new Pupa("get server", String.format(
                "session:%s|ip address:%s|mac address:%s",
                HexTools.byte2HexStr(IClient.getSession().getBytes()),
                HexTools.byte2HexStr(IClient.getIPAddress().getBytes()),
                HexTools.byte2HexStr(IClient.getMacAddress())
        ));
    }

    public Pupa certificatePupa(){
        return new Pupa("login",String.format(
                "session:%s|username:%s|password:%s|ip address:%s|mac address:%s|access point:%s|version:%s|is dhcp enabled:%s",
                HexTools.byte2HexStr(IClient.getSession().getBytes()),
                HexTools.byte2HexStr(IClient.getUsername().getBytes()),
                HexTools.byte2HexStr(IClient.getPassword().getBytes()),
                HexTools.byte2HexStr(IClient.getIPAddress().getBytes()),
                HexTools.byte2HexStr(IClient.getMacAddress()),
                HexTools.byte2HexStr("internet".getBytes()),
                HexTools.byte2HexStr(IClient.getVersion().getBytes()),
                "00"
        ));
    }

    public Pupa breathPupa(){
        return new Pupa("breathe", String.format(
                "session:%s|ip address:%s|serial no:0%x|mac address:%s",
                HexTools.byte2HexStr(IClient.getSession().getBytes()),
                HexTools.byte2HexStr(IClient.getIPAddress().getBytes()),
                IClient.getSerialNo(),
                HexTools.byte2HexStr(IClient.getMacAddress())
        ));
    }

    public Pupa logoutPupa(){
        return new Pupa("logout", String.format(
                "session:%s|ip address:%s|mac address:%s",
                HexTools.byte2HexStr(IClient.getSession().getBytes()),
                HexTools.byte2HexStr(IClient.getIPAddress().getBytes()),
                HexTools.byte2HexStr(IClient.getMacAddress())
        ));
    }

    public static Pupa serverPupa(DatagramPacket datagramPacket){
        byte[] data = new byte[datagramPacket.getLength()];
        System.arraycopy(Pronunciation.decrypt3848(datagramPacket.getData()), 0, data, 0, data.length);
        return new Pupa(data);
    }
}
