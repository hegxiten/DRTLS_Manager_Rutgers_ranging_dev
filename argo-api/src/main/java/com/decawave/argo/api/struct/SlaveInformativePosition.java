package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SlaveInformativePosition {

    // lowest byte (first byte) of each integer might be compromised by observation (Zezhou Wang, 02282021)
    // relative X, Y, Z coordinate, each 2 Bytes. Skipping the lowest byte (first byte). Skipping 3 bytes.
    // e.g. 0x00CDAB003412007856 -> X: 0xABCD; Y: 0x1234; Z: 0x5678
    // pos[0], pos[4], pos[8] must be set as zero; not going to use them.
    // See getters and setters of X, Y, Z for details.
    private byte[] pos = new byte[9];
    // association id: 0 - 255
    private byte[] assocIdByteArray = new byte[1];
    // reservedLastTwoBytes 2 Bytes for future use
    // storing association ID cannot use the byte of qualityfactor. The fw of DWM1001-Dev anchor/slave
    // doesn't report individual qualityfactor per each UWB ranging request from tag. Therefore the
    // tag/master side cannot recover association id if stored in the last byte of 13-element array.
    private byte[] reservedLastTwoBytes = new byte[2];
    private byte[] qualityFactorByteArray = new byte[1];


    // Test 1 (correct):
    // encoded:   002519cc007f64fc00fb000000
    // decoded: 00002519cc007f64fc00fb000000
    // Implication: the original firmware might not be able to handle some value cases and minor positioning
    // errors should happen due to the compromised lowest byte of each millimeter integer.

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
        this.assocIdByteArray = source.assocIdByteArray;
        this.reservedLastTwoBytes = source.reservedLastTwoBytes;
        this.qualityFactorByteArray = source.qualityFactorByteArray;
    }

    @Override
    public String toString() {
        return "SlaveInfoPosition{" +
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

    public void setX(int x) {
        if (x < -32768 || x > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.pos[0] = 0x00;
        this.pos[1] = (byte) x;
        this.pos[2] = (byte) (x >>> 8);
    }

    public void setY(int y) {
        if (y < -32768 || y > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.pos[3] = (byte) y;
        this.pos[4] = 0x00;
        this.pos[5] = (byte) (y >>> 8);
    }

    public void setZ(int z) {
        if (z < -32768 || z > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.pos[6] = (byte) z;
        this.pos[7] = (byte) (z >>> 8);
        this.pos[8] = 0x00;
    }

    public void setAssocId(int assocId) {
        if (assocId < 0 || assocId > 255) {
            throw new IllegalArgumentException("association id range has to be limited from 0 to 255!");
        }
        this.assocIdByteArray[0] = (byte) assocId;
    }

    public int getX() {
        return signedIntFromTwoBytes(pos[2], pos[1]);
    }

    public int getY() {
        return signedIntFromTwoBytes(pos[5], pos[3]);
    }

    public int getZ() {
        return signedIntFromTwoBytes(pos[7], pos[6]);
    }

    public Integer getAssocId() {
        return unsignedIntFromByte(assocIdByteArray[0]);
    }

    public byte[] getEncodedByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(this.pos);
            outputStream.write(this.assocIdByteArray);
            outputStream.write(this.reservedLastTwoBytes);    // field for reservedLastTwoBytes
            outputStream.write(this.qualityFactorByteArray);    // field for quality factor
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[13];
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
