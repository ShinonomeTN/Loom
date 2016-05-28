package com.shinonometn.loom.core.data;

import com.shinonometn.pupa.Pupa;
import com.shinonometn.pupa.tools.Pronunciation;

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
