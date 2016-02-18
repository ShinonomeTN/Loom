package com.shinonometn.Loom.common;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
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

    public static InetAddress getInetAddress(NetworkInterface networkInterface){
        List<InterfaceAddress> interfaceAddressList = networkInterface.getInterfaceAddresses();
        for(InterfaceAddress iA:interfaceAddressList){
            if(iA.toString().matches(".*[\\d\\w].{3}[\\d\\w].*")) {
                return iA.getAddress();
            }
        }
        return null;
    }
}
