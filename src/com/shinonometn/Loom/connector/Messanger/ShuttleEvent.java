package com.shinonometn.Loom.connector.Messanger;

/**
 * Created by catten on 15/11/3.
 */
public interface ShuttleEvent {
    int SHUTTLE_SERVER_RESPONSE = 0;
    int SHUTTLE_SERVER_NOT_FOUNT = 1;
    int SHUTTLE_SERVER_NO_RESPONSE = 2;
    int SHUTTLE_PORT_IN_USE = 3;
    int SHUTTLE_GET_SOCKET_SUCCESS = 4;
    int SHUTTLE_OTHER_EXCEPTION = 5;

    int SHUTTLE_CERTIFICATE_SUCCESS = 6;
    int SHUTTLE_CERTIFICATE_FAILED = 7;

    int SHUTTLE_SERVER_MESSAGE = 8;

    int SHUTTLE_BREATHE_SUCCESS = 9;
    int SHUTTLE_BREATHE_FAILED = 10;
    int SHUTTLE_BREATHE_EXCEPTION = 11;

    int SHUTTLE_MSGTHREAD_CLOSE = 12;

    int SHUTTLE_OFFLINE = 13;

    void onMessage(int messageType, String message);
}
