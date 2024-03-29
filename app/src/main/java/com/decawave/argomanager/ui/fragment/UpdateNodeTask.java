/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import com.annimon.stream.function.Function;
import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.MasterInformativePosition;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NetworkOperationMode;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.SlaveInformativePosition;
import com.decawave.argo.api.struct.SlaveMasterSide;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.argoapi.ext.UpdateRate;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogEntryTag;
import com.decawave.argomanager.debuglog.LogEntryTagFactory;
import com.decawave.argomanager.prefs.LengthUnit;
import com.decawave.argomanager.util.ConnectionUtil;
import com.decawave.argomanager.util.NetworkIdGenerator;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action2;

/**
 * Task updating a network node.
 */
class UpdateNodeTask {
    public static final ComponentLog log = new ComponentLog(UpdateNodeTask.class);
    private static final ApplicationComponentLog appLog = ApplicationComponentLog.newComponentLog(log, "UPDATE");

    private final NetworkNodeManager networkNodeManager;
    private final BleConnectionApi bleConnectionApi;

    private boolean running;
    private boolean cancelled;
    private NetworkNodeConnection connection;
    // Question: NetworkNodeConnection is an interface, how is this private field (connection)
    //      able to execute abstract method disconnect() without implementation?

    UpdateNodeTask(NetworkNodeManager networkNodeManager,
                   BleConnectionApi bleConnectionApi) {
        this.networkNodeManager = networkNodeManager;
        this.bleConnectionApi = bleConnectionApi;
    }


    boolean isRunning() {
        return running;
    }

    // do not report changes anymore
    void cancel() {
        cancelled = true;
        if (connection != null) {
            // initiate disconnect
            connection.disconnect();
        }
    }

