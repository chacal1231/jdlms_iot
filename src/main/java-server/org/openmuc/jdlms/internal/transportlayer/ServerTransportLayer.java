package org.openmuc.jdlms.internal.transportlayer;

import java.io.IOException;

public interface ServerTransportLayer extends AutoCloseable {

    void start() throws IOException;

    @Override
    void close() throws IOException;

}
