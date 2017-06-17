/*
 * Copyright 2012-17 Fraunhofer ISE
 *
 * This file is part of jDLMS.
 * For more information visit http://www.openmuc.org
 *
 * jDLMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jDLMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jDLMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jdlms;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.openmuc.jdlms.internal.sessionlayer.hdlc.HdlcAddress;
import org.openmuc.jdlms.internal.sessionlayer.hdlc.HdlcAddressPair;
import org.openmuc.jdlms.sessionlayer.HdlcLayer;
import org.openmuc.jdlms.sessionlayer.SessionLayer;
import org.openmuc.jdlms.sessionlayer.WrapperLayer;
import org.openmuc.jdlms.settings.client.HdlcTcpSettings;
import org.openmuc.jdlms.transportlayer.client.TcpLayer;

/**
 * Builder class to establish a DLMS connection via TCP.
 */
public class TcpConnectionBuilder extends ConnectionBuilder<TcpConnectionBuilder> {

    private static final int DEFAULT_DLMS_PORT = 4059;
    private InetAddress inetAddress;
    private int tcpPort;
    private TcpSessionLayerType sessionLayerType;

    /**
     * Construct a {@link TcpConnectionBuilder} with client ID 1, logical device address 16 and a default TCP port 4059.
     * 
     * @param inetAddress
     *            the Internet address of the remote meter.
     */
    public TcpConnectionBuilder(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
        this.tcpPort = DEFAULT_DLMS_PORT;
        this.sessionLayerType = TcpSessionLayerType.WRAPPER;

    }

    public TcpConnectionBuilder(String inetAddress) throws UnknownHostException {
        this(InetAddress.getByName(inetAddress));
    }

    /**
     * Set the Internet address of the remote meter.
     * 
     * @param inetAddress
     *            the Internet address.
     * @return the builder.
     */
    public TcpConnectionBuilder setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
        return this;
    }

    /**
     * Set the TCP port of the remote meter.
     * 
     * @param tcpPort
     *            the TCP port.
     * @return the builder.
     */
    public TcpConnectionBuilder setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
        return this;
    }

    public TcpConnectionBuilder useHdlc() {
        this.sessionLayerType = TcpSessionLayerType.HDLC;
        return this;
    }

    public TcpConnectionBuilder useWrapper() {
        this.sessionLayerType = TcpSessionLayerType.WRAPPER;
        return this;
    }

    @Override
    public DlmsConnection build() throws IOException {
        TcpSettingsImpl settings = new TcpSettingsImpl(this);

        SessionLayer sessionLayer = buildSessionLayer(settings);

        return buildConnection(settings, sessionLayer);
    }

    private SessionLayer buildSessionLayer(TcpSettingsImpl settings) throws IOException {
        switch (sessionLayerType) {
        case HDLC:
            return new HdlcLayer(settings);

        default:
        case WRAPPER:
            TcpLayer tcpLayer = new TcpLayer(settings);
            return new WrapperLayer(settings, tcpLayer);
        }
    }

    public class TcpSettingsImpl extends SettingsImpl implements HdlcTcpSettings {

        private final InetAddress inetAddress;
        private final int tcpPort;
        private final HdlcAddressPair addressPair;

        public TcpSettingsImpl(TcpConnectionBuilder connectionBuilder) {
            super(connectionBuilder);
            this.inetAddress = connectionBuilder.inetAddress;
            this.tcpPort = connectionBuilder.tcpPort;

            HdlcAddress source = new HdlcAddress(clientId());
            HdlcAddress destination = new HdlcAddress(logicalDeviceId(), physicalDeviceId());
            this.addressPair = new HdlcAddressPair(source, destination);
        }

        @Override
        public InetAddress inetAddress() {
            return this.inetAddress;
        }

        @Override
        public int tcpPort() {
            return this.tcpPort;
        }

        @Override
        public HdlcAddressPair addressPair() {
            return this.addressPair;
        }

    }

    private enum TcpSessionLayerType {
        HDLC,
        WRAPPER
    }
}
