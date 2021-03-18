package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MasterInformativePosition {
    public final int TOTAL_BYTES = 12;

    public final int X_L_BYTE_IDX = 0;
    public final int X_H_BYTE_IDX = 1;
    public final int Y_L_BYTE_IDX = 2;
    public final int Y_H_BYTE_IDX = 3;
    public final int Z_L_BYTE_IDX = 4;
    public final int Z_H_BYTE_IDX = 5;

    public final int ASSOC_ID_BYTE_IDX = 6;
    public final int SIDE_BYTE_IDX = 7;
    public final int[] RESERVED_BYTE_IDX = new int[]{8, 9 ,10, 11};
    //  |00 |01 |02 |03 |04 |05 |06 |07 |08 |09 |10 |11 | (12-bytes)
    //  |XL |XH |YL |YH |ZL |ZH |ID |MS |RS |RS |RS |RS |
    //  RS: stable reserved byte field; L: lower-byte; H: higher-byte; ID: association (vehicle) Id; MS: Master End Side
    //  MAX_LABEL_BYTE_LENGTH (in NodeDetailFragment.java) is set is 16, meaning that by Base64 encoding:
    //  12 bytes at max can be encoded into label string - 12 Bytes / 3 * 4 = 16 Base64 characters
    private byte[] masterInfoBytes = new byte[TOTAL_BYTES];

    public MasterInformativePosition(int x, int y, int z) {
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setAssocId(0); // if not set, set to 0
        this.setMasterSideByValue(SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE);
        this.setReserved();
    }

    public MasterInformativePosition(int x, int y, int z, int associationId) {
        this(x, y, z);
        this.setAssocId(associationId);
        this.setMasterSideByValue(SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE);
        this.setReserved();
    }

    public MasterInformativePosition(int x, int y, int z, int associationId, int masterSideValue) {
        this(x, y, z);
        this.setAssocId(associationId);
        this.setMasterSideByValue(masterSideValue);
        this.setReserved();
    }

    public MasterInformativePosition(byte[] byteArray) {
        this.masterInfoBytes = Arrays.copyOfRange(byteArray, 0, TOTAL_BYTES);
        this.setX(signedIntFromTwoBytes(this.masterInfoBytes[X_H_BYTE_IDX], this.masterInfoBytes[X_L_BYTE_IDX]));
        this.setY(signedIntFromTwoBytes(this.masterInfoBytes[Y_H_BYTE_IDX], this.masterInfoBytes[Y_L_BYTE_IDX]));
        this.setZ(unSignedIntFromTwoBytes(this.masterInfoBytes[Z_H_BYTE_IDX], this.masterInfoBytes[Z_L_BYTE_IDX]));
        this.setAssocId(unsignedIntFromByte(this.masterInfoBytes[ASSOC_ID_BYTE_IDX]));
        this.setMasterSideByValue(unsignedIntFromByte(this.masterInfoBytes[SIDE_BYTE_IDX]));
    }

    public MasterInformativePosition() {
    }

    @SuppressWarnings("IncompleteCopyConstructor")
    public MasterInformativePosition(@NotNull MasterInformativePosition masterInfoPosition) {
        copyFrom(masterInfoPosition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MasterInformativePosition otherMasterInfoPosition = (MasterInformativePosition) o;
        if (this.getX() != otherMasterInfoPosition.getX()) return false;
        if (this.getY() != otherMasterInfoPosition.getY()) return false;
        if (this.getZ() != otherMasterInfoPosition.getZ()) return false;
        if (this.getMasterSide() != otherMasterInfoPosition.getMasterSide()) return false;
        return this.getAssocId() != null ? (this.getAssocId() == otherMasterInfoPosition.getAssocId()) : (otherMasterInfoPosition.getAssocId() == null);

    }

    @Override
    public int hashCode() {
        int result = this.getX();
        result = 31 * result + this.getY();
        result = 31 * result + this.getZ();
        result = 31 * result + (this.getAssocId() != null ? this.getAssocId().hashCode() : 0);
        result = 31 * result + (this.getMasterSideValue() != null ? this.getMasterSideValue().hashCode() : 0);
        return result;
    }

    public void copyFrom(@NotNull MasterInformativePosition source) {
        this.masterInfoBytes = source.masterInfoBytes;
    }

    public boolean equalsInCoordinates(MasterInformativePosition masterInfoPos) {
        return masterInfoPos != null
                && this.getX() == masterInfoPos.getX()
                && this.getY() == masterInfoPos.getY()
                && this.getZ() == masterInfoPos.getZ();
    }

    public void setX(int x) {
        if (x < -32768 || x > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.masterInfoBytes[X_L_BYTE_IDX] = (byte) x;
        this.masterInfoBytes[X_H_BYTE_IDX] = (byte) (x >>> 8);
    }

    public void setY(int y) {
        if (y < -32768 || y > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.masterInfoBytes[Y_L_BYTE_IDX] = (byte) y;
        this.masterInfoBytes[Y_H_BYTE_IDX] = (byte) (y >>> 8);
    }

    public void setZ(int z) {
        if (z < 0 || z > 65535) {
            throw new IllegalArgumentException("input position value out of range! (min 0 max 65535)");
        }
        this.masterInfoBytes[Z_L_BYTE_IDX] = (byte) z;
        this.masterInfoBytes[Z_H_BYTE_IDX] = (byte) (z >>> 8);
    }

    public void setAssocId(int assocId) {
        if (assocId < 0 || assocId > 255) {
            throw new IllegalArgumentException("association id range has to be limited from 0 to 255!");
        }
        this.masterInfoBytes[ASSOC_ID_BYTE_IDX] = (byte) assocId;
    }

    public void setMasterSideByValue(int sideInt) {
        if (sideInt < 0 || sideInt > 2) {
            throw new IllegalArgumentException("side value range has to be limited from 0 to 2!");
        }
        this.masterInfoBytes[SIDE_BYTE_IDX] = (byte) sideInt;
    }

    public void setReserved() {
        for (int idx : RESERVED_BYTE_IDX) {
            this.masterInfoBytes[idx] = (byte) 0x00;
        }
    }

    public int getX() {
        return signedIntFromTwoBytes(masterInfoBytes[X_H_BYTE_IDX], masterInfoBytes[X_L_BYTE_IDX]);
    }

    public int getY() {
        return signedIntFromTwoBytes(masterInfoBytes[Y_H_BYTE_IDX], masterInfoBytes[Y_L_BYTE_IDX]);
    }

    public int getZ() {
        return unSignedIntFromTwoBytes(masterInfoBytes[Z_H_BYTE_IDX], masterInfoBytes[Z_L_BYTE_IDX]);
    }

    public Integer getAssocId() {
        return unsignedIntFromByte(masterInfoBytes[ASSOC_ID_BYTE_IDX]);
    }

    public Integer getMasterSideValue() {
        return unsignedIntFromByte(masterInfoBytes[SIDE_BYTE_IDX]);
    }

    public SlaveMasterSide getMasterSide() {
        int masterSideValue = this.getMasterSideValue();
        switch (masterSideValue) {
            case SlaveMasterSide.Constants.A_SIDE_VALUE:
                return SlaveMasterSide.A;
            case SlaveMasterSide.Constants.B_SIDE_VALUE:
                return SlaveMasterSide.B;
            case SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE: default:
                return SlaveMasterSide.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return "MasterInfoPosition{" +
                "x=" + this.getX() +
                ", y=" + this.getY() +
                ", z=" + this.getZ() +
                ", association=" + this.getAssocId() +
                ", side=" + this.getMasterSide() +
                '}';
    }

    public byte[] getEncodedByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(this.masterInfoBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[TOTAL_BYTES];
        }
        return outputStream.toByteArray();
    }

    public byte[] getMasterInfoBytes() {
        return this.masterInfoBytes;
    }

    public static int signedIntFromTwoBytes(byte highByte, byte lowByte) {
        boolean isNegative;
        if (firstBitZero(highByte)) {
            isNegative = false;
        }
        else {
            isNegative = true;
        }
        if (isNegative) {
            int ret = ((highByte & 0xFF) << 8 | (lowByte & 0xFF)) | 0xFFFF0000; // sign ext.
            return ret;
        }
        else {
            int ret = ((highByte & 0xFF) << 8 | (lowByte & 0xFF)) | 0x00000000; // no sign ext.
            return ret;
        }
    }

    public static int unSignedIntFromTwoBytes(byte highByte, byte lowByte) {
        int ret = 0;
        for (int idx=0; idx<=7; idx++) {
            ret = (int) (ret + ((lowByte >> idx) & 1) * Math.pow(2, idx));
        }
        for (int idx=0; idx<=7; idx++) {
            ret = (int) (ret + ((highByte >> idx) & 1) * Math.pow(2, idx + 8));
        }
        return ret;
    }

    public static int unsignedIntFromByte(byte b) {
        int ret = 0;
        for (int idx=0; idx<=7; idx++) {
            ret = (int) (ret + ((b >> idx) & 1) * Math.pow(2, idx));
        }
        return ret;
    }

    public static boolean firstBitZero(byte input) {
        return (input >> (7) & 1) == 0;
    }
}
