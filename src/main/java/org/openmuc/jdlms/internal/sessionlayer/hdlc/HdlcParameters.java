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
package org.openmuc.jdlms.internal.sessionlayer.hdlc;

import java.nio.ByteBuffer;
import java.text.MessageFormat;

import org.bouncycastle.util.Arrays;

/**
 * This class represents optional parameter that are negotiated during the connection phase between client and server on
 * the HDLC layer.
 * 
 * For more information, see IEC 62056-46 section 6.4.4.4.3.2 and ISO 13239 section 5.5.3.2.2
 */
public class HdlcParameters {
    public static final int MIN_INFORMATION_LENGTH = 128;
    public static final int MAX_INFORMATION_LENGTH = 2030;
    public static final int MIN_WINDOW_SIZE = 1;
    public static final int MAX_WINDOW_SIZE = 7;

    private static final byte MAX_TRANS_INFO_LENGTH_ID = 0x05;
    private static final byte MAX_REC_INFO_LENGTH_ID = 0x06;
    private static final byte TRANS_WINDOW_SIZE_ID = 0x07;
    private static final byte REC_WINDOW_SIZE_ID = 0x08;

    private static final byte FORMAT_IDENTIFIER = (byte) 0x81;
    private static final byte HDLC_PARAM_IDENTIFIER = (byte) 0x80;
    private static final byte USER_PARAM_IDENTIFIER = (byte) 0xF0;

    private Param maxTransmitInformationLength;
    private Param maxReceiveInformationLength;
    private Param transmitWindowSize;
    private Param receiveWindowSize;

    private HdlcParameters() {
        this.maxTransmitInformationLength = new Param(MIN_INFORMATION_LENGTH);
        this.maxReceiveInformationLength = new Param(MIN_INFORMATION_LENGTH);

        this.transmitWindowSize = new Param(MIN_WINDOW_SIZE);
        this.receiveWindowSize = new Param(MIN_WINDOW_SIZE);
    }

    public HdlcParameters(int receiveInformationLength, int receiveWindowSize) {
        this(receiveInformationLength, receiveWindowSize, MIN_INFORMATION_LENGTH, MIN_WINDOW_SIZE);
    }

    public HdlcParameters(int receiveInformationLength, int receiveWindowSize, int transmitInformationLength,
            int transmitWindowSize) {
        this.maxReceiveInformationLength = new Param(valueConsiderInformationLength(receiveInformationLength));
        this.receiveWindowSize = new Param(valueConsiderWindowSize(receiveWindowSize));

        this.maxTransmitInformationLength = new Param(valueConsiderInformationLength(transmitInformationLength));
        this.transmitWindowSize = new Param(valueConsiderWindowSize(transmitWindowSize));
    }

    public int getTransmitInformationLength() {
        return maxTransmitInformationLength.getValue();
    }

    public int getTransmitWindowSize() {
        return transmitWindowSize.getValue();
    }

    public int getReceiveInformationLength() {
        return maxReceiveInformationLength.getValue();
    }

