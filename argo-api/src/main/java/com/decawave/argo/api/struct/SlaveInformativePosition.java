package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class SlaveInformativePosition {

    // relative X, Y, Z coordinate, each 2 Bytes
    public byte[] pos = new byte[6];
    // reserved 6 Bytes for future use
    public byte[] reserved = new byte[6];
    // association id: 0 - 255
    public byte[] assocIdByteArray = new byte[1];

    public SlaveInformativePosition(int x, int y, int z) {
        if ((x < -32768) || (x > 32767) || (y < -32768) || (y > 32767) || (z < -32768) || (z > 32767)) {
            throw new IllegalArgumentException("relative position range has to be limited from -32,768 to 32,767 cm!");
        }
        this.pos[1] = (byte) x;
        this.pos[0] = (byte) (x >> 8);
        this.pos[3] = (byte) y;
        this.pos[2] = (byte) (y >> 8);
        this.pos[5] = (byte) z;
        this.pos[4] = (byte) (z >> 8);
        this.assocIdByteArray[0] = (byte) 0x00; // if not set, set to 0
    }

    public SlaveInformativePosition(int x, int y, int z, int associationId) {
        this(x, y, z);
        if ((associationId < 0) || (associationId > 255)) {
            throw new IllegalArgumentException("association id range has to be limited from 0 to 255!");
        }
        this.assocIdByteArray[0] = (byte) associationId;
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
        result = 31 * result + (this.getAssociation() != null ? this.getAssociation().hashCode() : 0);
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
                ", association=" + this.getAssociation() +
                '}';
    }

    public boolean equalsInCoordinates(SlaveInformativePosition slaveInfoPos) {
        return slaveInfoPos != null
                && this.getX() == slaveInfoPos.getX()
                && this.getY() == slaveInfoPos.getY()
                && this.getZ() == slaveInfoPos.getZ();
    }

    private Integer getAssociation() {
        Byte associationIdByte = this.assocIdByteArray[0];
        return Integer.valueOf(associationIdByte.intValue());
    }

    private int getX() {
        byte[] byteArraySliceX = Arrays.copyOfRange(this.pos, 0, 2);
        return 0;
    }

    private int getY() {
        return 0;
    }

    private int getZ() {
        return 0;
    }
    //TODO: The following part has not yet been developed
    public static byte[] intToTwoBytes(int x) {
        if (x < 0 || x > 65535) {
            throw new IllegalArgumentException("input position value out of range! (min 0 max 65535)");
        }

        byte[] bytes = new byte[2];
        bytes[0] = (byte) x;
        bytes[1] = (byte)(x >>> 8);
        return bytes;
    }

    public static int TwoBytesToInt(byte[] bytes) {
        if (bytes.length != 2) {
            throw new IllegalArgumentException("invalid input byte array! length is fixed to 2");
        }

        int ret;
        byte[] bytes = new byte[2];


        return bytes;
    }

}
