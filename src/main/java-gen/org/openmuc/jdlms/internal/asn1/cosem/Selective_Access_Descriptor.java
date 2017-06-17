/**
 * This class file was automatically generated by the AXDR compiler that is part of jDLMS (http://www.openmuc.org)
 */

package org.openmuc.jdlms.internal.asn1.cosem;

import java.io.IOException;
import java.io.InputStream;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jdlms.internal.asn1.axdr.AxdrType;

public class Selective_Access_Descriptor implements AxdrType {

    public byte[] code = null;
    public Unsigned8 access_selector = null;

    public Data access_parameters = null;

    public Selective_Access_Descriptor() {
    }

    public Selective_Access_Descriptor(byte[] code) {
        this.code = code;
    }

    public Selective_Access_Descriptor(Unsigned8 access_selector, Data access_parameters) {
        this.access_selector = access_selector;
        this.access_parameters = access_parameters;
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
            codeLength += access_parameters.encode(axdrOStream);

            codeLength += access_selector.encode(axdrOStream);

        }

        return codeLength;

    }

    @Override
    public int decode(InputStream iStream) throws IOException {
        int codeLength = 0;

        access_selector = new Unsigned8();
        codeLength += access_selector.decode(iStream);

        access_parameters = new Data();
        codeLength += access_parameters.decode(iStream);

        return codeLength;
    }

    public void encodeAndSave(int encodingSizeGuess) throws IOException {
        BerByteArrayOutputStream axdrOStream = new BerByteArrayOutputStream(encodingSizeGuess);
        encode(axdrOStream);
        code = axdrOStream.getArray();
    }

    @Override
    public String toString() {
        return "sequence: {" + "access_selector: " + access_selector + ", access_parameters: " + access_parameters
                + "}";
    }

}
