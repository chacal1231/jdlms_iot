/**
 * This class file was automatically generated by jASN1 v1.8.0 (http://www.openmuc.org)
 */

package org.openmuc.jdlms.internal.asn1.iso.acse;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jasn1.ber.BerLength;
import org.openmuc.jasn1.ber.BerTag;
import org.openmuc.jasn1.ber.types.BerBitString;
import org.openmuc.jasn1.ber.types.BerObjectIdentifier;

public class AARQApdu implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final BerTag tag = new BerTag(BerTag.APPLICATION_CLASS, BerTag.CONSTRUCTED, 0);

    public byte[] code = null;
    private BerBitString protocolVersion = null;
    private BerObjectIdentifier applicationContextName = null;
    private APTitle calledAPTitle = null;
    private AEQualifier calledAEQualifier = null;
    private APInvocationIdentifier calledAPInvocationIdentifier = null;
    private AEInvocationIdentifier calledAEInvocationIdentifier = null;
    private APTitle callingAPTitle = null;
    private AEQualifier callingAEQualifier = null;
    private APInvocationIdentifier callingAPInvocationIdentifier = null;
    private AEInvocationIdentifier callingAEInvocationIdentifier = null;
    private ACSERequirements senderAcseRequirements = null;
    private MechanismName mechanismName = null;
    private AuthenticationValue callingAuthenticationValue = null;
    private ApplicationContextNameList applicationContextNameList = null;
    private ImplementationData implementationInformation = null;
    private AssociationInformation userInformation = null;

    public AARQApdu() {
    }

    public AARQApdu(byte[] code) {
        this.code = code;
    }

    public void setProtocolVersion(BerBitString protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public BerBitString getProtocolVersion() {
        return protocolVersion;
    }

    public void setApplicationContextName(BerObjectIdentifier applicationContextName) {
        this.applicationContextName = applicationContextName;
    }

    public BerObjectIdentifier getApplicationContextName() {
        return applicationContextName;
    }

    public void setCalledAPTitle(APTitle calledAPTitle) {
        this.calledAPTitle = calledAPTitle;
    }

    public APTitle getCalledAPTitle() {
        return calledAPTitle;
    }

    public void setCalledAEQualifier(AEQualifier calledAEQualifier) {
        this.calledAEQualifier = calledAEQualifier;
    }

    public AEQualifier getCalledAEQualifier() {
        return calledAEQualifier;
    }

    public void setCalledAPInvocationIdentifier(APInvocationIdentifier calledAPInvocationIdentifier) {
        this.calledAPInvocationIdentifier = calledAPInvocationIdentifier;
    }

    public APInvocationIdentifier getCalledAPInvocationIdentifier() {
        return calledAPInvocationIdentifier;
    }

    public void setCalledAEInvocationIdentifier(AEInvocationIdentifier calledAEInvocationIdentifier) {
        this.calledAEInvocationIdentifier = calledAEInvocationIdentifier;
    }

    public AEInvocationIdentifier getCalledAEInvocationIdentifier() {
        return calledAEInvocationIdentifier;
    }

    public void setCallingAPTitle(APTitle callingAPTitle) {
        this.callingAPTitle = callingAPTitle;
    }

    public APTitle getCallingAPTitle() {
        return callingAPTitle;
    }

    public void setCallingAEQualifier(AEQualifier callingAEQualifier) {
        this.callingAEQualifier = callingAEQualifier;
    }

    public AEQualifier getCallingAEQualifier() {
        return callingAEQualifier;
    }

    public void setCallingAPInvocationIdentifier(APInvocationIdentifier callingAPInvocationIdentifier) {
        this.callingAPInvocationIdentifier = callingAPInvocationIdentifier;
    }

    public APInvocationIdentifier getCallingAPInvocationIdentifier() {
        return callingAPInvocationIdentifier;
    }

    public void setCallingAEInvocationIdentifier(AEInvocationIdentifier callingAEInvocationIdentifier) {
        this.callingAEInvocationIdentifier = callingAEInvocationIdentifier;
    }

    public AEInvocationIdentifier getCallingAEInvocationIdentifier() {
        return callingAEInvocationIdentifier;
    }

    public void setSenderAcseRequirements(ACSERequirements senderAcseRequirements) {
        this.senderAcseRequirements = senderAcseRequirements;
    }

    public ACSERequirements getSenderAcseRequirements() {
        return senderAcseRequirements;
    }

    public void setMechanismName(MechanismName mechanismName) {
        this.mechanismName = mechanismName;
    }

    public MechanismName getMechanismName() {
        return mechanismName;
    }

    public void setCallingAuthenticationValue(AuthenticationValue callingAuthenticationValue) {
        this.callingAuthenticationValue = callingAuthenticationValue;
    }

    public AuthenticationValue getCallingAuthenticationValue() {
        return callingAuthenticationValue;
    }

    public void setApplicationContextNameList(ApplicationContextNameList applicationContextNameList) {
        this.applicationContextNameList = applicationContextNameList;
    }

    public ApplicationContextNameList getApplicationContextNameList() {
        return applicationContextNameList;
    }

    public void setImplementationInformation(ImplementationData implementationInformation) {
        this.implementationInformation = implementationInformation;
    }

    public ImplementationData getImplementationInformation() {
        return implementationInformation;
    }

    public void setUserInformation(AssociationInformation userInformation) {
        this.userInformation = userInformation;
    }

    public AssociationInformation getUserInformation() {
        return userInformation;
    }

    public int encode(BerByteArrayOutputStream os) throws IOException {
        return encode(os, true);
    }

    public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

        if (code != null) {
            for (int i = code.length - 1; i >= 0; i--) {
                os.write(code[i]);
            }
            if (withTag) {
                return tag.encode(os) + code.length;
            }
            return code.length;
        }

        int codeLength = 0;
        int sublength;

        if (userInformation != null) {
            sublength = userInformation.encode(os, true);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 30
            os.write(0xBE);
            codeLength += 1;
        }

        if (implementationInformation != null) {
            codeLength += implementationInformation.encode(os, false);
            // write tag: CONTEXT_CLASS, PRIMITIVE, 29
            os.write(0x9D);
            codeLength += 1;
        }

        if (applicationContextNameList != null) {
            codeLength += applicationContextNameList.encode(os, false);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 13
            os.write(0xAD);
            codeLength += 1;
        }

        if (callingAuthenticationValue != null) {
            sublength = callingAuthenticationValue.encode(os);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 12
            os.write(0xAC);
            codeLength += 1;
        }

        if (mechanismName != null) {
            codeLength += mechanismName.encode(os, false);
            // write tag: CONTEXT_CLASS, PRIMITIVE, 11
            os.write(0x8B);
            codeLength += 1;
        }

        if (senderAcseRequirements != null) {
            codeLength += senderAcseRequirements.encode(os, false);
            // write tag: CONTEXT_CLASS, PRIMITIVE, 10
            os.write(0x8A);
            codeLength += 1;
        }

        if (callingAEInvocationIdentifier != null) {
            sublength = callingAEInvocationIdentifier.encode(os, true);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 9
            os.write(0xA9);
            codeLength += 1;
        }

        if (callingAPInvocationIdentifier != null) {
            sublength = callingAPInvocationIdentifier.encode(os, true);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 8
            os.write(0xA8);
            codeLength += 1;
        }

        if (callingAEQualifier != null) {
            sublength = callingAEQualifier.encode(os);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 7
            os.write(0xA7);
            codeLength += 1;
        }

        if (callingAPTitle != null) {
            sublength = callingAPTitle.encode(os);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 6
            os.write(0xA6);
            codeLength += 1;
        }

        if (calledAEInvocationIdentifier != null) {
            sublength = calledAEInvocationIdentifier.encode(os, true);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 5
            os.write(0xA5);
            codeLength += 1;
        }

        if (calledAPInvocationIdentifier != null) {
            sublength = calledAPInvocationIdentifier.encode(os, true);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 4
            os.write(0xA4);
            codeLength += 1;
        }

        if (calledAEQualifier != null) {
            sublength = calledAEQualifier.encode(os);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 3
            os.write(0xA3);
            codeLength += 1;
        }

        if (calledAPTitle != null) {
            sublength = calledAPTitle.encode(os);
            codeLength += sublength;
            codeLength += BerLength.encodeLength(os, sublength);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 2
            os.write(0xA2);
            codeLength += 1;
        }

        sublength = applicationContextName.encode(os, true);
        codeLength += sublength;
        codeLength += BerLength.encodeLength(os, sublength);
        // write tag: CONTEXT_CLASS, CONSTRUCTED, 1
        os.write(0xA1);
        codeLength += 1;

        if (protocolVersion != null) {
            codeLength += protocolVersion.encode(os, false);
            // write tag: CONTEXT_CLASS, PRIMITIVE, 0
            os.write(0x80);
            codeLength += 1;
        }

        codeLength += BerLength.encodeLength(os, codeLength);

        if (withTag) {
            codeLength += tag.encode(os);
        }

        return codeLength;

    }

    public int decode(InputStream is) throws IOException {
        return decode(is, true);
    }

    public int decode(InputStream is, boolean withTag) throws IOException {
        int codeLength = 0;
        int subCodeLength = 0;
        BerTag berTag = new BerTag();

        if (withTag) {
            codeLength += tag.decodeAndCheck(is);
        }

        BerLength length = new BerLength();
        codeLength += length.decode(is);

        int totalLength = length.val;
        codeLength += totalLength;

        subCodeLength += berTag.decode(is);
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 0)) {
            protocolVersion = new BerBitString();
            subCodeLength += protocolVersion.decode(is, false);
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
            subCodeLength += length.decode(is);
            applicationContextName = new BerObjectIdentifier();
            subCodeLength += applicationContextName.decode(is, true);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }
        else {
            throw new IOException("Tag does not match the mandatory sequence element tag.");
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 2)) {
            subCodeLength += length.decode(is);
            calledAPTitle = new APTitle();
            subCodeLength += calledAPTitle.decode(is, null);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 3)) {
            subCodeLength += length.decode(is);
            calledAEQualifier = new AEQualifier();
            subCodeLength += calledAEQualifier.decode(is, null);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 4)) {
            subCodeLength += length.decode(is);
            calledAPInvocationIdentifier = new APInvocationIdentifier();
            subCodeLength += calledAPInvocationIdentifier.decode(is, true);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 5)) {
            subCodeLength += length.decode(is);
            calledAEInvocationIdentifier = new AEInvocationIdentifier();
            subCodeLength += calledAEInvocationIdentifier.decode(is, true);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 6)) {
            subCodeLength += length.decode(is);
            callingAPTitle = new APTitle();
            subCodeLength += callingAPTitle.decode(is, null);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 7)) {
            subCodeLength += length.decode(is);
            callingAEQualifier = new AEQualifier();
            subCodeLength += callingAEQualifier.decode(is, null);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 8)) {
            subCodeLength += length.decode(is);
            callingAPInvocationIdentifier = new APInvocationIdentifier();
            subCodeLength += callingAPInvocationIdentifier.decode(is, true);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 9)) {
            subCodeLength += length.decode(is);
            callingAEInvocationIdentifier = new AEInvocationIdentifier();
            subCodeLength += callingAEInvocationIdentifier.decode(is, true);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 10)) {
            senderAcseRequirements = new ACSERequirements();
            subCodeLength += senderAcseRequirements.decode(is, false);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 11)) {
            mechanismName = new MechanismName();
            subCodeLength += mechanismName.decode(is, false);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 12)) {
            subCodeLength += length.decode(is);
            callingAuthenticationValue = new AuthenticationValue();
            subCodeLength += callingAuthenticationValue.decode(is, null);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 13)) {
            applicationContextNameList = new ApplicationContextNameList();
            subCodeLength += applicationContextNameList.decode(is, false);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 29)) {
            implementationInformation = new ImplementationData();
            subCodeLength += implementationInformation.decode(is, false);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 30)) {
            subCodeLength += length.decode(is);
            userInformation = new AssociationInformation();
            subCodeLength += userInformation.decode(is, true);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
        }
        throw new IOException("Unexpected end of sequence, length tag: " + totalLength + ", actual sequence length: "
                + subCodeLength);

    }

    public void encodeAndSave(int encodingSizeGuess) throws IOException {
        BerByteArrayOutputStream os = new BerByteArrayOutputStream(encodingSizeGuess);
        encode(os, false);
        code = os.getArray();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendAsString(sb, 0);
        return sb.toString();
    }

    public void appendAsString(StringBuilder sb, int indentLevel) {

        sb.append("{");
        boolean firstSelectedElement = true;
        if (protocolVersion != null) {
            sb.append("\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("protocolVersion: ").append(protocolVersion);
            firstSelectedElement = false;
        }

        if (!firstSelectedElement) {
            sb.append(",\n");
        }
        for (int i = 0; i < indentLevel + 1; i++) {
            sb.append("\t");
        }
        if (applicationContextName != null) {
            sb.append("applicationContextName: ").append(applicationContextName);
        }
        else {
            sb.append("applicationContextName: <empty-required-field>");
        }

        if (calledAPTitle != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("calledAPTitle: ");
            calledAPTitle.appendAsString(sb, indentLevel + 1);
        }

        if (calledAEQualifier != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("calledAEQualifier: ");
            calledAEQualifier.appendAsString(sb, indentLevel + 1);
        }

        if (calledAPInvocationIdentifier != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("calledAPInvocationIdentifier: ").append(calledAPInvocationIdentifier);
        }

        if (calledAEInvocationIdentifier != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("calledAEInvocationIdentifier: ").append(calledAEInvocationIdentifier);
        }

        if (callingAPTitle != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("callingAPTitle: ");
            callingAPTitle.appendAsString(sb, indentLevel + 1);
        }

        if (callingAEQualifier != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("callingAEQualifier: ");
            callingAEQualifier.appendAsString(sb, indentLevel + 1);
        }

        if (callingAPInvocationIdentifier != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("callingAPInvocationIdentifier: ").append(callingAPInvocationIdentifier);
        }

        if (callingAEInvocationIdentifier != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("callingAEInvocationIdentifier: ").append(callingAEInvocationIdentifier);
        }

        if (senderAcseRequirements != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("senderAcseRequirements: ").append(senderAcseRequirements);
        }

        if (mechanismName != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("mechanismName: ").append(mechanismName);
        }

        if (callingAuthenticationValue != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("callingAuthenticationValue: ");
            callingAuthenticationValue.appendAsString(sb, indentLevel + 1);
        }

        if (applicationContextNameList != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("applicationContextNameList: ");
            applicationContextNameList.appendAsString(sb, indentLevel + 1);
        }

        if (implementationInformation != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("implementationInformation: ").append(implementationInformation);
        }

        if (userInformation != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("userInformation: ").append(userInformation);
        }

        sb.append("\n");
        for (int i = 0; i < indentLevel; i++) {
            sb.append("\t");
        }
        sb.append("}");
    }

}
