package org.openmuc.jdlms.internal.cli;

public interface ActionListener {

    public void actionCalled(String actionKey, int ValToRead) throws ActionException;

    public void quit();

}
