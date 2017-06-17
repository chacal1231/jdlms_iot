package org.openmuc.jdlms.internal.association;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.openmuc.jdlms.AuthenticationMechanism;
import org.openmuc.jdlms.LogicalDevice;
import org.openmuc.jdlms.SecuritySuite;
import org.openmuc.jdlms.SecuritySuite.EncryptionMechanism;
import org.openmuc.jdlms.internal.APdu;
import org.openmuc.jdlms.internal.AssociateSourceDiagnostic.AcseServiceUser;
import org.openmuc.jdlms.internal.ContextId;
import org.openmuc.jdlms.internal.DataDirectory.CosemLogicalDevice;
import org.openmuc.jdlms.internal.ObjectIdentifier;
import org.openmuc.jdlms.internal.ServerConnectionData;
import org.openmuc.jdlms.internal.asn1.cosem.COSEMpdu;
import org.openmuc.jdlms.internal.asn1.cosem.Conformance;
import org.openmuc.jdlms.internal.asn1.cosem.InitiateRequest;
import org.openmuc.jdlms.internal.asn1.iso.acse.AARQApdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.ACSEApdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.MechanismName;
import org.openmuc.jdlms.internal.security.HlsProcessorGmac;
import org.openmuc.jdlms.internal.security.HlsSecretProcessor;
import org.openmuc.jdlms.internal.security.RandomSequenceGenerator;

class InitiateMessageProcessor {

    private final ServerConnectionData connectionData;
    private final CosemLogicalDevice cosemLogicalDevice;
    private final LogicalDevice logicalDevice;
    private ContextId contextId;

    public InitiateMessageProcessor(ServerConnectionData connectionData, CosemLogicalDevice cosemLogicalDevice) {
        this.connectionData = connectionData;
        this.cosemLogicalDevice = cosemLogicalDevice;
        this.logicalDevice = cosemLogicalDevice.getLogicalDevice();

        Map<Integer, SecuritySuite> restrictions = cosemLogicalDevice.getLogicalDevice().getRestrictions();
        this.connectionData.securitySuite = restrictions.get(this.connectionData.clientId);
    }

    public ContextId getContextId() {
        return contextId;
    }

    public APdu processInitialMessage(byte[] messageData) throws IOException {
        SecuritySuite securitySuite = this.connectionData.securitySuite;
        InitiateResponseBuilder initialResponseBuilder = new InitiateResponseBuilder(lnConformance());

        if (cosemLogicalDevice == null) {
            throw new AssociatRequestException(AcseServiceUser.NO_REASON_GIVEN);
        }

        LogicalDevice logicalDevice = cosemLogicalDevice.getLogicalDevice();
        Map<Integer, SecuritySuite> restrictions = logicalDevice.getRestrictions();
        APdu aPdu = APdu.decode(messageData, null);

        this.contextId = ObjectIdentifier
                .applicationContextIdFor(aPdu.getAcseAPdu().getAarq().getApplicationContextName());
        if (restrictions.isEmpty()) {

            this.connectionData.authenticated = true;
            this.connectionData.securitySuite = SecuritySuite.builder().build();

            return initialResponseBuilder.setContextId(contextId).build();
        }

        if (securitySuite == null) {
            // unknown client ID
            throw new AssociatRequestException(AcseServiceUser.NO_REASON_GIVEN);
        }

        aPdu = decodeAPdu(messageData, securitySuite);

        if (aPdu.getCosemPdu() == null) {
            throw new AssociatRequestException(AcseServiceUser.NO_REASON_GIVEN);
        }

        COSEMpdu cosemPdu = aPdu.getCosemPdu();

        if (cosemPdu.getChoiceIndex() != COSEMpdu.Choices.INITIATEREQUEST) {
            throw new AssociatRequestException(AcseServiceUser.NO_REASON_GIVEN);
        }
        InitiateRequest initiateRequest = cosemPdu.initiateRequest;
        this.connectionData.clientMaxReceivePduSize = initiateRequest.client_max_receive_pdu_size.getValue() & 0xFFFF;

        ACSEApdu acseAPdu = aPdu.getAcseAPdu();

        if (acseAPdu == null) {
            throw new AssociatRequestException(AcseServiceUser.NO_REASON_GIVEN);
        }
        AARQApdu aarq = acseAPdu.getAarq();

        return tryToAuthenticate(initialResponseBuilder, aarq, securitySuite);
    }

    private static void checkChallangeLength(int challengeLength) throws AssociatRequestException {
        if (challengeLength < 8 || challengeLength > 64) {
            throw new AssociatRequestException(AcseServiceUser.AUTHENTICATION_FAILURE);
        }
    }

