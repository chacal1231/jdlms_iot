/**
 * This class file was automatically generated by the AXDR compiler that is part of jDLMS (http://www.openmuc.org)
 */

package org.openmuc.jdlms.internal.asn1.cosem;

import java.io.IOException;
import java.io.InputStream;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jdlms.internal.asn1.axdr.AxdrType;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrOptional;

public class Action_Request_Normal implements AxdrType {

    public byte[] code = null;
    public Invoke_Id_And_Priority invoke_id_and_priority = null;

    public Cosem_Method_Descriptor cosem_method_descriptor = null;

    public AxdrOptional<Data> method_invocation_parameters = new AxdrOptional<>(new Data(), false);

    public Action_Request_Normal() {
    }

    public Action_Request_Normal(byte[] code) {
        this.code = code;
    }

    public Action_Request_Normal(Invoke_Id_And_Priority invoke_id_and_priority,
            Cosem_Method_Descriptor cosem_method_descriptor, Data method_invocation_parameters) {
        this.invoke_id_and_priority = invoke_id_and_priority;
        this.cosem_method_descriptor = cosem_method_descriptor;
        this.method_invocation_parameters.setValue(method_invocation_parameters);
    }

    @Override
    public int encode(BerByteArrayOutputStream axdrOStream) throws IOException {

        int codeLength;

        if (code != null) {
            codeLength = code.length;
            for (int i = code.length - 1; i >= 0; i--) {
                axdrOStream.write(code[i]);
            }
        }
        else {
            codeLength = 0;
            codeLength += method_invocation_parameters.encode(axdrOStream);

            codeLength += cosem_method_descriptor.encode(axdrOStream);

            codeLength += invoke_id_and_priority.encode(axdrOStream);

        }

        return codeLength;

    }

    @Override
    public int decode(InputStream iStream) throws IOException {
        int codeLength = 0;

        invoke_id_and_priority = new Invoke_Id_And_Priority();
        codeLength += invoke_id_and_priority.decode(iStream);

        cosem_method_descriptor = new Cosem_Method_Descriptor();
        codeLength += cosem_method_descriptor.decode(iStream);

        method_invocation_parameters = new AxdrOptional<>(new Data(), false);
        codeLength += method_invocation_parameters.decode(iStream);

        return codeLength;
    }

    public void encodeAndSave(int encodingSizeGuess) throws IOException {
        BerByteArrayOutputStream axdrOStream = new BerByteArrayOutputStream(encodingSizeGuess);
        encode(axdrOStream);
        code = axdrOStream.getArray();
    }

    @Override
    public String toString() {
        return "sequence: {" + "invoke_id_and_priority: " + invoke_id_and_priority + ", cosem_method_descriptor: "
                + cosem_method_descriptor + ", method_invocation_parameters: " + method_invocation_parameters + "}";
    }

}
