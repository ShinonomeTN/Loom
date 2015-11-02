package com.shinonometn.Pupa;

import com.shinonometn.Pupa.ToolBox.Dictionary;
import com.shinonometn.Pupa.ToolBox.HexTools;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Scanner;
import java.util.Vector;

import static java.lang.System.*;

/**
 * Created by catten on 15/10/10.
 */
/*
*
* Data package container class
*
* Auto tidy fields and re-package.
*
* */

public class Pupa {

    public static int PUPA_GENERATE_FAILED = 1000;
    public static int PUPA_PACKAGE_HEALTHY = 1001;
    public static int PUPA_PACKAGE_BROKEN = 1002;
    public static int PUPA_PACKAGE_FAT = 1003;

    private byte[] data;//Raw Data
    //The head of the package
    private byte action;//Package type
    private byte length;//Package length that titled
    private byte packet_length;//Real package length
    private byte[] MD5hash = new byte[16];//MD5 Hash
    //Fields
    Vector<byte[]> fields = new Vector<>();

    private String enchatAlgorithm = "MD5";

    private int problmPupa = PUPA_PACKAGE_HEALTHY;

    public int getProblmPupa(){
        return problmPupa;
    }

    //Constract using String
    public Pupa(String action, String fields){
        this.action = Dictionary.getByteActionKey(action);
        byte key;

        int pack_length = 0;
        //split fields
        String[] tempfields = fields.split("\\|");
        //Vector<int[]> fieldsbuffer = new Vector<>();
        for(String field : tempfields){
            //converting fields
            String[] subfields = field.split(":");
            key = Dictionary.getByteKeyCode(subfields[0]);
            byte[] datas = HexTools.hexStr2Bytes(subfields[1]);
            byte[] result = new byte[datas.length+2];
            result[0] = key;
            result[1] = (byte) (Dictionary.isTwoBytesLonger(this.action, key) ? datas.length:datas.length + 2);
            pack_length += 2;
            for(int i = 2; i < result.length; i++){
                result[i] = datas[i - 2];
                pack_length++;
            }
            //put into vector
            this.fields.add(result);
        }
        //get the whole package
        data = new byte[pack_length];
        int point = 0;
        for(byte[] arr: this.fields){
            for (byte anArr : arr) {
                data[point] = anArr;
                point++;
            }
        }

        point = 0;
        byte[] field_data = data;
        data = new byte[pack_length+2+16];
        data[point++] = this.action;
        data[point++] = (byte) (pack_length+2+16);
        for (byte aMD5hash : MD5hash) {
            data[point++] = aMD5hash;
        }
        for(byte afield_data : field_data){
            data[point++] = afield_data;
        }
        packet_length = (byte) point;

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(enchatAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            problmPupa = PUPA_GENERATE_FAILED;
        }
        byte[] temp_hash = new byte[0];
        if (messageDigest != null) {
            temp_hash = messageDigest.digest(data);
        }
        MD5hash = temp_hash;

        System.arraycopy(MD5hash, 0, data, 2, MD5hash.length);
    }

    //Constract using Raw data
    public Pupa(byte action, Vector<byte[]> fields){
        this.action = action;
        this.fields = fields;
        int packlength = 0;
        //Count whole package length
        for(byte[] arr : fields){
            packlength += arr[1];
            //+=2 if is a "short length" field
            if(Dictionary.isTwoBytesLonger(arr[0], arr[1])) packlength+=2;
        }
        //Refresh datas to an array
        byte[] fielddata = new byte[packlength];
        //A point for writing
        int point = 0;
        //Refreshing datas into the array
        for(byte[] arr : fields){
            for (byte anArr : arr) {
                fielddata[point] = anArr;
                point++;
            }
        }
        //new array length for the complete package
        packlength += (1+1+16);
        data = new byte[packlength];
        //First bit for action
        data[0] = action;
        //Second bit for the "fake"(maybe) length
        data[1] = (byte) packlength;
        //Fill data fields
        arraycopy(fielddata, 0, data, 2 + MD5hash.length, fielddata.length);

        //Get MD5 Hash for data fields
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(enchatAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            problmPupa = PUPA_GENERATE_FAILED;
        }
        byte[] temp_hash = new byte[0];
        if (messageDigest != null) {
            temp_hash = messageDigest.digest(fielddata);
        }
        MD5hash = temp_hash;

        //Fill MD5 Hash
        arraycopy(MD5hash, 0, data, 2, MD5hash.length);
    }

