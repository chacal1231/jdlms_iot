package org.openmuc.jdlms.internal.systemclasses;

import static org.openmuc.jdlms.AttributeAccessMode.READ_ONLY;
import static org.openmuc.jdlms.internal.WellKnownInstanceIds.SAP_ASSIGNMENT_ID;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.openmuc.jdlms.CosemAttribute;
import org.openmuc.jdlms.CosemClass;
import org.openmuc.jdlms.CosemSnInterfaceObject;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.internal.DataDirectory;

@CosemClass(id = 17, version = 0)
public class SapAssignment extends CosemSnInterfaceObject {

    private static final int SAP_ASSIGNMENT_BASE_NAME = 0xFB00;

    @CosemAttribute(id = 2, accessMode = READ_ONLY)
    private DataObject sapAssignmentList;

    @CosemDataDirectory
    private DataDirectory directory;

    public SapAssignment() {
        super(SAP_ASSIGNMENT_BASE_NAME, SAP_ASSIGNMENT_ID);
    }

    public DataObject getSapAssignmentList() {
        if (this.sapAssignmentList != null) {
            return this.sapAssignmentList;
        }

        List<DataObject> assListElements = new LinkedList<>();

        for (Integer logicalDeviceId : directory.getLogicalDeviceIds()) {
            byte[] logicalDeviceNameBytes = directory.getLogicalDeviceFor(logicalDeviceId)
                    .getLogicalDevice()
                    .getLogicalDeviceName()
                    .getBytes(StandardCharsets.US_ASCII);

            DataObject sap = DataObject.newUInteger16Data(logicalDeviceId);
            DataObject logicalDeviceNameDO = DataObject.newOctetStringData(logicalDeviceNameBytes);

            DataObject assListElement = DataObject.newStructureData(sap, logicalDeviceNameDO);
            assListElements.add(assListElement);
        }

        this.sapAssignmentList = DataObject.newArrayData(assListElements);
        return this.sapAssignmentList;
    }

}
