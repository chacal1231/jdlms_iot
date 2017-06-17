/**
 * This class file was automatically generated by the AXDR compiler that is part of jDLMS (http://www.openmuc.org)
 */

package org.openmuc.jdlms.internal.asn1.cosem;

import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrInteger;

public class Integer64 extends AxdrInteger {

    public Integer64() {
        super(-9223372036854775808L, 9223372036854775807L, -9223372036854775808L);
    }

    public Integer64(byte[] code) {
        super(-9223372036854775808L, 9223372036854775807L, -9223372036854775808L);
        this.code = code;
    }

    public Integer64(long val) {
        super(-9223372036854775808L, 9223372036854775807L, val);
    }

}
