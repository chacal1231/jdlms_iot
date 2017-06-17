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

import static java.util.Collections.emptyList;
import static org.openmuc.jdlms.ConformanceSetting.ACTION;
import static org.openmuc.jdlms.ConformanceSetting.ATTRIBUTE0_SUPPORTED_WITH_GET;
import static org.openmuc.jdlms.ConformanceSetting.ATTRIBUTE0_SUPPORTED_WITH_SET;
import static org.openmuc.jdlms.ConformanceSetting.BLOCK_TRANSFER_WITH_ACTION;
import static org.openmuc.jdlms.ConformanceSetting.BLOCK_TRANSFER_WITH_GET_OR_READ;
import static org.openmuc.jdlms.ConformanceSetting.BLOCK_TRANSFER_WITH_SET_OR_WRITE;
import static org.openmuc.jdlms.ConformanceSetting.GET;
import static org.openmuc.jdlms.ConformanceSetting.MULTIPLE_REFERENCES;
import static org.openmuc.jdlms.ConformanceSetting.PRIORITY_MGMT_SUPPORTED;
import static org.openmuc.jdlms.ConformanceSetting.SELECTIVE_ACCESS;
import static org.openmuc.jdlms.ConformanceSetting.SET;
import static org.openmuc.jdlms.internal.DataConverter.convertDataObjectToData;
import static org.openmuc.jdlms.internal.DlmsEnumFunctions.enumValueFrom;
import static org.openmuc.jdlms.internal.PduHelper.invokeIdFrom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openmuc.jdlms.JDlmsException.ExceptionId;
import org.openmuc.jdlms.JDlmsException.Fault;
import org.openmuc.jdlms.SecuritySuite.EncryptionMechanism;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.interfaceclass.method.AssociationLnMethod;
import org.openmuc.jdlms.internal.ContextId;
import org.openmuc.jdlms.internal.DataConverter;
import org.openmuc.jdlms.internal.PduHelper;
import org.openmuc.jdlms.internal.WellKnownInstanceIds;
import org.openmuc.jdlms.internal.asn1.axdr.AxdrType;
import org.openmuc.jdlms.internal.asn1.axdr.NullOutputStream;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrEnum;
import org.openmuc.jdlms.internal.asn1.cosem.ACTION_Request;
import org.openmuc.jdlms.internal.asn1.cosem.ACTION_Response;
import org.openmuc.jdlms.internal.asn1.cosem.Action_Request_Next_Pblock;
import org.openmuc.jdlms.internal.asn1.cosem.Action_Request_Normal;
import org.openmuc.jdlms.internal.asn1.cosem.Action_Request_With_List;
import org.openmuc.jdlms.internal.asn1.cosem.Action_Response_With_Optional_Data;
import org.openmuc.jdlms.internal.asn1.cosem.COSEMpdu;
import org.openmuc.jdlms.internal.asn1.cosem.Cosem_Attribute_Descriptor;
import org.openmuc.jdlms.internal.asn1.cosem.Cosem_Attribute_Descriptor_With_Selection;
import org.openmuc.jdlms.internal.asn1.cosem.Cosem_Method_Descriptor;
import org.openmuc.jdlms.internal.asn1.cosem.Data;
import org.openmuc.jdlms.internal.asn1.cosem.GET_Request;
import org.openmuc.jdlms.internal.asn1.cosem.GET_Response;
import org.openmuc.jdlms.internal.asn1.cosem.Get_Data_Result;
import org.openmuc.jdlms.internal.asn1.cosem.Get_Request_Next;
import org.openmuc.jdlms.internal.asn1.cosem.Get_Request_Normal;
import org.openmuc.jdlms.internal.asn1.cosem.Get_Request_With_List;
import org.openmuc.jdlms.internal.asn1.cosem.Get_Response_With_List.SubSeqOf_result;
import org.openmuc.jdlms.internal.asn1.cosem.Invoke_Id_And_Priority;
import org.openmuc.jdlms.internal.asn1.cosem.SET_Request;
import org.openmuc.jdlms.internal.asn1.cosem.SET_Response;
import org.openmuc.jdlms.internal.asn1.cosem.Selective_Access_Descriptor;
import org.openmuc.jdlms.internal.asn1.cosem.Set_Request_Normal;
import org.openmuc.jdlms.internal.asn1.cosem.Set_Request_With_List;
import org.openmuc.jdlms.internal.asn1.cosem.Unsigned8;
import org.openmuc.jdlms.sessionlayer.SessionLayer;
import org.openmuc.jdlms.settings.client.Settings;

