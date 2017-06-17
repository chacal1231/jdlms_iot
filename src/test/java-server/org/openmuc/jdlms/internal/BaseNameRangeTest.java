// package org.openmuc.jdlms.internal;
//
// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertFalse;
// import static org.junit.Assert.assertNotNull;
// import static org.junit.Assert.assertTrue;
//
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.openmuc.jdlms.internal.BaseNameRange.Access;
// import org.openmuc.jdlms.internal.BaseNameRange.AccessType;
//
// import junitparams.JUnitParamsRunner;
// import junitparams.Parameters;
//
// @RunWith(JUnitParamsRunner.class)
// public class BaseNameRangeTest {
//
// @Test
// public void testRangeIntersect() throws Exception {
// int baseName = 0;
// int numAttributes = 4;
// int numMethods = 3;
// BaseNameRange range = new BaseNameRange(baseName, null, numAttributes, numMethods);
//
// for (int i = 0; i < numAttributes; i++) {
// assertTrue(range.intersects(baseName + i * 0x08));
// }
//
// // testing the methods
// assertTrue(range.intersects(baseName + 0x30));
// assertTrue(range.intersects(baseName + 0x38));
// assertTrue(range.intersects(baseName + 0x40));
//
// assertFalse(range.intersects(baseName - 1 * 0x08));
// }
//
// @Test
// @Parameters(method = "test2Params")
// public void test2(int baseName, int numAtr, int numMet, int expectedUpperBound) throws Exception {
// BaseNameRange range = new BaseNameRange(baseName, null, numAtr, numMet);
//
// assertEquals(baseName, range.getLowerBound()
// .intValue());
//
// assertEquals(expectedUpperBound, range.getUpperBound()
// .intValue());
// }
//
// public Object test2Params() {
// int base = 0x0B2C;
// Object[] p1 = { base, 2, 2, base + 0x30 };
// Object[] p2 = { base, 22, 12, base + 0x08 * 21 + (3 + 12) * 0x08 };
// Object[] p3 = { base, 19, 0, base + 0x08 * 18 };
// return new Object[][] { p1, p2, p3 };
// }
//
// @Test
// @Parameters(method = "testAccessForParams")
// public void testAccessFor(int baseName, int numAtr, int numMet, int varName, AccessType accessType, int memberId)
// throws Exception {
// BaseNameRange range = new BaseNameRange(baseName, null, numAtr, numMet);
//
// Access access = range.accessFor(varName);
//
// assertNotNull(access);
//
// assertEquals(accessType, access.getAccessType());
//
// assertEquals(memberId, access.getMemberId());
// }
//
// public Object testAccessForParams() {
// Object[] p1 = { 0xFA00, 5, 2, 0xFA00 + (6 + 3) * 0x08, AccessType.METHOD, 2 };
// Object[] p2 = { 0xFA00, 5, 2, 0xFA00 + (5 + 3) * 0x08, AccessType.METHOD, 1 };
//
// Object[] p3 = { 0xFA00, 5, 2, 0xFA00 + 0 * 0x08, AccessType.ATTRIBUTE, 1 };
// Object[] p4 = { 0xFA00, 5, 2, 0xFA00 + 4 * 0x08, AccessType.ATTRIBUTE, 5 };
// return new Object[][] { p1, p2, p3, p4 };
// }
//
// }
