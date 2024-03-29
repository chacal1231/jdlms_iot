package org.openmuc.jdlms.transportlayer.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stream accessor interface to access a physical stream to a remote meter.
 * 
 * @see TransportLayer
 */
public interface StreamAccessor extends AutoCloseable {

    /**
     * Enable/disable TIMEOUT with the specified timeout, in milliseconds. With this option set to a non-zero timeout, a
     * read() call on the {@link InputStream}m associated with this StreamAcessor will block for only this amount of
     * time. If the timeout expires, a java.net.SocketTimeoutException is raised, though the Socket is still valid. The
     * option must be enabled prior to entering the blocking operation to have effect. The timeout must be
     * <code>&gt; 0</code>. A timeout of zero is interpreted as an infinite timeout.
     * 
     * @param timeout
     *            the specified timeout, in milliseconds.
     * @throws IOException
     *             if there is an error in the underlying protocol, such as a TCP error.
     */
    void setTimeout(int timeout) throws IOException;

    /**
     * Returns the input stream for this stream accessor.
     * 
     * @return the input stream.
     * @throws IOException
     *             an I/O error accessing the input stream, the stream accessor is closed, the stream accessor is not
     *             connected.
     */
    DataInputStream getInputStream() throws IOException;

    /**
     * Returns the output stream for this stream accessor.
     * 
     * @return the output stream.
     * @throws IOException
     *             an I/O error accessing the output stream, the stream accessor is closed, the stream accessor is not
     *             connected.
     */
    DataOutputStream getOutpuStream() throws IOException;

    @Override
    void close() throws IOException;

}
