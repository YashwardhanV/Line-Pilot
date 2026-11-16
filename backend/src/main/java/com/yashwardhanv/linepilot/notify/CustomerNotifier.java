package com.yashwardhanv.linepilot.notify;

/** Sends a short text message to a customer's phone (E.164 format, e.g. +919876543210). */
public interface CustomerNotifier {
    void send(String phone, String message);
}
