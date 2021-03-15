/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import com.decawave.argo.api.interaction.LocationData;

import org.jetbrains.annotations.Nullable;

/**
 * Tag network node.
 */
public interface TagNode extends NetworkNode {

    void setAccelerometerEnable(Boolean enable);

    Boolean isAccelerometerEnable();

    Integer getUpdateRate();

    void setUpdateRate(Integer updateRate);

    Integer getStationaryUpdateRate();

    void setStationaryUpdateRate(Integer updateRate);

    Boolean isLowPowerModeEnable();

    void setLowPowerModeEnable(Boolean enable);

    Boolean isLocationEngineEnable();

    void setLocationEngineEnable(Boolean enable);

    LocationData getLocationData();

    boolean anyRangingAnchorInLocationData();

    MasterInformativePosition getMasterInfoPosition();

    /**
     * For master tag this returns simply master informative position
     * @return master informative position, direct reference
     */
    @Nullable
    MasterInformativePosition extractMasterInfoPositionDirect();

    void setMasterInfoPosition(MasterInformativePosition masterInfoPosition);
}