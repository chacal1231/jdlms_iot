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

import static org.openmuc.jdlms.internal.ConformanceSettingConverter.conformanceFor;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openmuc.jasn1.ber.types.BerOctetString;
import org.openmuc.jdlms.JDlmsException.ExceptionId;
import org.openmuc.jdlms.JDlmsException.Fault;
import org.openmuc.jdlms.RawMessageData.MessageSource;
import org.openmuc.jdlms.RawMessageData.RawMessageDataBuilder;
import org.openmuc.jdlms.SecuritySuite.EncryptionMechanism;
import org.openmuc.jdlms.datatypes.DataObject.Type;
import org.openmuc.jdlms.internal.APdu;
import org.openmuc.jdlms.internal.ConformanceSettingConverter;
import org.openmuc.jdlms.internal.ContextId;
import org.openmuc.jdlms.internal.ObjectIdentifier;
import org.openmuc.jdlms.internal.ReleaseReqReason;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrBoolean;
import org.openmuc.jdlms.internal.asn1.cosem.COSEMpdu;
import org.openmuc.jdlms.internal.asn1.cosem.ConfirmedServiceError;
import org.openmuc.jdlms.internal.asn1.cosem.EXCEPTION_Response;
import org.openmuc.jdlms.internal.asn1.cosem.InitiateRequest;
import org.openmuc.jdlms.internal.asn1.cosem.Invoke_Id_And_Priority;
import org.openmuc.jdlms.internal.asn1.cosem.Unsigned16;
import org.openmuc.jdlms.internal.asn1.cosem.Unsigned8;
import org.openmuc.jdlms.internal.asn1.iso.acse.AAREApdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.AARQApdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.ACSEApdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.ACSERequirements;
import org.openmuc.jdlms.internal.asn1.iso.acse.APTitle;
import org.openmuc.jdlms.internal.asn1.iso.acse.APTitleForm2;
import org.openmuc.jdlms.internal.asn1.iso.acse.AssociationInformation;
import org.openmuc.jdlms.internal.asn1.iso.acse.AuthenticationValue;
import org.openmuc.jdlms.internal.asn1.iso.acse.RLRQApdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.ReleaseRequestReason;
import org.openmuc.jdlms.internal.security.HlsProcessorGmac;
import org.openmuc.jdlms.internal.security.HlsSecretProcessor;
import org.openmuc.jdlms.internal.security.RandomSequenceGenerator;
import org.openmuc.jdlms.internal.sessionlayer.SessionLayerListener;
import org.openmuc.jdlms.sessionlayer.SessionLayer;
import org.openmuc.jdlms.settings.client.ConfirmedMode;
import org.openmuc.jdlms.settings.client.Settings;

/**
 * Class used to interact with a DLMS/Cosem Server.
 * 
 * @see ConnectionBuilder
 */
public abstract class DlmsConnection implements AutoCloseable {

    private static final int HIGH_PRIO_FLAG = 0x80;
    private static final int CONFIRMED_MODE_FLAG = 0x40;

    private final Settings settings;

    private final SessionLayer sessionLayer;
    private Set<ConformanceSetting> negotiatedFeatures;
    private int maxSendPduSize = 0xffff;

    private byte[] buffer;

    private final APduBlockingQueue incomingResponses = new APduBlockingQueue();

    private int invokeId;

    private IOException ioException;

    private byte[] serverSystemTitle = null;

    private int frameCounter;

    DlmsConnection(Settings settings, SessionLayer sessionLayer) {
        this.settings = settings;

        this.sessionLayer = sessionLayer;

        this.buffer = new byte[maxSendPduSize];
        this.invokeId = 1;
        this.frameCounter = 1;
        this.serverSystemTitle = settings.systemTitle();
    }