/*
 * Variant of the connection class using decrypted messages with logical name referencing to communicate with the remote
 * smart meter
 */
class DlmsLnConnection extends DlmsConnection {

    private final ResponseQueue<ACTION_Response> actionResponseQueue = new ResponseQueue<>();
    private final ResponseQueue<GET_Response> getResponseQueue = new ResponseQueue<>();
    private final ResponseQueue<SET_Response> setResponseQueue = new ResponseQueue<>();

    DlmsLnConnection(Settings settings, SessionLayer sessionlayer) throws IOException {
        super(settings, sessionlayer);
    }

    @Override
    public List<GetResult> get(boolean priority, List<AttributeAddress> params) throws IOException {
        if (params.isEmpty()) {
            return emptyList();
        }

        Invoke_Id_And_Priority id = invokeIdAndPriorityFor(priority);
        int invokeId = PduHelper.invokeIdFrom(id);
        COSEMpdu pdu = createGetPdu(id, params);

        int pduSize = pduSizeOf(pdu);
        if (maxSendPduSize() != 0 && pduSize > maxSendPduSize()) {
            // IEC 62056-5-3 2013, Section 6.6 The GET service, Page 52:
            // A GET.request service primitive shall always fit in a single APDU
            throw new FatalJDlmsException(ExceptionId.GET_REQUEST_TOO_LARGE, Fault.USER,
                    MessageFormat.format(
                            "PDU ({0} byte) is too long for single GET.request. Max send PDU size is {1} byte.",
                            pduSize, maxSendPduSize()));
        }
        send(pdu);
        int responseTimeout = connectionSettings().responseTimeout();
        GET_Response response = getResponseQueue.poll(invokeId, responseTimeout);

        switch (response.getChoiceIndex()) {
        case GET_RESPONSE_NORMAL:
            return Arrays.asList(convertPduToGetResult(response.get_response_normal.result));
        case GET_RESPONSE_WITH_DATABLOCK:
            return readDataBlockG(priority, response, params);
        case GET_RESPONSE_WITH_LIST:
            return convertListToDataObject(response.get_response_with_list.result.list());
        default:
            throw new IllegalStateException(String.format(
                    "Unknown response type with Choice Index %s. Please report to developer of the stack.",
                    response.getChoiceIndex()));
        }
    }

    private List<GetResult> readDataBlockG(boolean priority, GET_Response response, List<AttributeAddress> params)
            throws IOException {
        GetResult res;
        byte[] byteArray = readBlocks(response, priority);
        InputStream dataByteStream = new ByteArrayInputStream(byteArray);

        if (params.size() > 1) {
            SubSeqOf_result subSeqOfResult = new SubSeqOf_result();
            subSeqOfResult.decode(dataByteStream);
            return convertListToDataObject(subSeqOfResult.list());
        }
        else {
            Data resultPduData = new Data();
            resultPduData.decode(dataByteStream);

            Get_Data_Result getResult = new Get_Data_Result();
            getResult.setdata(resultPduData);

            res = convertPduToGetResult(getResult);
            return Arrays.asList(res);
        }
    }

