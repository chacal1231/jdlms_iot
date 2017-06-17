package org.openmuc.jdlms.internal.association;

import org.openmuc.jdlms.internal.DataDirectory;
import org.openmuc.jdlms.internal.ServerConnectionData;

public class RequestProcessorData {
    public final int logicalDeviceId;
    public final DataDirectory directory;
    public final ServerConnectionData connectionData;

    public RequestProcessorData(int logicalDeviceId, DataDirectory dataDirectory, ServerConnectionData connectionData) {
        this.logicalDeviceId = logicalDeviceId;
        this.directory = dataDirectory;
        this.connectionData = connectionData;
    }
}