    void doUpdate(@NotNull NetworkNode originalInput,
                  @NotNull NodeType operationMode,
                  @Nullable UwbMode uwbMode,
                  boolean isInitiator,
                  boolean isFirmwareUpdateEnable,
                  boolean isAccelerometerEnable,
                  boolean isLedIndicationEnable,
                  boolean isBleEnable,
                  boolean isLocationEngineEnable,
                  boolean isLowPowerModeEnable,
                  @Nullable String selectedNewNetworkName,
                  @Nullable Short selectedNetworkId,
                  @NotNull String nodeLabel,
                  @Nullable UpdateRate updateRate,
                  @Nullable UpdateRate stationaryUpdateRate,
                  @Nullable String origPosX,
                  @Nullable String origPosY,
                  @Nullable String origPosZ,
                  @Nullable String posX,
                  @Nullable String posY,
                  @Nullable String posZ,
                  LengthUnit lengthUnit,
                  @Nullable String origMasterPosX,
                  @Nullable String origMasterPosY,
                  @Nullable String origMasterPosZ,
                  @Nullable String origMasterAssocId,
                  @Nullable String origMasterVehicleLength,
                  @Nullable String posMasterX,
                  @Nullable String posMasterY,
                  @Nullable String posMasterZ,
                  @Nullable String masterAssocId,
                  @Nullable String masterVehicleLength,
                  @Nullable String origSlavePosX,
                  @Nullable String origSlavePosY,
                  @Nullable String origSlavePosZ,
                  @Nullable String origSlaveAssocId,
                  @Nullable String origSlaveVehicleLength,
                  @Nullable String posSlaveX,
                  @Nullable String posSlaveY,
                  @Nullable String posSlaveZ,
                  @Nullable String slaveAssocId,
                  @Nullable String slaveVehicleLength,
                  @Nullable SlaveMasterSide slaveMasterSide
                  ) {

        LogEntryTag tag = LogEntryTagFactory.getDeviceLogEntryTag(originalInput.getBleAddress());

        if (Constants.DEBUG) {
            Preconditions.checkNotNull(originalInput);
            appLog.d("updating node " + originalInput, tag);
        }
        // adjust position
        if (posX != null && posX.length() == 0) {
            posX = null;
        }
        if (posY != null && posY.length() == 0) {
            posY = null;
        }
        if (posZ != null && posZ.length() == 0) {
            posZ = null;
        }
        if (posMasterX != null && posMasterX.length() == 0) {
            posMasterX = null;
        }
        if (posMasterY != null && posMasterY.length() == 0) {
            posMasterY = null;
        }
        if (posMasterZ != null && posMasterZ.length() == 0) {
            posMasterZ = null;
        }
        if (posSlaveX != null && posSlaveX.length() == 0) {
            posSlaveX = null;
        }
        if (posSlaveY != null && posSlaveY.length() == 0) {
            posSlaveY = null;
        }
        if (posSlaveZ != null && posSlaveZ.length() == 0) {
            posSlaveZ = null;
        }
        // first process the network
        boolean networkActionPerformed = false;
        if (selectedNewNetworkName != null) {
            Preconditions.checkState(selectedNetworkId == null);
            short networkId;
            // user has chosen 'new network'
            Short exNetworkId = originalInput.getNetworkId();
            if (exNetworkId != null) {
                // the node was already part of 'some' network
                if (networkNodeManager.getNetworks().get(exNetworkId) != null) {
                    // and we know this network, we need to create a new network with new ID
                    networkNodeManager.declareNetwork(new NetworkModel(networkId = NetworkIdGenerator.newNetworkId(), selectedNewNetworkName));
                } else {
                    // it was part of unknown network, let's just name the network and let the node stay
                    // in the same network
                    networkNodeManager.declareNetwork(new NetworkModel(networkId = exNetworkId, selectedNewNetworkName));
                }
            } else {
                // the node wasn't part of any network
                networkNodeManager.declareNetwork(new NetworkModel(networkId = NetworkIdGenerator.newNetworkId(), selectedNewNetworkName));
            }
            // and adjust the selected network ID
            selectedNetworkId = networkId;
            networkActionPerformed = true;
        } // else: user has chosen the node to be part of some existing network, no actions to do
        // now check if there are actually any changes
        Preconditions.checkNotNull(selectedNetworkId);
        // create the target node where we will set only changed characteristics
        NetworkNode targetEntity = uiContentToNetworkNode(originalInput, operationMode, selectedNetworkId, nodeLabel, updateRate, stationaryUpdateRate,
                uwbMode, isInitiator, isFirmwareUpdateEnable, isLedIndicationEnable, isBleEnable, isAccelerometerEnable, isLocationEngineEnable, isLowPowerModeEnable,
                origPosX, origPosY, origPosZ, posX, posY, posZ, tag, lengthUnit,
                origMasterPosX, origMasterPosY, origMasterPosZ, origMasterAssocId,
                origMasterVehicleLength,
                posMasterX, posMasterY, posMasterZ, masterAssocId,
                masterVehicleLength,
                origSlavePosX, origSlavePosY, origSlavePosZ, origSlaveAssocId,
                origSlaveVehicleLength,
                posSlaveX, posSlaveY, posSlaveZ, slaveAssocId,
                slaveVehicleLength,
                slaveMasterSide);
        if (targetEntity == null) {
            // no change detected
            running = false;
            // no change detected
            if (!networkActionPerformed) {
                // no change
                InterfaceHub.getHandlerHub(Ih.class).onNoChangeDetected();
            } else {
                // else: network change performed
                InterfaceHub.getHandlerHub(Ih.class).onUpdatePerformed(originalInput);
            }
            return;
        } // else: there was a change
        running = true;
        // save the configured properties to the node
        InterfaceHub.getHandlerHub(Ih.class).onUpdateStarted();
        // do the update
        connectAndUpdate(targetEntity, tag);
    }

