package com.shinonometn.Loom.common;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Created by catten on 15/11/2.
 */
public class Logger {
    public static boolean outPrint = true;
    private static boolean noLogFileMode = false;

    private static Date date = new Date();
    private static DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.CHINA);

    private static File pathLog;
    private static FileWriter fileWriter;

    static{
        init();
    }

    private static void init(){
        pathLog = new File("./log/" + dateFormat.format(date) + ".log");

        try{
            if(!pathLog.exists()){
                pathLog.getParentFile().mkdir();
                pathLog.createNewFile();
            }

            fileWriter = new FileWriter(pathLog);

        }catch (IOException e){
            noLogFileMode = true;
            System.out.println(e.getMessage());
        }finally {
            if(noLogFileMode) System.out.println("Opening log file failed. Program will run under no-log mode.");
        }
    }

    public static void deleteLog(){
        pathLog.delete();
    }

    public static void closeLog(){
        try {
            Logger.log("Close Log");
            noLogFileMode = true;
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static boolean isWriteToFile(){
        return fileWriter != null;
    }

    private static void makeLog(String log){
        if(outPrint){
            System.out.println(log);
        }

        if(!noLogFileMode){
            try {
                fileWriter.write((log + "\n"));
                fileWriter.flush();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                noLogFileMode = true;
                System.out.println("Write log failed. Changed to no-log mode.");
            }
        }
    }

    public static int clearLog(){
        try {
            int count = 0;
            Logger.log("Try to clean log directory.");
            File file_list[] = pathLog.getParentFile().listFiles();

            for(File file : file_list){
                if(file.getAbsolutePath().equals(pathLog.getAbsolutePath())) continue;
                file.delete();
                count++;
            }
            Logger.log("Cleaning log success, " + count + " log(s).");
            return count;
        }catch (Exception e){
            error("Clean log file failed. cause:" + e.getMessage());
            System.out.println("Clean log file failed. Logs will stay here.");
        }
        return -1;
    }

    public static void log(String message){
        makeLog(String.format("[%s][Log]%s",new Date(),message));
    }

    public static void error(String message){
        makeLog(String.format("[%s][Error]%s",new Date(),message));
    }
}
