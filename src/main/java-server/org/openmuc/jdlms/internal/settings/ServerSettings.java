package org.openmuc.jdlms.internal.settings;

import org.openmuc.jdlms.ServerConnectionListener;
import org.openmuc.jdlms.settings.client.ReferencingMethod;

public abstract class ServerSettings {

    public int inactivityTimeout;

    public int responseTimeout;

    public int maxClients;

    public ServerConnectionListener connectionListener;

    public ReferencingMethod referencingMethod;

    public ServerSettings() {
    }
}