    private byte[] readBlocks(GET_Response response, boolean priority) throws IOException {
        final Invoke_Id_And_Priority invokeIdAndPriority = response.get_response_with_datablock.invoke_id_and_priority;
        final int invokeId = invokeIdAndPriority.getValue()[0] & 0xf;

        ByteArrayOutputStream datablocks = new ByteArrayOutputStream();
        GET_Request getRequest = new GET_Request();
        COSEMpdu pdu = new COSEMpdu();

        Get_Request_Next nextBlock = new Get_Request_Next();
        while (!response.get_response_with_datablock.result.last_block.getValue()) {
            datablocks.write(response.get_response_with_datablock.result.result.raw_data.getValue());

            nextBlock.block_number = response.get_response_with_datablock.result.block_number;
            nextBlock.invoke_id_and_priority = invokeIdAndPriority;

            getRequest.setget_request_next(nextBlock);
            pdu.setget_request(getRequest);
            send(pdu);
            try {
                response = getResponseQueue.poll(invokeId, connectionSettings().responseTimeout());
            } catch (ResponseTimeoutException e) {
                // Send PDU with wrong block number to indicate the
                // device that the block transfer is
                // aborted.
                // This is the well defined behavior to abort a block
                // transfer as in IEC 62056-53 section
                // 7.4.1.8.2
                // receiveTimedOut(pdu);
                send(pdu);

                throw e;
            }
        }
        // TODO: check this
        // if (response.getChoiceIndex().equals(Choices.GET_RESPONSE_NORMAL)) {
        // throw new IOException("Meter response with error, access result code: "
        // + response.get_response_normal.result.data_access_result);
        // }

        // TODO: evaluate on error
        // if (response.get_response_with_datablock.result.result.raw_data == null) {
        // AccessResultCode accessResultCode = AccessResultCode
        // .forValue(response.get_response_with_datablock.result.result.data_access_result.getValue());
        // }

        datablocks.write(response.get_response_with_datablock.result.result.raw_data.getValue());

        return datablocks.toByteArray();
    }

    private List<GetResult> convertListToDataObject(List<Get_Data_Result> resultList) {
        List<GetResult> result = new ArrayList<>(resultList.size());
        for (Get_Data_Result resultPdu : resultList) {
            GetResult res = convertPduToGetResult(resultPdu);
            result.add(res);
        }

        return result;
    }

    private GetResult convertPduToGetResult(Get_Data_Result pdu) {
        if (pdu.getChoiceIndex() == Get_Data_Result.Choices.DATA) {
            return new GetResult(DataConverter.convertDataToDataObject(pdu.data));
        }
        else {
            AccessResultCode resultCode = enumValueFrom(pdu.data_access_result, AccessResultCode.class);
            return new GetResult(resultCode);
        }
    }

    @Override
    public List<AccessResultCode> set(boolean priority, List<SetParameter> params) throws IOException {
        if (params.isEmpty()) {
            return emptyList();
        }

        Invoke_Id_And_Priority invokeIdAndPriority = invokeIdAndPriorityFor(priority);
        SET_Response response = createAndSendSetPdu(invokeIdAndPriority, params);

        switch (response.getChoiceIndex()) {
        case SET_RESPONSE_NORMAL:
            return axdrEnumToAccessResultCode(response.set_response_normal.result);

        case SET_RESPONSE_WITH_LIST:
            return axdrEnumsToAccessResultCodes(response.set_response_with_list.result.list());

        case SET_RESPONSE_LAST_DATABLOCK:
            return axdrEnumToAccessResultCode(response.set_response_last_datablock.result);

        case SET_RESPONSE_LAST_DATABLOCK_WITH_LIST:
            return axdrEnumsToAccessResultCodes(response.set_response_last_datablock_with_list.result.list());

        default:
            throw new IllegalStateException("Unknown response type");
        }

    }

    private List<AccessResultCode> axdrEnumToAccessResultCode(AxdrEnum axdrEnum) {
        return Arrays.asList(enumValueFrom(axdrEnum, AccessResultCode.class));
    }

    private List<AccessResultCode> axdrEnumsToAccessResultCodes(List<AxdrEnum> enums) {
        List<AccessResultCode> result = new ArrayList<>(enums.size());
        for (AxdrEnum res : enums) {
            result.add(enumValueFrom(res, AccessResultCode.class));
        }
        return result;
    }

    @Override
    public List<MethodResult> action(boolean priority, List<MethodParameter> params) throws IOException {
        if (params.isEmpty()) {
            return emptyList();
        }

        final Invoke_Id_And_Priority id = invokeIdAndPriorityFor(priority);

        ACTION_Response response = createAndSendActionPdu(id, params);

        switch (response.getChoiceIndex()) {
        case ACTION_RESPONSE_NORMAL:
            return processActionNormal(response);
        case ACTION_RESPONSE_WITH_LIST:
            return processActionWithList(response);
        case ACTION_RESPONSE_WITH_PBLOCK:
            return processActionWithPblock(id, response);

        default:
        case ACTION_RESPONSE_NEXT_PBLOCK:
        case _ERR_NONE_SELECTED:
            throw new IOException("Server answered with an illegal response.");
        }

    }

