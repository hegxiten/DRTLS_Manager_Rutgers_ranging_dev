/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util.gatt;

import android.util.Log;

import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.FirmwareMeta;
import com.decawave.argo.api.struct.LocationDataMode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.OperatingFirmware;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.SlaveInformativePosition;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.argoapi.ble.connection.FwPushCommandType;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;

import eu.kryl.android.common.log.ComponentLog;

/**
 * Various GATT / BLE utility routines.
 */
public class GattEncoder {
    // constants
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    // logging
    public static final ComponentLog log = new ComponentLog(GattEncoder.class);
    private static final byte[] BYTE_ARRAY_SINGLE_ZERO = {0};
    private static final byte[] BYTE_ARRAY_SINGLE_ONE = {1};
    private static final byte[] BYTE_ARRAY_SINGLE_TWO = {2};

    public static String printByteArray(byte[] arr) {
        if (arr == null) {
            return "null";
        }
        if (arr.length == 0) {
            return "<empty>";
        }
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : arr) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    //
    private static final byte[] BOOLEAN_TRUE = { 1 };
    private static final byte[] BOOLEAN_FALSE = { 0 };

    public static byte[] encodeLocationDataMode(@NotNull LocationDataMode locationDataMode) {
        switch (locationDataMode) {
            case POSITION:
                return BYTE_ARRAY_SINGLE_ZERO;
            case DISTANCES:
                return BYTE_ARRAY_SINGLE_ONE;
            case POSITION_AND_DISTANCES:
                return BYTE_ARRAY_SINGLE_TWO;
            default:
                throw new IllegalArgumentException("unexpected location data mode value: " + locationDataMode);
        }
    }

    public static byte[] encodeBoolean(Boolean value) {
        return value ? BOOLEAN_TRUE : BOOLEAN_FALSE;
    }

