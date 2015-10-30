package com.shinonometn.Pupa.ToolBox;

import java.io.Console;
import java.io.UnsupportedEncodingException;

/**
 * Created by Catten Linger on 2015/10/11.
 *
 * Tools about Hex stream
 */
public class HexTool {
    public static byte[] intArrToByteArr(int[] arr, int startPos, int endPos){
        byte[] result = new byte[endPos - startPos + 1];
        for(int i = startPos - 1; i < endPos; i++ ){
            result[i] = (byte)arr[i];
        }
        return result;
    }

    public static byte[] intArrToByteArr(int[] arr){
        byte[] result = new byte[arr.length];
        for(int i = 0; i < result.length; i++){
            result[i] = (byte)arr[i];
        }
        return result;
    }
    
    public static int[] byteArrToIntArr(byte[] arr, int startPos, int endPos){
        int[] result = new int[endPos - startPos + 1];
        for(int i = startPos - 1; i < endPos; i++){
            result[i] = (int)arr[i] & 0xFF;
        }
        return result;
    }

    public static int[] byteArrToIntArr(byte[] arr){
        int[] result = new int[arr.length];
        for(int i = 0; i < result.length; i++){
            result[i] = (int)arr[i] & 0xFF;
        }
        return result;
    }

    public static String toStr(byte[] data){
        try {
            return new String(data,"GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String toHexStr(int[] data){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i:data) stringBuilder.append(String.format((i > 0x10 ? "%x" : "0%x"),i));
        return stringBuilder.toString();
    }

    public static int toInt(int[] data){
        int buffer = 0;
        if(data.length > 4){
            throw new NumberFormatException("Nomber too bing");
        }
        int i = data.length - 1;
        while (i >= 0) {
            buffer <<= 8;
            buffer += data[i];
            i--;
        }
        return buffer;
    }

    public static boolean toBool(int[] data){
        if(data == null) return false;
        return data[data.length-1] > 0;
    }
}
