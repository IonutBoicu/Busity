package com.example.john.driverbusity;

/**
 * Created by John on 4/24/2017.
 */

public class ResponseMessage {
    private String type;
    private String message;
    public ResponseMessage(){

    }
    public ResponseMessage(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }
}