package org.openmuc.jdlms.internal.transportlayer;

import org.openmuc.jdlms.internal.association.Association;
import org.openmuc.jdlms.internal.sessionlayer.ServerSessionLayer;
import org.openmuc.jdlms.internal.settings.ServerSettings;

public interface Associationfactory {
    Association newAssociation(ServerSessionLayer sessionLayer, long connectionId, ServerSettings settings,
            ServerConnectionInformationImpl connInfo);
}
