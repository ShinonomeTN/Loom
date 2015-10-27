package com.shinonometn.Pupa;

import com.shinonometn.Pupa.ToolBox.CHexConvert;
import com.shinonometn.Pupa.ToolBox.Cypherbook;
import com.shinonometn.Pupa.ToolBox.HexTool;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public static int PUPA_PACKAGE_HEALTHY = 1001;
    public static int PUPA_PACKAGE_BROKEN = 1002;
    public static int PUPA_PACKAGE_FAT = 1003;

    private int[] data;//Raw Data
    //The head of the package
    private int action;//Package type
    private int length;//Package length that titled
    private int packet_length;//Real package length
    private int[] MD5hash = new int[16];//MD5 Hash
    //Fields
    Vector<int[]> fields = new Vector<>();

    private String enchatAlgorithm = "MD5";

    private int problmPupa = PUPA_PACKAGE_HEALTHY;

    public int getProblmPupa(){
        return problmPupa;
    }

    //Constract using String
    public Pupa(String action, String fields){
        this.action = Cypherbook.getActionKey(action);
        int key;

        int pack_length = 0;
        //split fields
        String[] tempfields = fields.split("\\|");
        //Vector<int[]> fieldsbuffer = new Vector<>();
        for(String field : tempfields){
            //converting fields
            String[] subfields = field.split(":");
            key = Cypherbook.getKeyCode(subfields[0]);
            byte[] datas = CHexConvert.hexStr2Bytes(subfields[1]);
            int[] result = new int[datas.length+2];
            result[0] = key;
            result[1] = (Cypherbook.isTwoBytesLonger(this.action, key) ? datas.length:datas.length + 2);
            pack_length += 2;
            for(int i = 2; i < result.length; i++){
                result[i] = (int)datas[i - 2];
                pack_length++;
            }
            //put into vector
            this.fields.add(result);
        }
        //get the whole package
        data = new int[pack_length];
        int point = 0;
        for(int[] arr: this.fields){
            for(int i = 0; i < arr.length; i++){
                data[point] = arr[i];
                point++;
            }
        }

        point = 0;
        int[] field_data = data;
        data = new int[pack_length+2+16];
        data[point++] = this.action;
        data[point++] = pack_length+2+16;
        for (int aMD5hash : MD5hash) {
            data[point++] = aMD5hash;
        }
        for(int afield_data : field_data){
            data[point++] = afield_data;
        }
        packet_length = point;

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(enchatAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] temp_hash = messageDigest.digest(HexTool.intArrToByteArr(data));
        MD5hash = HexTool.byteArrToIntArr(temp_hash);

        System.arraycopy(MD5hash, 0, data, 2, MD5hash.length);
    }

    //Constract using Raw data
    public Pupa(int action, Vector<int[]> fields){
        this.action = action;
        this.fields = fields;
        int packlength = 0;
        //Count whole package length
        for(int[] arr : fields){
            packlength += arr[1];
            //+=2 if is a "short length" field
            if(Cypherbook.isTwoBytesLonger(arr[0], arr[1])) packlength+=2;
        }
        //Refresh datas to an array
        int[] fielddata = new int[packlength];
        //A point for writing
        int point = 0;
        //Refreshing datas into the array
        for(int[] arr : fields){
            for (int anArr : arr) {
                fielddata[point] = anArr;
                point++;
            }
        }
        //new array length for the complete package
        packlength += (1+1+16);
        data = new int[packlength];
        //First bit for action
        data[0] = action;
        //Second bit for the "fake"(maybe) length
        data[1] = packlength;
        //Fill data fields
        arraycopy(fielddata, 0, data, 2 + MD5hash.length, fielddata.length);

        //Get MD5 Hash for data fields
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(enchatAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] temp_hash = messageDigest.digest(HexTool.intArrToByteArr(fielddata));
        MD5hash = HexTool.byteArrToIntArr(temp_hash);

        //Fill MD5 Hash
        arraycopy(MD5hash, 0, data, 2, MD5hash.length);
    }

    public Pupa(int[] stream) {
        this.data = stream;//Raw data
        int point = 0;//Point for reading datas
        action = stream[point];//Package type
        length = stream[++point];//Package length that titled
        //Get MD5hash
        for (int i = 0; i < 16; i++) MD5hash[i] = stream[++point];
        //Package length without head
        packet_length = stream.length - point + 1;
        try {
            //Spliting the package
            while (point < length - 1) {
                int key = stream[++point];//The first bit is field type
                int field_length = stream[++point];//Second bit is field length
                //Some field titled a fake length, I recorded that at the Cypherbook
                if(Cypherbook.isTwoBytesLonger(action, key)) field_length+=2;
                //Refresh data to a array
                int[] field = new int[field_length];
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
    public int getAction(){
        return action;
    }

    public int getLength(){
        return length;
    }

    public int[] getMD5hash(){
        return MD5hash;
    }

    public int getDataFieldLength(){
        return packet_length;
    }

    public int[] getData(){
        return data;
    }

    public Vector<int[]> getFields(){
        return fields;
    }

    public String toString(){
        return HexTool.hexBinToHexStr(data);
    }

    public static String toPrintabelString(Pupa aPupa){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("[Decrypted Package]\n%s\n",HexTool.hexBinToHexStr(aPupa.getData())));
        stringBuilder.append(String.format("[Action]\n%s(0x%x)\n", Cypherbook.actionNames(aPupa.getAction()), aPupa.getAction()));
        stringBuilder.append(String.format("[Length]\n%d\n", aPupa.getLength()));
        stringBuilder.append(String.format("[Data Fields Length]\n%d\n", aPupa.getDataFieldLength()));
        stringBuilder.append(String.format("[MD5 Hash]\n%s\n",HexTool.hexBinToHexStr(aPupa.getMD5hash())));
        for(int i = 0; i < aPupa.getFields().size(); i++){
            int[] field = aPupa.getFields().get(i);
            int key = field[0];
            int length = field[1];
            int[] value = new int[length];
            arraycopy(field, 2, value, 0, length - 2);
            stringBuilder.append(String.format("[Field %d]\n", i));
            stringBuilder.append(String.format("Key\t: %s(0x%x)\n", Cypherbook.keyNames(key),key));
            stringBuilder.append(String.format("Size\t: %d%s\n",length,(Cypherbook.isTwoBytesLonger(aPupa.getAction(), key)?" + 2":"")));
            stringBuilder.append(String.format("Context\t: "));
            switch (Cypherbook.checkType(key)){
                case Cypherbook.TYPE_STRING:
                    try {
                        stringBuilder.append(String.format("%s\n", HexTool.hexBinToStr(HexTool.intArrToByteArr(value,1,value.length))));
                    }catch (Exception w){
                        stringBuilder.append(String.format("null\n"));
                    }
                    break;
                case Cypherbook.TYPE_INT_ADDRESS:
                    for (int j = 0; j < value.length - 2; j++) stringBuilder.append(String.format((j == value.length - 3 ? "%d":"%d."),value[j]));
                    stringBuilder.append("\n");
                    break;
                case Cypherbook.TYPE_INT_MAC:
                    for (int j = 0; j < value.length - 2; j++) stringBuilder.append(String.format((j == value.length - 3 ? "%x":"%x:"),value[j]));
                    stringBuilder.append("\n");
                    break;
                case Cypherbook.TYPE_INT:
                    int toInt = 0;
                    for(int j = 0; j < value.length - 2; j++) {
                        toInt += value[j];
                        if(j == (value.length - 3)) toInt <<=1; else toInt<<=8;
                    }
                    stringBuilder.append(String.format("0x%x\n", toInt));
                    break;
                case Cypherbook.TYPE_BOOLEAN:
                    stringBuilder.append(String.format("%b\n", (value[0] != 0)));
                    break;
                case Cypherbook.TYPE_UNKNOWN:
                default:
                    stringBuilder.append(String.format("(Not support yet)\n"));
                    break;
            }
            stringBuilder.append(String.format("Raw data:\n%s\n",HexTool.hexBinToHexStr(field)));
        }
        return stringBuilder.toString();
    }
}
