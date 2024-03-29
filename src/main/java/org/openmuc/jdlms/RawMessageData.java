package org.openmuc.jdlms;

import static java.text.MessageFormat.format;

/**
 * jDLMS raw message data for logging purposes.
 * 
 * @see TcpConnectionBuilder#setRawMessageListener(RawMessageListener)
 */
public class RawMessageData {

    private final MessageSource messageSource;
    private final byte[] message;
    private final Apdu apdu;

    private RawMessageData(MessageSource messageSource, byte[] message, Apdu apdu) {
        this.messageSource = messageSource;
        this.message = message;
        this.apdu = apdu;
    }

    /**
     * Get the source of the message.
     * 
     * @return the source of the message.
     */
    public MessageSource getMessageSource() {
        return this.messageSource;

    }

    /**
     * Get the whole message, which is transmitted between client and server.
     * 
     * @return the message as byte array.
     */
    public byte[] getMessage() {
        return this.message;

    }

    /**
     * Get the APDU.
     * 
     * @return return the APDU.
     */
    public Apdu getApdu() {
        return this.apdu;
    }

    @Override
    public String toString() {
        String pattern2 = "{0}: {1}";
        String pattern = "{0}: {1},\n";
        return new StringBuilder().append(format(pattern, "Source", this.messageSource))
                .append(format(pattern, "Message", HexConverter.toHexString(this.message, 0, 30)))
                .append(format(pattern2, "APDU", this.apdu))
                .toString();
    }

    /**
     * The source of the message.
     */
    public enum MessageSource {
        /**
         * The client has send the message.
         */
        CLIENT,
        /**
         * The server has send the message.
         */
        SERVER
    }

    /**
     * Representation of an APDU.
     */
    public static class Apdu {

        private final CosemPdu cosemPdu;
        private final byte[] acsePdu;

        /**
         * Construct an new APDU.
         * 
         * @param cosemPdu
         *            the COSEM PDU.
         * @param acsePdu
         *            the ACSE PDU.
         */
        public Apdu(CosemPdu cosemPdu, byte[] acsePdu) {
            this.cosemPdu = cosemPdu;
            this.acsePdu = acsePdu;
        }

        /**
         * Get the COSEM pdu.
         * 
         * <p>
         * This is null for a graceful disconnect.
         * </p>
         * 
         * @return the COSEM PDU.
         */
        public CosemPdu getCosemPdu() {
            return this.cosemPdu;

        }

        /**
         * Get the ACSE PDU of the DLMS message.
         * 
         * <p>
         * Note: this may be null.
         * </p>
         * 
         * @return the ACSE PDU.
         */
        public byte[] getAcsePdu() {
            return this.acsePdu;
        }

        @Override
        public String toString() {
            final String formatStr = "{0}:\n{1}\n";
            return new StringBuilder().append('\n')
                    .append(format(formatStr, "ACSE PDU", nullableArrayToString(getAcsePdu())))
                    .append(format(formatStr, "COSEM PDU", getCosemPdu()))
                    .toString();
        }

    }

    private static String nullableArrayToString(byte[] data) {
        return data == null ? null : HexConverter.toHexString(data);
    }

    /**
     * Representation of a raw COSEM PDU.
     */
    public static class CosemPdu {
        private final byte[] cipheredCosemPdu;
        private final byte[] plainCosemPdu;

        public CosemPdu(byte[] cipheredCosemPdu, byte[] plainCosemPdu) {
            this.cipheredCosemPdu = cipheredCosemPdu;
            this.plainCosemPdu = plainCosemPdu;
        }

        /**
         * Get the ciphered COSEM PDU.
         * 
         * <p>
         * NOTE: this may be {@code null} if the transmitted message is no encrypted.
         * </p>
         * 
         * @return the ciphered COSEM PDU as byte array.
         */
        public byte[] getCipheredCosemPdu() {
            return this.cipheredCosemPdu;
        }

        /**
         * Get the plain COSEM PDU
         * 
         * @return returns the plain/unencrypted COSEM PDU.
         */
        public byte[] getPlainCosemPdu() {
            return this.plainCosemPdu;
        }

        @Override
        public String toString() {
            return format("Ciphered COSEM PDU: {0}\nPlain COSEM PDU: {1}", arrayToString(this.cipheredCosemPdu),
                    arrayToString(this.plainCosemPdu));
        }

        private static String arrayToString(byte[] array) {
            return array == null ? null : convertBytesToHexString(array, 30);
        }
    }

    private static String convertBytesToHexString(byte[] data, int maxNumOfBytes) {
        if (data.length > maxNumOfBytes) {

            int length1 = maxNumOfBytes / 2;
            int length2 = maxNumOfBytes - length1;
            return new StringBuilder().append(HexConverter.toHexString(data, 0, length1))
                    .append(" [...] ")
                    .append(HexConverter.toHexString(data, data.length - length2, length2))
                    .toString();

        }
        else {
            return HexConverter.toHexString(data);
        }
    }

    /**
     * Constructs a RawMessageDataBuilder.
     * 
     * @return the newly constructed RawRawMessageDataBuilder
     */
    public static RawMessageDataBuilder builder() {
        return new RawMessageDataBuilder();
    }

    public static class RawMessageDataBuilder {
        private MessageSource messageSource;
        private byte[] message;
        private Apdu apdu;

        private RawMessageDataBuilder() {
        }

        /**
         * Set the message source of the message.
         * 
         * @param messageSource
         *            the message source.
         * @return the builder.
         */
        public RawMessageDataBuilder setMessageSource(MessageSource messageSource) {
            this.messageSource = messageSource;
            return this;
        }

        /**
         * Set the complete message, transmitted over the line.
         * 
         * @param message
         *            the copmlete message as byte array.
         * @return the builder.
         */
        public RawMessageDataBuilder setMessage(byte[] message) {
            this.message = message;
            return this;
        }

        /**
         * Set the APDU.
         * 
         * @param apdu
         *            the APDU-
         * @return the builder.
         */
        public RawMessageDataBuilder setApdu(Apdu apdu) {
            this.apdu = apdu;
            return this;
        }

        /**
         * Build a new RawMessageData object form the settings.
         * 
         * @return a new RawMessageData.
         */
        public RawMessageData build() {
            return new RawMessageData(messageSource, message, apdu);
        }

    }

}
