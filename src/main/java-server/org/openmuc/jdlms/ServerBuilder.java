package org.openmuc.jdlms;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.openmuc.jdlms.datatypes.DataObject.newOctetStringData;
import static org.openmuc.jdlms.internal.WellKnownInstanceIds.LOGICAL_DEVICE_NAME_ID;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openmuc.jdlms.SecuritySuite.EncryptionMechanism;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.datatypes.DataObject.Type;
import org.openmuc.jdlms.internal.AttributeAccessor;
import org.openmuc.jdlms.internal.AttributeAccessor.LogicalNameFakeAccessor;
import org.openmuc.jdlms.internal.BaseNameRange;
import org.openmuc.jdlms.internal.BaseNameRangeSet;
import org.openmuc.jdlms.internal.DataDirectory;
import org.openmuc.jdlms.internal.DataDirectory.Attribute;
import org.openmuc.jdlms.internal.DataDirectory.CosemClassInstance;
import org.openmuc.jdlms.internal.DataDirectory.CosemLogicalDevice;
import org.openmuc.jdlms.internal.MethodAccessor;
import org.openmuc.jdlms.internal.settings.ServerSettings;
import org.openmuc.jdlms.internal.systemclasses.AssociationLnClass;
import org.openmuc.jdlms.internal.systemclasses.AssociationSnClass;
import org.openmuc.jdlms.internal.systemclasses.CosemDataDirectory;
import org.openmuc.jdlms.internal.systemclasses.ReadOnlyOctetStrData;
import org.openmuc.jdlms.internal.systemclasses.SapAssignment;
import org.openmuc.jdlms.internal.systemclasses.SecuritySetup;
import org.openmuc.jdlms.internal.transportlayer.ServerTransportLayer;
import org.openmuc.jdlms.settings.client.ReferencingMethod;

/**
 * Builder class to build a DLMS Server.
 * 
 * @param <T>
 *            the concrete class.
 */
public abstract class ServerBuilder<T extends ServerBuilder<T>> {
    private static final CosemAttribute LOGICAL_NAME_ATTRIBUTE = logicalNameAttribute();

    private static final int MANAGEMENT_LOGICAL_DEVICE_ID = 1;
    private final List<LogicalDevice> logicalDevices;
    private LogicalDevice managementLd;

    private int inactivityTimeout;
    private int responseTimeout;
    private int maxClients;

    private ServerConnectionListener connectionListener;

    private ReferencingMethod referencingMethod;

    public ServerBuilder() {
        this.logicalDevices = new LinkedList<>();

        this.inactivityTimeout = 0;
        this.responseTimeout = 0;
        this.maxClients = 0;
        this.connectionListener = null;

        this.referencingMethod = ReferencingMethod.LOGICAL;
    }

    /**
     * Builds a new server and starts it, with the provided settings.
     * 
     * @return the new Server.
     * 
     * @throws IOException
     *             if an I/O exception occurs while starting the server.
     */
    public abstract DlmsServer build() throws IOException;

    /**
     * Register one or more logical devices to the server.
     * 
     * @param logicalDevice
     *            a logical device.
     * @return the self reference of the connection builder.
     */
    public final T registerLogicalDevice(LogicalDevice... logicalDevice) {
        return registerLogicalDevice(Arrays.asList(logicalDevice));
    }

    /**
     * Register list of logical devices to the server.
     * 
     * @param newlogicalDevices
     *            a list of logical devices.
     * @return the self reference of the connection builder.
     */
    public T registerLogicalDevice(List<LogicalDevice> newlogicalDevices) {
        for (LogicalDevice logicalDevice : newlogicalDevices) {
            if (logicalDeviceIsManagementDevice(logicalDevice)) {
                this.managementLd = logicalDevice;
            }

            if (logicalDevice.getMasterKey() == null) {
                checkIfKeyIsRequired(logicalDevice);
            }

            this.logicalDevices.add(logicalDevice);
        }

        return self();
    }

