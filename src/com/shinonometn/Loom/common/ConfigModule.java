package com.shinonometn.Loom.common;

import com.shinonometn.Loom.Program;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;
import com.sun.org.apache.xpath.internal.operations.Bool;
import sun.rmi.runtime.Log;

import javax.swing.text.StyledEditorKit;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by catten on 15/11/17.
 */
public class ConfigModule {
    public static boolean useLog = true;
    public static String username = "";
    public static String password = "";
    public static boolean saveUserInfo = true;
    public static String defaultInterface = "";

    private static File profilePath;
    private static byte[] profileBuffer;

    private static boolean noConfigMode = false;
    private static FileWriter fileWriter;
    private static FileReader fileReader;

    static {

        try{
            profilePath = new File("./profile/encrypted_profile");
            if(!profilePath.exists()) {
                Logger.log("Profile not exist. Create.");
                if(!profilePath.getParentFile().exists()) {
                    Logger.log("Profile directory not exist. Create.");
                    profilePath.getParentFile().mkdir();
                }
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
        fileReader = new FileReader(profilePath);
        int aChar;
        StringBuilder buffer = new StringBuilder();
        while((aChar = fileReader.read()) != -1){
            buffer.append(String.format((aChar > 0x10 ? "%x":"0%x"),(byte)aChar));
        }
        profileBuffer = Pronunciation.decrypt3848(HexTools.hexStr2Bytes(buffer.toString()));
        Logger.log(Program.isDeveloperMode()?HexTools.byte2HexStr(profileBuffer):"----Ignored----");
        if(profileBuffer != null){
            String s[] = new String(profileBuffer).split("\n");
            if ("crypt3849".equals(s[0])) {
                useLog = Boolean.parseBoolean(s[1]);
                username = s[2];
                password = s[3];
                saveUserInfo = Boolean.parseBoolean(s[4]);
                defaultInterface = s[5];
            }
            fileReader.close();
        }
    }

    public static void writeProfile(){
        if(noConfigMode) return;

        try {
            profileBuffer = Pronunciation.encrypt3849(String.format(
                    "%s\n%s\n%s\n%s\n%s\n%s", "crypt3849", useLog, username, password, saveUserInfo, defaultInterface).getBytes());

            fileWriter = new FileWriter(profilePath);
            fileWriter.write(HexTools.byte2HexStr(profileBuffer));
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Logger.error("Profile write error. Switch to no-profile mode. cause:"+e.getMessage());
            noConfigMode = true;
        }
    }
}
