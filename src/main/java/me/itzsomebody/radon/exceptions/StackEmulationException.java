package me.itzsomebody.radon.exceptions;

public class StackEmulationException extends Exception { // Force handling
    private static final long serialVersionUID = -6972872732925009110L;

    public StackEmulationException(String msg) {
        super(msg);
    }
}
