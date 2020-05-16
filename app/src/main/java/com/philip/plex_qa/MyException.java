package com.philip.plex_qa;

public class MyException extends Exception {
    String message;
    public MyException(String message){
        this.message=message;
    }
    public String getMessage(){
        return message;
    }
}