    /**
     * Restrict the maximum number of connected clients.
     * 
     * <p>
     * A value of zero imposes no restriction.
     * </p>
     * 
     * @param maxClients
     *            positive integer, restricting the maximum number of connected clients.
     * @return the self reference of the connection builder.
     * @throws IllegalArgumentException
     *             if the <code>maxClients &lt; 0</code>.
     */
    public T setMaxClients(int maxClients) {
        if (maxClients < 0) {
            throw new IllegalArgumentException("max clients can't be negative");
        }

        this.maxClients = maxClients;
        return self();
    }

    /**
     * Sets the inactivity timeout. If a client doesn't send a request within that time, the server closes the
     * connection.
     * 
     * <p>
     * The timeout must be greater than 0. A timeout of values 0 implies that the option is disabled (i.e., timeout of
     * infinity).
     * </p>
     * 
     * <p>
     * The default value is 0.
     * </p>
     * 
     * @param inactivityTimeout
     *            the timeout value.
     * 
     * @return the self reference of the connection builder.
     * 
     * @throws IllegalArgumentException
     *             if the <code>inactivityTimeout &lt; 0</code>.
     */
    public T setInactivityTimeout(int inactivityTimeout) {
        if (inactivityTimeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        this.inactivityTimeout = inactivityTimeout;
        return self();
    }

    /**
     * Set the Referencing Method.
     * 
     * Default value is {@code LOGICAL}.
     * 
     * @param referencingMethod
     *            the referencing method.
     * 
     * @return the self reference of the connection builder.
     */
    public T setRefernceingMethod(ReferencingMethod referencingMethod) {
        this.referencingMethod = referencingMethod;
        return self();
    }

    /**
     * The max time a server waits for a response from the client, after sending a request to the client.
     * 
     * <p>
     * The timeout must be greater than 0. A timeout of values 0 implies that the option is disabled (i.e., timeout of
     * infinity).
     * </p>
     * 
     * <p>
     * The default value is 0.
     * </p>
     * 
     * @param responseTimeout
     *            the timeout value.
     * 
     * @return the self reference of the connection builder.
     * 
     * @throws IllegalArgumentException
     *             if the <code>responseTimeout &lt; 0</code>.
     */
    public T setResponseTimeout(int responseTimeout) {
        if (responseTimeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        this.responseTimeout = responseTimeout;
        return self();
    }

    public T setConnectionListener(ServerConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
        return self();
    }

    protected void setPropertiesTo(ServerSettings settings) {
        settings.inactivityTimeout = this.inactivityTimeout;
        settings.responseTimeout = this.responseTimeout;
        settings.maxClients = this.maxClients;
        settings.connectionListener = this.connectionListener;
        settings.referencingMethod = this.referencingMethod;
    }

    protected DataDirectory parseLogicalDevices() {
        DataDirectory attributeDirectory = new DataDirectory();

        registerSystemClassesToManagementLd();

        for (LogicalDevice logicalDevice : logicalDevices) {
            registerSystemClassesTo(logicalDevice);

            List<CosemInterfaceObject> dlmsClasses = logicalDevice.getCosemObjects();
            DataDirectory.CosemLogicalDevice logicalDeviceD = parseLogicalDlmsClasses(logicalDevice, dlmsClasses,
                    attributeDirectory);

            DataDirectory.CosemLogicalDevice res = attributeDirectory
                    .addLogicalDevice(logicalDevice.getLogicalDeviceId(), logicalDeviceD);

            if (res != null) {
                String message = MessageFormat.format(
                        "Logical Device with Logical Device ID = {0} was already registered.",
                        logicalDevice.getLogicalDeviceId());
                throw new IllegalPametrizationError(message);
            }
        }

        return attributeDirectory;
    }

    protected DlmsServer newServer(ServerTransportLayer serverTransportLayer) throws IOException {
        DlmsServer dlmsServer = new DlmsServer(serverTransportLayer);
        dlmsServer.start();
        return dlmsServer;
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    private CosemLogicalDevice parseLogicalDlmsClasses(LogicalDevice logicalDevice,
            List<CosemInterfaceObject> cosemObjects, DataDirectory dataDirectory) {

        BaseNameRangeSet baseNameRanges = new BaseNameRangeSet();
        CosemLogicalDevice logicalDeviceD = new CosemLogicalDevice(logicalDevice, baseNameRanges);

        for (CosemInterfaceObject instance : cosemObjects) {
            Class<? extends CosemInterfaceObject> klass = instance.getClass();

            if (referencingMethod == ReferencingMethod.SHORT && !CosemSnInterfaceObject.class.isAssignableFrom(klass)) {
                // TODO better msg
                // throw new IllegalPametrizationError(
                // "Object referencing is chosen to be SN, but class X implements only the LN interface.");
            }

            CosemClass cosemClass = klass.getAnnotation(CosemClass.class);
            checkClassAnnotation(klass, cosemClass);

            ObisCode instanceId = instance.getInstanceId();

            CosemClassInstance classInstance = new CosemClassInstance(cosemClass, instance);
            addClassInstanceToLD(logicalDevice.getLogicalDeviceId(), logicalDeviceD, klass, instanceId, classInstance);

            int numOfFields = findCosemFields(dataDirectory, instance, klass, instanceId, classInstance);

            findCosemMethods(klass, classInstance);

            if (referencingMethod == ReferencingMethod.SHORT && instance instanceof CosemSnInterfaceObject) {
                CosemSnInterfaceObject cosemSnInterfaceObject = (CosemSnInterfaceObject) instance;
                int baseName = cosemSnInterfaceObject.getObjectName();

                List<Entry<Byte, MethodAccessor>> methodPairs = classInstance.getMethodPairs();
                ListIterator<Entry<Byte, MethodAccessor>> iter = methodPairs.listIterator();

                Map<Integer, Integer> snMethodIdMap = new HashMap<>(methodPairs.size());

                final int fieldsOff = 0x08 * (numOfFields - 1);
                int lastSnIndex = baseName + fieldsOff;

                if (iter.hasNext()) {

                    Entry<Byte, MethodAccessor> firstMethod = iter.next();

                    final int firstMethodSn = lastSnIndex = baseName + fieldsOff
                            + 0x08 * cosemSnInterfaceObject.getFirstMethodOffset();

                    final int firstSnMethod = firstMethod.getKey().intValue();
                    snMethodIdMap.put(firstMethodSn, firstSnMethod);
                    while (iter.hasNext()) {
                        Entry<Byte, MethodAccessor> next = iter.next();

                        int methodId = next.getKey().intValue();
                        int methodsSn = lastSnIndex = firstMethodSn + (methodId - firstSnMethod) * 0x08;

                        snMethodIdMap.put(methodsSn, methodId);
                    }
                }

                BaseNameRange interval = new BaseNameRange(baseName, lastSnIndex, classInstance, snMethodIdMap,
                        numOfFields);
                if (baseNameRanges.add(interval) != null) {

                    // TODO better msg
                    throw new IllegalPametrizationError("sn error: base names don't fit..");
                }
            }
        }

        return logicalDeviceD;
    }

    private void registerSystemClassesToManagementLd() {
        if (this.managementLd == null) {
            // TODO: set correct name!
            this.managementLd = new LogicalDevice(1, "ISE-42", "ISE", 424242);
            this.logicalDevices.add(this.managementLd);
        }

        // TODO: register correct classes!
        // this.managementLd.registerCosemClass(new TcpUdpSetupClass(), new Ipv4Setup());
    }

    private void registerSystemClassesTo(LogicalDevice logicalDevice) {
        int ldId = logicalDevice.getLogicalDeviceId();
        DataObject logicalDeviceName = newOctetStringData(logicalDevice.getLogicalDeviceName().getBytes(US_ASCII));
        final int baseName = 0xFD00;
        ReadOnlyOctetStrData logicalDeviceNameData = new ReadOnlyOctetStrData(logicalDeviceName, LOGICAL_DEVICE_NAME_ID,
                baseName);

        CosemInterfaceObject associationObject;
        if (referencingMethod == ReferencingMethod.LOGICAL) {
            associationObject = new AssociationLnClass(ldId);
        }
        else {
            associationObject = new AssociationSnClass(ldId);
        }

        logicalDevice.registerCosemObject(associationObject, new SapAssignment(), logicalDeviceNameData,
                new SecuritySetup(logicalDevice));
    }

    private static String fieldLocationAsString(Class<? extends Object> klass, Field field) {
        return MessageFormat.format("Field {0} in class {1}", field.getName(), klass.getName());
    }

    private int findCosemFields(DataDirectory dataDirectory, Object object, Class<? extends Object> klass,
            ObisCode instanceId, CosemClassInstance classInstance) {
        Field[] declaredFields = klass.getDeclaredFields();

        int numOfFields = 1;
        for (Field field : declaredFields) {

            CosemDataDirectory dlmsDataDirectory = field.getAnnotation(CosemDataDirectory.class);

            if (dlmsDataDirectory != null) {
                setDataDirectroyToFiled(object, field, dataDirectory);
                continue;
            }

            CosemAttribute cosemAttribute = field.getAnnotation(CosemAttribute.class);
            if (cosemAttribute == null) {
                continue;
            }

            if (cosemAttribute.id() == 1) {
                String message = MessageFormat.format("{0} is not allowed to use attribute ID 1. Reserved for system.",
                        fieldLocationAsString(klass, field));

                throw new IllegalPametrizationError(message);
            }

            checkFieldReturnType(klass, field);

            AttributeAccessor accessor = buildAttributeAccessor(klass, field, cosemAttribute);

            Attribute entry = new Attribute(accessor, cosemAttribute);

            addAccessorToClass(klass, classInstance, cosemAttribute, entry);

            ++numOfFields;
        }

        LogicalNameFakeAccessor accessor = new AttributeAccessor.LogicalNameFakeAccessor(instanceId.bytes());
        Attribute entry = new Attribute(accessor, LOGICAL_NAME_ATTRIBUTE);
        addAccessorToClass(klass, classInstance, LOGICAL_NAME_ATTRIBUTE, entry);

        return numOfFields;
    }

    private static void findCosemMethods(Class<? extends Object> klass, CosemClassInstance classInstance) {
        for (Method method : klass.getDeclaredMethods()) {
            CosemMethod cosemMethod = method.getAnnotation(CosemMethod.class);

            if (cosemMethod == null) {
                continue;
            }

            boolean returnTypeIsVoid = method.getReturnType().equals(Void.TYPE);
            if (!(method.getReturnType().equals(DataObject.class) || returnTypeIsVoid)) {
                String message = MessageFormat.format("{0} must return a {1} or void.",
                        methodLocationToString(klass, method), DataObject.class.getSimpleName());
                throw new IllegalPametrizationError(message);
            }
            Type returnType = returnTypeIsVoid ? null : Type.DONT_CARE;

            verifyIfMethodIsPublic(klass, method);

            checkThrowsDeclarations(klass, method);

            Type parameterType = findAndVerifyParameters(cosemMethod, klass, method);

            MethodAccessor methodAccessor = new MethodAccessor(method, cosemMethod, parameterType, returnType);

            if (classInstance.putMethod(cosemMethod.id(), methodAccessor) != null) {
                String message = MessageFormat.format("Method ID = {0} is ambiguous in class {1}.", cosemMethod.id(),
                        klass.getName());
                throw new IllegalPametrizationError(message);
            }

        }
    }

    private static Type findAndVerifyParameters(CosemMethod dlmsMethod, Class<?> klass, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        int parameterLength = parameterTypes.length;

        Type parameterType;

        if (parameterLength == 0) {
            parameterType = null;
        }
        else if (parameterLength == 1) {
            parameterType = getParameterType1(dlmsMethod, klass, method, parameterTypes);
        }
        else if (parameterLength == 2) {
            parameterType = getParameterType2(dlmsMethod, klass, method, parameterTypes);
        }
        else {
            throw new IllegalPametrizationError(buildWrongParamExceptionMsg(klass, method));
        }

        return parameterType;
    }

    private static Type getParameterType2(CosemMethod dlmsMethod, Class<?> klass, Method method,
            Class<?>[] parameterTypes) throws IllegalPametrizationError {

        Type parameterType;

        Class<?> param1Class = parameterTypes[0];
        Class<?> param2Class = parameterTypes[1];

        if (DataObject.class.isAssignableFrom(param1Class) && Long.class.isAssignableFrom(param2Class)) {
            parameterType = dlmsMethod.consumes();
        }
        else {
            throw new IllegalPametrizationError(buildWrongParamExceptionMsg(klass, method));
        }
        return parameterType;
    }

    private static Type getParameterType1(CosemMethod dlmsMethod, Class<?> klass, Method method,
            Class<?>[] parameterTypes) throws IllegalPametrizationError {
        Type parameterType;

        Class<?> param1Class = parameterTypes[0];
        if (DataObject.class.isAssignableFrom(param1Class)) {
            parameterType = dlmsMethod.consumes();
        }
        else if (Long.class.isAssignableFrom(param1Class)) {
            parameterType = null;
        }
        else {
            throw new IllegalPametrizationError(buildWrongParamExceptionMsg(klass, method));
        }
        return parameterType;
    }

    private static String buildWrongParamExceptionMsg(Class<? extends Object> klass, Method method) {
        return MessageFormat.format(
                "{0} is only allowed to take one parameter of class {1} and a parameter of class {2}",
                methodLocationToString(klass, method), DataObject.class.getSimpleName(), Long.class.getSimpleName());
    }

    private static void checkThrowsDeclarations(Class<? extends Object> klass, Method method) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();

        if (exceptionTypes.length > 1 || exceptionTypes.length == 1
                && !IllegalMethodAccessException.class.isAssignableFrom(exceptionTypes[0])) {

            String message = MessageFormat.format("{0} can only throw a {1}.", methodLocationToString(klass, method),
                    IllegalMethodAccessException.class.getSimpleName());
            throw new IllegalPametrizationError(message);
        }
    }

    private static boolean logicalDeviceIsManagementDevice(LogicalDevice logicalDevice) {
        return logicalDevice.getLogicalDeviceId() == MANAGEMENT_LOGICAL_DEVICE_ID;
    }

    private static CosemAttribute logicalNameAttribute() {
        CosemAttribute dlmsAttribute = new CosemAttribute() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CosemAttribute.class;
            }

            @Override
            public Type type() {
                return Type.OCTET_STRING;
            }

            @Override
            public byte id() {
                return 1;
            }

            @Override
            public AttributeAccessMode accessMode() {
                return AttributeAccessMode.READ_ONLY;
            }

            @Override
            public int[] selector() {
                return new int[0];
            }
        };
        return dlmsAttribute;
    }

