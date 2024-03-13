package ru.otus.exceptions;

public class NodeFaultException extends RuntimeException {
    public NodeFaultException(String message) {
        super(message);
    }
}
