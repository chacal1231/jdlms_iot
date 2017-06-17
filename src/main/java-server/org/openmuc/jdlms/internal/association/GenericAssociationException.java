package org.openmuc.jdlms.internal.association;

import java.io.IOException;

import org.openmuc.jdlms.internal.APdu;

public abstract class GenericAssociationException extends IOException {

    public abstract APdu getErrorMessageApdu();

}
