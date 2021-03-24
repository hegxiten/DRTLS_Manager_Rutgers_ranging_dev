package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SlaveInformativePosition {
    public final int TOTAL_BYTES = 13;

    public final int X_L_BYTE_IDX = 2;
    public final int X_H_BYTE_IDX = 3;
    public final int Y_L_BYTE_IDX = 6;
    public final int Y_H_BYTE_IDX = 7;
    public final int Z_L_BYTE_IDX = 10;
    public final int Z_H_BYTE_IDX = 11;

    public final int LENGTH_L_BYTE_IDX = 8;
    public final int LENGTH_H_BYTE_IDX = 9;

    public final int ASSOC_ID_BYTE_IDX = 1;

    public final int QUAL_FACTOR_BYTE_IDX = 12;
    public final int[] RESERVED_BYTE_IDX = new int[]{0, 4, 5};
    // lowest byte (first byte) of each integer might be compromised by observation (Zezhou Wang, 03122021)
    // relative X, Y, Z coordinate, each 2 Bytes. Lower bytes of original 3 (4-byte) integers are volatile.
    // Using the original higher bytes for storing our X (CPLR), Y (CL), Z (TOR) values.
    // Current Position design:
    //  |00 |01 |02 |03 |04 |05 |06 |07 |08 |09 |10 |11 |12 | (13-bytes)
    //  |RV |RS |XL |XH |RV |RS |YL |YH |RV |RS |ZL |ZH |qf |
    //  R: reserved; V: volatile; S: stable (relatively); L: lower-byte; H: higher-byte; ID: association (vehicle) Id; qf: Quality Factor (cannot use)
    // Using byte 01 for storing vehicle association id. If needed, can expand to 2/3 byte id.
    // e.g. 0x00ABCDAB0000341200007856 -> X: 0xABCD; Y: 0x1234; Z: 0x5678; assocId: 0xAB
    // See getters and setters of X, Y, Z, assocId for details.
    // association id: 0 - 255 unsigned integer

    // storing association ID cannot use the byte of qualityfactor. The fw of DWM1001-Dev anchor/slave
    // doesn't report individual qualityfactor per each UWB ranging request from tag. Therefore the
    // tag/master side cannot recover association id if stored in the last byte of 13-element array.
    private byte[] slaveInfoBytes = new byte[TOTAL_BYTES];

    public SlaveInformativePosition(int x, int y, int z) {
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setAssocId(0); // if not set, set to 0
        this.setSlaveSideByValue(SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE);
        this.setVehicleLength(0);
        this.setReserved();
    }

    public SlaveInformativePosition(int x, int y, int z, int associationId) {
        this(x, y, z);
        this.setAssocId(associationId);
        this.setSlaveSideByValue(SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE);
        this.setVehicleLength(0);
        this.setReserved();
    }

    public SlaveInformativePosition(int x, int y, int z, int associationId, int slaveSideValue) {
        this(x, y, z);
        this.setAssocId(associationId);
        this.setSlaveSideByValue(slaveSideValue);
        this.setReserved();
    }

    public SlaveInformativePosition(int x, int y, int z, int associationId, int slaveSideValue, int length) {
        this(x, y, z);
        this.setAssocId(associationId);
        this.setSlaveSideByValue(slaveSideValue);
        this.setVehicleLength(length);
        this.setReserved();
    }

    public SlaveInformativePosition(byte[] byteArray) {
        this.slaveInfoBytes = Arrays.copyOfRange(byteArray, 0, TOTAL_BYTES);
        this.setX(signedIntFromTwoBytes(this.slaveInfoBytes[X_H_BYTE_IDX], this.slaveInfoBytes[X_L_BYTE_IDX]));
        this.setY(signedIntFromTwoBytes(this.slaveInfoBytes[Y_H_BYTE_IDX], this.slaveInfoBytes[Y_L_BYTE_IDX]));
        this.setZ(unSignedIntFromTwoBytes(this.slaveInfoBytes[Z_H_BYTE_IDX], this.slaveInfoBytes[Z_L_BYTE_IDX]));
        this.setAssocId(unsignedIntFromByte(this.slaveInfoBytes[ASSOC_ID_BYTE_IDX]));
        this.setVehicleLength(unSignedIntFromTwoBytes(this.slaveInfoBytes[LENGTH_H_BYTE_IDX], this.slaveInfoBytes[LENGTH_L_BYTE_IDX]));
    }

    public SlaveInformativePosition() {
    }

    @SuppressWarnings("IncompleteCopyConstructor")
    public SlaveInformativePosition(@NotNull SlaveInformativePosition slaveInfoPosition) {
        copyFrom(slaveInfoPosition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlaveInformativePosition otherSlaveInfoPosition = (SlaveInformativePosition) o;
        if (this.getX() != otherSlaveInfoPosition.getX()) return false;
        if (this.getY() != otherSlaveInfoPosition.getY()) return false;
        if (this.getZ() != otherSlaveInfoPosition.getZ()) return false;
        if (this.getSlaveSide() != otherSlaveInfoPosition.getSlaveSide()) return false;
        if (this.getVehicleLength() != otherSlaveInfoPosition.getVehicleLength()) return false;
        return this.getAssocId() != null ? (this.getAssocId() == otherSlaveInfoPosition.getAssocId()) : (otherSlaveInfoPosition.getAssocId() == null);

    }

    @Override
    public int hashCode() {
        int result = this.getX();
        result = 31 * result + this.getY();
        result = 31 * result + this.getZ();
        result = 31 * result + this.getVehicleLength();
        result = 31 * result + (this.getAssocId() != null ? this.getAssocId().hashCode() : 0);
        result = 31 * result + (this.getSlaveSideValue() != null ? this.getSlaveSideValue().hashCode() : 0);
        return result;
    }

    public void copyFrom(@NotNull SlaveInformativePosition source) {
        this.slaveInfoBytes = source.slaveInfoBytes;
    }

    public boolean equalsInCoordinates(SlaveInformativePosition slaveInfoPos) {
        return slaveInfoPos != null
                && this.getX() == slaveInfoPos.getX()
                && this.getY() == slaveInfoPos.getY()
                && this.getZ() == slaveInfoPos.getZ();
    }

    public void setX(int x) {
        if (x < -32768 || x > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.slaveInfoBytes[X_L_BYTE_IDX] = (byte) x;
        this.slaveInfoBytes[X_H_BYTE_IDX] = (byte) (x >>> 8);
    }

    public void setY(int y) {
        if (y < -32768 || y > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.slaveInfoBytes[Y_L_BYTE_IDX] = (byte) y;
        this.slaveInfoBytes[Y_H_BYTE_IDX] = (byte) (y >>> 8);
    }

    public void setZ(int z) {
        if (z < 0 || z > 65535) {
            throw new IllegalArgumentException("input position value out of range! (min 0 max 65535)");
        }
        this.slaveInfoBytes[Z_L_BYTE_IDX] = (byte) z;
        this.slaveInfoBytes[Z_H_BYTE_IDX] = (byte) (z >>> 8);
    }


    public void setVehicleLength(int length) {
        if (length < 0 || length > 65535) {
            throw new IllegalArgumentException("input length value out of range! (min 0 max 65535)");
        }
        this.slaveInfoBytes[LENGTH_L_BYTE_IDX] = (byte) length;
        this.slaveInfoBytes[LENGTH_H_BYTE_IDX] = (byte) (length >>> 8);
    }

    public void setAssocId(int assocId) {
        if (assocId < 0 || assocId > 255) {
            throw new IllegalArgumentException("association id range has to be limited from 0 to 255!");
        }
        this.slaveInfoBytes[ASSOC_ID_BYTE_IDX] = (byte) assocId;    // relatively stable byte from original X
    }

    public void setSlaveSideByValue(int sideInt) {
        int slaveInfoPosZ = this.getZ();
        switch (sideInt) {
            case SlaveMasterSide.Constants.A_SIDE_VALUE:
                this.setZ(closestIntEncodeByRemainderAndDivisor(
                        slaveInfoPosZ,
                        SlaveMasterSide.Constants.A_SIDE_VALUE,
                        SlaveMasterSide.Constants.TOTAL_SIDE_CASES,
                        true
                ));
                break;
            case SlaveMasterSide.Constants.B_SIDE_VALUE:
                this.setZ(closestIntEncodeByRemainderAndDivisor(
                        slaveInfoPosZ,
                        SlaveMasterSide.Constants.B_SIDE_VALUE,
                        SlaveMasterSide.Constants.TOTAL_SIDE_CASES,
                        true
                ));
                break;
            case SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE:
                this.setZ(closestIntEncodeByRemainderAndDivisor(
                        slaveInfoPosZ,
                        SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE,
                        SlaveMasterSide.Constants.TOTAL_SIDE_CASES,
                        true));
                break;
        }
    }

    public void setReserved() {
        for (int idx : RESERVED_BYTE_IDX) {
            this.slaveInfoBytes[idx] = (byte) 0x00;
        }
    }

    public int getX() {
        return signedIntFromTwoBytes(slaveInfoBytes[X_H_BYTE_IDX], slaveInfoBytes[X_L_BYTE_IDX]);
    }

    public int getY() {
        return signedIntFromTwoBytes(slaveInfoBytes[Y_H_BYTE_IDX], slaveInfoBytes[Y_L_BYTE_IDX]);
    }

    public int getZ() {
        return unSignedIntFromTwoBytes(slaveInfoBytes[Z_H_BYTE_IDX], slaveInfoBytes[Z_L_BYTE_IDX]);
    }

    public int getVehicleLength() {
        return unSignedIntFromTwoBytes(slaveInfoBytes[LENGTH_H_BYTE_IDX], slaveInfoBytes[LENGTH_L_BYTE_IDX]);
    }

    public Integer getAssocId() {
        return unsignedIntFromByte(slaveInfoBytes[ASSOC_ID_BYTE_IDX]);
    }

    public Integer getSlaveSideValue() {
        return Math.floorMod(this.getZ(), SlaveMasterSide.Constants.TOTAL_SIDE_CASES); // Modulo of 3
    }

    public SlaveMasterSide getSlaveSide() {
        int slaveSideValue = this.getSlaveSideValue();
        switch (slaveSideValue) {
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
        return "SlaveInfoPosition{" +
                "x=" + this.getX() +
                ", y=" + this.getY() +
                ", z=" + this.getZ() +
                ", association=" + this.getAssocId() +
                ", vehicle length=" + this.getVehicleLength() +
                ", side=" + this.getSlaveSide() +
                '}';
    }

    public byte[] getEncodedByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(this.slaveInfoBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[TOTAL_BYTES];
        }
        return outputStream.toByteArray();
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

    public static int closestIntEncodeByRemainderAndDivisor(int origInt, int remainder, int divisor, boolean isUnsigned) {
        int[] options = new int[]{
                closestMultipleByDivisor(origInt - divisor, divisor) + remainder,
                closestMultipleByDivisor(origInt, divisor) + remainder,
                closestMultipleByDivisor(origInt + divisor, divisor) + remainder
        };
        int idx = 0;
        int distance = Math.abs(options[0] - origInt);
        for (int c = 1; c < options.length; c++) {
            int cDistance = Math.abs(options[c] - origInt);
            if (cDistance < distance) {
                idx = c;
                distance = cDistance;
            }
        }
        int ret = options[idx];
        if (!isUnsigned) {
            return ret;
        }
        else if (ret >= 0 || ret <= 65535) {
            return ret;
        }
        // handle the corner case: if the return is negative but an unsigned value is needed:
        else if (ret < 0) {
            return ret + divisor; // preserve the same remainder value after modulo operation.
        }
        else return ret - divisor; // when 65535
    }

    private static int closestMultipleByDivisor (int origInt, int divisor) {
        origInt = origInt + divisor/2;
        origInt = origInt - Math.floorMod(origInt, divisor);
        return origInt;
    }

}
