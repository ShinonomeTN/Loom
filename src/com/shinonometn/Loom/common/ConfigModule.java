package com.shinonometn.Loom.common;

import com.shinonometn.Loom.Program;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

import java.io.*;

/**
 * Created by catten on 15/11/17.
 */
public class ConfigModule {
    //公开的可读取的字段
    public static boolean useLog = true;
    public static String username = "";
    public static String password = "";
    public static boolean autoSaveSetting = true;
    public static String defaultInterface = "";
    public static boolean outPrintLog = true;
    public static int windowWidth = 240;
    public static int windowHeight = 400;
    public static boolean hideNoClose = true;

    //配置文件目录
    private static File profilePath;
    //暂时保存读渠道的配置文件
    private static byte[] profileBuffer;

    //配置模块的状态
    private static boolean noConfigMode = false;

    //写入读取器
    private static FileOutputStream fileWriter;
    private static FileInputStream fileReader;

    static {

        try{
            profilePath = new File("./profile/encrypted_profile");

            if(!profilePath.getParentFile().exists()) {
                Logger.log("Profile directory not exist. Create.");
                profilePath.getParentFile().mkdir();
            }
            if(!profilePath.exists()) {
                Logger.log("Profile not exist. Create.");
                profilePath.createNewFile();
            }else{
                readProfile();
            }
        }catch (IOException e){
            Logger.error("Profile loading exception. No-profile mode on. cause :" + e.getMessage());
            noConfigMode = true;
        }
    }

    public static void readProfiles(){
        try{
            readProfile();
        }catch (IOException e){
            Logger.error("Profile loading exception. No-profile mode on. cause :" + e.getMessage());
            noConfigMode = true;
        }
    }

    private static void readProfile() throws IOException {
        Logger.log("Try to read profile.");
        fileReader = new FileInputStream(profilePath);
        int aChar;
        int length = 0;
        byte[] buffer = new byte[512];
        while((aChar = fileReader.read()) != -1){
            buffer[length++] = (byte)aChar;
        }
        profileBuffer = new byte[length];
        System.arraycopy(buffer,0,profileBuffer,0,length);
        profileBuffer = Pronunciation.decrypt3849(profileBuffer);

        Logger.log(Program.isDeveloperMode()?HexTools.byte2HexStr(profileBuffer):"----Ignored----");
        Logger.log(Program.isDeveloperMode()?new String(profileBuffer):"----Ignored----");
        if(profileBuffer != null){
            String fields[] = new String(profileBuffer).split("\n");
            if ("crypt3849".equals(fields[0])) {
                for(String field:fields){
                    try{
                        String split[] = field.split("=");

                        if("useLog".equals(split[0])) useLog = Boolean.parseBoolean(split[1]);
                        else if("username".equals(split[0])) username = split[1];
                        else if("password".equals(split[0])) password = split[1];
                        else if("autoSaveSetting".equals(split[0])) autoSaveSetting = Boolean.parseBoolean(split[1]);
                        else if("defaultInterface".equals(split[0])) defaultInterface = split[1];
                        else if("outPrintLog".equals(split[0])) outPrintLog = Boolean.parseBoolean(split[1]);
                        else if("windowWidth".equals(split[0])) windowWidth = Integer.parseInt(split[1]);
                        else if("windowHeight".equals(split[0])) windowHeight = Integer.parseInt(split[1]);

                        Logger.log(Program.isDeveloperMode()?field + " copied.":"----Ignored----");
                    }catch (NullPointerException | ArrayIndexOutOfBoundsException e){
                        Logger.error("An empty field found.");
                    }
                }

                Logger.log("Fields copied to ConfigModule.");

            }else Logger.log("Wrong profile format.");

            fileReader.close();
        }
    }

    public static void writeProfile(){
        if(noConfigMode) return;

        try {
            Logger.log("Writing profile.");
            profileBuffer = Pronunciation.encrypt3849(String.format(
                    "%s\n" +
                            "useLog=%s\n" +
                            "username=%s\n" +
                            "password=%s\n" +
                            "autoSaveSetting=%s\n" +
                            "defaultInterface=%s\n" +
                            "outPrintLog=%s\n" +
                            "windowWidth=%d\n" +
                            "windowHeight=%d\n"
                    ,
                    "crypt3849",
                    useLog,
                    username,
                    password,
                    autoSaveSetting,
                    defaultInterface,
                    outPrintLog,
                    windowWidth,
                    windowHeight
            ).getBytes());

            fileWriter = new FileOutputStream(profilePath);
            fileWriter.write(profileBuffer);
            fileWriter.flush();
            fileWriter.close();
            Logger.log("Profile writing success.");
        } catch (IOException e) {
            Logger.error("Profile write error. Switch to no-profile mode. cause:"+e.getMessage());
            noConfigMode = true;
        }
    }
}