    private static void setDataDirectroyToFiled(Object object, Field field, DataDirectory attributeDirectory) {
        if (!DataDirectory.class.isAssignableFrom(field.getType())) {
            // TODO: system bug!!
            throw new RuntimeException("Bug in assignDataDirectroyToFiled");
        }

        try {
            field.setAccessible(true);
            field.set(object, attributeDirectory);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("error can't set field.. System error notify developers", e); // TODO
        }
    }

    private static void addAccessorToClass(Class<? extends Object> klass, CosemClassInstance classInstance,
            CosemAttribute dlmsAttribute, Attribute entry) {
        Attribute res = classInstance.putAttribute(dlmsAttribute.id(), entry);
        if (res != null) {
            String message = MessageFormat.format("Attribute ID = {0} is ambiguous in class {1}.", dlmsAttribute.id(),
                    klass.getName());
            throw new IllegalPametrizationError(message);
        }
    }

    private static void addClassInstanceToLD(int logicalDeviceId, DataDirectory.CosemLogicalDevice logicalDevice,
            Class<? extends CosemInterfaceObject> klass, ObisCode instanceId, CosemClassInstance classInstance) {
        CosemClassInstance res = logicalDevice.put(instanceId, classInstance);
        if (res != null) {
            String message = MessageFormat.format(
                    "Class {0} and {1} in logical device with ID = {2} have identical instance ID {3}.",
                    klass.getName(), res.getInstance().getClass().getName(), logicalDeviceId, instanceId.toString());
            throw new IllegalPametrizationError(message);
        }
    }

