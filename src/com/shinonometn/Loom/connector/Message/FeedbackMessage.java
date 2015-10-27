package com.shinonometn.Loom.connector.Message;

/**
 * Created by catten on 15/10/25.
 */
public class FeedbackMessage extends Message {
    public static int SERVER_NORESPONSE = -1;
    public static int SERVER_RESPONSE = 0;
    public static int SERVER_TIMEOUT = 1;
    public static int SERVER_NO_ROUTE = 2;
    public static int PORT_INUSE = 3;
    public static int SUPPLICAN_FAILED = 4;
    public static int SUPPLICAN_SUCCESS = 5;
    //public static int


    private int messageNumber;

    public FeedbackMessage(int MessageNumber){
        super();
        messageNumber = MessageNumber;
        if(MessageNumber == SERVER_NO_ROUTE){
            message = "server no route";
        }else if(MessageNumber == SERVER_NORESPONSE){
            message = "server no response";
        }else if(MessageNumber == SERVER_RESPONSE){
            message = "server response";
        }else if(MessageNumber == SERVER_TIMEOUT) {
            message = "server response timeout";
        }else if(MessageNumber == PORT_INUSE) {
            message = "net port in use";
        }else if(MessageNumber == SUPPLICAN_FAILED) {
            message = "server reject";
        }else if(MessageNumber == SUPPLICAN_SUCCESS){
            message = "auth. success";
        }else {
            message = "thread feedback message";
        }
    }

    public int getMessageNumber(){
        return messageNumber;
    }
}
