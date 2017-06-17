package org.openmuc.jdlms.app.server;

import org.openmuc.jdlms.CosemAttribute;
import org.openmuc.jdlms.CosemClass;
import org.openmuc.jdlms.CosemInterfaceObject;
import org.openmuc.jdlms.CosemMethod;
import org.openmuc.jdlms.IllegalMethodAccessException;
import org.openmuc.jdlms.MethodResultCode;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.datatypes.DataObject.Type;

@CosemClass(id = 99)
public class SampleClass extends CosemInterfaceObject {

    @CosemAttribute(id = 2, type = Type.OCTET_STRING)
    private DataObject d1;

    public SampleClass() {
        super("0.0.0.2.1.255");
    }

    @CosemMethod(id = 1)
    public void hello() throws IllegalMethodAccessException {
        System.out.println("Has been called");
        return;
    }

    @CosemMethod(id = 2, consumes = Type.OCTET_STRING)
    public DataObject hello2(DataObject datO) throws IllegalMethodAccessException {
        throw new IllegalMethodAccessException(MethodResultCode.OTHER_REASON);
    }

    public DataObject getD1() {
        return DataObject.newOctetStringData("HELLO WORLD".getBytes());
    }

    public void setD1(DataObject d1) {
        byte[] value = d1.getValue();
        System.out.println(new String(value));
        this.d1 = d1;
    }
}
