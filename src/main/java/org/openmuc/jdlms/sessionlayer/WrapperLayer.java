package org.openmuc.jdlms.sessionlayer;

import static java.lang.String.format;
import static org.openmuc.jdlms.JDlmsException.Fault.SYSTEM;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.openmuc.jdlms.FatalJDlmsException;
import org.openmuc.jdlms.JDlmsException.ExceptionId;
import org.openmuc.jdlms.RawMessageData;
import org.openmuc.jdlms.RawMessageData.MessageSource;
import org.openmuc.jdlms.RawMessageData.RawMessageDataBuilder;
import org.openmuc.jdlms.RawMessageListener;
import org.openmuc.jdlms.internal.sessionlayer.SessionLayerListener;
import org.openmuc.jdlms.sessionlayer.WrapperHeader.WrapperHeaderBuilder;
import org.openmuc.jdlms.settings.client.TcpSettings;
import org.openmuc.jdlms.transportlayer.client.TransportLayer;

public class WrapperLayer implements SessionLayer {

    private final WrapperHeaderBuilder headerBuilder;
    private final TransportLayer transportLayer;

    private SessionLayerListener eventListener;
    private final TcpSettings settings;
    private boolean closed;

    public WrapperLayer(TcpSettings settings, TransportLayer transportLayer) throws IOException {
        this.settings = settings;
        this.headerBuilder = WrapperHeader.builder(settings.clientId(), settings.logicalDeviceId());
        this.transportLayer = transportLayer;

        this.closed = true;
    }

    @Override
    public void startListening(SessionLayerListener eventListener) throws IOException {
        this.eventListener = eventListener;
        if (!closed) {
            return;
        }

        transportLayer.open();
        this.closed = false;

        Thread readerThread = new Thread(new ConnectionReader());
        readerThread.setName("jDLMS - WRAPPER/TCP-CONNECTION_READER");
        readerThread.start();
    }

    @Override
    public void send(byte[] tSdu, int off, int len, RawMessageDataBuilder rawMessageDataBuilder) throws IOException {
        byte[] encodeBytes = this.headerBuilder.setLength(len).build().encode();

        byte[] wpdu = ByteBuffer.allocate(len + WrapperHeader.HEADER_LENGTH)
                .put(encodeBytes)
                .put(tSdu, off, len)
                .array();

        try {
            this.transportLayer.getOutpuStream().write(wpdu);
            this.transportLayer.getOutpuStream().flush();
        } finally {
            RawMessageListener rawMessageListener = this.settings.rawMessageListener();
            if (rawMessageListener != null) {
                rawMessageListener.messageCaptured(
                        rawMessageDataBuilder.setMessage(wpdu).setMessageSource(MessageSource.CLIENT).build());
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.transportLayer.close();
        this.closed = true;
    }

    private void validate(WrapperHeader header, int payloadLength) throws FatalJDlmsException {
        if (header.sourceWPort() != settings.logicalDeviceId()) {
            throw new FatalJDlmsException(ExceptionId.WRAPPER_HEADER_INVALID_SRC_DEST_ADDR, SYSTEM,
                    format("Connection was initiated with logical device address %d, but server answered with %d",
                            settings.logicalDeviceId(), header.sourceWPort()));
        }

        if (payloadLength != header.payloadLength()) {
            throw new FatalJDlmsException(ExceptionId.WRAPPER_HEADER_INVALID_PAYLOAD_LENGTH, SYSTEM,
                    format("Header specified length to be %d, but was %d.", header.payloadLength(), payloadLength));
        }

    }

    private class ConnectionReader implements Runnable {

        @Override
        public void run() {

            try {
                DataInputStream inputStream = transportLayer.getInputStream();
                while (!transportLayer.isClosed()) {
                    read(inputStream);
                    Thread.yield();
                }
            } catch (EOFException e) {
                if (!closed) {
                    EOFException ex = new EOFException("Socket was closed by remote host.");
                    eventListener.connectionInterrupted(ex);
                }
            } catch (IOException e) {
                eventListener.connectionInterrupted(e);
            } finally {
                unsaveClose();
            }
        }

        private void read(DataInputStream inputStream) throws IOException {
            transportLayer.setTimeout(0);
            WrapperHeader header = WrapperHeader.decode(transportLayer, settings.responseTimeout());
            byte[] messageData = new byte[header.payloadLength()];
            inputStream.readFully(messageData);

            validate(header, messageData.length);

            byte[] message = null;
            if (settings.rawMessageListener() != null) {
                message = ByteBuffer.allocate(WrapperHeader.HEADER_LENGTH + header.payloadLength())
                        .put(header.encode())
                        .put(messageData)
                        .array();
            }
            RawMessageDataBuilder rawMessageBuilder = null;

            if (WrapperLayer.this.settings.rawMessageListener() != null) {
                rawMessageBuilder = RawMessageData.builder().setMessage(message);
            }
            eventListener.dataReceived(messageData, rawMessageBuilder);

        }

    }

    private void unsaveClose() {
        if (!closed) {
            try {
                close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

}