    private void connectAndUpdate(@NotNull final NetworkNode targetEntity, LogEntryTag tag) {
        boolean[] cleanDisconnectRequired = {false};
        ConnectionUtil.connectAndUpdate(bleConnectionApi, targetEntity.getBleAddress(), 3,
                () -> targetEntity,
                networkNodeConnection -> {},
                (writeEffect, networkNode) -> {
                    // now we MUST check if the changes got propagated (sometimes a device restart is necessary)
                    if (!cancelled) {
                        switch (writeEffect) {
                            case WRITE_DELAYED_EFFECT:
                                // we need to check if the entity corresponds to what we have written
                                appLog.imp("update succeeded, clean disconnect required", tag);
                                cleanDisconnectRequired[0] = true;
                                break;
                            case WRITE_IMMEDIATE_EFFECT:
                                // we are done
                                running = false;
                                // this means success
                                appLog.imp("update succeeded, notifying", tag);
                                // let the network node manager know
                                networkNodeManager.onNodeIntercepted(targetEntity);
                                // notify the listeners
                                InterfaceHub.getHandlerHub(Ih.class).onUpdatePerformed(targetEntity);
                                break;
                            case WRITE_SKIPPED:
                                // no change, we are done
                                appLog.imp("update succeeded, no change necessary", tag);
                                running = false;
                                InterfaceHub.getHandlerHub(Ih.class).onNoChangeDetected();
                                break;

                        }
                    } else {
                        // we were cancelled in the meantime
                        running = false;
                    }
                },
                fail -> {
                    // we will handle the failure on onFinished
                },
                (errCode) -> {
                    if (Constants.DEBUG) {
                        Preconditions.checkNotNull(errCode, "error code cannot be null!");
                    }
                    // cleanup the connection
                    UpdateNodeTask.this.connection = null;
                    // onFinished
                    if (!cancelled && running) {
                        running = false;
                        if (errCode != ErrorCode.NO_ERROR) {
                            // there was an error
                            appLog.we("update failed, BLE failure", ErrorCode.FAILED_UPDATE, tag);
                            InterfaceHub.getHandlerHub(Ih.class).onUpdateFailed();
                        } else if (cleanDisconnectRequired[0]) {
                            // there was no error
                            appLog.imp("update succeeded, cleanly disconnected", tag);
                            // let the network node manager know
                            networkNodeManager.onNodeIntercepted(targetEntity);
                            // notify the listeners
                            InterfaceHub.getHandlerHub(Ih.class).onUpdatePerformed(targetEntity);
                        }
                    }
                }
        );
    }