    void connect() throws IOException {

        this.sessionLayer.startListening(new SessionLayerListenerImpl());

        ContextId contextId = getContextId();

        AARQApdu aarq = new AARQApdu();
        aarq.setApplicationContextName(ObjectIdentifier.applicationContextNameFor(contextId));

        HlsSecretProcessor hlsSecretProcessor = null;
        byte[] clientToServerChallenge = null;

        SecuritySuite securitySuite = settings.securitySuite();
        aarq.setMechanismName(ObjectIdentifier.mechanismNameFor(settings.securitySuite().getAuthenticationMechanism()));

        switch (securitySuite.getAuthenticationMechanism()) {
        case NONE:
            break;
        case LOW:
            setupAarqAuthentication(aarq, securitySuite.password);
            break;
        // case HLS3_MD5:
        // setupAarqAuthentication(aarq, connectionSettings.authenticationKey());
        // hlsSecretProcessor = new HlsProcessorMd5();
        // break;
        // case HLS4_SHA1:
        // setupAarqAuthentication(aarq, connectionSettings.authenticationKey());
        // hlsSecretProcessor = new HlsProcessorSha1();
        // break;
        case HLS5_GMAC:
            clientToServerChallenge = RandomSequenceGenerator.generate(settings.challengeLength());
            setupAarqAuthentication(aarq, clientToServerChallenge);
            hlsSecretProcessor = new HlsProcessorGmac();

            APTitle apTitle = new APTitle();
            apTitle.setApTitleForm2(new APTitleForm2(settings.systemTitle()));
            aarq.setCallingAPTitle(apTitle);
            break;
        // case HLS6_SHA256:
        // setupAarqAuthentication(aarq, connectionSettings.authenticationKey());
        // hlsSecretProcessor = new HlsProcessorSha256();
        // break;
        default:
            throw new IllegalArgumentException(MessageFormat.format("Authentication {0} mechanism not supported.",
                    securitySuite.getAuthenticationMechanism()));
        }

        ACSEApdu aarqAcseAPdu = new ACSEApdu();
        aarqAcseAPdu.setAarq(aarq);

        COSEMpdu xDlmsInitiateRequestPdu = new COSEMpdu();
        xDlmsInitiateRequestPdu.setinitiateRequest(new InitiateRequest(null, new AxdrBoolean(confirmedModeEnabled()),
                null, new Unsigned8(6), conformanceFor(proposedConformance()), new Unsigned16(0xFFFF)));

        APdu aarqAPdu = new APdu(aarqAcseAPdu, xDlmsInitiateRequestPdu);

        RawMessageDataBuilder rawMessageBuilder = newRawMessageDataBuilder();

        try {
            int length = encodeAPdu(aarqAPdu, rawMessageBuilder);

            this.sessionLayer.send(buffer, buffer.length - length, length, rawMessageBuilder);

            APdu decodedResponsePdu = waitForServerResponseAPdu();
            connectWithEnablededConfirmedMode(hlsSecretProcessor, clientToServerChallenge, decodedResponsePdu);
        } catch (IOException e) {
            try {
                this.sessionLayer.close();
            } catch (IOException e2) {
                // ignore thrown ex here.
            }

            throw e;
        }
    }

    private RawMessageDataBuilder newRawMessageDataBuilder() {
        if (settings.rawMessageListener() == null) {
            return null;
        }
        return RawMessageData.builder();
    }

    private int encodeAPdu(final APdu aPdu, final RawMessageDataBuilder rawMessageBuilder) throws IOException {
        final SecuritySuite securitySuite = settings.securitySuite();

        if (securitySuite.getEncryptionMechanism() != EncryptionMechanism.NONE) {
            return aPdu.encode(buffer, this.frameCounter++, settings.systemTitle(), securitySuite, rawMessageBuilder);
        }
        else {
            return unencryptedEncode(aPdu, rawMessageBuilder);
        }
    }

    private int unencryptedEncode(final APdu aPdu, final RawMessageDataBuilder rawMessageBuilder) throws IOException {
        return aPdu.encode(buffer, rawMessageBuilder);
    }

    private static void setupAarqAuthentication(AARQApdu aarq, byte[] clientToServerChallenge) {
        aarq.setSenderAcseRequirements(new ACSERequirements(new byte[] { (byte) 0x80 }, 2));

        AuthenticationValue authenticationValue = new AuthenticationValue();
        authenticationValue.setCharstring(new BerOctetString(clientToServerChallenge));
        aarq.setCallingAuthenticationValue(authenticationValue);

    }

    abstract ContextId getContextId();

    boolean confirmedModeEnabled() {
        return settings.confirmedMode() == ConfirmedMode.CONFIRMED;
    }

    Set<ConformanceSetting> negotiatedFeatures() {
        return this.negotiatedFeatures;
    }

    /**
     * PDU size of zero implies, that the server does not indicate a limit.
     * 
     * @return the maximum PDU size.
     */
    int maxSendPduSize() {
        return this.maxSendPduSize;
    }

