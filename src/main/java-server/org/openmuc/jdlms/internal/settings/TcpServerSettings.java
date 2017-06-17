package org.openmuc.jdlms.internal.settings;

public class TcpServerSettings extends ServerSettings {

    public int tcpPort;

    public TcpServerSettings(int tcpPort) {
        this.tcpPort = tcpPort;
    }
}