    /**
     * Converts the UI content to NetworkNode instance.
     * If there is no change performed (comparing to the original), null is returned.
     * @param originalInput original
     * @param targetNodeType target node type
     * @return built network node or null if no change is detected
     */
    private NetworkNode uiContentToNetworkNode(NetworkNode originalInput,
                                               NodeType targetNodeType,
                                               short selectedNetworkId,
                                               String nodeLabel,
                                               UpdateRate updateRate,
                                               UpdateRate stationaryUpdateRate,
                                               UwbMode uwbMode,
                                               boolean isInitiator,
                                               boolean isFirmwareUpdateEnable,
                                               boolean isLedIndicationEnable,
                                               boolean isBleEnable,
                                               boolean isAccelerometerEnable,
                                               boolean isLocationEngineEnable,
                                               boolean isLowPowerModeEnable,
                                               @Nullable String origPosX,
                                               @Nullable String origPosY,
                                               @Nullable String origPosZ,
                                               @Nullable String posX,
                                               @Nullable String posY,
                                               @Nullable String posZ,
                                               LogEntryTag logTag,
                                               LengthUnit lengthUnit,
                                               @Nullable String origMasterPosX,
                                               @Nullable String origMasterPosY,
                                               @Nullable String origMasterPosZ,
                                               @Nullable String origMasterAssoc,
                                               @Nullable String origMasterVehicleLength,
                                               @Nullable String posMasterX,
                                               @Nullable String posMasterY,
                                               @Nullable String posMasterZ,
                                               @Nullable String masterAssocId,
                                               @Nullable String masterVehicleLength,
                                               @Nullable String origSlavePosX,
                                               @Nullable String origSlavePosY,
                                               @Nullable String origSlavePosZ,
                                               @Nullable String origSlaveAssoc,
                                               @Nullable String origSlaveVehicleLength,
                                               @Nullable String posSlaveX,
                                               @Nullable String posSlaveY,
                                               @Nullable String posSlaveZ,
                                               @Nullable String slaveAssocId,
                                               @Nullable String slaveVehicleLength,
                                               @Nullable SlaveMasterSide slaveMasterSide) {
        if (Constants.DEBUG) {
            Preconditions.checkState(posX == null && posY == null && posZ == null ||
                    posX != null && posY != null && posZ != null);
        }
        NodeFactory.NodeBuilder builder = NodeFactory.newBuilder(targetNodeType, originalInput.getId());
        // the address does not change
        builder.setBleAddress(originalInput.getBleAddress());
        // propagate only those values which were changed in the UI (comparing to the initial node state - represented in 'node')
        boolean b = false;
        // network
        if (!Objects.equal(selectedNetworkId, originalInput.getNetworkId())) {
            appLog.d("network ID change detected", logTag);
            builder.setNetworkId(selectedNetworkId);
            b = true;
        }
        // label
        if (!nodeLabel.equals(originalInput.getLabel())) {
            appLog.d("node LABEL change detected", logTag);
            builder.setLabel(nodeLabel);
            b = true;
        }
        //
        boolean nodeTypeSwitch = targetNodeType != originalInput.getType();
        // online
        if (!Objects.equal(uwbMode, originalInput.getUwbMode())) {
            appLog.d("node ONLINE status change detected", logTag);
            builder.setUwbMode(uwbMode);
            b = true;
        }
        if (!Objects.equal(isFirmwareUpdateEnable, originalInput.isFirmwareUpdateEnable())) {
            appLog.d("node FIRMWARE_UPDATE status change detected", logTag);
            builder.setFirmwareUpdateEnable(isFirmwareUpdateEnable);
            b = true;
        }
        if (!Objects.equal(isBleEnable, originalInput.isBleEnable())) {
            appLog.d("node BLE status change detected", logTag);
            builder.setBleEnable(isBleEnable);
            b = true;
        }
        if (!Objects.equal(isLedIndicationEnable, originalInput.isLedIndicationEnable())) {
            appLog.d("node LED_INDICATION status change detected", logTag);
            builder.setLedIndicationEnable(isLedIndicationEnable);
            b = true;
        }
        // anchor specific
        if (targetNodeType == NodeType.ANCHOR) {
            NodeFactory.AnchorNodeBuilder anchorBuilder = (NodeFactory.AnchorNodeBuilder) builder;
            // initiator
            Boolean originalInitiator = null;
            if (originalInput.isAnchor()) {
                originalInitiator = ((AnchorNode) originalInput).isInitiator();
            }
            if (nodeTypeSwitch || !Objects.equal(isInitiator, originalInitiator)) {
                if (!nodeTypeSwitch) {
                    appLog.d("node INITIATOR status change detected", logTag);
                }
                anchorBuilder.setInitiator(isInitiator);
                b = true;
            }
            if (targetNodeType == NodeType.ANCHOR) {
                // position and slave informative position
                Position position = null;
                if (posX != null && posY != null && posZ != null) {
                    Integer uiPosX;
                    Integer uiPosY;
                    Integer uiPosZ;
                    try {
                        uiPosX = Util.parseLength(posX, lengthUnit);
                        uiPosY = Util.parseLength(posY, lengthUnit);
                        uiPosZ = Util.parseLength(posZ, lengthUnit);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("wrong number format", e);
                    }
                    if (nodeTypeSwitch || origPosX == null || !posX.equals(origPosX)) {
                        position = new Position();
                        position.x = uiPosX;
                    }
                    if (position != null || !posY.equals(origPosY)) {
                        if (position == null) {
                            position = new Position();
                            // we will take the value from the UI (it makes sense as a whole)
                            position.x = uiPosX;
                        }
                        position.y = uiPosY;
                    }
                    if (position != null || !posZ.equals(origPosZ)) {
                        if (position == null) {
                            position = new Position();
                            // we will take the value from the UI (it makes sense as a whole)
                            position.x = uiPosX;
                            position.y = uiPosY;
                        }
                        position.z = uiPosZ;
                    }
                }
                SlaveInformativePosition slaveInfoPosition = null;
                if (posSlaveX != null && posSlaveY != null && posSlaveZ != null
                        && slaveAssocId != null && slaveMasterSide != null && slaveVehicleLength != null) {
                    Integer uiSlavePosX;
                    Integer uiSlavePosY;
                    Integer uiSlavePosZ;
                    Integer uiSlaveAssocId;
                    Integer uiSlaveMasterSide;
                    Integer uiSlaveVehicleLength;
                    try {
                        uiSlavePosX = Util.parseSlaveMasterPositionSigned(posSlaveX, lengthUnit);
                        uiSlavePosY = Util.parseSlaveMasterPositionSigned(posSlaveY, lengthUnit);
                        uiSlavePosZ = Util.parseSlaveMasterPositionUnsigned(posSlaveZ, lengthUnit);
                        uiSlaveAssocId = Util.parseSlaveMasterAssoc(slaveAssocId);
                        uiSlaveVehicleLength = Util.parseSlaveMasterPositionUnsigned(slaveVehicleLength, lengthUnit);
                        uiSlaveMasterSide = Integer.valueOf(slaveMasterSide.getValue());
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("wrong number format", e);
                    }
                    if (nodeTypeSwitch || origSlavePosX == null || !posSlaveX.equals(origSlavePosX)) {
                        slaveInfoPosition = new SlaveInformativePosition();
                        slaveInfoPosition.setX(uiSlavePosX);
                    }
                    if (slaveInfoPosition != null || !posSlaveY.equals(origSlavePosY)) {
                        if (slaveInfoPosition == null) {
                            slaveInfoPosition = new SlaveInformativePosition();
                            // we will take the value from the UI (it makes sense as a whole)
                            slaveInfoPosition.setX(uiSlavePosX);
                        }
                        slaveInfoPosition.setY(uiSlavePosY);
                    }
                    if (slaveInfoPosition != null || !posSlaveZ.equals(origSlavePosZ)) {
                        if (slaveInfoPosition == null) {
                            slaveInfoPosition = new SlaveInformativePosition();
                            // we will take the value from the UI (it makes sense as a whole)
                            slaveInfoPosition.setX(uiSlavePosX);
                            slaveInfoPosition.setY(uiSlavePosY);
                        }
                        slaveInfoPosition.setZ(uiSlavePosZ);
                    }
                    if (slaveInfoPosition != null || !slaveAssocId.equals(origSlaveAssoc)) {
                        if (slaveInfoPosition == null) {
                            slaveInfoPosition = new SlaveInformativePosition();
                            // we will take the value from the UI (it makes sense as a whole)
                            slaveInfoPosition.setX(uiSlavePosX);
                            slaveInfoPosition.setY(uiSlavePosY);
                            slaveInfoPosition.setZ(uiSlavePosZ);
                        }
                        slaveInfoPosition.setAssocId(uiSlaveAssocId);
                    }
                    if (slaveInfoPosition != null || !slaveMasterSide.equals(SlaveMasterSide.UNKNOWN)) {
                        if (slaveInfoPosition == null) {
                            slaveInfoPosition = new SlaveInformativePosition();
                            // we will take the value from the UI (it makes sense as a whole)
                            slaveInfoPosition.setX(uiSlavePosX);
                            slaveInfoPosition.setY(uiSlavePosY);
                            slaveInfoPosition.setZ(uiSlavePosZ);
                            slaveInfoPosition.setAssocId(uiSlaveAssocId);
                        }
                        slaveInfoPosition.setSlaveSideByValue(uiSlaveMasterSide);
                    }
                    if (slaveInfoPosition != null || !slaveVehicleLength.equals(origSlaveVehicleLength)) {
                        if (slaveInfoPosition == null) {
                            slaveInfoPosition = new SlaveInformativePosition();
                            // we will take the value from the UI (it makes sense as a whole)
                            slaveInfoPosition.setX(uiSlavePosX);
                            slaveInfoPosition.setY(uiSlavePosY);
                            slaveInfoPosition.setZ(uiSlavePosZ);
                            slaveInfoPosition.setAssocId(uiSlaveAssocId);
                            slaveInfoPosition.setSlaveSideByValue(uiSlaveMasterSide);
                        }
                        slaveInfoPosition.setVehicleLength(uiSlaveVehicleLength);
                    }
                }
                if (nodeTypeSwitch) {
                    // let's avoid a situation when user switched from TAG to ANCHOR and did not set explicit position
                    // there might be stored position from previous anchor mode
                    position = new Position();
                    position.x = position.y = position.z = 0;
                }
                if (position != null) {
                    appLog.d("POSITION change detected", logTag);
                    position.qualityFactor = Position.MAX_QUALITY_FACTOR;
                    anchorBuilder.setPosition(position);
                    b = true;
                }
                if (slaveInfoPosition != null) {
                    if (nodeTypeSwitch || isPropertyChanged(originalInput, NodeType.ANCHOR, NetworkNodeProperty.ANCHOR_SLAVE_INFO_POSITION, slaveInfoPosition)) {
                        appLog.d("SLAVE INFORMATIVE POSITION change detected", logTag);
                        anchorBuilder.setSlaveInfoPosition(slaveInfoPosition);
                        b = true;
                    }
                }
            }
        } else {
            // TAG
            NodeFactory.TagNodeBuilder tagBuilder = (NodeFactory.TagNodeBuilder) builder;
            MasterInformativePosition masterInfoPosition = null;
            if (posMasterX != null && posMasterY != null && posMasterZ != null
                    && masterAssocId != null && slaveMasterSide != null && masterVehicleLength != null) {
                Integer uiMasterPosX;
                Integer uiMasterPosY;
                Integer uiMasterPosZ;
                Integer uiMasterAssocId;
                Integer uiSlaveMasterSide;
                Integer uiMasterVehicleLength;
                try {
                    uiMasterPosX = Util.parseSlaveMasterPositionSigned(posMasterX, lengthUnit);
                    uiMasterPosY = Util.parseSlaveMasterPositionSigned(posMasterY, lengthUnit);
                    uiMasterPosZ = Util.parseSlaveMasterPositionUnsigned(posMasterZ, lengthUnit);
                    uiMasterAssocId = Util.parseSlaveMasterAssoc(masterAssocId);
                    uiMasterVehicleLength = Util.parseSlaveMasterPositionUnsigned(masterVehicleLength, lengthUnit);
                    uiSlaveMasterSide = Integer.valueOf(slaveMasterSide.getValue());
                } catch (NumberFormatException e) {
                    throw new RuntimeException("wrong number format", e);
                }
                if (nodeTypeSwitch || origMasterPosX == null || !posMasterX.equals(origMasterPosX)) {
                    masterInfoPosition = new MasterInformativePosition();
                    masterInfoPosition.setX(uiMasterPosX);
                }
                if (masterInfoPosition != null || !posMasterY.equals(origMasterPosY)) {
                    if (masterInfoPosition == null) {
                        masterInfoPosition = new MasterInformativePosition();
                        // we will take the value from the UI (it makes sense as a whole)
                        masterInfoPosition.setX(uiMasterPosX);
                    }
                    masterInfoPosition.setY(uiMasterPosY);
                }
                if (masterInfoPosition != null || !posMasterZ.equals(origMasterPosZ)) {
                    if (masterInfoPosition == null) {
                        masterInfoPosition = new MasterInformativePosition();
                        // we will take the value from the UI (it makes sense as a whole)
                        masterInfoPosition.setX(uiMasterPosX);
                        masterInfoPosition.setY(uiMasterPosY);
                    }
                    masterInfoPosition.setZ(uiMasterPosZ);
                }
                if (masterInfoPosition != null || !masterAssocId.equals(origMasterAssoc)) {
                    if (masterInfoPosition == null) {
                        masterInfoPosition = new MasterInformativePosition();
                        // we will take the value from the UI (it makes sense as a whole)
                        masterInfoPosition.setX(uiMasterPosX);
                        masterInfoPosition.setY(uiMasterPosY);
                        masterInfoPosition.setZ(uiMasterPosZ);
                    }
                    masterInfoPosition.setAssocId(uiMasterAssocId);
                }
                if (masterInfoPosition != null || !slaveMasterSide.equals(SlaveMasterSide.UNKNOWN)) {
                    if (masterInfoPosition == null) {
                        masterInfoPosition = new MasterInformativePosition();
                        // we will take the value from the UI (it makes sense as a whole)
                        masterInfoPosition.setX(uiMasterPosX);
                        masterInfoPosition.setY(uiMasterPosY);
                        masterInfoPosition.setZ(uiMasterPosZ);
                        masterInfoPosition.setAssocId(uiMasterAssocId);
                    }
                    masterInfoPosition.setMasterSideByValue(uiSlaveMasterSide);
                }
                if (masterInfoPosition != null || !masterVehicleLength.equals(origMasterVehicleLength)) {
                    if (masterInfoPosition == null) {
                        masterInfoPosition = new MasterInformativePosition();
                        // we will take the value from the UI (it makes sense as a whole)
                        masterInfoPosition.setX(uiMasterPosX);
                        masterInfoPosition.setY(uiMasterPosY);
                        masterInfoPosition.setZ(uiMasterPosZ);
                        masterInfoPosition.setAssocId(uiMasterAssocId);
                        masterInfoPosition.setMasterSideByValue(uiSlaveMasterSide);
                    }
                    masterInfoPosition.setVehicleLength(uiMasterVehicleLength);
                }
            }
            // update rate
            else if (nodeTypeSwitch) {
                // we have to set update rate explicitly to the value shown in the UI, or default if null (?)
                handleUpdateRate(updateRate, tagBuilder, originalInput, TagNode::getUpdateRate, NodeFactory.TagNodeBuilder::setUpdateRate);
                handleUpdateRate(stationaryUpdateRate, tagBuilder, originalInput, TagNode::getStationaryUpdateRate, NodeFactory.TagNodeBuilder::setStationaryUpdateRate);
            }
            if ((updateRate != null && isPropertyChanged(originalInput, NodeType.TAG, NetworkNodeProperty.TAG_UPDATE_RATE, updateRate.msValue))
                    ||
                    (stationaryUpdateRate != null && isPropertyChanged(originalInput, NodeType.TAG, NetworkNodeProperty.TAG_STATIONARY_UPDATE_RATE, stationaryUpdateRate.msValue))) {
                // we have to handle both update rates
                handleUpdateRate(updateRate, tagBuilder, originalInput, TagNode::getUpdateRate, NodeFactory.TagNodeBuilder::setUpdateRate);
                handleUpdateRate(stationaryUpdateRate, tagBuilder, originalInput, TagNode::getStationaryUpdateRate, NodeFactory.TagNodeBuilder::setStationaryUpdateRate);
                b = true;
            }
            if (nodeTypeSwitch || isPropertyChanged(originalInput, NodeType.TAG, NetworkNodeProperty.TAG_ACCELEROMETER_ENABLE, isAccelerometerEnable)) {
                tagBuilder.setAccelerometerEnable(isAccelerometerEnable);
                b = true;
            }
            if (nodeTypeSwitch || isPropertyChanged(originalInput, NodeType.TAG, NetworkNodeProperty.TAG_LOW_POWER_MODE_ENABLE, isLowPowerModeEnable)) {
                tagBuilder.setLowPowerModeEnable(isLowPowerModeEnable);
                b = true;
            }
            if (nodeTypeSwitch || isPropertyChanged(originalInput, NodeType.TAG, NetworkNodeProperty.TAG_LOCATION_ENGINE_ENABLE, isLocationEngineEnable)) {
                tagBuilder.setLocationEngineEnable(isLocationEngineEnable);
                b = true;
            }
            boolean isRanging = networkNodeManager.getActiveNetwork() != null && networkNodeManager.getActiveNetwork().getNetworkOperationMode() == NetworkOperationMode.RANGING;
            if (nodeTypeSwitch || isPropertyChanged(originalInput, NodeType.TAG, NetworkNodeProperty.TAG_MASTER_INFO_POSITION, masterInfoPosition)
                && isRanging) {
                appLog.d("MASTER INFORMATIVE POSITION change detected", logTag);
                tagBuilder.setMasterInfoPosition(masterInfoPosition);
                b = true;
            }
        }
        if (b) {
            return builder.build();
        } else {
            return null;
        }
    }

