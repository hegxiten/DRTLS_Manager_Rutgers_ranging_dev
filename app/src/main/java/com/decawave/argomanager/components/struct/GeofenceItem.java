package com.decawave.argomanager.components.struct;

import com.decawave.argo.api.struct.Position;

import java.util.List;

public class GeofenceItem {

    private short id;

    private String name;

    private List<Position> pathList;
    // struct of a geofence
    public short getId() {
        return this.id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Position> getPathList() {return pathList;}

}
