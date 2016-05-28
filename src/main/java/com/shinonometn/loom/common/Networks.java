package com.shinonometn.loom.common;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by catten on 15/10/25.
 */
public class Networks {
    //Find out available net card
    private static List<NetworkInterface> networkInterfaceList;
    public static List<NetworkInterface> getNetworkInterfaces(boolean refreshList){
        if(refreshList || networkInterfaceList == null) try {
            networkInterfaceList = new ArrayList<>(3);
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if(networkInterface.getHardwareAddress() != null){
                    Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();

                    while(inetAddressEnumeration.hasMoreElements()){
                        InetAddress address = inetAddressEnumeration.nextElement();

                        if(address instanceof Inet4Address) networkInterfaceList.add(networkInterface);
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return networkInterfaceList;
    }
}
