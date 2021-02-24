package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class SlaveInformativePosition {

    // relative X, Y, Z coordinate, each 2 Bytes
    private byte[] pos = new byte[6];
    // reserved 6 Bytes for future use
    private byte[] reserved = new byte[6];
    // association id: 0 - 255
    private byte[] assocIdByteArray = new byte[1];

    public SlaveInformativePosition(int x, int y, int z) {
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setAssocId(0); // if not set, set to 0
    }

    public SlaveInformativePosition(int x, int y, int z, int associationId) {
        this(x, y, z);
        this.setAssocId(associationId);
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

        if (this.pos != otherSlaveInfoPosition.pos) return false;
        return assocIdByteArray != null ? Arrays.equals(assocIdByteArray, otherSlaveInfoPosition.assocIdByteArray) : otherSlaveInfoPosition.assocIdByteArray == null;

    }

    @Override
    public int hashCode() {
        int result = this.getX();
        result = 31 * result + this.getY();
        result = 31 * result + this.getZ();
        result = 31 * result + (this.getAssocId() != null ? this.getAssocId().hashCode() : 0);
        return result;
    }

    public void copyFrom(@NotNull SlaveInformativePosition source) {
        this.pos = source.pos;
        this.reserved = source.reserved;
        this.assocIdByteArray = source.assocIdByteArray;
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + this.getX() +
                ", y=" + this.getY() +
                ", z=" + this.getZ() +
                ", association=" + this.getAssocId() +
                '}';
    }

    public boolean equalsInCoordinates(SlaveInformativePosition slaveInfoPos) {
        return slaveInfoPos != null
                && this.getX() == slaveInfoPos.getX()
                && this.getY() == slaveInfoPos.getY()
                && this.getZ() == slaveInfoPos.getZ();
    }

    private void setX(int x) {
        if (x < -32768 || x > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.pos[1] = (byte) x;
        this.pos[0] = (byte) (x >> 8);
    }

    private void setY(int y) {
        if (y < -32768 || y > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.pos[3] = (byte) y;
        this.pos[2] = (byte) (y >> 8);
    }

    private void setZ(int z) {
        if (z < -32768 || z > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.pos[5] = (byte) z;
        this.pos[4] = (byte) (z >> 8);
    }

    private void setAssocId(int assocId) {
        if (assocId < 0 || assocId > 255) {
            throw new IllegalArgumentException("association id range has to be limited from 0 to 255!");
        }
        this.assocIdByteArray[0] = (byte) assocId;
    }

    private int getX() {
        return signedIntFromTwoBytes(pos[0], pos[1]);
    }

    private int getY() {
        return signedIntFromTwoBytes(pos[2], pos[3]);
    }

    private int getZ() {
        return signedIntFromTwoBytes(pos[4], pos[5]);
    }

    private Integer getAssocId() {
        return unsignedIntFromByte(assocIdByteArray[0]);
    }

    private static int signedIntFromTwoBytes(byte highByte, byte lowByte) {
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

    private static int unsignedIntFromByte(byte b) {
        int ret = 0;
        for (int idx=0; idx<=7; idx++) {
            ret = (int) (ret + ((b >> idx) & 1) * Math.pow(2, idx));
        }
        return ret;
    }

    private static boolean firstBitZero(byte input) {
        return (input >> (7) & 1) == 0;
    }

}