    private List<MethodResult> processActionNormal(ACTION_Response response) {
        Action_Response_With_Optional_Data resWithOpt = response.action_response_normal.single_response;

        DataObject resultData = null;
        if (resWithOpt.return_parameters.isUsed()) {
            resultData = DataConverter.convertDataToDataObject(resWithOpt.return_parameters.getValue().data);
        }
        return Arrays.asList(new MethodResult(enumValueFrom(resWithOpt.result, MethodResultCode.class), resultData));
    }

    private List<MethodResult> processActionWithList(ACTION_Response response) {
        List<MethodResult> result = new LinkedList<>();
        for (Action_Response_With_Optional_Data resp : response.action_response_with_list.list_of_responses.list()) {
            DataObject resultData = null;
            if (resp.return_parameters.isUsed()) {
                resultData = DataConverter.convertDataToDataObject(resp.return_parameters.getValue().data);
            }
            MethodResultCode methodResultCode = enumValueFrom(resp.result, MethodResultCode.class);
            result.add(new MethodResult(methodResultCode, resultData));
        }
        return result;
    }

    private List<MethodResult> processActionWithPblock(final Invoke_Id_And_Priority id, ACTION_Response response)
            throws IOException {
        List<MethodResult> result = new LinkedList<>();
        ByteArrayOutputStream datablocks = new ByteArrayOutputStream();
        COSEMpdu pdu = new COSEMpdu();
        ACTION_Request request = new ACTION_Request();
        Action_Request_Next_Pblock nextBlock = new Action_Request_Next_Pblock();
        nextBlock.invoke_id_and_priority = response.action_response_with_pblock.invoke_id_and_priority;

        while (!response.action_response_with_pblock.pblock.last_block.getValue()) {
            datablocks.write(response.action_response_with_pblock.pblock.raw_data.getValue());

            nextBlock.block_number = response.action_response_with_pblock.pblock.block_number;
            request.setaction_request_next_pblock(nextBlock);
            pdu.setaction_request(request);
            send(pdu);

            response = this.actionResponseQueue.poll(invokeIdFrom(id), connectionSettings().responseTimeout());
        }
        datablocks.write(response.action_response_with_pblock.pblock.raw_data.getValue());
        InputStream dataByteStream = new ByteArrayInputStream(datablocks.toByteArray());
        while (dataByteStream.available() > 0) {
            Get_Data_Result dataResult = new Get_Data_Result();
            dataResult.decode(dataByteStream);
            // If remote Method call returns a pdu that must be
            // segmented into datablocks, we can assume that the call
            // was successful.
            DataObject resultData = DataConverter.convertDataToDataObject(dataResult.data);
            result.add(new MethodResult(MethodResultCode.SUCCESS, resultData));
        }
        return result;
    }

    @Override
    void processPdu(COSEMpdu pdu) {

        try {
            switch (pdu.getChoiceIndex()) {
            case GET_RESPONSE:
                getResponseQueue.put(PduHelper.invokeIdFrom(pdu.get_response), pdu.get_response);
                break;
            case SET_RESPONSE:
                setResponseQueue.put(PduHelper.invokeIdFrom(pdu.set_response), pdu.set_response);
                break;
            case ACTION_RESPONSE:
                actionResponseQueue.put(PduHelper.invokeIdFrom(pdu.action_response), pdu.action_response);
                break;
            case EVENT_NOTIFICATION_REQUEST:
                // TODO: implement listening
                // if (connectionSettings().clientConnectionEventListener() != null) {
                // EventNotification notification = DataConverter.toApi(pdu.event_notification_request);
                // connectionSettings().clientConnectionEventListener().onEventReceived(notification);
                // }
                break;

            default:
                // TODO: handle this case..
            }
        } catch (InterruptedException e) {
            // ignore this here
        }
    }