    Invoke_Id_And_Priority invokeIdAndPriorityFor(boolean priority) {

        byte[] invokeIdAndPriorityBytes = new byte[] { (byte) (invokeId & 0xF) };
        if (confirmedModeEnabled()) {
            invokeIdAndPriorityBytes[0] |= CONFIRMED_MODE_FLAG;
        }
        if (priority) {
            invokeIdAndPriorityBytes[0] |= HIGH_PRIO_FLAG;
        }
        Invoke_Id_And_Priority result = new Invoke_Id_And_Priority(invokeIdAndPriorityBytes);

        invokeId = (invokeId + 1) % 16;
        return result;
    }

    void send(COSEMpdu pdu) throws IOException {
        APdu aPdu = new APdu(null, pdu);
        RawMessageDataBuilder rawMessageBuilder = RawMessageData.builder().setMessageSource(MessageSource.CLIENT);

        int length = encodeAPdu(aPdu, rawMessageBuilder);

        int offset = buffer.length - length;
        this.sessionLayer.send(buffer, offset, length, rawMessageBuilder);
    }

    /**
     * Convenience method to call {@code #get(false, List)}
     * 
     * 
     * @param params
     *            args of specifiers which attributes to send (See {@link AttributeAddress})
     * @return List of results from the smart meter in the same order as the requests
     * 
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     * 
     * @see #get(boolean, List)
     */
    public final List<GetResult> get(List<AttributeAddress> params) throws IOException {
        return get(false, params);
    }

    /**
     * Requests the remote smart meter to send the values of several attributes.
     * 
     * <p>
     * Convenience method to call {@code #get(false, AttributeAddress)}.
     * </p>
     * 
     * @param attributeAddress
     *            specifiers which attributes to send (See {@link AttributeAddress})
     * @return single result from the meter.
     * 
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     * 
     * @see #get(boolean, AttributeAddress)
     */
    public final GetResult get(AttributeAddress attributeAddress) throws IOException {
        return get(false, attributeAddress);
    }

    /**
     * Requests the remote smart meter to send the values of several attributes.
     * 
     * 
     * @param priority
     *            if true: sends this request with high priority, if supported
     * @param attributeAddress
     *            specifiers which attributes to send (See {@link AttributeAddress})
     * 
     * @return single results from the smart meter in the same order as the requests
     * 
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     * 
     * @see #get(boolean, List)
     */
    public final GetResult get(boolean priority, AttributeAddress attributeAddress) throws IOException {
        List<GetResult> result = get(priority, Arrays.asList(attributeAddress));
        return result.isEmpty() ? new GetResult(AccessResultCode.OTHER_REASON) : result.get(0);
    }

    /**
     * Requests the remote smart meter to send the values of one or several attributes
     * 
     * @param priority
     *            if true: sends this request with high priority, if supported
     * @param params
     *            args of specifiers which attributes to send (See {@link AttributeAddress})
     * @return List of results from the smart meter in the same order as the requests
     * 
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     * 
     */
    public abstract List<GetResult> get(boolean priority, List<AttributeAddress> params) throws IOException;

    /**
     * Requests the remote smart meter to set one attribute to the committed value.
     * 
     * <p>
     * Convenience method to call {@code set(false, SetParameter...)}.
     * </p>
     * 
     * @param params
     *            args of specifier which attributes to set to which values (See {@link SetParameter})
     * @return List of results from the smart meter in the same order as the requests or null if confirmed has been set
     *         to false on creation of this object. A true value indicates that this particular value has been
     *         successfully set
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     * 
     * 
     * @see #set(boolean, List)
     */
    public final List<AccessResultCode> set(List<SetParameter> params) throws IOException {
        return set(false, params);
    }

    /**
     * Requests the remote smart meter to set one or several attributes to the committed values
     * 
     * @param priority
     *            Sends this request with high priority, if supported
     * @param params
     *            Varargs of specifier which attributes to set to which values (See {@link SetParameter})
     * @return List of results from the smart meter in the same order as the requests or null if confirmed has been set
     *         to false on creation of this object. A true value indicates that this particular value has been
     *         successfully set
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     * 
     */
    public abstract List<AccessResultCode> set(boolean priority, List<SetParameter> params) throws IOException;

    /**
     * Requests the remote smart meter to set one attributes to the committed values.
     * 
     * @param priority
     *            Sends this request with high priority, if supported
     * @param setParameter
     *            Varargs of specifier which attributes to set to which values (See {@link SetParameter})
     * @return results from the smart meter in the same order as the requests or null if confirmed has been set to false
     *         on creation of this object. A true value indicates that this particular value has been successfully set
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     * 
     */
    public final AccessResultCode set(boolean priority, SetParameter setParameter) throws IOException {
        List<AccessResultCode> result = set(priority, Arrays.asList(setParameter));
        return result.isEmpty() ? AccessResultCode.OTHER_REASON : result.get(0);
    }

