package com.shinonometn.loom.core;

import com.shinonometn.loom.Program;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by catten on 16/5/28.
 */
public class Shuttle {
    private 

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

            shuttle = new Shuttle(networkInterface, null);
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
}