    private static AttributeAccessor buildAttributeAccessor(Class<?> klass, Field field,
            CosemAttribute cosemAttribute) {
        Method[] methods = klass.getDeclaredMethods();

        Set<Integer> accessSelectors = new HashSet<>();
        for (int accessSel : cosemAttribute.selector()) {
            accessSelectors.add(accessSel);
        }

        Method setMethod = null;
        Method getMethod = null;

        for (Method method : methods) {
            if (method.getAnnotation(CosemMethod.class) != null) {
                continue;
            }

            String methodName = method.getName().toLowerCase();
            if (!methodIsModifierForField(field.getName(), methodName, "get")
                    && !methodIsModifierForField(field.getName(), methodName, "set")) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            int signatureLength = parameterTypes.length;

            if (method.getReturnType().equals(DataObject.class)) {
                getMethod = extractGetMethod(klass, accessSelectors, method, parameterTypes, signatureLength);
            }
            else if (method.getReturnType().equals(Void.TYPE) && signatureLength >= 1
                    && parameterTypes[0].equals(DataObject.class)) {

                setMethod = extractSetMethod(klass, accessSelectors, method, parameterTypes, signatureLength);
            }
            else {
                // ignoring all other methods..
                continue;
            }
            if (accessSelectors.isEmpty()) {
                for (Class<?> clazz : parameterTypes) {
                    if (SelectiveAccessDescription.class.isAssignableFrom(clazz)) {
                        String message = MessageFormat.format("{0} is not allowed to use a selective.",
                                methodLocationToString(klass, method));
                        throw new IllegalPametrizationError(message);
                    }
                }
            }

            verifyIfMethodIsPublic(klass, method);

            verifyDeclaredExceptions(klass, method);

            if (getMethod != null && setMethod != null) {
                break;
            }
        }

        if (!accessSelectors.isEmpty()) {
            checkIfSelAccessParamExists(klass, field, cosemAttribute, accessSelectors, setMethod, getMethod);
        }
        field.setAccessible(true);

        if (getMethod != null && setMethod != null) {
            return new AttributeAccessor.MethodAttributeAccessor(getMethod, setMethod, cosemAttribute, accessSelectors);
        }
        else if (getMethod == null && setMethod == null) {
            return new AttributeAccessor.FieldAccessor(field, cosemAttribute);
        }
        else if (getMethod == null && setMethod != null) {
            return new AttributeAccessor.MethodSetFieldGetAccessor(field, setMethod, cosemAttribute, accessSelectors);
        }
        else {
            return new AttributeAccessor.FieldSetMethodGetAccessor(field, getMethod, cosemAttribute, accessSelectors);
        }

    }