    /**
     * Requests the remote smart meter to set one or several attributes to the committed values.
     * 
     * <p>
     * Convenience method to call {@code set(false, SetParameter)}
     * </p>
     * 
     * @param setParameter
     *            attribute and values (see {@link SetParameter})
     * @return results from the smart meter in the same order as the requests or null if confirmed has been set to false
     *         on creation of this object. A true value indicates that this particular value has been successfully set
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     * 
     */
    public final AccessResultCode set(SetParameter setParameter) throws IOException {
        return set(false, setParameter);
    }

    /**
     * Requests the remote smart meter to call one methods with or without committed parameters.
     * 
     * @param priority
     *            Sends this request with high priority, if supported
     * 
     * @param methodParameter
     *            method to be called and, if needed, what parameters to call (See {@link MethodParameter}
     * @return results from the smart meter in the same order as the requests or null if confirmed has been set to false
     *         on creation of this object.
     * 
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     */
    public final MethodResult action(boolean priority, MethodParameter methodParameter) throws IOException {
        List<MethodResult> action = action(priority, Arrays.asList(methodParameter));
        return action.isEmpty() ? new MethodResult(MethodResultCode.OTHER_REASON) : action.get(0);
    }

    /**
     * 
     * Requests the remote smart meter to call one methods with or without committed parameters.
     * <p>
     * Convenience method to call {@code action(false, methodParameter)}
     * </p>
     * 
     * @param methodParameter
     *            specifier which method to be called and, if needed, what parameters to call (See
     *            {@link MethodParameter}
     * @return results from the smart meter in the same order as the requests or null if confirmed has been set to false
     *         on creation of this object.
     * 
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     */
    public final MethodResult action(MethodParameter methodParameter) throws IOException {
        return action(false, methodParameter);
    }

    /**
     * 
     * Convenience method to call {@code action(false, params)}
     * 
     * @param params
     *            List of specifier which methods to be called and, if needed, what parameters to call (See
     *            {@link MethodParameter}
     * 
     * @return List of results from the smart meter in the same order as the requests or null if confirmed has been set
     *         to false on creation of this object
     * 
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     */
    public final List<MethodResult> action(List<MethodParameter> params) throws IOException {
        return action(false, params);
    }

    /**
     * Requests the remote smart meter to call one or several methods with or without committed parameters
     * 
     * @param priority
     *            Sends this request with high priority, if supported
     * @param params
     *            List of specifier which methods to be called and, if needed, what parameters to call (See
     *            {@link MethodParameter}
     * @return List of results from the smart meter in the same order as the requests or null if confirmed has been set
     *         to false on creation of this object
     * @throws IOException
     *             if the connection breaks, while requesting.
     *             <p>
     *             May be of type {@link FatalJDlmsException} or {@link ResponseTimeoutException}
     *             </p>
     */
    public abstract List<MethodResult> action(boolean priority, List<MethodParameter> params) throws IOException;

    /**
     * Disconnects gracefully from the server.
     * 
     * @throws IOException
     *             if an I/O Exception occurs while closing
     */
    public synchronized void disconnect() throws IOException {
        try {
            ReleaseReqReason releaseReason = ReleaseReqReason.NORMAL;

            ReleaseRequestReason reason = new ReleaseRequestReason(releaseReason.getCode());

            AssociationInformation userInformation = null;
            RLRQApdu rlrq = new RLRQApdu();
            rlrq.setReason(reason);
            rlrq.setUserInformation(userInformation);

            ACSEApdu acseApdu = new ACSEApdu();
            acseApdu.setRlrq(rlrq);

            COSEMpdu cosemPdu = null;

            APdu aPduOut = new APdu(acseApdu, cosemPdu);
            RawMessageDataBuilder rawMessageBuilder = newRawMessageDataBuilder();
            int length = unencryptedEncode(aPduOut, rawMessageBuilder);

            int offset = buffer.length - length;
            this.sessionLayer.send(buffer, offset, length, rawMessageBuilder);

            try {
                APdu aPdu = this.incomingResponses.take();
                if (aPdu == null) {
                    throw new ResponseTimeoutException("Disconnect timed out.");
                }

                if (aPdu == null || aPdu.getAcseAPdu() == null) {
                    throw new FatalJDlmsException(ExceptionId.CONNECTION_DISCONNECT_ERROR, Fault.SYSTEM,
                            "Server did not answer on disconnect");
                }
            } catch (InterruptedException e) {
                // ignore, shouldn't occur
            }

        } finally {
            close();
        }
    }

