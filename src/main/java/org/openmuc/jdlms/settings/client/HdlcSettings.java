package org.openmuc.jdlms.settings.client;

import org.openmuc.jdlms.internal.sessionlayer.hdlc.HdlcAddressPair;

public interface HdlcSettings extends Settings {

    HdlcAddressPair addressPair();

}
