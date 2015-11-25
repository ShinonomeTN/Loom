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
            case "1":
                return "Mon";
            case "2":
                return "Tue";
            case "3":
                return "Wed";
            case "4":
                return "Thu";
            case "5":
                return "Fri";
            case "6":
                return "Sat";
            case "7":
                return "Sun";
            default:
                return "Out Of Range";
        }
    }
}