    /**
     * Closes the connection.
     * 
     * @throws IOException
     *             if an I/O Exception occurs while closing
     */
    @Override
    public void close() throws IOException {
        this.sessionLayer.close();
    }

    private class SessionLayerListenerImpl implements SessionLayerListener {
        @Override
        public void dataReceived(byte[] data, RawMessageDataBuilder rawMessageBuilder) {
            APdu aPdu;
            try {
                Settings settings = DlmsConnection.this.settings;

                SecuritySuite securitySuite = settings.securitySuite();

                if (rawMessageBuilder == null) {
                    rawMessageBuilder = RawMessageData.builder();
                }

                if (securitySuite.getEncryptionMechanism() != EncryptionMechanism.NONE) {
                    aPdu = APdu.decode(data, serverSystemTitle, frameCounter, securitySuite, rawMessageBuilder);
                }
                else {
                    aPdu = APdu.decode(data, rawMessageBuilder);
                }
                COSEMpdu cosemPdu = aPdu.getCosemPdu();

                if (cosemPdu != null && checkForErrors(cosemPdu)) {
                    return;
                }

            } catch (IOException e) {
                errorOnIncome(e);
                return;
            }

            RawMessageListener rawMessageListener = settings.rawMessageListener();
            if (rawMessageListener != null) {
                RawMessageData rawMessageData = rawMessageBuilder.setMessageSource(MessageSource.SERVER).build();
                rawMessageListener.messageCaptured(rawMessageData);
            }

            if (aPdu.getAcseAPdu() != null) {
                incomingResponses.put(aPdu);
            }
            else {
                processPdu(aPdu.getCosemPdu());
            }

        }

        private boolean checkForErrors(COSEMpdu cosemPdu) {
            COSEMpdu.Choices choiceIndex = cosemPdu.getChoiceIndex();
            if (choiceIndex == COSEMpdu.Choices.EXCEPTION_RESPONSE) {
                EXCEPTION_Response exceptionResponse = cosemPdu.exception_response;
                errorOnIncome(new IOException(exceptionResponse.toString()));
                return true;
            }
            else if (choiceIndex == COSEMpdu.Choices.CONFIRMEDSERVICEERROR) {
                ConfirmedServiceError confirmedServiceError = cosemPdu.confirmedServiceError;
                errorOnIncome(new IOException(confirmedServiceError.toString()));
                return true;
            }
            else {
                return false;
            }
        }

        private void errorOnIncome(IOException ex) {
            ioException = ex;
            incomingResponses.putError(ex);
        }

        @Override
        public void connectionInterrupted(IOException e) {
            errorOnIncome(e);
        }
    }

    /**
     * Change the global authentication key used by the client.
     * 
     * @param key
     *            the new key
     */
    public void changeClientGlobalAuthenticationKey(byte[] key) {
        settings.updateAuthenticationKey(key);
    }

    /**
     * Change the global encryption used by the client.
     * 
     * @param key
     *            the new key
     */
    public void changeClientGlobalEncryptionKey(byte[] key) {
        settings.updateGlobalEncryptionKey(key);
    }

    abstract Set<ConformanceSetting> proposedConformance();

    abstract void processPdu(COSEMpdu pdu);

    abstract void validateReferencingMethod() throws IOException;

    abstract MethodResult hlsAuthentication(byte[] processedChallenge) throws IOException;

    private void connectWithEnablededConfirmedMode(HlsSecretProcessor hlsSecretProcessor,
            byte[] clientToServerChallenge, APdu decodedResponsePdu) throws IOException {

        try {
            validate(decodedResponsePdu);
        } catch (EOFException e) {
            throw new IOException("Connection closed by remote host while waiting for association response (AARE).", e);
        } catch (FatalJDlmsException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("Error while receiving association response: " + e.getMessage(), e);
        }

        AAREApdu aare = decodedResponsePdu.getAcseAPdu().getAare();
        if (settings.securitySuite().getAuthenticationMechanism().isHlsMechanism()) {
            this.serverSystemTitle = aare.getRespondingAPTitle().getApTitleForm2().value;

        }

        COSEMpdu xDlmsInitResponse = decodedResponsePdu.getCosemPdu();

        this.maxSendPduSize = (int) xDlmsInitResponse.initiateResponse.server_max_receive_pdu_size.getValue();
        if (this.maxSendPduSize == 0) {
            this.maxSendPduSize = 0xFFFF;
        }

        this.buffer = new byte[this.maxSendPduSize];

        this.negotiatedFeatures = ConformanceSettingConverter
                .conformanceSettingFor(xDlmsInitResponse.initiateResponse.negotiated_conformance);

        validateReferencingMethod();

        // Step 3 and 4 of HLS
        SecuritySuite securitySuite = settings.securitySuite();
        AuthenticationMechanism authenticationMechanism = securitySuite.getAuthenticationMechanism();

        switch (authenticationMechanism) {
        case HLS5_GMAC:
            hls5Connect(hlsSecretProcessor, clientToServerChallenge, aare, securitySuite);

        case LOW:
        case NONE:
        default:
            break;
        }

    }

