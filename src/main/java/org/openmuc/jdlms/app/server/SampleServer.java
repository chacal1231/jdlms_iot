package org.openmuc.jdlms.app.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.openmuc.jdlms.AuthenticationMechanism;
import org.openmuc.jdlms.DlmsServer;
import org.openmuc.jdlms.LogicalDevice;
import org.openmuc.jdlms.SecuritySuite;

public class SampleServer {
    private final static int PORT = 6789;
    private final static String MANUFACTURE_ID = "ISE";
    private final static long DEVICE_ID = 9999L;
    private final static String LOGICAL_DEVICE_ID = "L_D_I";

    public static void main(String[] args) throws IOException {
        printServer("starting");
        LogicalDevice logicalDevice = new LogicalDevice(1, LOGICAL_DEVICE_ID, MANUFACTURE_ID, DEVICE_ID);
        SecuritySuite securitySuite = SecuritySuite.builder()
                .setAuthenticationMechanism(AuthenticationMechanism.LOW)
                .setPassword("Password".getBytes(StandardCharsets.US_ASCII))
                .build();

        logicalDevice.addRestriction(16, securitySuite);
        logicalDevice.registerCosemObject(new SampleClass());

        try (DlmsServer dlmsServer = DlmsServer.tcpServerBuilder(PORT)
                .registerLogicalDevice(logicalDevice)
                .setMaxClients(3)
                .build()) {
            printServer("started");

            printServer("Press any key to exit.");
            System.in.read();

            dlmsServer.close();
        } catch (IOException e) {
            throw new IOException("DemoServer: " + e);
        }

    }

    private static void printServer(String message) {
        System.out.println("DemoServer: " + message);
    }
}
