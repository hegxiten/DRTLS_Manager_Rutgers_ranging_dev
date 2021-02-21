package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

public class SlaveInformativePosition extends Position {

    public String associationId;

    public SlaveInformativePosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SlaveInformativePosition(int x, int y, int z, String associationId) {
        this(x, y, z);
        this.associationId = associationId;
    }

    public SlaveInformativePosition(@NotNull SlaveInformativePosition infoPosition) {
        copyFrom(infoPosition);
    }

    public SlaveInformativePosition() {
    }

    public void copyFrom(@NotNull SlaveInformativePosition source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
        this.associationId = source.associationId;
    }

    @Override
    public String toString() {

        return "Slave InfoPosition{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", association=" + associationId +
                '}';
    }

}