    private void hls5Connect(HlsSecretProcessor hlsSecretProcessor, byte[] clientToServerChallenge, AAREApdu aare,
            SecuritySuite securitySuite) throws IOException, FatalJDlmsException {
        byte[] serverToClientChallenge = aare.getRespondingAuthenticationValue().getCharstring().value;
        byte[] processedChallenge;
        MethodResult remoteResponse;
        byte[] frameCounter = new byte[4];
        int frameCounterInt;
        processedChallenge = hlsSecretProcessor.process(serverToClientChallenge, securitySuite.getAuthenticationKey(),
                securitySuite.getGlobalUnicastEncryptionKey(), settings.systemTitle(), this.frameCounter);
        try {
            remoteResponse = hlsAuthentication(processedChallenge);
        } catch (ResponseTimeoutException e) {
            throw new FatalJDlmsException(ExceptionId.CONNECTION_ESTABLISH_ERROR, Fault.SYSTEM,
                    "Server replied late in HLS exchange.", e);
        } catch (IOException e) {
            Fault assumedFault = Fault.USER;
            if (e instanceof JDlmsException) {
                assumedFault = ((JDlmsException) e).getAssumedFault();
            }
            throw new FatalJDlmsException(ExceptionId.AUTHENTICATION_ERROR, assumedFault,
                    "Exception during HLS authentication steps 3 and 4");
        }
        if (remoteResponse.getResultCode() != MethodResultCode.SUCCESS
                || remoteResponse.getResultData().getType() != Type.OCTET_STRING) {
            throw new FatalJDlmsException(ExceptionId.AUTHENTICATION_ERROR, Fault.USER,
                    "Failed to authenticate to server. HLS authentication step 4.");
        }
        byte[] remoteChallenge = remoteResponse.getResultData().getValue();
        System.arraycopy(remoteChallenge, 1, frameCounter, 0, 4);
        frameCounterInt = ByteBuffer.wrap(frameCounter).getInt();
        this.serverSystemTitle = aare.getRespondingAPTitle().getApTitleForm2().value;
        processedChallenge = hlsSecretProcessor.process(clientToServerChallenge, securitySuite.authenticationKey,
                securitySuite.globalUnicastEncryptionKey, aare.getRespondingAPTitle().getApTitleForm2().value,
                frameCounterInt);
        validateChallengeResponse(processedChallenge, remoteChallenge);
    }

    private static void validateChallengeResponse(byte[] processedChallenge, byte[] remoteChallenge)
            throws IOException {
        if (!Arrays.equals(remoteChallenge, processedChallenge)) {
            String message = "Server could not authenticate itself. Server is a possible attacker.";
            throw new FatalJDlmsException(ExceptionId.AUTHENTICATION_ERROR, Fault.SYSTEM, message);
        }
    }

    private APdu waitForServerResponseAPdu() throws IOException {
        try {
            if (settings.responseTimeout() == 0) {
                return incomingResponses.take();
            }
            else {
                return incomingResponses.poll(settings.responseTimeout(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void validate(APdu decodedResponsePdu) throws IOException {
        if (decodedResponsePdu == null) {
            if (ioException != null) {
                throw ioException;
            }
            throw new ResponseTimeoutException(
                    "Timeout waiting for associate response message (AARE). No further information.");
        }

        if (decodedResponsePdu.getCosemPdu() == null) {
            throw ioException;
        }

        if (decodedResponsePdu.getAcseAPdu() == null || decodedResponsePdu.getAcseAPdu().getAare() == null) {
            throw new IOException("Did not receive expected associate response (AARE) message.");
        }
    }

    Settings connectionSettings() {
        return this.settings;
    }

}
