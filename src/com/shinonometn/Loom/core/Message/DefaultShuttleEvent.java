package com.shinonometn.Loom.core.Message;

import com.shinonometn.Loom.common.Logger;

/**
 * Created by catten on 16/2/18.
 */
public class DefaultShuttleEvent implements ShuttleEvent {
        @Override
        public void onMessage(int messageType, String message) {
            Logger.log(String.format("[Message code %d]:%s",messageType,message));
        }

        @Override
        public void onNetworkError(int errorType, String message) {
            Logger.error(String.format("[Error code %d]:%s",errorType,message));
        }
}
