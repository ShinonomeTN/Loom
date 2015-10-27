package com.shinonometn.Loom;

import com.shinonometn.Loom.common.Networks;
import com.shinonometn.Loom.connector.Shuttle;
import com.shinonometn.Loom.ui.MainForm;
import com.shinonometn.Pupa.ToolBox.HexTool;

import javax.swing.*;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.Vector;

/**
 * Created by catten on 15/10/20.
 */
public class Program {
    public static void main(String[] args){
        try{
            //UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
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
                System.out.println(HexTool.hexBinToHexStr(HexTool.byteArrToIntArr(n.getHardwareAddress())));
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
