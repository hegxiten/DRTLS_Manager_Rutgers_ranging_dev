package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class SlaveInformativePosition {

    // relative X, Y, Z coordinate, each 2 Bytes
    public byte[] pos = new byte[6];
    // reserved 6 Bytes for future use
    public byte[] reserved = new byte[6];
    // association id: 0 - 255
    public byte[] associationId = new byte[1];

    public SlaveInformativePosition(int x, int y, int z) {
        this.pos[0] = (byte) x;
        this.pos[1] = (byte) (x >>> 8);
        this.pos[2] = (byte) y;
        this.pos[3] = (byte) (y >>> 8);
        this.pos[4] = (byte) z;
        this.pos[5] = (byte) (z >>> 8);
    }

    public SlaveInformativePosition(int x, int y, int z, byte associationId) {
        this(x, y, z);
        this.associationId[0] = associationId;
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
        return associationId != null ? Arrays.equals(associationId, otherSlaveInfoPosition.associationId) : otherSlaveInfoPosition.associationId == null;

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        result = 31 * result + (qualityFactor != null ? qualityFactor.hashCode() : 0);
        return result;
    }

    public void copyFrom(@NotNull SlaveInformativePosition source) {
        this.pos = source.pos;
        this.reserved = source.reserved;
        this.associationId = source.associationId;
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
    //TODO: The following part has not yet been developed
    private int getAssociation() {
        Byte associationIdByte = this.associationId[0];
        return associationIdByte.intValue();
    }

    private int getX() {
        byte[] xFieldBytes = this.pos[]
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
