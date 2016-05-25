package com.shinonometn.loom.core.data;

import com.shinonometn.loom.core.Shuttle;
import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

import java.net.DatagramPacket;

/**
 * Created by catten on 16/2/18.
 */
public class PupaFactory {
    public ShuttleClient getClient() {
        return client;
    }

    public void setClient(ShuttleClient client) {
        this.client = client;
    }

    private ShuttleClient client;

    public PupaFactory(ShuttleClient client) {
        this.client = client;
    }

    public Pupa knockPupa(){
        return new Pupa("get server", String.format(
                "session:%s|ip address:%s|mac address:%s",
                HexTools.byte2HexStr(client.getSession().getBytes()),
                HexTools.byte2HexStr(client.getIPAddress().getBytes()),
                HexTools.byte2HexStr(client.getMacAddress())
        ));
    }

    public Pupa certificatePupa(){
        return new Pupa("login",String.format(
                "session:%s|username:%s|password:%s|ip address:%s|mac address:%s|access point:%s|version:%s|is dhcp enabled:%s",
                HexTools.byte2HexStr(client.getSession().getBytes()),
                HexTools.byte2HexStr(client.getUsername().getBytes()),
                HexTools.byte2HexStr(client.getPassword().getBytes()),
                HexTools.byte2HexStr(client.getIPAddress().getBytes()),
                HexTools.byte2HexStr(client.getMacAddress()),
                HexTools.byte2HexStr("internet".getBytes()),
                HexTools.byte2HexStr(client.getVersion().getBytes()),
                "00"
        ));
    }

    public Pupa breathPupa(){
        return new Pupa("breathe", String.format(
                "session:%s|ip address:%s|serial no:0%x|mac address:%s",
                HexTools.byte2HexStr(client.getSession().getBytes()),
                HexTools.byte2HexStr(client.getIPAddress().getBytes()),
                client.getSerialNo(),
                HexTools.byte2HexStr(client.getMacAddress())
        ));
    }

    public Pupa logoutPupa(){
        return new Pupa("logout", String.format(
                "session:%s|ip address:%s|mac address:%s",
                HexTools.byte2HexStr(client.getSession().getBytes()),
                HexTools.byte2HexStr(client.getIPAddress().getBytes()),
                HexTools.byte2HexStr(client.getMacAddress())
        ));
    }

    public static Pupa serverPupa(DatagramPacket datagramPacket){
        byte[] data = new byte[datagramPacket.getLength()];
        System.arraycopy(Pronunciation.decrypt3848(datagramPacket.getData()), 0, data, 0, data.length);
        return new Pupa(data);
    }
}