    private APdu decodeAPdu(byte[] messageData, SecuritySuite sec) throws IOException {
        APdu aPdu;

        if (sec.getEncryptionMechanism() != EncryptionMechanism.NONE) {
            this.connectionData.clientSystemTitle = systemTitle();

            aPdu = APdu.decode(messageData, this.connectionData.clientSystemTitle, connectionData.frameCounter, sec,
                    null);
        }
        else {
            aPdu = APdu.decode(messageData, null);
        }
        return aPdu;
    }

    private APdu tryToAuthenticate(InitiateResponseBuilder initialResponseBuilder, AARQApdu aarq,
            SecuritySuite securitySuite) throws IOException {

        MechanismName mechanismName = aarq.getMechanismName();

        if (mechanismName == null && securitySuite.getAuthenticationMechanism() != AuthenticationMechanism.NONE) {
            throw new AssociatRequestException(AcseServiceUser.AUTHENTICATION_MECHANISM_NAME_REQUIRED);
        }
        else if (mechanismName == null && securitySuite.getAuthenticationMechanism() == AuthenticationMechanism.NONE) {
            this.connectionData.authenticated = true;
            return initialResponseBuilder.build();
        }

        if (mechanismName == null) {
            throw new AssociatRequestException(AcseServiceUser.NO_REASON_GIVEN);
        }
        AuthenticationMechanism authenticationLevel = ObjectIdentifier.mechanismIdFor(mechanismName);

        if (authenticationLevel == AuthenticationMechanism.NONE) {
            throw new AssociatRequestException(AcseServiceUser.AUTHENTICATION_REQUIRED);
        }

        this.connectionData.clientToServerChallenge = aarq.getCallingAuthenticationValue().getCharstring().value;

        if (authenticationLevel == AuthenticationMechanism.NONE
                && securitySuite.getAuthenticationMechanism() != AuthenticationMechanism.NONE) {
            this.connectionData.authenticated = true;
            return initialResponseBuilder.setContextId(contextId).build();
        }

        if (authenticationLevel != securitySuite.getAuthenticationMechanism()) {
            throw new AssociatRequestException(AcseServiceUser.AUTHENTICATION_FAILURE);
        }

        switch (authenticationLevel) {
        case LOW:
            return processLowAuthentciationRequest(aarq, securitySuite.getPassword());

        case HLS5_GMAC:
            return processHls5GmacAuthentciationRequest(aarq, securitySuite);
        default:
            throw new AssociatRequestException(AcseServiceUser.APPLICATION_CONTEXT_NAME_NOT_SUPPORTED);
        }
    }

    private APdu processHls5GmacAuthentciationRequest(AARQApdu aarq, SecuritySuite sec)
            throws IOException, AssociatRequestException {
        byte[] clientToServerChallenge = this.connectionData.clientToServerChallenge;

        this.connectionData.clientSystemTitle = aarq.getCallingAPTitle().getApTitleForm2().value;
        byte[] clientSystemTitle = this.connectionData.clientSystemTitle;

        int challengeLength = clientToServerChallenge.length;

        checkChallangeLength(challengeLength);

        this.connectionData.frameCounter = 1;

        byte[] serverToClientChallenge = RandomSequenceGenerator.generate(challengeLength);

        HlsSecretProcessor hlSecretProcessor = new HlsProcessorGmac();

        this.connectionData.processedServerToClientChallenge = hlSecretProcessor.process(serverToClientChallenge,
                sec.getAuthenticationKey(), sec.getGlobalUnicastEncryptionKey(), clientSystemTitle,
                ++this.connectionData.frameCounter);

        return new InitiateResponseBuilder(lnConformance()).setContextId(contextId)
                .setAuthenticationValue(serverToClientChallenge)
                .setSystemTitle(systemTitle())
                .build();

    }

    private APdu processLowAuthentciationRequest(AARQApdu aarq, byte[] authenticationKey)
            throws AssociatRequestException {
        byte[] clientAuthenticaionValue = aarq.getCallingAuthenticationValue().getCharstring().value;

        if (Arrays.equals(clientAuthenticaionValue, authenticationKey)) {
            this.connectionData.authenticated = true;
            return new InitiateResponseBuilder(lnConformance()).build();
        }

        throw new AssociatRequestException(AcseServiceUser.AUTHENTICATION_FAILURE);
    }

    private Conformance lnConformance() {
        return this.logicalDevice.getConformance();
    }

    private byte[] systemTitle() {
        return this.logicalDevice.getSystemTitle();
    }

}
