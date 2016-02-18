package com.shinonometn.Loom.core.data;

import com.shinonometn.Pupa.Pupa;
import com.shinonometn.Pupa.ToolBox.Pronunciation;

/**
 * Created by catten on 16/2/18.
 */
public class DataFactory {
    public static byte[] encrypt(Pupa pupa){
        return Pronunciation.encrypt3848(pupa.getData());
    }

    public static Pupa decrypt(byte[] enData){
        return new Pupa(Pronunciation.decrypt3848(enData));
    }
}
