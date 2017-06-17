package org.openmuc.jdlms;

import java.io.IOException;

import org.openmuc.jdlms.internal.DataDirectory;
import org.openmuc.jdlms.internal.sessionlayer.ServerSessionLayerFactories;
import org.openmuc.jdlms.internal.sessionlayer.ServerSessionLayerFactory;
import org.openmuc.jdlms.internal.settings.TcpServerSettings;
import org.openmuc.jdlms.internal.transportlayer.ServerTcpLayer;
import org.openmuc.jdlms.internal.transportlayer.ServerTransportLayer;

/**
 * Class representing a physical device (DLMS/COSEM server).
 */
public class DlmsServer implements AutoCloseable {

    private final ServerTransportLayer serverTransportLayer;

    DlmsServer(ServerTransportLayer serverTransportLayer) {
        this.serverTransportLayer = serverTransportLayer;
    }

    /**
     * Starts the DLMS/COSEM server.
     * 
     * @throws IOException
     *             if an exception occurs during the start of the server.
     */
    void start() throws IOException {
        serverTransportLayer.start();
    }

    /**
     * Stops the server immediately. Releases all acquired resources.
     */
    @Override
    public void close() throws IOException {
        this.serverTransportLayer.close();
    }

    /**
     * Sends disconnect messages to all connected clients.
     * 
     * @throws IOException
     *             if an exception occurs, while releasing the resources.
     */
    public void shutdown() throws IOException {
        // TODO:
        close();
    }

    /**
     * Create a new TCP server builder.
     * 
     * @param port
     *            the TCP port the server starts listening on.
     * @return a new TcpServerBuilder.
     */
    public static TcpServerBuilder tcpServerBuilder(int port) {
        return new TcpServerBuilder(port);
    }

    /**
     * Builder to create a TCP physical device/server.
     */
    public static class TcpServerBuilder extends ServerBuilder<TcpServerBuilder> {

        private int port;
        private ServerSessionLayerFactory sessionLayerFactory;

        private TcpServerBuilder(int port) {
            this.port = port;
            this.sessionLayerFactory = ServerSessionLayerFactories.newWrapperSessionLayerFactory();
        }

        /**
         * 
         * Set the server session layer factory.
         * 
         * @param sessionLayerFactory
         *            the session layer factory.
         * @return the current builder instance.
         * 
         * @see ServerSessionLayerFactories#newWrapperSessionLayerFactory()
         * @see ServerSessionLayerFactories#newHdlcSessionLayerFactory()
         */
        public TcpServerBuilder setSessionLayerFactory(ServerSessionLayerFactory sessionLayerFactory) {
            this.sessionLayerFactory = sessionLayerFactory;
            return this;
        }

        /**
         * The port a client may access the server.
         * 
         * @param port
         *            the TCP port the server starts listening on.
         * @return the current builder instance.
         */
        public TcpServerBuilder setTcpPort(int port) {
            this.port = port;
            return this;
        }

        @Override
        public DlmsServer build() throws IOException {
            final DataDirectory dataDirectory = parseLogicalDevices();

            final TcpServerSettings settings = new TcpServerSettings(this.port);
            setPropertiesTo(settings);

            ServerTcpLayer serverLayer = new ServerTcpLayer(settings, dataDirectory, sessionLayerFactory);
            return newServer(serverLayer);
        }

    }

}
