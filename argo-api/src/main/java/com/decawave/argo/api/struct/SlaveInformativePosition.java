package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SlaveInformativePosition {

    // lowest byte (first byte) of each integer might be compromised by observation (Zezhou Wang, 03122021)
    // relative X, Y, Z coordinate, each 2 Bytes. Lower bytes of original 3 (4-byte) integers are volatile.
    // Using the original higher bytes for storing our X (CPLR), Y (CL), Z (TOR) values.
    // Current Position design:
    //  |00 |01 |02 |03 |04 |05 |06 |07 |08 |09 |10 |11 |12 | (13-bytes)
    //  |RV |ID |XL |XH |RV |RS |YL |YH |RV |RV |ZL |ZH |qf |
    //  R: reserved; V: volatile; S: stable (relatively); L: lower-byte; H: higher-byte; ID: assocId
    // Using byte 01 for storing vehicle association id. If needed, can expand to 2/3 byte id.
    // e.g. 0x00ABCDAB0000341200007856 -> X: 0xABCD; Y: 0x1234; Z: 0x5678; assocId: 0xAB
    // See getters and setters of X, Y, Z, assocId for details.
    // association id: 0 - 255 unsigned integer
    private byte[] slaveInfoBytes = new byte[12];
    // storing association ID cannot use the byte of qualityfactor. The fw of DWM1001-Dev anchor/slave
    // doesn't report individual qualityfactor per each UWB ranging request from tag. Therefore the
    // tag/master side cannot recover association id if stored in the last byte of 13-element array.
    private byte[] qualityFactorByteArray = new byte[1];

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

    public SlaveInformativePosition(byte[] byteArray) {
        this.slaveInfoBytes = Arrays.copyOfRange(byteArray, 0, 12);
        this.qualityFactorByteArray = Arrays.copyOfRange(byteArray, 12, 13);
        this.setX(signedIntFromTwoBytes(this.slaveInfoBytes[3], this.slaveInfoBytes[2]));
        this.setY(signedIntFromTwoBytes(this.slaveInfoBytes[7], this.slaveInfoBytes[6]));
        this.setZ(signedIntFromTwoBytes(this.slaveInfoBytes[11],this.slaveInfoBytes[10]));
        this.setAssocId(unsignedIntFromByte(this.slaveInfoBytes[1]));
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
        if (this.slaveInfoBytes != otherSlaveInfoPosition.slaveInfoBytes) return false;
        return this.getAssocId() != null ? (this.getAssocId() == otherSlaveInfoPosition.getAssocId()) : (otherSlaveInfoPosition.getAssocId() == null);

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
        this.slaveInfoBytes = source.slaveInfoBytes;
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
        this.slaveInfoBytes[0] = 0x00;  // Reserved
        this.slaveInfoBytes[2] = (byte) x;
        this.slaveInfoBytes[3] = (byte) (x >>> 8);
    }

    public void setY(int y) {
        if (y < -32768 || y > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.slaveInfoBytes[4] = 0x00;  // Reserved
        this.slaveInfoBytes[5] = 0x00;  // Reserved
        this.slaveInfoBytes[6] = (byte) y;
        this.slaveInfoBytes[7] = (byte) (y >>> 8);
    }

    public void setZ(int z) {
        if (z < -32768 || z > 32767) {
            throw new IllegalArgumentException("input position value out of range! (min -32768 max 32767)");
        }
        this.slaveInfoBytes[8] = 0x00;  // Reserved
        this.slaveInfoBytes[9] = 0x00;  // Reserved
        this.slaveInfoBytes[10] = (byte) z;
        this.slaveInfoBytes[11] = (byte) (z >>> 8);
    }

    public void setAssocId(int assocId) {
        if (assocId < 0 || assocId > 255) {
            throw new IllegalArgumentException("association id range has to be limited from 0 to 255!");
        }
        this.slaveInfoBytes[1] = (byte) assocId;    // relatively stable byte from original X
    }

    public int getX() {
        return signedIntFromTwoBytes(slaveInfoBytes[3], slaveInfoBytes[2]);
    }

    public int getY() {
        return signedIntFromTwoBytes(slaveInfoBytes[7], slaveInfoBytes[6]);
    }

    public int getZ() {
        return signedIntFromTwoBytes(slaveInfoBytes[11], slaveInfoBytes[10]);
    }

    public Integer getAssocId() {
        return unsignedIntFromByte(slaveInfoBytes[1]);
    }

    public byte[] getEncodedByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(this.slaveInfoBytes);
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