    @Override
    Set<ConformanceSetting> proposedConformance() {
        return new HashSet<>(Arrays.asList(GET, SET, ACTION, /* EVENT_NOTIFICATION, */ SELECTIVE_ACCESS,
                PRIORITY_MGMT_SUPPORTED, MULTIPLE_REFERENCES, BLOCK_TRANSFER_WITH_ACTION,
                BLOCK_TRANSFER_WITH_GET_OR_READ, BLOCK_TRANSFER_WITH_SET_OR_WRITE, ATTRIBUTE0_SUPPORTED_WITH_GET,
                ATTRIBUTE0_SUPPORTED_WITH_SET));
    }

    @Override
    MethodResult hlsAuthentication(byte[] processedChallenge) throws IOException {
        DataObject param = DataObject.newOctetStringData(processedChallenge);

        MethodParameter authenticate = new MethodParameter(AssociationLnMethod.REPLY_TO_HLS_AUTHENTICATION,
                WellKnownInstanceIds.CURRENT_ASSOCIATION_ID, param);

        return action(true, authenticate);
    }

    /*
     * Creates a PDU to read all attributes listed in params
     */
    private COSEMpdu createGetPdu(Invoke_Id_And_Priority id, List<AttributeAddress> params) {
        if (!negotiatedFeatures().contains(ATTRIBUTE0_SUPPORTED_WITH_GET)) {
            checkAttributeIdValidty(params);
        }
        if (!negotiatedFeatures().contains(ConformanceSetting.SELECTIVE_ACCESS)) {
            for (AttributeAddress param : params) {
                if (param.getAccessSelection() != null) {
                    throw new IllegalArgumentException("Selective Access not supported on this connection");
                }
            }
        }

        GET_Request getRequest = new GET_Request();
        if (params.size() == 1) {
            Get_Request_Normal requestNormal = new Get_Request_Normal();
            requestNormal.invoke_id_and_priority = id;
            AttributeAddress attributeAddress = params.get(0);
            requestNormal.cosem_attribute_descriptor = attributeAddress.toDescriptor();
            SelectiveAccessDescription accessSelection = attributeAddress.getAccessSelection();
            if (accessSelection != null) {
                requestNormal.access_selection
                        .setValue(new Selective_Access_Descriptor(new Unsigned8(accessSelection.getAccessSelector()),
                                DataConverter.convertDataObjectToData(accessSelection.getAccessParameter())));
            }

            getRequest.setget_request_normal(requestNormal);
        }
        else {
            Get_Request_With_List requestList = new Get_Request_With_List();
            requestList.invoke_id_and_priority = id;
            requestList.attribute_descriptor_list = new Get_Request_With_List.SubSeqOf_attribute_descriptor_list();
            for (AttributeAddress p : params) {
                Selective_Access_Descriptor access = null;
                SelectiveAccessDescription accessSelection = p.getAccessSelection();
                if (accessSelection != null) {
                    access = new Selective_Access_Descriptor(new Unsigned8(accessSelection.getAccessSelector()),
                            DataConverter.convertDataObjectToData(accessSelection.getAccessParameter()));
                }
                requestList.attribute_descriptor_list
                        .add(new Cosem_Attribute_Descriptor_With_Selection(p.toDescriptor(), access));
            }

            getRequest.setget_request_with_list(requestList);
        }

        COSEMpdu pdu = new COSEMpdu();
        pdu.setget_request(getRequest);

        return pdu;
    }

    private static void checkAttributeIdValidty(List<AttributeAddress> params) {
        for (AttributeAddress param : params) {
            if (param.getId() == 0) {
                throw new IllegalArgumentException("No Attribute 0 on get allowed");
            }
        }
    }

    private static int pduSizeOf(AxdrType pdu) throws IOException {
        return pdu.encode(new NullOutputStream());
    }