    private static void checkIfSelAccessParamExists(Class<?> klass, Field field, CosemAttribute cosemAttribute,
            Set<Integer> accessSelectors, Method setMethod, Method getMethod) throws IllegalPametrizationError {
        boolean failture = false;

        switch (cosemAttribute.accessMode()) {
        case AUTHENTICATED_READ_AND_WRITE:
        case READ_AND_WRITE:
            failture = getMethod == null || setMethod == null;

            break;

        case AUTHENTICATED_READ_ONLY:
        case READ_ONLY:
            failture = getMethod == null;
            break;

        case AUTHENTICATED_WRITE_ONLY:
        case WRITE_ONLY:
            failture = setMethod == null;
            break;

        case NO_ACCESS:
        default:
            break;
        }

        if (failture) {

            String message = MessageFormat.format("{0} a set/get method for the selective access must be provided!",
                    fieldLocationAsString(klass, field));
            throw new IllegalPametrizationError(message);
        }

    }

    private static Method extractSetMethod(Class<?> klass, Set<Integer> accessSelectors, Method method,
            Class<?>[] parameterTypes, int signatureLength) throws IllegalPametrizationError {
        boolean accessSelectorMissing = signatureLength == 1
                || signatureLength == 2 && !SelectiveAccessDescription.class.isAssignableFrom(parameterTypes[1]);

        if (!accessSelectors.isEmpty() && accessSelectorMissing) {
            String message = MessageFormat.format("{0} must contain a selective access descriptor.",
                    methodLocationToString(klass, method));
            throw new IllegalPametrizationError(message);
        }

        return method;
    }

