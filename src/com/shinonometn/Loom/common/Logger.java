package com.shinonometn.Loom.common;

import java.util.Date;

/**
 * Created by catten on 15/11/2.
 */
public class Logger {
    private static Date date = new Date();
    public static boolean outPrint = true;

    public static void log(String message){
        if(outPrint){
            System.out.println(String.format("[%s][Log]%s",date,message));
        }
    }

    public static void error(String message){
        if(outPrint){
            System.out.println(String.format("[%s][Error]%s",date,message));
        }
    }
}
