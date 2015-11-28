package com.shinonometn.Loom.common;

/**
 * Created by catten on 15/11/18.
 */
public class Toolbox {

    private static String systemName = null;
    private static boolean macOSX = getSystemName().contains("mac");

    public static String getSystemName(){
        if(systemName == null){
            systemName = System.getProperty("os.name").toLowerCase();
        }
        return systemName;
    }

    public static boolean isMacOSX(){
        return macOSX;
    }

    public static String praseWeek(String weekNum){
        switch (weekNum){
            case "0":
                return "Mon";
            case "1":
                return "Tue";
            case "2":
                return "Wed";
            case "3":
                return "Thu";
            case "4":
                return "Fri";
            case "5":
                return "Sat";
            case "6":
                return "Sun";
            default:
                return "Out Of Range";
        }
    }
}
