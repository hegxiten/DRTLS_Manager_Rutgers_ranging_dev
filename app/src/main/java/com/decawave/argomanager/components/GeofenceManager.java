package com.decawave.argomanager.components;

import com.decawave.argo.api.struct.Position;
import com.decawave.argomanager.components.struct.GeofenceItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface GeofenceManager {

    /**
     * Must be called before used.
     */
    void init();

    @NotNull
    List<GeofenceItem> getActiveGeofences();

    @NotNull
    List<GeofenceItem> getGeofencesByNetwork(short networkId);

    @NotNull
    List<GeofenceItem> getAllGeofences();   // including geofences that are not active

    GeofenceItem getGeofence(short geofenceId);

    GeofenceItem getGeofence(String geofenceName);

    @NotNull
    List<Position> getGeofencePath(short geofenceId);

    @NotNull
    List<Position> getGeofencePath(String geofenceName);

    void insertGeofenceNode(GeofenceItem geofence, int geofenceNodeIdx, Position pos);

    void removeGeofenceNode(GeofenceItem geofence, int geofenceNodeIdx);

    void removeGeofence(short geofenceId);

    void removeGeofence(String geofenceName, boolean explicitUserAction);

    void removeActiveGeofence();

    void undoRemoveGeofence(short geofenceId);

    boolean hasNetworkByName(String networkName);

    int getNumberOfNodes(short geofenceId);

    /**
     * Does not interaction with the node. It just removes it's representation from internal
     * container.
     * @param nodeId identifies the node
     * @param userInitiated whether the action was initiated by user
     */
    void forgetNode(Long nodeId, boolean userInitiated);

}
