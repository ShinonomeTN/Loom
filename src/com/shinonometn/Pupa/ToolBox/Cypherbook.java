package com.shinonometn.Pupa.ToolBox;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by catten on 15/10/10.
 */

/*
* Tidy package again, make it clear to read.
*
* */
public class Cypherbook {
    /*
    * Translate action code to action name(Dictionary);
    *
    * */
    public static final int TYPE_STRING = 1;
    public static final int TYPE_INT = 0;
    public static final int TYPE_BOOLEAN = 2;
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_INT_ADDRESS = 3;
    public static final int TYPE_INT_MAC = 4;

    private static Map<String,Integer> actionCodes = new HashMap<>();
    private static Map<String,Integer> keyCodes = new HashMap<>();

    private static Map<String,Byte> actionCodes1 = new HashMap<>();
    private static Map<String,Byte> keyCodes1 = new HashMap<>();

    static {
        for(int i = 0; i < 14; i++){
            actionCodes.put(actionNames(i), i);
            actionCodes1.put(actionNames((byte)i),(byte)i);
        }
        for(int i = 0; i < 0x38; i++){
            keyCodes.put(keyNames(i),i);
            keyCodes1.put(keyNames((byte)i),(byte)i);

        }
    }
    @Deprecated
    public static int getActionKey(String Name){
        return actionCodes.get(Name);
    }
    @Deprecated
    public static int getKeyCode(String Name){
        return keyCodes.get(Name);
    }

    public static byte getByteActionKey(String Name){
        return actionCodes1.get(Name);
    }

    public static byte getByteKeyCode(String Name){
        return keyCodes1.get(Name);
    }

    @Deprecated
    public static String actionNames(int action){
        switch (action){
            case 1: return "login";
            case 2: return "login result";
            case 3: return "breathe";
            case 4: return "breath result";
            case 5: return "logout";
            case 6: return "logout result";
            case 7: return "get access point";
            case 8: return "return access point";
            case 9: return "disconnect";
            case 10: return "confirm login";
            case 11: return "confirm login result";
            case 12: return "get server";
            case 13: return "return server";
            default: return "no match";
        }
    }


    public static String actionNames(byte action){
        switch (action){
            case 0x01: return "login";
            case 0x02: return "login result";
            case 0x03: return "breathe";
            case 0x04: return "breath result";
            case 0x05: return "logout";
            case 0x06: return "logout result";
            case 0x07: return "get access point";
            case 0x08: return "return access point";
            case 0x09: return "disconnect";
            case 0x0A: return "confirm login";
            case 0x0B: return "confirm login result";
            case 0x0C: return "get server";
            case 0x0D: return "return server";
            default: return "no match";
        }
    }

    @Deprecated
    public static String keyNames(int key){
        switch (key){
            case 0x01 : return "username";
            case 0x02 : return "password";
            case 0x03 : return "is success";
            case 0x05 : return "success_unknown05";
            case 0x06 : return "success_unknown06";
            case 0x07 : return "mac address";
            case 0x08 : return "session";
            case 0x09 : return "ip address";
            case 0x0A : return "access point";
            case 0x0B : return "message";
            case 0x0C : return "server ip address";
            case 0x0D : return "server_unknown0D";
            case 0x0E : return "is dhcp enabled";
            case 0x13 : return "self-services website link";
            case 0x14 : return "serial no";
            case 0x1F : return "version";
            case 0x20 : return "login successfully_unknown20";
            case 0x23 : return "login successfully_unknown23";
            case 0x24 : return "disconnect reason";
            case 0x2A : return "breathe||logout_block2A";
            case 0x2B : return "breathe||logout_block2B";
            case 0x2C : return "breathe||logout_block2C";
            case 0x2D : return "breathe||logout_block2D";
            case 0x2E : return "breathe||logout_block2E";
            case 0x2F : return "breathe||logout_block2F";
            case 0x30 : return "confirmed_block30";
            case 0x31 : return "confirmed_block31";
            case 0x32 : return "unknown32";
            case 0x34 : return "login successfully_block34";
            case 0x35 : return "login successfully_block35";
            case 0x36 : return "login successfully_block36";
            case 0x37 : return "login successfully_block37";
            case 0x38 : return "login successfully_block38";
            default: return "no match";
        }
    }

    public static String keyNames(byte key){
        switch (key){
            case 0x01 : return "username";
            case 0x02 : return "password";
            case 0x03 : return "is success";
            case 0x05 : return "success_unknown05";
            case 0x06 : return "success_unknown06";
            case 0x07 : return "mac address";
            case 0x08 : return "session";
            case 0x09 : return "ip address";
            case 0x0A : return "access point";
            case 0x0B : return "message";
            case 0x0C : return "server ip address";
            case 0x0D : return "server_unknown0D";
            case 0x0E : return "is dhcp enabled";
            case 0x13 : return "self-services website link";
            case 0x14 : return "serial no";
            case 0x1F : return "version";
            case 0x20 : return "login successfully_unknown20";
            case 0x23 : return "login successfully_unknown23";
            case 0x24 : return "disconnect reason";
            case 0x2A : return "breathe||logout_block2A";
            case 0x2B : return "breathe||logout_block2B";
            case 0x2C : return "breathe||logout_block2C";
            case 0x2D : return "breathe||logout_block2D";
            case 0x2E : return "breathe||logout_block2E";
            case 0x2F : return "breathe||logout_block2F";
            case 0x30 : return "confirmed_block30";
            case 0x31 : return "confirmed_block31";
            case 0x32 : return "unknown32";
            case 0x34 : return "login successfully_block34";
            case 0x35 : return "login successfully_block35";
            case 0x36 : return "login successfully_block36";
            case 0x37 : return "login successfully_block37";
            case 0x38 : return "login successfully_block38";
            default: return "no match";
        }
    }
    @Deprecated
    public static int checkType(int key){
        switch (key){
            case 0x1:
            case 0x2:
            case 0x9:
            case 0x8:
            case 0xA:
            case 0xB:
            case 0x13:
            //case 0x14:
            case 0x1F:
            case 0x24:
                return TYPE_STRING;
            case 0x14:
                return TYPE_INT;
            case 0xC:
                return TYPE_INT_ADDRESS;
            case 0x7:
                return TYPE_INT_MAC;
            case 0x3:
            case 0xe:
                return TYPE_BOOLEAN;
            default:
                return TYPE_UNKNOWN;
        }
    }

    public static int checkType(byte key){
        switch (key){
            case 0x1:
            case 0x2:
            case 0x9:
            case 0x8:
            case 0xA:
            case 0xB:
            case 0x13:
                //case 0x14:
            case 0x1F:
            case 0x24:
                return TYPE_STRING;
            case 0x14:
                return TYPE_INT;
            case 0xC:
                return TYPE_INT_ADDRESS;
            case 0x7:
                return TYPE_INT_MAC;
            case 0x3:
            case 0xe:
                return TYPE_BOOLEAN;
            default:
                return TYPE_UNKNOWN;
        }
    }
    @Deprecated
    public static boolean isTwoBytesLonger(int action, int key){
        switch (key){
            case 0x8:
            case 0xB:
            case 0x9:
                switch (action){
                    case 0x4:
                    case 0x2:
                    case 0x6:
                        return true;
                }
            default:
                return false;
        }
    }

    public static boolean isTwoBytesLonger(byte action, byte key){
        switch (key){
            case 0x8:
            case 0xB:
            case 0x9:
                switch (action){
                    case 0x4:
                    case 0x2:
                    case 0x6:
                        return true;
                }
            default:
                return false;
        }
    }
}
