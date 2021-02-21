/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.ih;

import com.decawave.argo.api.struct.NetworkOperationMode;
import com.decawave.argomanager.ui.view.FloorPlan;
import com.decawave.argomanager.ui.view.Geofence;

/**
 * Argo project.
 */
public class IhNetworkChangeListenerAdapter implements IhNetworkChangeListener {

    @Override
    public void onNetworkAdded(short networkId) {

    }

    @Override
    public void onNetworkUpdated(short networkId) {

    }

    @Override
    public void onNetworkRemoved(short networkId, String networkName, boolean explicitUserAction) {

    }

    @Override
    public void onNetworkRenamed(short networkId, String newName) {

    }

    @Override
    public void onFloorPlanChanged(short networkId, FloorPlan floorPlan) {

    }

    @Override
    public void onGeofenceChanged(short networkId, Geofence geofence) {

    }

    @Override
    public void onNetworkOperationModeChanged(short networkId, NetworkOperationMode newOperationMode) {

    }
}