    public static UUID decodeUuid(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteBuffer bb = Util.newByteBuffer(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(secondLong, firstLong);
    }

    public static byte[] encodeOperationMode(NetworkNode networkNode, GattDecodeContext context) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(context.getOperationMode(), "operation mode of " + networkNode.getBleAddress() + " must not be null! one must first decode and THEN encode to get the context filled");
        }
        short s = encodeOperationMode(networkNode.getType(),
                gv(networkNode.getUwbMode(), context.getOperationMode().uwbMode),
                gv(networkNode.getOperatingFirmware(), context.getOperationMode().operatingFirmware),
                gv(networkNode.isFirmwareUpdateEnable(), context.getOperationMode().firmwareUpdateEnable),
                gv(networkNode.isBleEnable(), context.getOperationMode().bleEnable),
                gv(networkNode.isLedIndicationEnable(), context.getOperationMode().ledIndicationEnable),
                gv(networkNode.isAnchor() ? ((AnchorNode) networkNode).isInitiator() : null, context.getOperationMode().initiator),
                gv(networkNode.isTag() ? ((TagNode) networkNode).isAccelerometerEnable() : null, context.getOperationMode().accelerometerEnable),
                gv(networkNode.isTag() ? ((TagNode) networkNode).isLocationEngineEnable() : null, context.getOperationMode().locationEngineEnable),
                gv(networkNode.isTag() ? ((TagNode) networkNode).isLowPowerModeEnable() : null, context.getOperationMode().lowPowerModeEnable)
        );
        ByteBuffer bb = Util.newByteBuffer(new byte[2]);
        bb.putShort(s);
        return bb.array();
    }

    private static @NotNull <T> T gv(T preferValue, T defaultValue) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(defaultValue, "default value is null! one must first decode and THEN encode to get the context filled");
        }
        return preferValue != null ? preferValue : defaultValue;
    }

    @SuppressWarnings("WeakerAccess")
    public static short encodeOperationMode(@NotNull NodeType operationMode, @NotNull UwbMode uwbMode,
                                            @NotNull OperatingFirmware operatingFirmware, boolean firmwareUpdateEnabled,
                                            boolean bleEnable, boolean ledIndicationEnable,
                                            boolean initiator, boolean accelerometerEnable,
                                            boolean locationEngineEnable, boolean lowPowerModeEnable) {
        return (short) ((
                // encode the first byte
                        (operationMode == NodeType.TAG ? 0 : (1 << 7))
                        |
                        (getUwbEncodeNumber(uwbMode) << 5)
                        |
                        (operatingFirmware == OperatingFirmware.FW2 ? (1 << 4) : 0)
                        |
                        ((accelerometerEnable) ? 1 << 3 : 0)
                        |
                        ((ledIndicationEnable) ? 1 << 2 : 0)
                        |
                        (firmwareUpdateEnabled ? (1 << 1) : 0)
                        |
                        (bleEnable ? 1 : 0)
        )
                        |
                ((
                        (initiator ? (1 << 7) : 0)
                        |
                        (lowPowerModeEnable ? (1 << 6) : 0)
                        |
                        (locationEngineEnable ? (1 << 5) : 0)
                )  << 8) // shift the entire result into the first byte
        );
    }

    private static byte getUwbEncodeNumber(UwbMode uwbMode) {
        return (byte) uwbMode.ordinal();
    }

    public static byte[] encodeUuid(UUID uuid) {
        if (uuid == null) {
            return EMPTY_BYTE_ARRAY;
        } // else:
        ByteBuffer bb = Util.newByteBuffer(new byte[16]);
        bb.putLong(uuid.getLeastSignificantBits());
        bb.putLong(uuid.getMostSignificantBits());
        return bb.array();
    }

    public static byte[] encodeUpdateFirmwareOffer(FirmwareMeta firmwareMeta) {
        ByteBuffer bb = Util.newByteBuffer(new byte[17]);
        bb.put(mapPushCommandTypeToMessageType(FwPushCommandType.UPDATE_OFFER));
        bb.putInt(firmwareMeta.hardwareVersion);
        bb.putInt(firmwareMeta.firmwareVersion);
        bb.putInt(firmwareMeta.firmwareChecksum);
        bb.putInt(firmwareMeta.size);
        return bb.array();
    }

    public static byte[] encodeFwChunk(int offset, byte[] chunk) {
        int bufferLength = chunk.length + 5;
        if (Constants.DEBUG) {
            // MTU must be bigger than encoded chunk length
            Preconditions.checkState(BleConstants.MTU_ON_FW_UPLOAD >= bufferLength);
        }
        ByteBuffer bb = Util.newByteBuffer(new byte[bufferLength]);
        // encode offset first
        bb.put(mapPushCommandTypeToMessageType(FwPushCommandType.FIRMWARE_DATA_CHUNK));
        bb.putInt(offset);
        bb.put(chunk);
        //
        return bb.array();
    }

    private static byte mapPushCommandTypeToMessageType(FwPushCommandType pushCommandType) {
        return (byte) pushCommandType.ordinal();
    }

    /**
     * We never encode anything else than position (ranging anchors are always provided by the device only).
     */
    public static byte[] encodePosition(Position position) {
        if (position == null) {
            return EMPTY_BYTE_ARRAY;
        } // else:
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(position);
        }
        ByteBuffer buff = Util.newByteBuffer(new byte[13]);
        // position follows
        buff.putInt(position.x);
        buff.putInt(position.y);
        buff.putInt(position.z);
        buff.put(position.qualityFactor);
        Log.d("bytearrayencodedecode", "encodePosition, anchor raw bytes: " + new String(Hex.encodeHex(buff.array())) + " length: " + buff.array().length);
        return buff.array();
    }
    //    Positioning config test 1: (error)
    //    1010 0100 1101 0111 1101 0101 0000 0011|(x)|0100 0001 0000 1011 1111 0011 0000 0001|(y)|0111 0000 1101 1100 0111 0010 1111 1111|(z)|
    //    1010 1000 1101 0111 1101 0101 0000 0011|(x)|0100 0010 0000 1011 1111 0011 0000 0001|(y)|0110 1111 1101 1100 0111 0010 1111 1111|(z)|
    //    in        |  |a4.d7d503(x)|41.0bf301(y)|70.dc72ff(z)|64(qf)|
    //    out       |00|a8.d7d503(x)|42.0bf301(y)|6f.dc72ff(z)|64(qf)|
    //    Positioning config test 2: (correct)
    //    1100 1111 0010 1001 0000 0001 0000 0000|(x)|0100 0111 0111 0111 1001 1010 1111 1111|(y)|0111 0000 0000 0111 0000 0000 0000 0000|(z)|
    //    1100 1111 0010 1001 0000 0001 0000 0000|(x)|0100 0111 0111 0111 1001 1010 1111 1111|(y)|0111 0000 0000 0111 0000 0000 0000 0000|(z)|
    //    in        |  |cf.290100(x)|47.779aff(y)|70.070000(z)|64(qf)|
    //    out       |00|cf.290100(x)|47.779aff(y)|70.070000(z)|64(qf)|
    //    Positioning config test 3: (correct)
    //    0100 0111 0111 0111 1001 1010 1111 1111|(x)|1110 0011 1001 0011 0110 0101 0000 0000|(y)|0101 1001 1001 0000 0001 1101 0000 0000|(z)|
    //    0100 0111 0111 0111 1001 1010 1111 1111|(x)|1110 0011 1001 0011 0110 0101 0000 0000|(y)|0101 1001 1001 0000 0001 1101 0000 0000|(z)|
    //    in        |  |47.779aff(x)|e3.936500(y)|59.901d00(z)|64(qf)|
    //    out       |00|47.779aff(x)|e3.936500(y)|59.901d00(z)|64(qf)|
    //    Positioning config test 4: (error, same as 1)
    //    in        |  |a4.d7d503(x)|410bf301(y)|70dc72ff(z)|64(qf)|
    //    out       |00|a8.d7d503(x)|420bf301(y)|6fdc72ff(z)|64(qf)|
    //    Positioning config test 5: (error)
    //    in        |  |68.548b04(x)|87.3b53fc(y)|70.cd5c00(z)|64(qf)|
    //    out       |00|68.548b04(x)|84.3b53fc(y)|70.cd5c00(z)|64(qf)|
    //    Positioning config test 6: (error)
    //        set Position{x=76239973, y=-61654145, z=6081904, qualityFactor=100}
    //        get Position{x=76239976, y=-61654148, z=6081904, qualityFactor=100}
    //    in        |  |65.548b04(x)|7f.3b53fc(y)|70.cd5c00(z)|64(qf)|
    //    out       |00|68.548b04(x)|7c.3b53fc(y)|70.cd5c00(z)|64(qf)|
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //    Preliminary conclusion:
    //    The lowest byte (first byte) of each integer might be compromised.
    //    Avoid using this for SlaveInformativePosition
    /**
     * encode the 13-byte SlaveInformativePosition (with the last byte unusable due to fw drawback)
     * 6-byte of X, Y, Z, followed by 1 byte of association id. See SlaveInformativePosition.java
     */
    public static byte[] encodeSlaveInfoPosition(SlaveInformativePosition slaveInfoPosition) {
        if (slaveInfoPosition == null) {
            return EMPTY_BYTE_ARRAY;
        } // else:
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(slaveInfoPosition);
        }
        ByteBuffer buff = Util.newByteBuffer(new byte[13]);
        // slave info position follows
        byte[] slaveInfoPosBytes = slaveInfoPosition.getEncodedByteArray();
        for(int i=0; i < slaveInfoPosBytes.length; i++) {
            buff.put(slaveInfoPosBytes[i]);
        }
        if (Constants.DEBUG) {
            Log.d("bytearrayencodedecode", "encodeSlaveInfoPosition, anchor raw bytes: "
                    + new String(Hex.encodeHex(buff.array())) + " length: " + buff.array().length + " "
                    + slaveInfoPosition.toString());
        }
        return buff.array();
    }

    public static byte[] encodeUpdateRate(int updateRate, int stationaryUpdateRate) {
        ByteBuffer bb = Util.newByteBuffer(new byte[8]);
        bb.putInt(updateRate);
        bb.putInt(stationaryUpdateRate);
        return bb.array();
    }

}
