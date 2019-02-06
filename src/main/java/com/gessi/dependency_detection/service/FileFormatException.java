package com.gessi.dependency_detection.service;

public class FileFormatException extends Exception {

    public FileFormatException() {
	super();
    }

    public FileFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }

    public FileFormatException(String message, Throwable cause) {
	super(message, cause);
    }

    public FileFormatException(String message) {
	super(message);
    }

    public FileFormatException(Throwable cause) {
	super(cause);
    }

}
