package org.openmuc.jdlms;

/**
 * This error indicates, that the server was set up in a incorrect way.
 */
public class IllegalPametrizationError extends Error {

    public IllegalPametrizationError(String message) {
        super(message);
    }

}
