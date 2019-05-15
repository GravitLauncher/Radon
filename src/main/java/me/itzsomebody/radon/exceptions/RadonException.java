package me.itzsomebody.radon.exceptions;

public class RadonException extends RuntimeException {
    private static final long serialVersionUID = -3890080206593518650L;

    public RadonException() {
        super();
    }

    public RadonException(String msg) {
        super(msg);
    }

    public RadonException(Throwable t) {
        super(t);
    }
}
