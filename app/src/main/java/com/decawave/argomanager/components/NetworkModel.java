/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.ui.view.FloorPlan;
import com.decawave.argomanager.ui.view.Geofence;
import com.decawave.argomanager.components.NetworkOperationModeListener.OperationModeEnum;
import com.google.common.base.Objects;

/**
 * Represents an ARGO network properties.
 *
 * Nodes are kept in network node manager.
 */
public class NetworkModel {
    // id of the network - as it is advertised
    public final short networkId;
    private OperationModeEnum operationMode;
    // modifiable members
    private String networkName;
    // nodes
    private FloorPlan floorPlan;
    private Geofence geofence;
    //
    private NetworkPropertyChangeListener changeListener;
    private NetworkOperationModeListener operationModeChangeListener;

    public NetworkModel(short networkId) {
        this(networkId, null);
    }


    public NetworkModel(short networkId, String networkName) {
        this.networkId = networkId;
        this.networkName = networkName;
        this.operationMode = OperationModeEnum.POSITIONING;
        this.changeListener = VOID_LISTENER;
        this.operationModeChangeListener = VOID_OPERATION_MODE_LISTENER;
    }

    public void setChangeListener(NetworkPropertyChangeListener changeListener) {
        this.changeListener = changeListener == null ? VOID_LISTENER : changeListener;
    }

    public void setOperationModeChangeListener(NetworkOperationModeListener operationModeChangeListener) {
        this.operationModeChangeListener = operationModeChangeListener == null ? VOID_OPERATION_MODE_LISTENER : operationModeChangeListener;
    }

    public short getNetworkId() {
        return networkId;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
        this.changeListener.onNetworkRenamed(networkId, networkName);
    }

    public void setNetworkOperationMode(OperationModeEnum operationMode) {
        this.operationMode = operationMode;
        this.operationModeChangeListener.onNetworkModeChanged(networkId, operationMode);
    }

    public OperationModeEnum getOperationMode() {
        return this.operationMode;
    }

    public void setFloorPlan(FloorPlan floorPlan) {
        if (this.operationMode == operationMode.POSITIONING){
            FloorPlan oldFp = this.floorPlan;
            this.floorPlan = floorPlan;
            if (!Objects.equal(floorPlan, oldFp)) {
                // notify
                changeListener.onGeofenceChanged(networkId, geofence);
            }
        }
    }

    public FloorPlan getFloorPlan() {
        return floorPlan;
    }

    public void setGeofence(Geofence geofence) {
        if(this.operationMode == operationMode.POSITIONING){
            Geofence oldGf = this.geofence;
            this.geofence = geofence;
            if (!Objects.equal(geofence, oldGf)) {
                // notify
                changeListener.onGeofenceChanged(networkId, geofence);
            }
        }
    }

    public Geofence getGeofence() {
        return geofence;
    }

    public String getNetworkName() {
        return networkName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkModel that = (NetworkModel) o;

        //noinspection SimplifiableIfStatement
        if (networkId != that.networkId) return false;
        return networkName != null ? networkName.equals(that.networkName) : that.networkName == null;

    }

    @Override
    public int hashCode() {
        int result = (int) networkId;
        result = 31 * result + (networkName != null ? networkName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NetworkModel{" +
                "networkId='" + networkId + '\'' +
                ", networkOperationMode='" + (this.operationMode == OperationModeEnum.POSITIONING ? "Positioning" : "Ranging") + '\'' +
                ", networkName='" + networkName + '\'' +
                '}';
    }

    private static NetworkPropertyChangeListener VOID_LISTENER = new NetworkPropertyChangeListener() {

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
  };

    private static NetworkOperationModeListener VOID_OPERATION_MODE_LISTENER = new NetworkOperationModeListener() {

        @Override
        public void onNetworkModeChanged(short networkId, OperationModeEnum operationMode) {

        }
    };

}
