package com.hotel.pms.fiscal;

public class FiscalException extends Exception {
    public FiscalException(String message) {
        super(message);
    }

    public FiscalException(String message, Throwable cause) {
        super(message, cause);
    }
}

