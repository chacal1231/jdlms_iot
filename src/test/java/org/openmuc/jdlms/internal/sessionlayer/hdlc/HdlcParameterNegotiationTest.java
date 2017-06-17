/*
 * Copyright 2012-17 Fraunhofer ISE
 *
 * This file is part of jDLMS. For more information visit http://www.openmuc.org
 *
 * jDLMS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * jDLMS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with jDLMS. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jdlms.internal.sessionlayer.hdlc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openmuc.jasn1.util.HexConverter;

public class HdlcParameterNegotiationTest {

    @Test
    public void tesEncodeDecodetSymmetry() throws Exception {
        final int receiveInformationLength = HdlcParameters.MIN_INFORMATION_LENGTH + 2;
        final int receiveWindowSize = HdlcParameters.MIN_WINDOW_SIZE + 3;
        HdlcParameters parameterNegotiation = new HdlcParameters(receiveInformationLength, receiveWindowSize);

        byte[] encodedParameterNegotiation = parameterNegotiation.encode();

        HdlcParameters parameterNegotiation2 = HdlcParameters.decode(encodedParameterNegotiation);

        assertEquals(receiveInformationLength, parameterNegotiation2.getReceiveInformationLength());
        assertEquals(receiveWindowSize, parameterNegotiation2.getReceiveWindowSize());
    }

    @Test
    public void testSymmetry() throws Exception {
        HdlcParameters par = new HdlcParameters(HdlcParameters.MAX_INFORMATION_LENGTH, HdlcParameters.MIN_WINDOW_SIZE);

        HdlcParameters parameters = HdlcParameters.decode(par.encode());

        assertEquals(par.getReceiveInformationLength(), parameters.getReceiveInformationLength());
        assertEquals(par.getTransmitInformationLength(), parameters.getTransmitInformationLength());

        assertEquals(par.getReceiveWindowSize(), parameters.getReceiveWindowSize());
        assertEquals(par.getTransmitWindowSize(), parameters.getTransmitWindowSize());

    }

    @Test
    public void encode() throws Exception {
        HdlcParameters dNegotiation = new HdlcParameters(0x80, 0x01, 0x80, 0x01);

        byte[] enc = HexConverter.fromShortHexString("81800C050180060180070101080101");

        byte[] data = dNegotiation.encode();

        assertArrayEquals(enc, data);
    }
}