    private void handleUpdateRate(UpdateRate updateRate,
                                  NodeFactory.TagNodeBuilder targetNodeBuilder,
                                  NetworkNode originalInput,
                                  Function<TagNode, Integer> updateRateGetter,
                                  Action2<NodeFactory.TagNodeBuilder, Integer> updateRateSetter) {
        Integer iUpdateRate;
        if (updateRate != null) {
            iUpdateRate = updateRate.msValue;
        } else {
            Integer originalUpdateRate = null;
            if (originalInput.isTag()) {
                // extract the update rate
                originalUpdateRate = updateRateGetter.apply((TagNode) originalInput);
            }
            iUpdateRate = originalUpdateRate != null ? originalUpdateRate : UpdateRate.DEFAULT.msValue;
        }
        updateRateSetter.call(targetNodeBuilder, iUpdateRate);
    }

    private boolean isPropertyChanged(NetworkNode originalNetworkNode,
                                      NodeType requiredNodeType,
                                      NetworkNodeProperty property,
                                      Object newPropertyValue) {
        Object originalValue = null;
        if (originalNetworkNode.getType() == requiredNodeType) {
            originalValue = originalNetworkNode.getProperty(property);
        }
        return !Objects.equal(originalValue, newPropertyValue);
    }

    interface Ih extends InterfaceHubHandler {

        void onUpdatePerformed(NetworkNode node);

        void onNoChangeDetected();

        void onUpdateFailed();

        void onUpdateStarted();

    }
}