    private SET_Response createAndSendSetPdu(Invoke_Id_And_Priority id, List<SetParameter> params) throws IOException {
        if (!negotiatedFeatures().contains(ATTRIBUTE0_SUPPORTED_WITH_SET)) {
            for (SetParameter param : params) {
                if (param.getAttributeAddress().getId() == 0) {
                    throw new IllegalArgumentException("No Attribute 0 on set allowed");
                }
            }
        }

        SET_Request request = new SET_Request();

        if (params.size() == 1) {
            Set_Request_Normal requestNormal = new Set_Request_Normal();
            requestNormal.invoke_id_and_priority = id;
            SetParameter setParameter = params.get(0);
            requestNormal.cosem_attribute_descriptor = setParameter.getAttributeAddress().toDescriptor();
            requestNormal.value = DataConverter.convertDataObjectToData(setParameter.getData());
            SelectiveAccessDescription accessSelection = setParameter.getAttributeAddress().getAccessSelection();
            if (accessSelection != null) {
                requestNormal.access_selection
                        .setValue(new Selective_Access_Descriptor(new Unsigned8(accessSelection.getAccessSelector()),
                                DataConverter.convertDataObjectToData(accessSelection.getAccessParameter())));
            }
            request.setset_request_normal(requestNormal);
        }
        else {
            Set_Request_With_List requestList = new Set_Request_With_List();
            requestList.invoke_id_and_priority = id;
            requestList.attribute_descriptor_list = new Set_Request_With_List.SubSeqOf_attribute_descriptor_list();
            requestList.value_list = new Set_Request_With_List.SubSeqOf_value_list();
            for (SetParameter p : params) {
                Selective_Access_Descriptor access = null;
                SelectiveAccessDescription accessSelection = p.getAttributeAddress().getAccessSelection();
                if (accessSelection != null) {
                    access = new Selective_Access_Descriptor(new Unsigned8(accessSelection.getAccessSelector()),
                            DataConverter.convertDataObjectToData(accessSelection.getAccessParameter()));
                }
                Cosem_Attribute_Descriptor desc = p.getAttributeAddress().toDescriptor();
                requestList.attribute_descriptor_list.add(new Cosem_Attribute_Descriptor_With_Selection(desc, access));
                requestList.value_list.add(DataConverter.convertDataObjectToData(p.getData()));
            }
            request.setset_request_with_list(requestList);
        }

        if (maxSendPduSize() == 0 || pduSizeOf(request) <= maxSendPduSize()) {
            COSEMpdu pdu = new COSEMpdu();
            pdu.setset_request(request);

            send(pdu);

            return setResponseQueue.poll(invokeIdFrom(id), connectionSettings().responseTimeout());
        }
        else {

            // if (params.length == 1) {
            // baos.reset();
            // request.set_request_normal.value.encode(baos);
            // dataBuffer = ByteBuffer.wrap(baos.getArray());
            //
            // Set_Request_With_First_Datablock requestFirstBlock = new Set_Request_With_First_Datablock();
            // requestFirstBlock.invoke_id_and_priority = id;
            // requestFirstBlock.cosem_attribute_descriptor = request.set_request_normal.cosem_attribute_descriptor;
            // requestFirstBlock.access_selection = request.set_request_normal.access_selection;
            // requestFirstBlock.datablock = new DataBlock_SA(new AxdrBoolean(false), new Unsigned32(0),
            // new AxdrOctetString(0));
            //
            // // TODO what is baos used here for?
            // baos.reset();
            // int length = requestFirstBlock.encode(baos);
            // byte[] firstDataChunk = new byte[maxSendPduSize() - 2 - length];
            // dataBuffer.get(firstDataChunk, 0, firstDataChunk.length);
            // requestFirstBlock.datablock.raw_data = new AxdrOctetString(firstDataChunk);
            //
            // request.setset_request_with_first_datablock(requestFirstBlock);
            // pdu = new COSEMpdu();
            // pdu.setset_request(request);
            // result.add(pdu);
            // }
            // else {
            // baos.reset();
            // for (int i = request.set_request_with_list.value_list.size() - 1; i >= 0; i--) {
            // request.set_request_with_list.value_list.get(i).encode(baos);
            // }
            // dataBuffer = ByteBuffer.wrap(baos.getArray());
            //
            // Set_Request_With_List_And_First_Datablock requestListFirstBlock = new
            // Set_Request_With_List_And_First_Datablock();
            // requestListFirstBlock.invoke_id_and_priority = id;
            // requestListFirstBlock.attribute_descriptor_list = new
            // Set_Request_With_List_And_First_Datablock.SubSeqOf_attribute_descriptor_list();
            // requestListFirstBlock.datablock = new DataBlock_SA(new AxdrBoolean(false), new Unsigned32(1),
            // new AxdrOctetString(0));
            //
            // for (Cosem_Attribute_Descriptor_With_Selection desc :
            // request.set_request_with_list.attribute_descriptor_list
            // .list()) {
            // requestListFirstBlock.attribute_descriptor_list.add(desc);
            // }
            //
            // baos.reset();
            // int length = requestListFirstBlock.encode(baos);
            // byte[] firstDataChunk = new byte[maxSendPduSize() - 2 - length];
            // dataBuffer.get(firstDataChunk, 0, firstDataChunk.length);
            // requestListFirstBlock.datablock.raw_data = new AxdrOctetString(firstDataChunk);
            //
            // request.setset_request_with_list_and_first_datablock(requestListFirstBlock);
            // pdu = new COSEMpdu();
            // pdu.setset_request(request);
            // result.add(pdu);
            // }
            //
            // int blockNr = 1;
            // while (dataBuffer.hasRemaining()) {
            // blockNr++;
            // int blockLength = Math.min(maxSendPduSize() - 9, dataBuffer.remaining());
            // byte[] dataBlock = new byte[blockLength];
            // dataBuffer.get(dataBlock, 0, dataBlock.length);
            //
            // Set_Request_With_Datablock requestBlock = new Set_Request_With_Datablock();
            // requestBlock.invoke_id_and_priority = id;
            // requestBlock.datablock = new DataBlock_SA(new AxdrBoolean(dataBuffer.remaining() == 0),
            // new Unsigned32(blockNr), new AxdrOctetString(dataBlock));
            //
            // request.setset_request_with_datablock(requestBlock);
            // pdu = new COSEMpdu();
            // pdu.setset_request(request);
            // result.add(pdu);
            // }
            return null;
        }

    }

