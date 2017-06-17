package org.openmuc.jdlms.internal.sessionlayer;

import org.openmuc.jdlms.internal.settings.ServerSettings;
import org.openmuc.jdlms.transportlayer.client.StreamAccessor;

public class ServerSessionLayerFactories {

    public static ServerSessionLayerFactory newHdlcSessionLayerFactory() {
        return new ServerSessionLayerFactory() {

            @Override
            public ServerSessionLayer newSesssionLayer(StreamAccessor streamAccessor, ServerSettings settings) {
                return new ServerHdlcSessionLayer(streamAccessor, settings);
            }
        };
    }

    public static ServerSessionLayerFactory newWrapperSessionLayerFactory() {
        return new ServerSessionLayerFactory() {

            @Override
            public ServerSessionLayer newSesssionLayer(StreamAccessor streamAccessor, ServerSettings settings) {
                return new ServerWrapperLayer(streamAccessor, settings);
            }
        };
    }

    private ServerSessionLayerFactories() {
    }
}