    private static Method extractGetMethod(Class<?> klass, Set<Integer> accessSelectors, Method method,
            Class<?>[] parameterTypes, int signatureLength) throws IllegalPametrizationError {
        Method getMethod;
        boolean other = signatureLength == 1 && !SelectiveAccessDescription.class.isAssignableFrom(parameterTypes[0]);
        if (!accessSelectors.isEmpty() && (signatureLength == 0 || other)) {
            String message = MessageFormat.format("{0} must contain a selective access descriptor.",
                    methodLocationToString(klass, method));
            throw new IllegalPametrizationError(message);
        }
        else if (signatureLength > 2) {
            String message = MessageFormat.format("{0} must not have more than two parameters.",
                    methodLocationToString(klass, method));
            throw new IllegalPametrizationError(message);
        }

        getMethod = method;
        return getMethod;
    }

    private static boolean methodIsModifierForField(String fieldName, String methodName, String prefix) {
        return methodName.toLowerCase().equals((prefix + fieldName).toLowerCase());
    }

    private static void verifyDeclaredExceptions(Class<?> klass, Method method) throws IllegalPametrizationError {
        Class<?>[] exceptionTypes = method.getExceptionTypes();

        if (exceptionTypes.length > 1) {
            String message = MessageFormat.format("{0} throws more than one exception.",
                    methodLocationToString(klass, method));
            throw new IllegalPametrizationError(message);
        }
        else if (exceptionTypes.length == 1
                && !IllegalAttributeAccessException.class.isAssignableFrom(exceptionTypes[0])) {
            String message = MessageFormat.format("{0} throws an exception, which is not a subtype of {1}.",
                    methodLocationToString(klass, method), IllegalAttributeAccessException.class.getSimpleName());
            throw new IllegalPametrizationError(message);
        }
    }

