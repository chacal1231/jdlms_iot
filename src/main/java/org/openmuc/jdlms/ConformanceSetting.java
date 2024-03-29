package org.openmuc.jdlms;

public enum ConformanceSetting {
    // SN
    READ(3),
    WRITE(4),
    UNCONFIRMED_WRITE(5),

    ATTRIBUTE0_SUPPORTED_WITH_SET(8),
    PRIORITY_MGMT_SUPPORTED(9),
    ATTRIBUTE0_SUPPORTED_WITH_GET(10),
    BLOCK_TRANSFER_WITH_GET_OR_READ(11),
    BLOCK_TRANSFER_WITH_SET_OR_WRITE(12),
    BLOCK_TRANSFER_WITH_ACTION(13),
    MULTIPLE_REFERENCES(14),

    INFORMTION_REPORT(15),
    /**
     * Selective Access for SN connections.
     */
    PARAMETERIZED_ACCESS(18),

    GET(19),
    SET(20),
    SELECTIVE_ACCESS(21),
    EVENT_NOTIFICATION(22),
    ACTION(23),;

    private int index;

    private ConformanceSetting(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

}
