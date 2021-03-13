/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.SlaveInformativePosition;

import java.util.UUID;

/**
 * Write request visitor.
 */
interface WriteCharacteristicRequestVisitor {

    void visitText(WriteCharacteristicRequest<String> request);

    void visitUuid(WriteCharacteristicRequest<UUID> request);

    void visitPosition(WriteCharacteristicRequest<Position> request);

    void visitSlaveInfoPosition(WriteCharacteristicRequest<SlaveInformativePosition> request);

    void visitInteger(WriteCharacteristicRequest<Integer> request);

    void visitByte(WriteCharacteristicRequest<byte[]> request);

    void visitShort(WriteCharacteristicRequest<Short> request);

    void visitBoolean(WriteCharacteristicRequest<Boolean> request);

}
