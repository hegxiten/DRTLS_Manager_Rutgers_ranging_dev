/*
 * Copyright (c) 2021, Rutgers University, Zezhou Wang. All rights reserved.
 */

package com.decawave.argo.api.struct;

/**
 * Slave/Master side of the vehicle.
 */

public enum SlaveMasterSide {
    A(Constants.A_SIDE_VALUE),
    B(Constants.B_SIDE_VALUE),
    UNKNOWN(Constants.UNKNOWN_SIDE_VALUE);

    public static class Constants {
        public static final int A_SIDE_VALUE = 2;
        public static final int B_SIDE_VALUE = 1;
        public static final int UNKNOWN_SIDE_VALUE = 0;
        public static final int TOTAL_SIDE_CASES = 3;
    }

    private final int value;

    private SlaveMasterSide(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
