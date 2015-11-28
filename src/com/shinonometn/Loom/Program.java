package com.shinonometn.Loom;

import com.shinonometn.Loom.common.ConfigModule;
import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.common.Networks;
import com.shinonometn.Loom.common.Toolbox;
import com.shinonometn.Loom.core.Shuttle;
import com.shinonometn.Loom.resource.Resource;
import com.shinonometn.Loom.ui.MainForm;
import com.shinonometn.Pupa.ToolBox.HexTools;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

/**
 * Created by catten on 15/10/20.
 */
public class Program{
    private static boolean developerMode = false;

    public static boolean isDeveloperMode(){
        return developerMode;
    }

    public final static String appName = "Loom v2.2";

    public static void main(String[] args){

        if(args.length == 1 && args[0].toLowerCase().equals("-help")){
            Logger.closeLog();
            Logger.deleteLog();
            Logger.outPrint = false;
            System.out.println(appName);
            System.out.println(Resource.getResource().getResourceText("/com/shinonometn/Loom/resource/help.txt"));
            return;
        }

        File lockfile = new File("./profile/.lock");
        FileLock wlock;
        try {
            if(!lockfile.exists()){
                lockfile.createNewFile();
                lockfile.deleteOnExit();
            }
            wlock = new FileOutputStream(lockfile).getChannel().tryLock();
            if(wlock == null){
                System.out.println("Not Allow more than one Loom use same profile. Program exits.");
                return;
            }
            Logger.log("Get lock success.");

        } catch (IOException e) {
            Logger.log("Lock failed.");
        }

        Logger.log("System: " + Toolbox.getSystemName());
        if(args.length >= 2){
            if("-developerMode".toLowerCase().equals(args[1].toLowerCase())){
                developerMode = true;
            }
        }

        Logger.log(developerMode ? "DeveloperMode on" +
                "\n\t\t!!! Warning !!!" +
                "\nDeveloper mode will record all user data(included account and password)" +
                "\nPlease remember to clear logs for protect your personal Data" : "DeveloperMode off");

        if(args.length > 0 && "-consoleMode".toLowerCase().equals(args[0].toLowerCase())){
            Logger.log("Loom Console Mode");
            LoomConsole(null);
            if(Logger.isWriteToFile()){
                Logger.closeLog();
            }
        }

        if(args.length == 1 && args[0].toLowerCase().equals("-clearfakeinfo")){
            ConfigModule.fakeIP = "null";
            ConfigModule.fakeMac = "null";
            ConfigModule.writeProfile();
            Logger.log("Fake IP and Mac Cleared");
            System.out.println("Fake IP and Mac Cleared");
        }else if(args.length == 1 && "-closeautomode".equals(args[0].toLowerCase())){
            Logger.closeLog();
            Logger.deleteLog();
            Logger.outPrint = false;
            ConfigModule.autoOnlineTime = "";
            ConfigModule.autoOfflineTime = "";
            ConfigModule.writeProfile();
            System.out.println("Auto-mode closed.");
        }

        if(args.length >= 2 && "-setautomode".equals(args[0].toLowerCase())){
            Logger.closeLog();
            Logger.deleteLog();
            Logger.outPrint = false;
            //System.out.println("Please note that auto-online mode not support command line mode.");
            if(args[1].matches(ConfigModule.timeFormat + "\\-" + ConfigModule.timeFormat)){
                String[] matchBuffer = args[1].split("\\-");
                ConfigModule.autoOnlineTime = matchBuffer[0];
                ConfigModule.autoOfflineTime = matchBuffer[1];
                if(args.length == 3){
                    if(args[2].matches("(both|online|offline)")) ConfigModule.autoOnlineMode = args[2];
                }
                ConfigModule.writeProfile();
                System.out.println("Auto-mode set.");
                return;
            }
        }

        if(args.length == 4){
            if(args[0].toLowerCase().equals("-fakeip")){
                if(args[2].toLowerCase().equals("-fakemac")){
                    Logger.log("Writing fake IP and Mac to profile.");
                    System.out.println("Writing fake IP and Mac to profile.");
                    ConfigModule.fakeIP = args[1];
                    ConfigModule.fakeMac = args[3].replace(":","");
                    ConfigModule.writeProfile();
                    return;
                }
            }
        }

        if(args.length == 0 || "-graphicMode".toLowerCase().equals(args[0].toLowerCase())){
            Logger.log("Loom Graphic Mode");
            try{
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }catch (Exception e){
                Logger.log(e.toString());
            }

            new MainForm();

            Vector<NetworkInterface> nf = Networks.getNetworkInterfaces(false);
            StringBuilder stringBuilder = new StringBuilder("\n\n");
            if(nf != null) {
                stringBuilder.append("Network Interfaces found:\n");
                for (NetworkInterface n : nf) {
                    try {
                        stringBuilder.append(String.format("[%s]%n", n.getDisplayName())).append("\n");
                        List<InterfaceAddress> list = n.getInterfaceAddresses();
                        for (InterfaceAddress ia : list) {
                            try {
                                stringBuilder.append(ia.getAddress()).append("\n");
                            } catch (Exception e) {
                                stringBuilder.append("null\n");
                            }
                        }
                        stringBuilder.append(HexTools.byte2HexStr(n.getHardwareAddress())).append("\n");
                        //System.out.println();
                    } catch (Exception e) {
                        stringBuilder.append("Null\n");
                    }
                    stringBuilder.append("\n\n");
                }
            }
            Logger.log(stringBuilder.toString());
        }
    }

