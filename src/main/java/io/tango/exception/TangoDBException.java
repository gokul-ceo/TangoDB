package io.tango.exception;

public class TangoDBException extends  RuntimeException{

    public TangoDBException(String message, Throwable throwable){
        super(message,throwable);
    }

    public TangoDBException(String message){
        super(message);
    }
}
