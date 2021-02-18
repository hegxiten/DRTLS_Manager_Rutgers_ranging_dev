package com.decawave.argomanager.components;

/**
 * System network operation mode change manager (between positioning or ranging).
 */
public interface NetworkOperationModeListener {
    enum OperationModeEnum {
        POSITIONING,
        RANGING
    }
    /**
     * switch between network operation modes: ranging<->positioning.
     */
    void onNetworkModeChanged(short networkId, OperationModeEnum operationMode);

}
