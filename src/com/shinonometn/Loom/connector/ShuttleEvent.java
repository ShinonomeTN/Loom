package com.shinonometn.Loom.connector;

/**
 * Created by catten on 15/10/23.
 */
public interface ShuttleEvent {
    void onAction(int actionType);
    void onMail(String mail);
    void onStateChange(int state);
}
