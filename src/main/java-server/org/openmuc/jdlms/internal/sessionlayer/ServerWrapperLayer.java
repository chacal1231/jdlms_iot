package org.openmuc.jdlms.internal.sessionlayer;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.openmuc.jdlms.internal.settings.ServerSettings;
import org.openmuc.jdlms.sessionlayer.WrapperHeader;
import org.openmuc.jdlms.transportlayer.client.StreamAccessor;

public class ServerWrapperLayer implements ServerSessionLayer {

    private final StreamAccessor streamAccessor;

    private int logicalDevice;
    private int clientId;

    private WrapperHeader.WrapperHeaderBuilder headerBuilder;

    private final ServerSettings settings;

    public ServerWrapperLayer(StreamAccessor streamAccessor, ServerSettings settings) {
        this.streamAccessor = streamAccessor;
        this.settings = settings;

        this.logicalDevice = -1;
        this.clientId = -1;
    }

    @Override
    public void send(byte[] data) throws IOException {
        byte[] header = this.headerBuilder.setLength(data.length).build().encode();

        byte[] wpdu = ByteBuffer.allocate(data.length + WrapperHeader.HEADER_LENGTH).put(header).put(data).array();

        this.streamAccessor.getOutpuStream().write(wpdu);
        this.streamAccessor.getOutpuStream().flush();
    }

    @Override
    public byte[] readNextMessage() throws IOException {
        streamAccessor.setTimeout(this.settings.inactivityTimeout);
        WrapperHeader header = WrapperHeader.decode(streamAccessor, this.settings.responseTimeout);

        if (this.logicalDevice == -1 || this.clientId == -1) {
            this.logicalDevice = header.destinationWPort();
            this.clientId = header.sourceWPort();
            this.headerBuilder = WrapperHeader.builder(this.logicalDevice, this.clientId);
        }
        validateHeader(header);

        byte[] data = new byte[header.payloadLength()];
        this.streamAccessor.getInputStream().readFully(data);

        return data;
    }

    private void validateHeader(WrapperHeader header) {
        if (this.logicalDevice != header.destinationWPort() || this.clientId != header.sourceWPort()) {
            // TODO
        }
    }

    @Override
    public int getClientId() {
        return this.clientId;
    }

    @Override
    public int getLogicalDeviceId() {
        return this.logicalDevice;
    }

    @Override
    public void close() throws IOException {
        this.streamAccessor.close();
    }

    @Override
    public void initialize() {
        // nothing to do here..
    }

}
