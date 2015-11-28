package com.shinonometn.Loom.common;

import com.shinonometn.Loom.Program;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

import java.io.*;

/**
 * Created by catten on 15/11/17.
 */
public class ConfigModule{

    private static ConfigModule configModule = new ConfigModule();
    public static ConfigModule getConfigModule() { return configModule; }
    //公开的可读取的字段
    public static Boolean useLog = true;
    public static String username = "";
    public static String password = "";
    public static Boolean autoSaveSetting = true;
    public static String defaultInterface = "";
    public static Boolean outPrintLog = true;
    public static Integer windowWidth = 230;
    public static Integer windowHeight = 240;
    public static Boolean hideOnIconified = true;
    public static Boolean hideOnClose = true;
    public static Boolean showInfo = false;
    public static Boolean notShownAtLaunch = false;
    public static String fakeIP = "null";
    public static String fakeMac = "null";
    //public static Boolean autoModeOn = false;
    public static String autoOnlineTime = "";
    public static String autoOfflineTime = "";
    public static String autoOnlineMode = "both";
    public static Boolean autoOnline = false;
    public static String specialDays = "online:Mon,Tue,Wed,Thu,Fri,Sat,Sun;offline:Mon,Tue,Wed,Thu,Fri,Sat,Sun";

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

    public final static String ipFormat = "([1,2]?(\\d){1,2}\\.){3}[1,2]?(\\d){1,2}";
    public final static String macFormat = "[0-f,0-F]{12}";
    public static boolean isFakeMode(){
        return (ConfigModule.fakeIP.matches(ipFormat) && ConfigModule.fakeMac.matches(macFormat));
    }

    public final static String timeFormat = "([0-2]\\d\\:[0-5]\\d)";
    public static boolean allowAutoMode(){
        return (autoOnlineTime.matches(timeFormat) && autoOfflineTime.matches(timeFormat)) && !autoOnlineTime.equals(autoOfflineTime);
    }

    public static String getSpecialDays(){
        String s = specialDays;
        StringBuilder stringBuilder = new StringBuilder();
        String[] fields = s.split(";");
        int c;
        for(String field : fields){
            c = 0;
            for(int i = 1; i <= 7; i++){
                if(!field.contains(Toolbox.praseWeek(i))){
                    stringBuilder.append(Toolbox.praseWeek(i)).append(" ");
                    c++;
                }
            }
            if(c == 0) stringBuilder.append("无");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
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

        Logger.log(Program.isDeveloperMode()?HexTools.byte2HexStr(profileBuffer):"----Banned----");
        Logger.log(Program.isDeveloperMode()?new String(profileBuffer):"----Banned----");
        if(profileBuffer != null){
            String fields[] = new String(profileBuffer).split("\n");
            if ("crypt3849".equals(fields[0])) {
                for(String field:fields){
                    try{
                        String split[] = field.split("=");
                        //判断字段并存入相应的变量
                        if("useLog".equals(split[0])) useLog = Boolean.parseBoolean(split[1]);
                        else if("username".equals(split[0])) username = split[1];
                        else if("password".equals(split[0])) password = split[1];
                        else if("autoSaveSetting".equals(split[0])) autoSaveSetting = Boolean.parseBoolean(split[1]);
                        else if("defaultInterface".equals(split[0])) defaultInterface = split[1];
                        else if("outPrintLog".equals(split[0])) outPrintLog = Boolean.parseBoolean(split[1]);
                        else if("windowWidth".equals(split[0])) windowWidth = Integer.parseInt(split[1]);
                        else if("windowHeight".equals(split[0])) windowHeight = Integer.parseInt(split[1]);
                        else if("hideOnIconified".equals(split[0])) hideOnIconified = Boolean.parseBoolean(split[1]);
                        else if("showInfo".equals(split[0])) showInfo = Boolean.parseBoolean(split[1]);
                        else if("notShownAtLaunch".equals(split[0])) notShownAtLaunch = Boolean.parseBoolean(split[1]);
                        else if("fakeIP".equals(split[0])) fakeIP = split[1];
                        else if("fakeMac".equals(split[0])) fakeMac = split[1];
                        else if("autoOnlineTime".equals(split[0])) autoOnlineTime = split[1];
                        else if("autoOfflineTime".equals(split[0])) autoOfflineTime = split[1];
                        else if("hideOnClose".equals(split[0])) hideOnClose = Boolean.parseBoolean(split[1]);
                        else if("autoOnline".equals(split[0])) autoOnline = Boolean.parseBoolean(split[1]);
                        else if("specialDays".equals(split[0])) specialDays = split[1];

                        //保证配置的健壮性
                        if(!fakeIP.matches(ipFormat)) fakeIP = "null";
                        if(!fakeMac.matches(macFormat)) fakeMac = "null";
                        if(!autoOfflineTime.matches(timeFormat)) autoOfflineTime = "";
                        if(!autoOnlineTime.matches(timeFormat)) autoOnlineTime = "";
                        if(!(autoOnlineMode.equals("online") || autoOnlineMode.equals("offline"))) autoOnlineMode = "both";


                        Logger.log(Program.isDeveloperMode()?field + " copied.":"----Banned----");
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
            profileBuffer = Pronunciation.encrypt3849(
                    String.format(  "%s\n" +
                                    "useLog=%s\n" +
                                    "username=%s\n" +
                                    "password=%s\n" +
                                    "autoSaveSetting=%s\n" +
                                    "defaultInterface=%s\n" +
                                    "outPrintLog=%s\n" +
                                    "windowWidth=%d\n" +
                                    "windowHeight=%d\n" +
                                    "hideOnIconified=%s\n" +
                                    "showInfo=%s\n" +
                                    "notShownAtLaunch=%s\n" +
                                    "fakeIP=%s\n" +
                                    "fakeMac=%s\n" +
                                    "autoOnlineTime=%s\n" +
                                    "autoOfflineTime=%s\n" +
                                    "autoOnlineMode=%s\n" +
                                    "hideOnClose=%s\n" +
                                    "autoOnline=%s\n" +
                                    "specialDays=%s",
                            "crypt3849",
                            useLog,
                            username,
                            password,
                            autoSaveSetting,
                            defaultInterface,
                            outPrintLog,
                            windowWidth,
                            windowHeight,
                            hideOnIconified,
                            showInfo,
                            notShownAtLaunch,
                            fakeIP,
                            fakeMac,
                            autoOnlineTime,
                            autoOfflineTime,
                            autoOnlineMode,
                            hideOnClose,
                            autoOnline,
                            specialDays
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
