package com.shinonometn.Pupa.ToolBox;

/**
 * Created by catten on 15/10/10.
 */
public class Pronunciation {

    public static byte[] encrypt3848(byte[] buffer){
        if(buffer.length > 0){
            byte[] result = new byte[buffer.length];

            for (int i = 0; i < buffer.length; i++) {
                result[i] = (byte) ((buffer[i] & 0x80) >>> 6
                                        | (buffer[i] & 0x40) >>> 4
                                        | (buffer[i] & 0x20) >>> 2
                                        | (buffer[i] & 0x10) << 2
                                        | (buffer[i] & 0x08) << 2
                                        | (buffer[i] & 0x04) << 2
                                        | (buffer[i] & 0x02) >>> 1
                                        | (buffer[i] & 0x01) << 7);
            }

            return result;
        }
        return null;
    }

    public static byte[] decrypt3848(byte[] buffer){
        if(buffer.length > 0){
            byte[] result = new byte[buffer.length];

            for (int i = 0; i < buffer.length; i++){
                result[i] = (byte) ((buffer[i] & 0x80) >>> 7
                                            | (buffer[i] & 0x40) >>> 2
                                            | (buffer[i] & 0x20) >>> 2
                                            | (buffer[i] & 0x10) >>> 2
                                            | (buffer[i] & 0x08) << 2
                                            | (buffer[i] & 0x04) << 4
                                            | (buffer[i] & 0x02) << 6
                                            | (buffer[i] & 0x01) << 1);
            }

            return result;
        }
        return null;
    }

    public static byte[] encrypt3849(byte[] buffer){
        if(buffer.length > 0){
            byte[] result = new byte[buffer.length];

            for (int i = 0; i < buffer.length; i++){
                result[i] = (byte) ((buffer[i] & 0x80) >>> 4
                                            | (buffer[i] & 0x40) >>> 1
                                            | (buffer[i] & 0x20) << 1
                                            | (buffer[i] & 0x10) >>> 3
                                            | (buffer[i] & 0x08) << 4
                                            | (buffer[i] & 0x04)
                                            | (buffer[i] & 0x02) >>> 1
                                            | (buffer[i] & 0x01) << 4);
            }

            return result;
        }
        return null;
    }

    public static byte[] decrypt3849(byte[] buffer){
        if(buffer.length > 0){
            byte[] result = new byte[buffer.length];

            for (int i = 0; i < buffer.length; i++) {
                result[i] = (byte) ((buffer[i] & 0x80) >> 4
                                            | (buffer[i] & 0x40) >> 1
                                            | (buffer[i] & 0x20) << 1
                                            | (buffer[i] & 0x10) >> 4
                                            | (buffer[i] & 0x08) << 4
                                            | (buffer[i] & 0x04)
                                            | (buffer[i] & 0x02) << 3
                                            | (buffer[i] & 0x01) << 1);
            }

            return result;
        }
        return null;
    }
}
