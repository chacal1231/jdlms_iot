package org.openmuc.jdlms.internal.sessionlayer;

import org.openmuc.jdlms.internal.settings.ServerSettings;
import org.openmuc.jdlms.transportlayer.client.StreamAccessor;

public interface ServerSessionLayerFactory {

    ServerSessionLayer newSesssionLayer(StreamAccessor streamAccessor, ServerSettings settings);

}