    public int getReceiveWindowSize() {
        return receiveWindowSize.getValue();
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "'{'\"maxTransmitInformationLength\": {0}, \"transmitWindowSize\": {1}, \"maxReceiveInformationLength\": {2}, \"receiveWindowSize\": {3}'}'",
                this.maxTransmitInformationLength, this.transmitWindowSize, this.maxReceiveInformationLength,
                this.receiveWindowSize);
    }

    private static int valueConsiderWindowSize(int value) {
        return valueConsiderRange(MIN_WINDOW_SIZE, MAX_WINDOW_SIZE, value);
    }

    private static int valueConsiderInformationLength(int value) {
        return valueConsiderRange(MIN_INFORMATION_LENGTH, MAX_INFORMATION_LENGTH, value);
    }

    private static int valueConsiderRange(int lowerBound, int upperBound, int value) {
        int maxLowValue = Math.max(lowerBound, value);

        return Math.min(maxLowValue, upperBound);
    }

    public static HdlcParameters decode(byte[] data) throws FrameInvalidException {
        ByteBuffer bBuf = ByteBuffer.wrap(data);

        HdlcParameters parameterNegotiation = new HdlcParameters();

        byte nextByte = bBuf.get();

        if (nextByte != FORMAT_IDENTIFIER) {
            throw new FrameInvalidException("Information field is no HDLC parameter negotiation");
        }

        nextByte = bBuf.get();
        while (bBuf.hasRemaining()) {
            if (nextByte == USER_PARAM_IDENTIFIER) {
                readUserId(bBuf);
            }
            else if (nextByte == HDLC_PARAM_IDENTIFIER) {
                readParamId(bBuf, parameterNegotiation);
            }
            else {
                // error?
            }
        }

        return parameterNegotiation;
    }

    private static void readUserId(ByteBuffer bBuf) {
        byte length = bBuf.get();
        bBuf.position(bBuf.position() + length);
    }

    private static void readParamId(ByteBuffer bBuf, HdlcParameters parameterNegotiation) throws FrameInvalidException {
        int numOfRemainingBytes = bBuf.get();
        while (numOfRemainingBytes > 0) {
            int paramIdent = bBuf.get();
            int paramLength = bBuf.get();

            switch (paramIdent) {
            case MAX_TRANS_INFO_LENGTH_ID:
                parameterNegotiation.maxTransmitInformationLength = new Param(readData(bBuf, paramLength));
                break;
            case MAX_REC_INFO_LENGTH_ID:
                parameterNegotiation.maxReceiveInformationLength = new Param(readData(bBuf, paramLength));
                break;
            case TRANS_WINDOW_SIZE_ID:
                parameterNegotiation.transmitWindowSize = new Param(readData(bBuf, paramLength));
                break;
            case REC_WINDOW_SIZE_ID:
                parameterNegotiation.receiveWindowSize = new Param(readData(bBuf, paramLength));
                break;
            default:
                throw new FrameInvalidException("Hdlc parameter unknown");
            }

            numOfRemainingBytes -= (2 + paramLength);
        }
    }

    private static int readData(ByteBuffer bBuf, final int length) {
        int result = 0;

        for (int i = 0; i < length; i++) {
            result = (result << 8) | (bBuf.get() & 0xFF);
        }

        return result;
    }

    public byte[] encode() {
        int numOfBits = 11 + this.maxReceiveInformationLength.getNumOfBytes()
                + this.maxTransmitInformationLength.getNumOfBytes() + this.receiveWindowSize.getNumOfBytes()
                + this.transmitWindowSize.getNumOfBytes();

        byte infoLength = (byte) (numOfBits - 3);

        return ByteBuffer.allocate(numOfBits)
                .put(FORMAT_IDENTIFIER)
                .put(HDLC_PARAM_IDENTIFIER)
                .put(infoLength)
                .put(MAX_TRANS_INFO_LENGTH_ID)
                .put(this.maxTransmitInformationLength.getNumOfBytes())
                .put(this.maxTransmitInformationLength.encode())
                .put(MAX_REC_INFO_LENGTH_ID)
                .put(this.maxReceiveInformationLength.getNumOfBytes())
                .put(this.maxReceiveInformationLength.encode())
                .put(TRANS_WINDOW_SIZE_ID)
                .put(this.transmitWindowSize.getNumOfBytes())
                .put(this.transmitWindowSize.encode())
                .put(REC_WINDOW_SIZE_ID)
                .put(this.receiveWindowSize.getNumOfBytes())
                .put(this.receiveWindowSize.encode())
                .array();
    }

    private static class Param {
        private final int value;
        private final byte numOfBytes;

        public Param(int value) {
            this.value = value;

            this.numOfBytes = numofBytes(value);
        }

        public byte getNumOfBytes() {
            return numOfBytes;
        }

        public int getValue() {
            return value;
        }

        public byte[] encode() {

            ByteBuffer buffer = ByteBuffer.allocate(4).putInt(this.value);
            buffer.flip();
            buffer.position(4 - this.numOfBytes);

            return Arrays.copyOfRange(buffer.array(), 4 - this.numOfBytes, 4);
        }

        @Override
        public String toString() {
            return String.valueOf(getValue());
        }

        private static byte numofBytes(double x) {
            return (byte) Math.ceil(numOfBits(x) / 8.);
        }

        private static double numOfBits(double x) {
            return Math.floor(log2(x)) + 1;
        }

        private static double log2(double x) {
            return Math.log(x) / Math.log(2);
        }

    }

}
