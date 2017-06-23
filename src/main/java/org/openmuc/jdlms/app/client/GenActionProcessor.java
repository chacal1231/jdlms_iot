/*
 * Copyright 2012-17 Fraunhofer ISE
 *
 * This file is part of jDLMS.
 * For more information visit http://www.openmuc.org
 *
 * jDLMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jDLMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jDLMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jdlms.app.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Vector;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.interfaceclass.attribute.SapAssignmentAttribute;
import org.openmuc.jdlms.internal.WellKnownInstanceIds;
import org.openmuc.jdlms.internal.cli.Action;
import org.openmuc.jdlms.internal.cli.ActionException;
import org.openmuc.jdlms.internal.cli.ActionListener;
import org.openmuc.jdlms.internal.cli.ActionProcessor;

abstract class GenActionProcessor implements ActionListener {

    private static final String DATA_INPUT_FORMAT = "<Data_Type>:<Data>";
    private static final String POSSIBLE_DATA_TYPES = "(b)oolean (bool) / (f)loat (float 32) / (d)ouble (float64) / (l)ong (in64) / (i)nteger (int8) / (s)hort (int16) / (o)ctet (hex)";
    private static final String READ_ACTION_KEY = "r";
    private static final String WRITE_ACTION_KEY = "w";
    private static final String SCAN_OBJECTS_ACTION_KEY = "s";
    private static final String SCAN_LDS_ACTION_KEY = "l";
    private static final String LEER_ENERGIA_AC_IM      = "3/1.1.1.8.0.255/0";
    private static final String LEER_ENERGIA_REC_IM     = "3/1.1.3.8.0.255/0";
    private static final String LEER_ENERGIA_AC_EX      = "3/1.1.2.8.0.255/0";
    private static final String LEER_ENERGIA_REC_EX     = "3/1.1.4.8.0.255/0";
    private static final String LEER_FP                 = "3/1.1.13.7.0.255/0";
    private static final String LEER_FREC               = "3/1.1.14.7.0.255/0";
    private static final String LEER_TENSION_L1         = "3/1.1.32.7.0.255/0";
    private static final String LEER_TENSION_L2         = "3/1.1.52.7.0.255/0";
    private static final String LEER_TENSION_L3         = "3/1.1.72.7.0.255/0";
    private static final String LEER_CORRIENTE_L1       = "3/1.1.31.7.0.255/0";
    private static final String LEER_CORRIENTE_L2       = "3/1.1.51.7.0.255/0";
    private static final String LEER_CORRIENTE_L3       = "3/1.1.71.7.0.255/0";
    private static final String LEER_FECHA              = "8/0.0.1.0.0.255/0";
    private static final String FILENAME = "../log.txt";
    private static final String FILENAME2 = "../sigfox.txt";
    private static final ArrayList<String> OBIS_Val = new ArrayList<String>();


    private ActionProcessor actionProcessor;

    public void start() {
        OBIS_Val.add(LEER_FECHA);
        OBIS_Val.add(LEER_ENERGIA_AC_IM);      
        OBIS_Val.add(LEER_ENERGIA_REC_IM );    
        OBIS_Val.add(LEER_ENERGIA_AC_EX );      
        OBIS_Val.add( LEER_ENERGIA_REC_EX );    
        OBIS_Val.add( LEER_FP );               
        OBIS_Val.add( LEER_FREC );               
        OBIS_Val.add( LEER_TENSION_L1 );         
        OBIS_Val.add( LEER_TENSION_L2 );        
        OBIS_Val.add( LEER_TENSION_L3 );        
        OBIS_Val.add( LEER_CORRIENTE_L1 );      
        OBIS_Val.add( LEER_CORRIENTE_L2 );       
        OBIS_Val.add( LEER_CORRIENTE_L3 );
        actionProcessor = new ActionProcessor(this);
        actionProcessor.start();
    }

    @Override
    public void actionCalled(String actionKey, int ValToRead) throws ActionException {
        System.out.println("El valor a leer es " + OBIS_Val.get(ValToRead));
        try {
            switch (actionKey) {
            case READ_ACTION_KEY:
                processRead(ValToRead);
                break;
            case WRITE_ACTION_KEY:
                processWrite();
                break;
            case SCAN_OBJECTS_ACTION_KEY:
                System.out.println("** Scan objects started...");
                processScanObjects();
                break;
            case SCAN_LDS_ACTION_KEY:
                System.out.println("** Scan LD started...");
                processScanLd();
                break;
            default:
                break;
            }
        } catch (Exception e) {
            throw new ActionException(e);
        }
    }

    @Override
    public void quit() {
        System.out.println("** Closing connection.");
        close();
        return;
    }

    public final void processRead(int ValToRead) throws IOException {
        String timeStamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
        String requestParameter = OBIS_Val.get(ValToRead);

        GetResult result;
        try {
            result = callGet(requestParameter);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.err.println("Failed to process read.");

            return;
        } catch (IllegalArgumentException e) {
            System.err.printf(e.getMessage());
            return;
        }
        AccessResultCode resultCode = result.getResultCode();

        if (resultCode == AccessResultCode.SUCCESS) {
            //System.out.println("Result Code: " + result.getResultCode());

            DataObject resultData = result.getResultData();
            System.out.println(resultData.toString());
            //Log to Sigfox
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILENAME2,true))) {
                String ToWrite = (timeStamp +" ====> :" + resultData.toStringSigfox() + "\n");
                bw.write(ToWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
            //Log to file
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILENAME,true))) {
                String ToWrite = (timeStamp +" ====> :" + resultData.toStringSigfox() + "\n");
                bw.write(ToWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
        }
        else {
            System.err.printf("Failed to read. Access result code: %s%n", resultCode);
        }
    }

    public abstract void processScanObjects() throws IOException;

    public final void processWrite() throws IOException {
        System.out.println("Enter: " + nameFormat());
        String address = actionProcessor.getReader().readLine();

        System.out.println("Enter: " + DATA_INPUT_FORMAT);
        System.out.println("possible data types: " + POSSIBLE_DATA_TYPES);

        String inputData;

        do {
            inputData = actionProcessor.getReader().readLine();
        } while (inputData == null);

        DataObject dataToWrite = buildDataObject(inputData);

        AccessResultCode resultCode = callSet(address, dataToWrite);
        if (resultCode == AccessResultCode.SUCCESS) {
            System.out.println("Result Code: " + resultCode);
        }
        else {
            System.err.printf("Failed to write. Access result code: %s%n", resultCode);
        }
    }

    public void close() {
        actionProcessor.close();
    }

    protected abstract String nameFormat();

    protected abstract GetResult callGet(String requestParameter) throws IOException, TimeoutException;

    protected abstract AccessResultCode callSet(String requestParameter, DataObject dataToWrite) throws IOException;

    private DataObject buildDataObject(String line) {

        String[] arguments = line.split(":");

        if (arguments.length != 2) {
            throw new IllegalArgumentException(String.format("Wrong number of arguments. %s", DATA_INPUT_FORMAT));
        }

        String dataTypeString = arguments[0];
        String dataString = arguments[1];

        char datatype = dataTypeString.toUpperCase().charAt(0);

        DataObject dataObject;

        switch (datatype) {
        case 'S':
            short sData = Short.parseShort(dataString);
            dataObject = DataObject.newInteger16Data(sData);
            break;
        case 'I':
            int iData = Integer.parseInt(dataString);
            dataObject = DataObject.newInteger32Data(iData);
            break;
        case 'L':
            Long lData = Long.parseLong(dataString);
            dataObject = DataObject.newInteger64Data(lData);
            break;
        case 'F':
            float fData = Float.parseFloat(dataString);
            dataObject = DataObject.newFloat32Data(fData);
            break;
        case 'D':
            double dData = Double.parseDouble(dataString);
            dataObject = DataObject.newFloat64Data(dData);
            break;
        case 'B':
            boolean bData = Boolean.parseBoolean(dataString);
            dataObject = DataObject.newBoolData(bData);
            break;
        case 'O':
            byte[] byteData = dataString.getBytes();
            dataObject = DataObject.newOctetStringData(byteData);
            break;
        default:
            throw new IllegalArgumentException(String.format("Wrong data type. %s", POSSIBLE_DATA_TYPES));
        }

        return dataObject;
    }

    public void processScanLd() throws IOException {
        SapAssignmentAttribute sapAssignmentList = SapAssignmentAttribute.SAP_ASSIGNMENT_LIST;
        String requestParameter = MessageFormat.format("{0}/{1}/{2}", sapAssignmentList.interfaceClass().id(),
                WellKnownInstanceIds.SAP_ASSIGNMENT_ID, sapAssignmentList.attributeId());
        GetResult getResult;
        try {
            getResult = callGet(requestParameter);
        } catch (TimeoutException e) {
            System.out.println("");

            return;
        }

        AccessResultCode resultCode = getResult.getResultCode();
        if (resultCode == AccessResultCode.SUCCESS) {
            List<DataObject> assList = getResult.getResultData().getValue();
            for (DataObject assListElement : assList) {
                List<DataObject> element = assListElement.getValue();
                DataObject logicalDeviceName = element.get(1);

                Charset charset;
                byte[] bytes = logicalDeviceName.getValue();
                switch (logicalDeviceName.getType()) {
                default:
                case VISIBLE_STRING:
                case OCTET_STRING:
                    charset = StandardCharsets.US_ASCII;
                    break;
                case UTF8_STRING:
                    charset = StandardCharsets.UTF_8;
                    break;
                }
                String ldName = new String(bytes, charset);
                Number ldId = element.get(0).getValue();
                System.out.printf("LD ID:   %d\n", ldId.intValue());
                System.out.printf("LD name: %s\n", ldName);
            }
        }
        else {
            System.out.println("Failed to get the SAP assignment list. " + resultCode);
        }
    }
}