    public Pupa(byte[] stream) {
        this.data = stream;//Raw data
        int point = 0;//Point for reading datas
        action = stream[point];//Package type
        length = stream[++point];//Package length that titled
        //Get MD5hash
        for (int i = 0; i < 16; i++) MD5hash[i] = stream[++point];
        //Package length without head
        packet_length = (byte) (stream.length - point + 1);
        try {
            //Spliting the package
            while (point < length - 1) {
                byte key = stream[++point];//The first bit is field type
                byte field_length = stream[++point];//Second bit is field length
                //Some field titled a fake length, I recorded that at the Dictionary
                if(Dictionary.isTwoBytesLonger(action, key)) field_length+=2;
                //Refresh data to a array
                byte[] field = new byte[field_length];
                field[0] = key;
                field[1] = field_length;
                for (int i = 2; i < field_length; i++) {
                    field[i] = stream[++point];
                }
                fields.add(field);//Organize fields to the Vector
            }
        } catch (Exception e) {
            //If something goes wrong
            out.printf("[!]A bug fly out at converting field %d\nError Info:\n%s\n",fields.size(),e.toString());
            problmPupa = PUPA_PACKAGE_BROKEN;
        } finally {
            //if a fake package length
            if(stream.length > (packet_length +=2 + MD5hash.length))
                out.println("[!]It seems that some after-added data behind this package. Please check out that if needed.");
                problmPupa = PUPA_PACKAGE_FAT;
        }
    }
    //Geters
    public byte getAction(){
        return action;
    }

    public byte getLength(){
        return length;
    }

    public byte[] getMD5hash(){
        return MD5hash;
    }

    public int getDataFieldLength(){
        return packet_length;
    }

    public byte[] getData(){
        return data;
    }

    public Vector<byte[]> getFields(){
        return fields;
    }

    public String toString(){
        return HexTools.byte2HexStr(data, data.length);
    }

    public static byte[] findField(Pupa pupa ,String fieldName){
        Byte aKey = Dictionary.getByteKeyCode(fieldName);
        return findField(pupa, aKey);
    }

    public static byte[] findField(Pupa pupa, int Key){
        Vector<byte[]> fields = pupa.getFields();
        for (byte[] field : fields){
            if(field[0] == Key){
                return field;
            }
        }
        return null;
    }

    public static byte[] fieldData(byte field[]){
        byte[] result = new byte[field.length - 2];
        System.arraycopy(field, 2, result, 0, result.length);
        return result;
    }

    public static String toPrintabelString(Pupa aPupa){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("[Decrypted Package]\n%s\n", HexTools.byte2HexStr(aPupa.getData(), aPupa.getData().length)));
        stringBuilder.append(String.format("[Action]\n%s(0x%x)\n", Dictionary.actionNames(aPupa.getAction()), aPupa.getAction()));
        stringBuilder.append(String.format("[Length]\n%d\n", aPupa.getLength()));
        stringBuilder.append(String.format("[Data Fields Length]\n%d\n", aPupa.getDataFieldLength()));
        stringBuilder.append(String.format("[MD5 Hash]\n%s\n", HexTools.byte2HexStr(aPupa.getMD5hash(), aPupa.getMD5hash().length)));
        for(int i = 0; i < aPupa.getFields().size(); i++){
            byte[] field = aPupa.getFields().get(i);
            byte key = field[0];
            byte length = field[1];
            stringBuilder.append(String.format("[Field %d]\n", i));
            stringBuilder.append(String.format("Key\t: %s(0x%x)\n", Dictionary.keyNames(key),key));
            stringBuilder.append(String.format("Size\t: %d%s\n",length,(Dictionary.isTwoBytesLonger(aPupa.getAction(), key)?" + 2":"")));
            stringBuilder.append("Context\t: ");
            byte value[] = Pupa.fieldData(field);
            switch (Dictionary.checkType(key)){
                case Dictionary.TYPE_STRING:
                    try {
                        stringBuilder.append(String.format("%s\n", HexTools.toGB2312Str(value)));
                    }catch (Exception w){
                        stringBuilder.append("null\n");
                    }
                    break;
                case Dictionary.TYPE_INT_ADDRESS:
                    for (int j = 0; j < value.length; j++) {
                        stringBuilder.append(String.format((j == value.length - 1 ? "%d" : "%d."), value[j]));
                    }
                    stringBuilder.append("\n");
                    break;
                case Dictionary.TYPE_INT_MAC:
                    stringBuilder.append(HexTools.byte2HexStr(value, value.length, ':'));
                    stringBuilder.append("\n");
                    break;
                case Dictionary.TYPE_INT:
                    int toInt = 0;
                    for(int j = 0; j < value.length; j++) {
                        toInt += value[j];
                        if(j == (value.length - 1)) toInt <<=1; else toInt<<=8;
                    }
                    stringBuilder.append(String.format("0x%x\n", toInt));
                    break;
                case Dictionary.TYPE_BOOLEAN:
                    stringBuilder.append(String.format("%b\n", (value[0] != 0)));
                    break;
                case Dictionary.TYPE_UNKNOWN:
                default:
                    stringBuilder.append("(Not support yet)\n");
                    break;
            }
            stringBuilder.append(String.format("Raw data:\n%s\n", HexTools.byte2HexStr(field, field.length)));
        }
        return stringBuilder.toString();
    }

    public static void main(String[] args){
        System.out.println("Input a stream of data:");
        Scanner scanner = new Scanner(System.in);
        String data = scanner.next();
        Pupa pupa = new Pupa(Pronunciation.decrypt3848(HexTools.hexStr2Bytes(data)));
        System.out.println(Pupa.toPrintabelString(pupa));
    }
}
