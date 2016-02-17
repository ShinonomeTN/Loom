package com.shinonometn.Loom.core.Message;

/**
 * Created by catten on 15/11/3.
 */
public interface ShuttleEvent {

    //网络相关
    int SERVER_RESPONSE = 0;
    int SERVER_NOT_FOUNT = 1;
    int SERVER_NO_RESPONSE = 10;

    int SOCKET_PORT_IN_USE = 2;
    int SOCKET_GET_SUCCESS = 20;
    int SOCKET_OTHER_EXCEPTION = 21;
    int SOCKET_NO_ROUTE_TO_HOST = 22;
    int SOCKET_UNKNOWN_HOST_EXCEPTION = 23;

    //认证相关
    int CERTIFICATE_SUCCESS = 3;
    int CERTIFICATE_FAILED = 30;
    int CERTIFICATE_EXCEPTION = 31;
    int SERVER_MESSAGE = 32;
    int SERVER_RESPONSE_IPADDRESS = 33;

    int BREATHE_SUCCESS = 4;
    int BREATHE_FAILED = 40;
    int BREATHE_EXCEPTION = 41;

    int MESSAGE_CLOSE = 5;
    int MESSAGE_START = 50;
    int MESSAGE_EXCEPTION = 51;

    int OFFLINE = 13;

    void onMessage(int messageType, String message);

    void onNetworkError(int errorType ,String message);
}
