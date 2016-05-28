package com.shinonometn.loom.common;

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
}