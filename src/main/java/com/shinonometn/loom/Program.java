package com.shinonometn.loom;

import com.shinonometn.loom.common.Networks;
import com.shinonometn.loom.common.Toolbox;
import com.shinonometn.loom.resource.Resource;
import com.shinonometn.pupa.tools.HexTools;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import java.util.List;
import java.util.Properties;


/**
 * Created by catten on 15/10/20.
 */
public class Program{

    public static boolean isDeveloperMode(){
        return System.getProperty("loom.developerMode").equals("t");
    }

    public final static String appName = "Loom v2.2";

    public static void main(String[] args){
        try { initLogger(); }catch (Throwable t){ System.out.println("Logger Initialization failed!"); }
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("main");

        System.setProperty("loom.mode","graphical");
        System.setProperty("loom.appName","Loom");
        System.setProperty("loom.version","v2.2");
        System.setProperty("loom.developerMode","f");

        String ip = null;

        logger.info("System: " + Toolbox.getSystemName());

        logger.warn(isDeveloperMode() ? "DeveloperMode on" +
                "\n\t\t!!! Warning !!!" +
                "\nDeveloper mode will record all user data(included account and password)" +
                "\nPlease remember to clear logs for protect your personal Data" : "DeveloperMode off");

        printNetworkInterface();

        bootConsoleMode(ip);
    }

    private static void bootConsoleMode(String ip){
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("booting");
        logger.info("Loom Console Mode");
        //Shuttle.LoomConsole(ip);
    }

    private static void initLogger() throws IOException {
        Properties properties = new Properties();
        String configPath = "/com/shinonometn/loom/resource/configure/";
        if("t".equals(System.getProperty("loom.developerMode"))) configPath += "log4j.dev.properties";
        else configPath += "log4j.default.properties";
        properties.load(new InputStreamReader(Program.class.getResourceAsStream(configPath)));
        PropertyConfigurator.configure(properties);
    }

    //Show available network interfaces.
    private static void printNetworkInterface(){
        Logger logger = org.apache.log4j.Logger.getLogger("booting");
        List<NetworkInterface> nf = Networks.getNetworkInterfaces(false);
        StringBuilder stringBuilder = new StringBuilder("\n\n");

        if(nf != null) {
            stringBuilder.append("Network Interfaces found: ");
            for (NetworkInterface n : nf) {
                try {
                    stringBuilder.append(String.format("[%s](", n.getDisplayName()));
                    List<InterfaceAddress> list = n.getInterfaceAddresses();

                    for (InterfaceAddress ia : list) {
                        if(ia.getAddress() != null){
                            stringBuilder.append(ia.getAddress()).append(":");
                        }
                    }
                    stringBuilder.append(HexTools.byte2HexStr(n.getHardwareAddress())).append(")");
                } catch (Exception e) {
                    stringBuilder.append(" null ");
                }
            }
        }
        logger.info(stringBuilder.toString());
    }
}
