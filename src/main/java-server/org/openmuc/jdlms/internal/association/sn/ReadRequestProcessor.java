package org.openmuc.jdlms.internal.association.sn;

import static org.openmuc.jdlms.internal.DataConverter.convertDataToDataObject;
import static org.openmuc.jdlms.internal.DlmsEnumFunctions.enumToAxdrEnum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.IllegalAttributeAccessException;
import org.openmuc.jdlms.IllegalMethodAccessException;
import org.openmuc.jdlms.MethodParameter;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SelectiveAccessDescription;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.internal.APdu;
import org.openmuc.jdlms.internal.BaseNameRange;
import org.openmuc.jdlms.internal.BaseNameRange.Access;
import org.openmuc.jdlms.internal.DataConverter;
import org.openmuc.jdlms.internal.DataDirectory.CosemClassInstance;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrBoolean;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrOctetString;
import org.openmuc.jdlms.internal.asn1.cosem.COSEMpdu;
import org.openmuc.jdlms.internal.asn1.cosem.Data_Block_Result;
import org.openmuc.jdlms.internal.asn1.cosem.Parameterized_Access;
import org.openmuc.jdlms.internal.asn1.cosem.ReadRequest;
import org.openmuc.jdlms.internal.asn1.cosem.ReadResponse;
import org.openmuc.jdlms.internal.asn1.cosem.ReadResponse.SubChoice;
import org.openmuc.jdlms.internal.asn1.cosem.Unsigned16;
import org.openmuc.jdlms.internal.asn1.cosem.Variable_Access_Specification;
import org.openmuc.jdlms.internal.association.AssociationMessenger;
import org.openmuc.jdlms.internal.association.RequestProcessorData;

public class ReadRequestProcessor extends SnRequestProcessorBase {

    public ReadRequestProcessor(AssociationMessenger associationMessenger, RequestProcessorData requestProcessorData) {
        super(associationMessenger, requestProcessorData);
    }

    @Override
    public void processRequest(COSEMpdu request) throws IOException {
        ReadRequest readReq = request.readRequest;
        List<Variable_Access_Specification> list = readReq.list();

        ReadResponse readRes = new ReadResponse();
        for (Variable_Access_Specification varAccessSpec : list) {
            switch (varAccessSpec.getChoiceIndex()) {
            case VARIABLE_NAME:
                readRes.add(varNameAccess(varAccessSpec));
                break;
            case PARAMETERIZED_ACCESS:
                readRes.add(varParamAccess(varAccessSpec));
                break;

            case READ_DATA_BLOCK_ACCESS:
                // list of method access
                break;

            case BLOCK_NUMBER_ACCESS:
            case WRITE_DATA_BLOCK_ACCESS:
            case _ERR_NONE_SELECTED:
            default:
                // illegal at this point
                break;

            }

        }

        APdu aPdu = newAPdu();

        aPdu.getCosemPdu().setreadResponse(readRes);

        byte[] encodedApdu = this.associationMessenger.encode(aPdu);

        if (!this.associationMessenger.apduTooLarge(encodedApdu.length)) {
            this.associationMessenger.send(encodedApdu);
        }
        else {
            sendResposneAsBlocks(readRes, encodedApdu);

        }

    }

    private void sendResposneAsBlocks(ReadResponse readRes, byte[] encodedApdu) throws IOException {
        readRes.encodeAndSave(encodedApdu.length);
        byte[] rawDataToSend = readRes.code;

        final int expectedApduOverhead = encodedApdu.length - rawDataToSend.length;

        ByteBuffer rawDataBuffer = ByteBuffer.wrap(rawDataToSend);

        long blockNumber = 1;

        final int blockSize = this.associationMessenger.getMaxMessageLength() - expectedApduOverhead;

        byte[] blockVal = new byte[blockSize];
        while (true) {
            rawDataBuffer.get(blockVal);
            boolean lastBlock = !rawDataBuffer.hasRemaining();

            ReadResponse blockResponse = new ReadResponse();
            SubChoice block = new SubChoice();
            Data_Block_Result blockData = new Data_Block_Result(new AxdrBoolean(lastBlock),
                    new Unsigned16(blockNumber++), new AxdrOctetString(blockVal));
            block.setdata_block_result(blockData);
            blockResponse.add(block);

            APdu blockApdu = newAPdu();
            blockApdu.getCosemPdu().setreadResponse(blockResponse);

            this.associationMessenger.encodeAndSend(blockApdu);

            if (rawDataBuffer.remaining() < blockVal.length) {
                if (!rawDataBuffer.hasRemaining()) {
                    return;
                }

                blockVal = new byte[rawDataBuffer.remaining()];
            }

            veifyBlockNumberAccessResponse(blockNumber);
        }
    }

