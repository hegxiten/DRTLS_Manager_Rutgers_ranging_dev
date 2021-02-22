/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import android.util.Log;

import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argo.api.struct.SlaveInformativePosition;
import com.google.common.base.Objects;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tag diffing wrapper - actually there is little to diff (just initiator).
 * The API for setting the operation mode (Anchor, Tag, Offline) is elsewhere.
 */
public class AnchorNodeDiffingWrapper extends NetworkNodeDiffingWrapper<AnchorNode> implements AnchorNode {

    AnchorNodeDiffingWrapper(AnchorNode node) {
        super(node);
    }

    @Override
    public Byte getSeatNumber() {
        return delegate.getSeatNumber();
    }

    @Override
    public Short getClusterMap() {
        return delegate.getClusterMap();
    }

    @Override
    public Short getClusterNeighbourMap() {
        return delegate.getClusterNeighbourMap();
    }

    @Override
    public List<Short> getAnchorList() {
        return delegate.getAnchorList();
    }

    @Override
    public Integer getMacStats() {
        return delegate.getMacStats();
    }

    @Override
    public Boolean isInitiator() {
        return delegate.isInitiator();
    }

    @Override
    public void setInitiator(Boolean initiator) {
        delegate.setInitiator(initiator);
    }

    @Override
    public boolean anyDistance() {
        return delegate.anyDistance();
    }

    @Override
    public SlaveInformativePosition getSlaveInfoPosition() {
        return delegate.getSlaveInfoPosition();
    }

    @Override
    public Position getPosition() {
        return delegate.getPosition();
    }

    public List<RangingAnchor> getDistances() {
        return delegate.getDistances();
    }

    @Override
    public Boolean isBridge() {
        return delegate.isBridge();
    }

    @Override
    public void setPosition(Position position) {
        delegate.setPosition(position);
    }

    @Override
    public void setSlaveInfoPosition(SlaveInformativePosition infoPosition) {
        delegate.setSlaveInfoPosition(infoPosition);
    }

    public boolean isInitiatorChanged() {
        return !Objects.equal(delegate.isInitiator(), original.isInitiator());
    }

    public boolean isPositionChanged() {
        return isPropertyChanged(NetworkNodeProperty.ANCHOR_POSITION);
    }

    public boolean isSlaveInfoPosChanged() {
        return isPropertyChanged(NetworkNodeProperty.ANCHOR_SLAVE_INFO_POSITION);
    }

    @Override
    public void copyWritablePropertiesFrom(AnchorNode node) {
        super.copyWritablePropertiesFrom(node);
        // initiator
        setPosition(node.getPosition());
        setSlaveInfoPosition(node.getSlaveInfoPosition());
        setInitiator(node.isInitiator());
    }

    @Override
    public <T> T getProperty(NetworkNodeProperty property, boolean deepCopy) {
        return delegate.getProperty(property, deepCopy);
    }

    @Nullable
    @Override
    public SlaveInformativePosition extractSlaveInfoPositionDirect() {
        return delegate.getSlaveInfoPosition();
    }
}
