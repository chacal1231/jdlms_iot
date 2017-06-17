package org.openmuc.jdlms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.openmuc.jdlms.internal.PduHelper.invokeIdFrom;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.util.Scanner;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.interfaceclass.attribute.AssociationLnAttribute;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrBoolean;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrOctetString;
import org.openmuc.jdlms.internal.asn1.cosem.COSEMpdu;
import org.openmuc.jdlms.internal.asn1.cosem.DataBlock_G;
import org.openmuc.jdlms.internal.asn1.cosem.DataBlock_G.SubChoice_result;
import org.openmuc.jdlms.internal.asn1.cosem.GET_Request;
import org.openmuc.jdlms.internal.asn1.cosem.GET_Request.Choices;
import org.openmuc.jdlms.internal.asn1.cosem.GET_Response;
import org.openmuc.jdlms.internal.asn1.cosem.Get_Request_Next;
import org.openmuc.jdlms.internal.asn1.cosem.Get_Request_Normal;
import org.openmuc.jdlms.internal.asn1.cosem.Get_Response_With_Datablock;
import org.openmuc.jdlms.internal.asn1.cosem.Invoke_Id_And_Priority;
import org.openmuc.jdlms.internal.asn1.cosem.Unsigned32;
import org.openmuc.jdlms.settings.client.Settings;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DlmsLnConnection.class)
public class LnDataBlockGetTest {

    private static Scanner scanner;

    @BeforeClass
    public static void setUp() throws Exception {
        File file = new File("src/test/resources/kamsprupDataBlockGObjectListGet.txt");
        scanner = new Scanner(file);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        scanner.close();
    }

    /*
     * get data
     */
    @Test
    public void get1Test() throws Exception {
        Settings settings = PowerMockito.mock(Settings.class);
        when(settings.responseTimeout()).thenReturn(100);

        DlmsLnConnection connection = PowerMockito.spy(new DlmsLnConnection(settings, null));

        final ResponseQueue<GET_Response> responseQueue = Whitebox.getInternalState(connection, "getResponseQueue");

        when(connection.proposedConformance()).thenCallRealMethod();
        Set<ConformanceSetting> proposedConformance = connection.proposedConformance();
        when(connection.negotiatedFeatures()).thenReturn(proposedConformance);

        doAnswer(new Answer<Void>() {
            private int blockNumCounter = 1;

            private Invoke_Id_And_Priority initalInvokeIdAndPrio;

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                GET_Request getRequest = invocation.getArgumentAt(0, COSEMpdu.class).get_request;
                Invoke_Id_And_Priority invoke_id_and_priority;
                if (getRequest.getChoiceIndex() == Choices.GET_REQUEST_NORMAL) {
                    Get_Request_Normal getRequestNormal = getRequest.get_request_normal;
                    invoke_id_and_priority = getRequestNormal.invoke_id_and_priority;
                    initalInvokeIdAndPrio = invoke_id_and_priority;
                }
                else if (getRequest.getChoiceIndex() == Choices.GET_REQUEST_NEXT) {
                    Get_Request_Next getRequestNext = getRequest.get_request_next;
                    invoke_id_and_priority = getRequestNext.invoke_id_and_priority;

                    assertEquals("Client Send a new invoke ID.", invokeIdFrom(initalInvokeIdAndPrio),
                            invokeIdFrom(invoke_id_and_priority));
                }
                else {
                    fail("Client send wrong request type..");
                    return null;
                }
                GET_Response response = new GET_Response();

                SubChoice_result subChoice_result = new SubChoice_result();
                subChoice_result.raw_data = new AxdrOctetString(HexConverter.fromShortHexString(scanner.nextLine()));

                DataBlock_G dataBlockG = new DataBlock_G(new AxdrBoolean(!scanner.hasNextLine()),
                        new Unsigned32(blockNumCounter++), subChoice_result);

                response.setget_response_with_datablock(
                        new Get_Response_With_Datablock(invoke_id_and_priority, dataBlockG));

                int invokeId = invokeIdFrom(invoke_id_and_priority);
                responseQueue.put(invokeId, response);
                return null;
            }
        }).when(connection).send(any(COSEMpdu.class));

        GetResult result = connection.get(new AttributeAddress(AssociationLnAttribute.OBJECT_LIST, "0.0.40.0.0.255"));

        assertNotNull(result);

        DataObject resultData = result.getResultData();

        assertEquals(DataObject.Type.ARRAY, resultData.getType());
    }

}
