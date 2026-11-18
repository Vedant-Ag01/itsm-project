package edu.bits.itsm.domain;

public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException(String name) {
        super("Service '" + name + "' is not available. Choose a service from the catalogue.");
    }
}