    private static String methodLocationToString(Class<?> klass, Method method) {
        return MessageFormat.format("Method {0} in class {1}", method.getName(), klass.getName());
    }

    private static void checkIfKeyIsRequired(LogicalDevice logicalDevice) throws IllegalPametrizationError {
        for (SecuritySuite sec : logicalDevice.getRestrictions().values()) {
            boolean hlsMechanism = sec.getAuthenticationMechanism().isHlsMechanism();
            boolean usesCiphering = sec.getEncryptionMechanism() != EncryptionMechanism.NONE;
            if (hlsMechanism || usesCiphering) {
                throw new IllegalPametrizationError(
                        String.format("Master key for LD with ID = %d is not set, but is required.",
                                logicalDevice.getLogicalDeviceId()));
            }
        }
    }

    private static void verifyIfMethodIsPublic(Class<? extends Object> klass, Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            Class<?>[] parameterTypes = method.getParameterTypes();

            StringBuilder parmas = new StringBuilder();
            parmas.append('(');
            for (Class<?> class1 : parameterTypes) {
                parmas.append(class1.getSimpleName());
            }
            parmas.append(')');

            String message = "Method " + method.getName() + " with signature " + parmas.toString() + " in class "
                    + klass.getName() + " must be public.";
            throw new IllegalPametrizationError(message);
        }
    }

    private static void checkClassAnnotation(Class<? extends Object> klass, CosemClass dlmsClass) {
        if (dlmsClass == null) {
            String message = MessageFormat.format("Class {0} was passes as Cosem class, but is not annotated as {1}.",
                    klass.getName(), CosemClass.class.getSimpleName());
            throw new IllegalPametrizationError(message);
        }
    }

    private static void checkFieldReturnType(Class<? extends Object> klass, Field field) {
        if (!field.getType().equals(DataObject.class)) {
            String erroMessage = MessageFormat.format(
                    "Field {0} in class {1} was annotated as {2} but does not have the type {3}.", field.getName(),
                    klass.getName(), CosemAttribute.class.getSimpleName(), DataObject.class.getSimpleName());
            throw new IllegalPametrizationError(erroMessage);
        }
    }

}
