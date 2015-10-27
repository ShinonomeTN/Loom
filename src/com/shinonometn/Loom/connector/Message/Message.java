package com.shinonometn.Loom.connector.Message;

/**
 * Created by catten on 15/10/25.
 */
public class Message{
    protected String message = "a message";
    public Message(){
        message = "a new message";
    }

    public String toString(){
        return message;
    }
}
