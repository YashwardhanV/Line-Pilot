package com.yashwardhanv.linepilot.notify;

/** Published by QueueService when staff call a token whose customer left a phone number. */
public record TokenCalledEvent(String phone, String displayNumber, String queueName, String counterLabel) {

    public String message() {
        String where = counterLabel == null ? "the desk" : counterLabel;
        return "LinePilot: token " + displayNumber + " is being called at " + where
                + " (" + queueName + "). Please go there now.";
    }
}
