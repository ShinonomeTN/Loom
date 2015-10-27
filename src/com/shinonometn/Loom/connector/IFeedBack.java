package com.shinonometn.Loom.connector;

import com.shinonometn.Loom.connector.Message.FeedbackMessage;
import com.shinonometn.Pupa.Pupa;

/**
 * Created by catten on 15/10/23.
 */
public interface IFeedBack {
    void feedBackPackage(Pupa pupa,FeedbackMessage feedBackMessage);
}
