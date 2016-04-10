package com.shinonometn.loom.common;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Created by catten on 15/10/25.
 */
public class Networks {
    //Find out available net card
    private static Vector<NetworkInterface> networkInterfaceVector;
    public static Vector<NetworkInterface> getNetworkInterfaces(boolean refreshList){
        if(refreshList || networkInterfaceVector == null) try {
            networkInterfaceVector = new Vector<>();
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaceEnumeration.hasMoreElements()) {
                NetworkInterface temp = networkInterfaceEnumeration.nextElement();
                if(temp.getHardwareAddress() != null){
                    InetAddress address;
                    Enumeration<InetAddress> inetAddressEnumeration = temp.getInetAddresses();
                    while(inetAddressEnumeration.hasMoreElements()){
                        address = inetAddressEnumeration.nextElement();
                        if(address.toString().contains(".")){
                            networkInterfaceVector.add(temp);
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return networkInterfaceVector;
    }
}
