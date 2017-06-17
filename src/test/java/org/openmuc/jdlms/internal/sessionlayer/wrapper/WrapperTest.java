package org.openmuc.jdlms.internal.sessionlayer.wrapper;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.openmuc.jdlms.FatalJDlmsException;
import org.openmuc.jdlms.HexConverter;
import org.openmuc.jdlms.sessionlayer.WrapperHeader;
import org.openmuc.jdlms.transportlayer.client.StreamAccessor;
import org.powermock.api.mockito.PowerMockito;

public class WrapperTest {

    @Test
    public void decode1() throws Exception {
        byte[] bytes = HexConverter.fromShortHexString(
                "0001001000110022602080020780a109060760857405080102be0f040d01000000065f0400181a200000");

        StreamAccessor streamAccessor = dataToStreamAccessor(bytes);
        InputStream inputStream = streamAccessor.getInputStream();

        WrapperHeader wrapperHeader = WrapperHeader.decode(streamAccessor, 0);

        byte[] dlmsData = new byte[wrapperHeader.payloadLength()];
        int length = inputStream.read(dlmsData);

        assertEquals(wrapperHeader.payloadLength(), length);

    }

    @Test(expected = FatalJDlmsException.class)
    public void decode2() throws Exception {
        byte[] bytes = HexConverter.fromShortHexString(
                "0002001000110022602080020780a109060760857405080102be0f040d01000000065f0400181a200000");

        StreamAccessor streamAccessor = dataToStreamAccessor(bytes);

        WrapperHeader.decode(streamAccessor, 0);
    }

    private StreamAccessor dataToStreamAccessor(byte[] data) throws IOException {
        StreamAccessor streamAccessor = PowerMockito.mock(StreamAccessor.class);

        when(streamAccessor.getInputStream()).thenReturn(new DataInputStream(new ByteArrayInputStream(data)));
        return streamAccessor;
    }

}