    public static void LoomConsole(String args[]){
        try {
            System.out.println("Welcome to use " + appName + " Console!\n");

            String ip;
            final String username;
            final String password;

            final Shuttle shuttle;

            Scanner scanner = new Scanner(System.in);

            if(args.length == 1){
                System.out.print("Loom now running under pre-fix mode.");
                ip = args[0];
            }else{
                System.out.println("Input your IP address:");
                ip = scanner.next();
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
            if(ConfigModule.isFakeMode()) Logger.log("Please note that fake mode on.");
            if(ConfigModule.allowAutoMode()) Logger.log("Please note that auto-mode on.");
            shuttle.developerMode = Program.isDeveloperMode();
            System.out.println("Prepared to login.");

            if(args.length == 0){
                System.out.println("Please input your account");
                username = scanner.next();
                System.out.println("PIN Code?(Password)");
                password = scanner.next();
            }else{
                username = null;
                password = null;
            }

            if(!ConfigModule.allowAutoMode()){
                if(args.length == 0){
                    shuttle.setUsername(username);
                    shuttle.setPassword(password);
                }else{
                    shuttle.setUsername(ConfigModule.username);
                    shuttle.setPassword(ConfigModule.password);
                }
                System.out.println("Loom Start.");
                shuttle.start();

                do{
                    System.out.println("If you want to get offline or exit program, Please input \"exit\"");
                    if (!scanner.next().toLowerCase().equals("exit")){
                        if(scanner.next().equals("about")){
                            aboutMe();
                        }else System.out.println("If you want to get offline or exit program, Please input \"exit\"");
                    }else{
                        shuttle.offline();
                        while(shuttle.isBreathing() || shuttle.isMessageListening());
                        return;
                    }
                }while(shuttle.isAlive());

                if(!developerMode){
                    System.out.println(
                            "Some error accorded. Restart program or run " +
                                    "\n\"java -jar Loom.jar -consoleMode -developerMode\"" +
                                    "\n to know more."
                    );
                }
            }else{
                shuttle.offline();
                Thread thread = new Thread(){
                    boolean alertFlag = false;
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                    boolean runflag = true;
                    Shuttle shuttle1;
                    public void run(){
                        System.out.println("Loom running under auto-mode. Online: " + (shuttle1 == null?"Ture":"False"));
                        Logger.log("Loom running under auto-mode.");
                        while(runflag){
                            String date = simpleDateFormat.format(new Date());
                            System.out.println("Time check. " + date);
                            if(shuttle1 == null){
                                if(ConfigModule.autoOnlineMode.equals("both") || ConfigModule.autoOnlineMode.equals("online")){
                                    if(date.equals(ConfigModule.autoOnlineTime)){
                                        if(!alertFlag){

                                            shuttle1 = new Shuttle(networkInterface,null);
                                            shuttle1.setUsername(username);
                                            shuttle1.setPassword(password);
                                            shuttle1.start();
                                            alertFlag = true;
                                            Logger.log("Auto online because reach the online time point.");
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
                                            Logger.log("Auto offline because reach the offline time point.");
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
            Logger.log(e.toString());
        }
    }

    public static void aboutMe(){
        System.out.println(Resource.getResource().getResourceText("/com/shinonometn/Loom/resource/aboutMe.txt"));
    }
}
