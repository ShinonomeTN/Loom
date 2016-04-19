package com.shinonometn.loom.resource;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Created by Catten Linger on 2015/11/21.
 */
public class Resource {
    private static Resource resource = null;
    private static Logger logger = Logger.getLogger("resource");

    public static Resource getResource(){
        if (resource == null) resource = new Resource();
        return resource;
    }

    HashMap<String,Object> hashMap;

    private Resource(){
        hashMap = new HashMap<>();
    }

    public void disposeResources(){
        hashMap.clear();
    }

    //获取资源文本
    public String getResourceText(String path){
        if(hashMap.containsKey(path)) return (String)hashMap.get(path);
        try{
            InputStreamReader inputStreamReader = new InputStreamReader(getClass().getResourceAsStream(path), Charset.forName("utf8"));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sB = new StringBuilder();
            String s;
            while ((s=bufferedReader.readLine()) != null){
                sB.append(s);
                sB.append("\n");
            }
            sB.deleteCharAt(sB.length() - 1);
            hashMap.put(path, sB.toString());
            return sB.toString();
        }catch (IOException e){
            logger.error(e.getMessage());
        }
        return null;
    }
}