    private void veifyBlockNumberAccessResponse(long blockNumber) throws IOException {
        COSEMpdu cosemPdu = this.associationMessenger.readNextApdu().getCosemPdu();
        if (cosemPdu == null || cosemPdu.getChoiceIndex() != COSEMpdu.Choices.READREQUEST) {
            // error
            throw new IOException("wrong cosem pdu" + cosemPdu);
        }

        ReadRequest readRequest = cosemPdu.readRequest;

        if (readRequest.size() != 1) {
            // error
        }

        Variable_Access_Specification blockNumSpec = readRequest.get(0);
        if (blockNumSpec.getChoiceIndex() != Variable_Access_Specification.Choices.BLOCK_NUMBER_ACCESS) {
            // error
        }

        long blockNumRes = blockNumSpec.block_number_access.block_number.getValue();

        if (blockNumRes != blockNumber) {
            // error
        }
    }

    private SubChoice varParamAccess(Variable_Access_Specification varAccessSpec) throws IOException {
        Parameterized_Access parameterizedAccess = varAccessSpec.parameterized_access;
        final int variableName = (int) parameterizedAccess.variable_name.getValue() & 0xFFFF;

        BaseNameRange intersectingRange = this.nameRangeSet.getIntersectingRange(variableName);

        Access access;
        try {
            access = accessFor(variableName, intersectingRange);
        } catch (IllegalAttributeAccessException e) {
            return errorSubChoiceFor(e);
        }

        CosemClassInstance classInstance = intersectingRange.getClassInstance();

        int classId = classInstance.getCosemClass().id();

        ObisCode instanceId = classInstance.getInstance().getInstanceId();

        int memberId = access.getMemberId();

        DataObject parameter = convertDataToDataObject(parameterizedAccess.parameter);
        int selector = (int) (parameterizedAccess.selector.getValue() & 0xff);

        SelectiveAccessDescription accessDescription = new SelectiveAccessDescription(selector, parameter);

        switch (access.getAccessType()) {
        case ATTRIBUTE:
        default:
            return variableGet(classId, instanceId, memberId, accessDescription);
        case METHOD:
            return methodAction(memberId, classId, instanceId, accessDescription);
        }

    }

    private SubChoice varNameAccess(Variable_Access_Specification varAccessSpec) throws IOException {
        final int variableName = (int) varAccessSpec.variable_name.getValue() & 0xFFFF;
        BaseNameRange intersectingRange = this.nameRangeSet.getIntersectingRange(variableName);

        Access access;
        try {
            access = accessFor(variableName, intersectingRange);
        } catch (IllegalAttributeAccessException e) {
            return errorSubChoiceFor(e);
        }

        CosemClassInstance classInstance = intersectingRange.getClassInstance();

        int classId = classInstance.getCosemClass().id();

        ObisCode instanceId = classInstance.getInstance().getInstanceId();
        int memberId = access.getMemberId();

        switch (access.getAccessType()) {
        case ATTRIBUTE:
            return variableGet(classId, instanceId, memberId, null);

        default:
        case METHOD:
            // should not occur..
            throw new IOException();
        }
    }

    private static SubChoice errorSubChoiceFor(IllegalAttributeAccessException e) {
        SubChoice subChoice = new SubChoice();
        subChoice.setdata_access_error(enumToAxdrEnum(e.getAccessResultCode()));
        return subChoice;
    }

    private SubChoice methodAction(int memberId, int classId, ObisCode instanceId,
            SelectiveAccessDescription accessDescription) {

        DataObject param = accessDescription.getAccessParameter();

        MethodParameter methodParameter = new MethodParameter(classId, instanceId, memberId, param);

        SubChoice res = new SubChoice();

        try {
            DataObject result = this.requestProcessorData.directory.invokeMethod(logicalDeviceId(), methodParameter,
                    connectionId());

            if (result == null) {
                result = DataObject.newNullData();
            }
            res.setdata(DataConverter.convertDataObjectToData(result));
        } catch (IllegalMethodAccessException e) {
            res.setdata_access_error(enumToAxdrEnum(e.getMethodResultCode()));
        }
        return res;
    }

    private SubChoice variableGet(int classId, ObisCode instanceId, int memberId,
            SelectiveAccessDescription selectiveAccessDescription) {
        AttributeAddress attributeAddress = new AttributeAddress(classId, instanceId, memberId,
                selectiveAccessDescription);

        SubChoice res = new SubChoice();
        try {
            DataObject result = this.requestProcessorData.directory.get(logicalDeviceId(), attributeAddress,
                    connectionId());
            res.setdata(DataConverter.convertDataObjectToData(result));
        } catch (IllegalAttributeAccessException e) {
            res.setdata_access_error(enumToAxdrEnum(e.getAccessResultCode()));
        }
        return res;
    }

}