    private ACTION_Response createAndSendActionPdu(Invoke_Id_And_Priority invokeIdAndPrio, List<MethodParameter> params)
            throws IOException {
        for (MethodParameter param : params) {
            if (param.getId() == 0) {
                throw new IllegalArgumentException("MethodID 0 not allowed on action");
            }
        }

        ACTION_Request request = new ACTION_Request();

        if (params.size() == 1) {
            Action_Request_Normal requestNormal = new Action_Request_Normal();
            requestNormal.invoke_id_and_priority = invokeIdAndPrio;
            MethodParameter methodParameter = params.get(0);

            requestNormal.cosem_method_descriptor = methodParameter.toDescriptor();

            boolean paramIsUsed = !methodParameter.getParameter().isNull();

            requestNormal.method_invocation_parameters.setUsed(paramIsUsed);
            if (paramIsUsed) {
                Data convertedData = convertDataObjectToData(methodParameter.getParameter());
                requestNormal.method_invocation_parameters.setValue(convertedData);
            }

            request.setaction_request_normal(requestNormal);
        }
        else {
            Action_Request_With_List requestList = new Action_Request_With_List();
            requestList.invoke_id_and_priority = invokeIdAndPrio;
            requestList.cosem_method_descriptor_list = new Action_Request_With_List.SubSeqOf_cosem_method_descriptor_list();
            requestList.method_invocation_parameters = new Action_Request_With_List.SubSeqOf_method_invocation_parameters();
            for (MethodParameter param : params) {
                Cosem_Method_Descriptor desc = param.toDescriptor();
                requestList.cosem_method_descriptor_list.add(desc);
                requestList.method_invocation_parameters.add(convertDataObjectToData(param.getParameter()));
            }
            request.setaction_request_with_list(requestList);
        }

        if (maxSendPduSize() == 0 || pduSizeOf(request) <= maxSendPduSize()) {
            COSEMpdu pdu = new COSEMpdu();
            pdu.setaction_request(request);
            send(pdu);

            int invokeId = invokeIdFrom(invokeIdAndPrio);
            return actionResponseQueue.poll(invokeId, connectionSettings().responseTimeout());
        }
        else {
            // PDU is too large to send in one chunk to the meter
            // use of several Datablocks instead

            // if (params.length == 1) {
            // request.action_request_normal.method_invocation_parameters.encode(baos);
            // dataBuffer = ByteBuffer.wrap(baos.getArray());
            //
            // Action_Request_With_First_Pblock requestFirstBlock = new Action_Request_With_First_Pblock();
            // requestFirstBlock.invoke_id_and_priority = id;
            // requestFirstBlock.cosem_method_descriptor = request.action_request_normal.cosem_method_descriptor;
            //
            // baos.reset();
            // int length = requestFirstBlock.encode(baos);
            // byte[] firstDataChunk = new byte[maxSendPduSize() - 2 - length];
            // dataBuffer.get(firstDataChunk, 0, firstDataChunk.length);
            // requestFirstBlock.pblock = new DataBlock_SA(new AxdrBoolean(false), new Unsigned32(0),
            // new AxdrOctetString(firstDataChunk));
            //
            // request.setaction_request_with_first_pblock(requestFirstBlock);
            // pdu = new COSEMpdu();
            // pdu.setaction_request(request);
            // result.add(pdu);
            // }
            // else {
            // baos.reset();
            // request.action_request_with_list.method_invocation_parameters.encode(baos);
            // dataBuffer = ByteBuffer.wrap(baos.getArray());
            //
            // Action_Request_With_List_And_First_Pblock requestListFirstBlock = new
            // Action_Request_With_List_And_First_Pblock();
            // requestListFirstBlock.invoke_id_and_priority = id;
            // requestListFirstBlock.cosem_method_descriptor_list = new
            // Action_Request_With_List_And_First_Pblock.SubSeqOf_cosem_method_descriptor_list();
            //
            // for (Cosem_Method_Descriptor desc : request.action_request_with_list.cosem_method_descriptor_list
            // .list()) {
            // requestListFirstBlock.cosem_method_descriptor_list.add(desc);
            // }
            //
            // baos.reset();
            // int length = requestListFirstBlock.encode(baos);
            // byte[] firstDataChunk = new byte[maxSendPduSize() - 2 - length];
            // dataBuffer.get(firstDataChunk, 0, firstDataChunk.length);
            // requestListFirstBlock.pblock = new DataBlock_SA(new AxdrBoolean(false), new Unsigned32(0),
            // new AxdrOctetString(firstDataChunk));
            //
            // request.setaction_request_with_list_and_first_pblock(requestListFirstBlock);
            // }
            //
            // int blockNr = 1;
            // while (dataBuffer.hasRemaining()) {
            // int blockLength = Math.min(maxSendPduSize() - 8, dataBuffer.remaining());
            // byte[] dataBlock = new byte[blockLength];
            // dataBuffer.get(dataBlock, 0, dataBlock.length);
            //
            // Action_Request_With_Pblock requestBlock = new Action_Request_With_Pblock();
            // requestBlock.pBlock = new DataBlock_SA(new AxdrBoolean(dataBuffer.remaining() == 0),
            // new Unsigned32(blockNr), new AxdrOctetString(dataBlock));
            //
            // request.setaction_request_with_pblock(requestBlock);
            // pdu = new COSEMpdu();
            // pdu.setaction_request(request);
            // result.add(pdu);
            // }
            return null;
        }
    }

    @Override
    protected void validateReferencingMethod() throws IOException {
        if (!(negotiatedFeatures().contains(SET) || negotiatedFeatures().contains(ConformanceSetting.GET))) {
            close();
            throw new FatalJDlmsException(ExceptionId.WRONG_REFERENCING_METHOD, Fault.USER,
                    "Wrong referencing method. Remote smart meter can't use LN referencing.");
        }
    }

    @Override
    protected ContextId getContextId() {

        if (connectionSettings().securitySuite().getEncryptionMechanism() != EncryptionMechanism.NONE) {
            return ContextId.LOGICAL_NAME_REFERENCING_WITH_CIPHERING;
        }
        else {
            return ContextId.LOGICAL_NAME_REFERENCING_NO_CIPHERING;
        }
    }

}
