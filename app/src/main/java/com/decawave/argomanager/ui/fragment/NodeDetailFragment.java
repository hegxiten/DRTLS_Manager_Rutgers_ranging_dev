/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.MasterInformativePosition;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkOperationMode;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.SlaveInformativePosition;
import com.decawave.argo.api.struct.SlaveMasterSide;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.argoapi.ext.UpdateRate;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.LengthUnit;
import com.decawave.argomanager.ui.dialog.NetworkPickerDialogFragment;
import com.decawave.argomanager.ui.dialog.NewNetworkNameDialogFragment;
import com.decawave.argomanager.ui.dialog.NodeTypePickerDialogFragment;
import com.decawave.argomanager.ui.dialog.SlaveMasterSidePickerDialogFragment;
import com.decawave.argomanager.ui.dialog.UpdateRatePickerDialogFragment;
import com.decawave.argomanager.ui.dialog.UwbModePickerDialogFragment;
import com.decawave.argomanager.ui.uiutil.DecimalDigitsInputFilter;
import com.decawave.argomanager.util.ToastUtil;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import eu.kryl.android.common.Constants;
import eu.kryl.android.common.hub.InterfaceHub;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Displays node details (enter from the edit button).
 */
public class NodeDetailFragment extends AbstractArgoFragment implements NetworkPickerDialogFragment.IhCallback,
        NewNetworkNameDialogFragment.IhCallback,
        UpdateRatePickerDialogFragment.IhCallback,
        NodeTypePickerDialogFragment.IhCallback,
        UwbModePickerDialogFragment.IhCallback,
        SlaveMasterSidePickerDialogFragment.IhCallback,
        UpdateNodeTask.Ih {
    public static final String BK_PAUSED = "PAUSED";
    public static final String BK_NODE_ID = "NODE_ID";
    public static final String BK_SELECTED_NETWORK_ID = "NETWORK_ID";
    public static final String BK_SELECTED_NEW_NETWORK_NAME = "NETWORK_NAME";
    public static final String BK_SELECTED_UPDATE_RATE = "TAG_UPDATE_RATE";
    public static final String BK_SELECTED_STATIONARY_UPDATE_RATE = "TAG_STATIONARY_UPDATE_RATE";
    public static final String BK_SELECTED_NODE_TYPE = "NODE_TYPE";
    public static final String BK_SELECTED_UWB_MODE = "UWB_MODE";
    public static final String BK_SELECTED_SLAVE_MASTER_SIDE = "SLAVE_MASTER_SIDE";
    public static final String BK_RAW_UPDATE_RATE = "UPDATE_RATE_RAW";
    public static final String BK_ORIG_POS_X = "ORIG_POS_X";
    public static final String BK_ORIG_POS_Y = "ORIG_POS_Y";
    public static final String BK_ORIG_POS_Z = "ORIG_POS_Z";
    public static final String BK_ORIG_SLAVE_POS_X = "ORIG_SLAVE_POS_X";
    public static final String BK_ORIG_SLAVE_POS_Y = "ORIG_SLAVE_POS_Y";
    public static final String BK_ORIG_SLAVE_POS_Z = "ORIG_SLAVE_POS_Z";
    public static final String BK_ORIG_SLAVE_ASSOC = "ORIG_SLAVE_ASSOC";
    public static final String BK_ORIG_MASTER_POS_X = "ORIG_MASTER_POS_X";
    public static final String BK_ORIG_MASTER_POS_Y = "ORIG_MASTER_POS_Y";
    public static final String BK_ORIG_MASTER_POS_Z = "ORIG_MASTER_POS_Z";
    public static final String BK_ORIG_MASTER_ASSOC = "ORIG_MASTER_ASSOC";
    public static final DecimalDigitsInputFilter INPUT_FILTER_DECIMAL_5_3 = new DecimalDigitsInputFilter(5, 3);
    public static final DecimalDigitsInputFilter INPUT_FILTER_DECIMAL_5_0 = new DecimalDigitsInputFilter(5, 0);
    public static final DecimalDigitsInputFilter INPUT_FILTER_DECIMAL_3_0 = new DecimalDigitsInputFilter(3, 0);
    public static final InputFilter[] POSITION_INPUT_FILTERS = new InputFilter[]{INPUT_FILTER_DECIMAL_5_3};
    public static final InputFilter[] MASTER_POSITION_INPUT_FILTERS = new InputFilter[]{INPUT_FILTER_DECIMAL_5_0};
    public static final InputFilter[] SLAVE_POSITION_INPUT_FILTERS = new InputFilter[]{INPUT_FILTER_DECIMAL_5_0};
    public static final InputFilter[] MASTER_ASSOC_INPUT_FILTERS = new InputFilter[]{INPUT_FILTER_DECIMAL_3_0};
    public static final InputFilter[] SLAVE_ASSOC_INPUT_FILTERS = new InputFilter[]{INPUT_FILTER_DECIMAL_3_0};
    private static UpdateNodeTask updateNodeTask;

    private Runnable cancelUpdateRunnable = () -> {
        if (updateNodeTask != null) {
            updateNodeTask.cancel();
        }
    };

    // dependencies
    @Inject
    DiscoveryManager discoveryManager;

    @Inject
    NetworkNodeManager networkNodeManager;

    @Inject
    BleConnectionApi bleConnectionApi;

    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    //
    private long nodeId;

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // members - views
    @BindView(R.id.node_detail_position_x)
    EditText etPosX;                    // editable anchor X input/current value

    @BindView(R.id.node_detail_position_y)
    EditText etPosY;                    // editable anchor Y input/current value

    @BindView(R.id.node_detail_position_z)
    EditText etPosZ;                    // editable anchor Z input/current value

    @BindView(R.id.slave_detail_pos_x)
    EditText etSlavePosX;               // editable slave X input/current value

    @BindView(R.id.slave_detail_pos_y)
    EditText etSlavePosY;               // editable slave Y input/current value

    @BindView(R.id.slave_detail_pos_z)
    EditText etSlavePosZ;               // editable slave Z input/current value

    @BindView(R.id.slave_detail_association)
    EditText etSlaveAssoc;              // editable slave id input/current value

    @BindView(R.id.master_detail_pos_x)
    EditText etMasterPosX;              // editable master X input/current value

    @BindView(R.id.master_detail_pos_y)
    EditText etMasterPosY;              // editable master Y input/current value

    @BindView(R.id.master_detail_pos_z)
    EditText etMasterPosZ;              // editable master Z input/current value

    @BindView(R.id.master_detail_association)
    EditText etMasterAssoc;             // editable master id input/current value

    @BindView(R.id.etNodeLabel)
    EditText etNodeLabel;

    @BindView(R.id.tvNodeId)
    TextView tvNodeId;

    @BindView(R.id.tvNodeBleAddress)
    TextView tvNodeBleAddress;

    @BindView(R.id.tvNetworkName)
    TextView tvNetworkName;

    @BindView(R.id.updateRateLabel)
    View updateRateLabel;

    @BindView(R.id.updateRateSelector)
    View updateRateSelector;

    @BindView(R.id.tvUpdateRate)
    TextView tvUpdateRate;

    @BindView(R.id.arrowUpdateRate)
    View arrowUpdateRate;

    @BindView(R.id.stationaryUpdateRateLabel)
    View stationaryUpdateRateLabel;

    @BindView(R.id.stationaryUpdateRateSelector)
    View stationaryUpdateRateSelector;

    @BindView(R.id.tvStationaryUpdateRate)
    TextView tvStationaryUpdateRate;

    @BindView(R.id.arrowStationaryUpdateRate)
    View arrowStationaryUpdateRate;

    @BindView(R.id.tvNodeType)
    TextView tvNodeType;

    @BindView(R.id.tvUwbMode)
    TextView tvUwbMode;

    @BindView(R.id.tvSlaveMasterSide)
    TextView tvSlaveMasterSide;

    @BindView(R.id.networkNameSelector)
    View networkViewGroup;

    @BindView(R.id.nodeTypeSelector)
    View nodeTypeViewGroup;         // Anchor or Tag

    @BindView(R.id.uwbModeSelector)
    View uwbModeViewGroup;          // Shown as "UWB" as UI: off, active, passive

    @BindView(R.id.slaveMasterSideSelector)
    View slaveMasterSideViewGroup;

    @BindView(R.id.chboxInitiator)
    CheckBox chboxInitiator;

    @BindView(R.id.chboxFirmwareUpdate)
    CheckBox chboxFirmwareUpdate;

    @BindView(R.id.chboxAccelerometer)
    CheckBox chboxAccelerometer;    // STATIONARY DETECTION

    @BindView(R.id.chboxLedIndication)
    CheckBox chboxLedIndication;    // LED

    @BindView(R.id.chboxResponsiveMode)
    CheckBox chboxResponsiveMode;

    @BindView(R.id.chboxBleEnabled)
    CheckBox chboxBleEnabled;

    @BindView(R.id.chboxLocationEngine)
    CheckBox chboxLocationEngine;

    @BindView(R.id.progressFrame)
    View progressFrame;

    @BindView(R.id.contentFrame)
    View contentFrame;

    @BindView(R.id.tvPositionTitle)
    TextView tvPositionTitle;

    @BindView(R.id.tvSlaveMasterPosTitle)
    TextView tvSlavePosTitle;

    @BindViews({R.id.chboxInitiator, R.id.tvPositionTitle, R.id.tvPositionContainer})
    List<View> anchorSpecificViews;

    @BindViews({R.id.chboxInitiator, R.id.tvSlaveMasterPosTitle, R.id.tvSlavePosConfigContainer,
                R.id.slaveMasterSideLabel, R.id.slaveMasterSideSelector,
                R.id.tvSlaveMasterAssocTitle, R.id.tvSlaveAssocContainer, R.id.slaveMasterFieldExplainContainer})
    List<View> slaveSpecificViews;

    @BindViews({R.id.updateRateContainer, R.id.chboxAccelerometer, R.id.chboxResponsiveMode, R.id.chboxLocationEngine })
    List<View> tagSpecificViews;

    @BindViews({R.id.masterEtNodeLabelDisabledExplain, R.id.tvSlaveMasterPosTitle, R.id.tvMasterPosConfigContainer,
                R.id.slaveMasterSideLabel, R.id.slaveMasterSideSelector,
                R.id.tvSlaveMasterAssocTitle, R.id.tvMasterAssocContainer, R.id.slaveMasterFieldExplainContainer,
                R.id.updateRateContainer, R.id.chboxAccelerometer, R.id.chboxResponsiveMode, R.id.chboxLocationEngine })
    List<View> masterSpecificViews;

    //
    private boolean fillUi = false;
    // read-only node being modified
    private NetworkNode inputNode;
    // our-pseudo spinners
    private Short selectedNetworkId;
    private String selectedNewNetworkName;
    private UpdateRate selectedUpdateRate;
    private UpdateRate selectedStationaryUpdateRate;
    private NodeType selectedNodeType;
    private UwbMode selectedUwbMode;
    private SlaveMasterSide selectedSlaveMasterSide;
    private Integer rawUpdateRate;
    private Integer rawStationaryUpdateRate;

    private String origPosX, origPosY, origPosZ;
    private String origSlavePosX, origSlavePosY, origSlavePosZ, origSlaveAssocId;
    private String origMasterPosX, origMasterPosY, origMasterPosZ, origMasterAssocId;

    //
    private Unbinder unbinder;

    public static final int MAX_LABEL_BYTE_LENGTH = 16;

    private static InputFilter[] nodeLabelFilter = new InputFilter[] { new InputFilter() {

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            CharSequence before = dest.subSequence(0, dstart);
            CharSequence after = dest.subSequence(dend, dest.length());
            int availableBytes = MAX_LABEL_BYTE_LENGTH - (getByteLength(before.toString()) + getByteLength(after.toString()));
            if (availableBytes <= 0) {
                // do not accept replacement
                return "";
            } else if (availableBytes >= getByteLength(source.subSequence(start, end).toString())) {
                // accept replacement completely
                return null;
            } else {
                // decide how big part of source/replacement do we want to keep
                StringBuilder sb = new StringBuilder(source);
                // there will be at most availableBytes characters
                sb.setLength(availableBytes);
                // but some of the characters might be taking up more than 2 bytes
                while (getByteLength(sb.toString()) > availableBytes) {
                    // remove characters from the end
                    sb.setLength(sb.length() - 1);
                }
                return sb.toString();
            }
        }

        private int getByteLength(String str) {
            return str.getBytes().length;
        }

    }};

    public NodeDetailFragment() {
        super(FragmentType.NODE_DETAILS);
    }

    private void setToggleSaveButtonStateTextWatcher() {
        etNodeLabel.setFilters(nodeLabelFilter);
        for (EditText etPos : new EditText[] { etPosX, etPosY, etPosZ }) {
            etPos.setFilters(POSITION_INPUT_FILTERS);
        }
        for (EditText etSlavePos : new EditText[] {etSlavePosX, etSlavePosY, etSlavePosZ}) {
            etSlavePos.setFilters(SLAVE_POSITION_INPUT_FILTERS);
        }
        for (EditText etMasterPos : new EditText[] {etMasterPosX, etMasterPosY, etMasterPosZ}) {
            etMasterPos.setFilters(MASTER_POSITION_INPUT_FILTERS);
        }
        etMasterAssoc.setFilters(MASTER_ASSOC_INPUT_FILTERS);
        etSlaveAssoc.setFilters(SLAVE_ASSOC_INPUT_FILTERS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.nodeTypeSelector)
    void onNodeTypeClicked() {
        NodeTypePickerDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(), selectedNodeType, networkNodeManager.getActiveNetwork());
    }

    @OnClick(R.id.uwbModeSelector)
    void onUwbModeClicked() {
        UwbModePickerDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(), selectedUwbMode);
    }

    @OnClick(R.id.updateRateSelector)
    void onUpdateRateClicked() {
        UpdateRatePickerDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(),
                selectedUpdateRate, true);
    }

    @OnClick(R.id.slaveMasterSideSelector)
    void onSlaveMasterSideClicked() {
        SlaveMasterSidePickerDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(), selectedSlaveMasterSide);
    }

    @OnClick(R.id.stationaryUpdateRateSelector)
    void onStationaryUpdateRateClicked() {
        UpdateRatePickerDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(),
                selectedStationaryUpdateRate, false);
    }

    void setUpdateRateEnabled(boolean enable) {
        updateRateSelector.setEnabled(enable);
        tvUpdateRate.setEnabled(enable);
        arrowUpdateRate.setEnabled(enable);
        updateRateLabel.setEnabled(enable);
    }

    void setStationaryUpdateRateEnabled(boolean enable) {
        stationaryUpdateRateSelector.setEnabled(enable);
        tvStationaryUpdateRate.setEnabled(enable);
        arrowStationaryUpdateRate.setEnabled(enable);
        stationaryUpdateRateLabel.setEnabled(enable);
    }

    @OnClick(R.id.networkNameSelector)
    void onNetworkClicked() {
        if (selectedNetworkId != null) {
            NetworkPickerDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(),
                    selectedNetworkId);
        } else {
            // this is unknown network for us
            if (networkNodeManager.getNetworks().isEmpty()) {
                // there are no networks
                NewNetworkNameDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(),
                        selectedNewNetworkName, null, false);
            } else {
                // there are some networks
                NetworkPickerDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(),
                        selectedNewNetworkName);
            }
        }
    }

    private void showErrorHideProgress(String text) {
        // disconnect is performed automatically
        ToastUtil.showToast(text, Toast.LENGTH_LONG);
        handleFrames();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retrieve the original/to be modified node in each case
        Bundle args = getArguments();
        Log.d("NodeDetailGetArgs", "onCreate: " + args.toString());
        if (Constants.DEBUG) {
            Log.d("Debug Mode", "onCreate: " + args.toString());
            Preconditions.checkNotNull(args, "must specify node details to show!");
            Preconditions.checkState(args.getLong(BK_NODE_ID, 0) != 0, "must specify node ID in args!");
        }

        nodeId = args.getLong(BK_NODE_ID);
        NetworkNodeEnhanced nne = networkNodeManager.getNode(nodeId);
        Preconditions.checkNotNull(nne, "must specify valid node ID in arguments!");
        // create a copy of the node (so that it doesn't change in a non-controlled way)
        inputNode = NodeFactory.newNodeCopy(nne.asPlainNode());
        // check saved instance first
        if (savedInstanceState != null && savedInstanceState.containsKey(BK_PAUSED)) {
            // do not load data, they will be remembered by input elements
            // and we have to restore internal state manually...
            if (savedInstanceState.containsKey(BK_SELECTED_NETWORK_ID)) {
                selectedNetworkId = savedInstanceState.getShort(BK_SELECTED_NETWORK_ID);
            } else if (savedInstanceState.containsKey(BK_SELECTED_NEW_NETWORK_NAME)) {
                selectedNewNetworkName = savedInstanceState.getString(BK_SELECTED_NEW_NETWORK_NAME);
            }
            if (savedInstanceState.containsKey(BK_SELECTED_UPDATE_RATE)) {
                selectedUpdateRate = UpdateRate.valueOf(savedInstanceState.getString(BK_SELECTED_UPDATE_RATE));
            }
            if (savedInstanceState.containsKey(BK_SELECTED_STATIONARY_UPDATE_RATE)) {
                selectedStationaryUpdateRate = UpdateRate.valueOf(savedInstanceState.getString(BK_SELECTED_STATIONARY_UPDATE_RATE));
            }
            if (savedInstanceState.containsKey(BK_SELECTED_NODE_TYPE)) {
                selectedNodeType = NodeType.valueOf(savedInstanceState.getString(BK_SELECTED_NODE_TYPE));
            }
            if (savedInstanceState.containsKey(BK_SELECTED_UWB_MODE)) {
                selectedUwbMode = UwbMode.valueOf(savedInstanceState.getString(BK_SELECTED_UWB_MODE));
            }
            if (savedInstanceState.containsKey(BK_SELECTED_SLAVE_MASTER_SIDE)) {
                selectedSlaveMasterSide = SlaveMasterSide.valueOf(savedInstanceState.getString(BK_SELECTED_SLAVE_MASTER_SIDE));
            }
            if (savedInstanceState.containsKey(BK_RAW_UPDATE_RATE)) {
                rawUpdateRate = savedInstanceState.getInt(BK_RAW_UPDATE_RATE);
            }
            if (savedInstanceState.containsKey(BK_ORIG_POS_X)) {
                origPosX = savedInstanceState.getString(BK_ORIG_POS_X);
            }
            if (savedInstanceState.containsKey(BK_ORIG_POS_Y)) {
                origPosY = savedInstanceState.getString(BK_ORIG_POS_Y);
            }
            if (savedInstanceState.containsKey(BK_ORIG_POS_Z)) {
                origPosZ = savedInstanceState.getString(BK_ORIG_POS_Z);
            }
            if (savedInstanceState.containsKey(BK_ORIG_SLAVE_POS_X)) {
                origSlavePosX = savedInstanceState.getString(BK_ORIG_SLAVE_POS_X);
            }
            if (savedInstanceState.containsKey(BK_ORIG_SLAVE_POS_Y)) {
                origSlavePosY = savedInstanceState.getString(BK_ORIG_SLAVE_POS_Y);
            }
            if (savedInstanceState.containsKey(BK_ORIG_SLAVE_POS_Z)) {
                origSlavePosZ = savedInstanceState.getString(BK_ORIG_SLAVE_POS_Z);
            }
            if (savedInstanceState.containsKey(BK_ORIG_SLAVE_ASSOC)) {
                origSlaveAssocId = savedInstanceState.getString(BK_ORIG_SLAVE_ASSOC);
            }
            if (savedInstanceState.containsKey(BK_ORIG_MASTER_POS_X)) {
                origMasterPosX = savedInstanceState.getString(BK_ORIG_MASTER_POS_X);
            }
            if (savedInstanceState.containsKey(BK_ORIG_MASTER_POS_Y)) {
                origMasterPosY = savedInstanceState.getString(BK_ORIG_MASTER_POS_Y);
            }
            if (savedInstanceState.containsKey(BK_ORIG_MASTER_POS_Z)) {
                origMasterPosZ = savedInstanceState.getString(BK_ORIG_MASTER_POS_Z);
            }
            if (savedInstanceState.containsKey(BK_ORIG_MASTER_ASSOC)) {
                origMasterAssocId = savedInstanceState.getString(BK_ORIG_MASTER_ASSOC);
            }
            if (chboxResponsiveMode != null) {
                // set up enabled/disabled state
                enableDisableDependentControls();
            } // else: oncreateview has not been called yet
            fillUi = false;
        } else {
            fillUi = true;
            // reset the node task
            if (updateNodeTask != null) {
                // let the existing task know, that we are not interested in results (of IH) anymore
                updateNodeTask.cancel();
            }
            updateNodeTask = null;
        }
        // we need options menu (save button)
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_notedetail, menu);
        menu.findItem(R.id.action_save).setOnMenuItemClickListener(menuItem -> onSaveClick());
    }

    private void fromNodeToUiElements() {
        //
        etNodeLabel.setText(inputNode.getLabel());
        tvNodeId.setText(Util.formatAsHexa(nodeId, true));
        tvNodeBleAddress.setText(inputNode.getBleAddress());
        chboxFirmwareUpdate.setChecked(asBoolean(inputNode.isFirmwareUpdateEnable()));
        chboxLedIndication.setChecked(asBoolean(inputNode.isLedIndicationEnable()));
        chboxBleEnabled.setChecked(inputNode.isBleEnable());
        Short networkId = inputNode.getNetworkId();
        if (networkId != null) {
            NetworkModel networkModel = networkNodeManager.getNetworks().get(networkId);
            if (networkModel != null) {
                // propagate the selected network ID only if it is a known network
                selectedNetworkId = networkId;
                tvNetworkName.setText(networkModel.getNetworkName());
            } else {
                selectedNetworkId = null;
                tvNetworkName.setText(getString(R.string.unknown_network, Util.formatNetworkId(networkId)));
            }
        } else {
            tvNetworkName.setText(R.string.not_set);
        }
        // operation mode
        selectedNodeType = inputNode.getType();
        selectedUwbMode = inputNode.getUwbMode();
        tvNodeType.setText(Util.nodeTypeString(selectedNodeType, networkNodeManager.getActiveNetwork()));
        if (selectedUwbMode != null) {
            tvUwbMode.setText(Util.formatUwbMode(selectedUwbMode));
        } else {
            tvUwbMode.setText(R.string.value_not_known_dropdown);
        }
        if (inputNode.isAnchor()) {
            if (networkNodeManager.getActiveNetwork().getNetworkOperationMode() == NetworkOperationMode.POSITIONING) {
                // anchor specific
                AnchorNode anchor = (AnchorNode) inputNode;
                chboxInitiator.setChecked(anchor.isInitiator());
                // we do not need a deep copy
                Position position = anchor.extractPositionDirect();
                if (position != null) {
                    LengthUnit lengthUnit = appPreferenceAccessor.getLengthUnit();
                    origPosX = Util.formatLength(position.x, lengthUnit);
                    origPosY = Util.formatLength(position.y, lengthUnit);
                    origPosZ = Util.formatLength(position.z, lengthUnit);
                    Log.d("bytearrayencodedecode", "fromNodeToUiElements: " +
                            origPosX + ";" +
                            origPosY + ";" +
                            origPosZ + ";");
                    //
                    etPosX.setText(origPosX);   // anchor origin pos input
                    etPosY.setText(origPosY);   // anchor origin pos input
                    etPosZ.setText(origPosZ);   // anchor origin pos input
                }
                // when we switch to TAG we want to have all the checkboxes checked
                chboxAccelerometer.setChecked(true);
                chboxLocationEngine.setChecked(true);
                chboxResponsiveMode.setChecked(true);
                // .. and have default update rates selected
                selectedUpdateRate = UpdateRate.DEFAULT;
                selectedStationaryUpdateRate = UpdateRate.DEFAULT;
            }
            if (networkNodeManager.getActiveNetwork().getNetworkOperationMode() == NetworkOperationMode.RANGING) {
                // slave specific
                AnchorNode anchor = (AnchorNode) inputNode;
                chboxInitiator.setChecked(true);
                // we do not need a deep copy
                SlaveInformativePosition slaveInfoPosition = anchor.extractSlaveInfoPositionDirect();
                if (slaveInfoPosition != null) {
                    // LengthUnit lengthUnit = appPreferenceAccessor.getLengthUnit(); // res. for future unit flexibility
                    origSlavePosX = String.valueOf(slaveInfoPosition.getX()); // slave units in cm
                    origSlavePosY = String.valueOf(slaveInfoPosition.getY()); // slave units in cm
                    origSlavePosZ = String.valueOf(slaveInfoPosition.getZ()); // slave units in cm
                    origSlaveAssocId = slaveInfoPosition.getAssocId().toString();
                    etSlavePosX.setText(origSlavePosX);     // slave origin pos input
                    etSlavePosY.setText(origSlavePosY);     // slave origin pos input
                    etSlavePosZ.setText(origSlavePosZ);     // slave origin pos input
                    etSlaveAssoc.setText(origSlaveAssocId); // slave association id input
                    selectedSlaveMasterSide = slaveInfoPosition.getSlaveSide(); // slave side selector
                }
                if (selectedSlaveMasterSide != null) {
                    tvSlaveMasterSide.setText(Util.formatSlaveMasterSide(selectedSlaveMasterSide));
                } else {
                    tvSlaveMasterSide.setText(R.string.slave_master_side_unknown);
                }
                // when we switch to Master we want to have all the checkboxes checked
                chboxAccelerometer.setChecked(true);
                chboxLocationEngine.setChecked(true);
                chboxResponsiveMode.setChecked(true);
                // .. and have default update rates selected
                selectedUpdateRate = UpdateRate.DEFAULT;
                selectedStationaryUpdateRate = UpdateRate.DEFAULT;
            }
        } else if (inputNode.isTag()) {
            // tag specific
            TagNode tag = (TagNode) this.inputNode;
            Integer updateRate = tag.getUpdateRate();
            chboxAccelerometer.setChecked(asBoolean(tag.isAccelerometerEnable()));
            selectedUpdateRate = updateRate == null ? null : UpdateRate.getUpdateRateForValue(updateRate);
            if (selectedUpdateRate == null && updateRate != null) {
                rawUpdateRate = updateRate;
            }
            Integer stationaryUpdateRate = tag.getStationaryUpdateRate();
            selectedStationaryUpdateRate = stationaryUpdateRate == null ? null : UpdateRate.getUpdateRateForValue(stationaryUpdateRate);
            if (selectedStationaryUpdateRate == null && stationaryUpdateRate != null) {
                rawStationaryUpdateRate = stationaryUpdateRate;
            }
            chboxLocationEngine.setChecked(asBoolean(tag.isLocationEngineEnable()));
            chboxResponsiveMode.setChecked(!asBoolean(tag.isLowPowerModeEnable()));
            if (networkNodeManager.getActiveNetwork().getNetworkOperationMode() == NetworkOperationMode.RANGING) {
                MasterInformativePosition masterInfoPosition = tag.extractMasterInfoPositionDirect();

                if (masterInfoPosition != null) {
                    // LengthUnit lengthUnit = appPreferenceAccessor.getLengthUnit(); // res. for future unit flexibility
                    origMasterPosX = String.valueOf(masterInfoPosition.getX()); // master units in cm
                    origMasterPosY = String.valueOf(masterInfoPosition.getY()); // master units in cm
                    origMasterPosZ = String.valueOf(masterInfoPosition.getZ()); // master units in cm
                    origMasterAssocId = masterInfoPosition.getAssocId().toString();
                    etMasterPosX.setText(origMasterPosX);     // master origin pos input
                    etMasterPosY.setText(origMasterPosY);     // master origin pos input
                    etMasterPosZ.setText(origMasterPosZ);     // master origin pos input
                    etMasterAssoc.setText(origMasterAssocId); // master association id input
                    selectedSlaveMasterSide = masterInfoPosition.getMasterSide(); // master side selector
                }

                if (selectedSlaveMasterSide != null) {
                    tvSlaveMasterSide.setText(Util.formatSlaveMasterSide(selectedSlaveMasterSide));
                } else {
                    tvSlaveMasterSide.setText(R.string.slave_master_side_unknown);
                }
                // when we switch to Slave we want to have all the checkboxes checked
                chboxInitiator.setChecked(true);
            }
        }
    }

    private static boolean asBoolean(Boolean value) {
        return value == null ? false : value;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_node_details_frame, container, false);
        // extract references to important elements
        unbinder = ButterKnife.bind(this, v);
        //
        if (fillUi) {
            fromNodeToUiElements();
        }
        // set up checked changed listener
        chboxAccelerometer.setOnCheckedChangeListener((compoundButton, b) -> onAccelerometerChange());
        //
        chboxBleEnabled.setOnCheckedChangeListener((chbox,val) -> {
            setBleStyle();
        });
        // set up enabled/disabled status of controls
        enableDisableDependentControls();
        // do not fill the UI from the input node the next time
        setToggleSaveButtonStateTextWatcher();
        //
        adjustNodeTypeSpecificViews();
        configureNodeNameEditText();
        //
        return v;
    }

    public void setBleStyle() {
        boolean val = chboxBleEnabled.isChecked();
        if (val) {
            // make the text normal
            Util.applyStyle(getMainActivity(), chboxBleEnabled, R.style.NodeDetailPropertyTitle);
        } else {
            // make the text WARN
            Util.applyStyle(getMainActivity(), chboxBleEnabled, R.style.NodeDetailPropertyTitleWarn);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // remove cancel update first
        uiHandler.removeCallbacks(cancelUpdateRunnable);
        // do the rest
        handleFrames();
        // handle TV network name content
        if (selectedNetworkId != null) {
            tvNetworkName.setText(networkNodeManager.getNetworks().get(selectedNetworkId).getNetworkName());
        } else if (selectedNewNetworkName != null) {
            tvNetworkName.setText(selectedNewNetworkName);
        } else {
            tvNetworkName.setText(R.string.not_set);
        }
        //
        fillTvUpdateRate();
        fillTvStationaryUpdateRate();
        setBleStyle();
        tvPositionTitle.setText(daApp.getString(R.string.node_detail_position, daApp.getString(appPreferenceAccessor.getLengthUnit().unitLabelResource)));
        tvSlavePosTitle.setText(daApp.getString(R.string.slave_master_detail_config));
        InterfaceHub.registerHandler(this);
    }

    private void handleFrames() {
        if (updateNodeTask != null && updateNodeTask.isRunning()) {
            progressFrame.setVisibility(View.VISIBLE);
        } else {
            progressFrame.setVisibility(View.GONE);
        }
    }

    private void fillTvUpdateRate() {
        setUpdateRateGeneric(selectedUpdateRate, tvUpdateRate, rawUpdateRate);
    }

    private void fillTvStationaryUpdateRate() {
        setUpdateRateGeneric(selectedStationaryUpdateRate, tvStationaryUpdateRate, rawStationaryUpdateRate);
    }

    private void setUpdateRateGeneric(UpdateRate selectedUpdateRate,
                                      TextView tvUpdateRate, Integer rawUpdateRate) {
        if (selectedUpdateRate != null) {
            tvUpdateRate.setText(updateRateAsStringNullSafe(selectedUpdateRate));
        } else if (rawUpdateRate != null) {
            // simply show the raw update rate
            tvUpdateRate.setText(daApp.getString(R.string.millisecond, rawUpdateRate));
        } else {
            tvUpdateRate.setText(R.string.update_rate_default);
        }
    }

    private int updateRateAsStringNullSafe(UpdateRate ur) {
        if (ur == null) {
            return R.string.unsupported_update_rate_value;
        } else {
            return ur.text;
        }
    }

    private boolean onSaveClick() {
        // √ check sign on top right menu
        // for anchor, we have to consider selected position
        boolean positionAncInputOk;
        boolean slaveConfigValuesNotEmpty, masterConfigValuesNotEmpty;
        boolean slaveConfigValuesInputOk, masterConfigValuesInputOk;
        boolean slaveConfigValuesRangeOk, masterConfigValuesRangeOk;

        NetworkOperationMode networkMode = networkNodeManager.getActiveNetwork().getNetworkOperationMode();
        if (selectedNodeType == NodeType.ANCHOR) {
            if (networkMode == NetworkOperationMode.POSITIONING) {
                Editable posX = etPosX.getText();
                Editable posY = etPosY.getText();
                Editable posZ = etPosZ.getText();

                int posXl = posX.length();
                int posYl = posY.length();
                int posZl = posZ.length();
                positionAncInputOk = posXl == 0 && posYl == 0 && posZl == 0
                        || (posXl > 0 && posYl > 0 && posZl > 0
                        && !posX.toString().equals("-.") && !posY.toString().equals("-.") && !posZ.toString().equals("-.")
                        && !posX.toString().equals("-") && !posY.toString().equals("-") && !posZ.toString().equals("-")
                        && !posX.toString().equals(".") && !posY.toString().equals(".") && !posZ.toString().equals("."));


                if (!positionAncInputOk) {
                    ToastUtil.showToast(R.string.node_detail_position_input_invalid, Toast.LENGTH_LONG);
                    return false;
                }
            }
            else if (networkMode == NetworkOperationMode.RANGING) {
                Editable slavePosX = etSlavePosX.getText();
                Editable slavePosY = etSlavePosY.getText();
                Editable slavePosZ = etSlavePosZ.getText();
                Editable slaveAssoc = etSlaveAssoc.getText();
                SlaveMasterSide slaveMasterside = selectedSlaveMasterSide;

                int posSlaveXl = slavePosX.length();
                int posSlaveYl = slavePosY.length();
                int posSlaveZl = slavePosZ.length();
                int assocSlaveL = slaveAssoc.length();
                slaveConfigValuesNotEmpty = (posSlaveXl > 0 && posSlaveYl > 0 && posSlaveZl > 0 && assocSlaveL > 0 && slaveMasterside != null);
                slaveConfigValuesInputOk = posSlaveXl == 0 && posSlaveYl == 0 && posSlaveZl == 0 && assocSlaveL == 0
                        || (slaveConfigValuesNotEmpty);

                if (slaveConfigValuesNotEmpty) {
                    int inputSlaveX = Integer.valueOf(slavePosX.toString());
                    int inputSlaveY = Integer.valueOf(slavePosY.toString());
                    int inputSlaveZ = Integer.valueOf(slavePosZ.toString());
                    int inputSlaveAssoc = Integer.valueOf(slaveAssoc.toString());
                    int inputSlaveSide = Integer.valueOf(slaveMasterside.getValue());
                    slaveConfigValuesRangeOk = (inputSlaveX >= -32768 && inputSlaveX <= 32767)
                            && (inputSlaveY >= -32768 && inputSlaveY <= 32767)
                            && (inputSlaveZ >= 0 && inputSlaveZ <= 65535)
                            && (inputSlaveAssoc >= 0 && inputSlaveAssoc <= 255);
                    if (!slaveConfigValuesRangeOk) {
                        ToastUtil.showToast(R.string.slave_input_exceeds_range, Toast.LENGTH_LONG);
                        return false;
                    }
                    if (inputSlaveSide == SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE) {
                        ToastUtil.showToast(R.string.slave_master_must_select_side, Toast.LENGTH_LONG);
                        return false;
                    }
                }
                else if (!slaveConfigValuesInputOk) {
                    ToastUtil.showToast(R.string.slave_input_invalid, Toast.LENGTH_LONG);
                    return false;
                }
            }
            else if (selectedNetworkId == null && selectedNewNetworkName == null) {
                ToastUtil.showToast(R.string.node_detail_must_selected_network, Toast.LENGTH_LONG);
                return false;
            } else if (etNodeLabel.getText().length() == 0) {
                ToastUtil.showToast(R.string.node_detail_empty_node_label, Toast.LENGTH_LONG);
                return false;
            }
        }
        if (selectedNodeType == NodeType.TAG){
            if (networkMode == NetworkOperationMode.RANGING) {
                Editable masterPosX = etMasterPosX.getText();
                Editable masterPosY = etMasterPosY.getText();
                Editable masterPosZ = etMasterPosZ.getText();
                Editable masterAssoc = etMasterAssoc.getText();
                SlaveMasterSide slaveMasterside = selectedSlaveMasterSide;

                int posMasterXl = masterPosX.length();
                int posMasterYl = masterPosY.length();
                int posMasterZl = masterPosZ.length();
                int assocMasterL = masterAssoc.length();
                masterConfigValuesNotEmpty = (posMasterXl > 0 && posMasterYl > 0 && posMasterZl > 0 && assocMasterL > 0 && slaveMasterside != null);
                masterConfigValuesInputOk = posMasterXl == 0 && posMasterYl == 0 && posMasterZl == 0 && assocMasterL == 0
                        || (masterConfigValuesNotEmpty);

                if (masterConfigValuesNotEmpty) {
                    int inputMasterX = Integer.valueOf(masterPosX.toString());
                    int inputMasterY = Integer.valueOf(masterPosY.toString());
                    int inputMasterZ = Integer.valueOf(masterPosZ.toString());
                    int inputMasterAssoc = Integer.valueOf(masterAssoc.toString());
                    int inputMasterSide = Integer.valueOf(slaveMasterside.getValue());
                    masterConfigValuesRangeOk = (inputMasterX >= -32768 && inputMasterX <= 32767)
                            && (inputMasterY >= -32768 && inputMasterY <= 32767)
                            && (inputMasterZ >= 0 && inputMasterZ <= 65535)
                            && (inputMasterAssoc >= 0 && inputMasterAssoc <= 255);
                    if (!masterConfigValuesRangeOk) {
                        ToastUtil.showToast(R.string.master_input_exceeds_range, Toast.LENGTH_LONG);
                        return false;
                    }
                    if (inputMasterSide == SlaveMasterSide.Constants.UNKNOWN_SIDE_VALUE) {
                        ToastUtil.showToast(R.string.slave_master_must_select_side, Toast.LENGTH_LONG);
                        return false;
                    }
                }
                else if (!masterConfigValuesInputOk) {
                    ToastUtil.showToast(R.string.master_input_invalid, Toast.LENGTH_LONG);
                    return false;
                }
            }
            else if (selectedNetworkId == null && selectedNewNetworkName == null) {
                ToastUtil.showToast(R.string.node_detail_must_selected_network, Toast.LENGTH_LONG);
                return false;
            } else if (etNodeLabel.getText().length() == 0) {
                ToastUtil.showToast(R.string.node_detail_empty_node_label, Toast.LENGTH_LONG);
                return false;
            }
        }
        // do the save if reaches here
        hideKeyboard();
        updateNodeTask = new UpdateNodeTask(networkNodeManager, bleConnectionApi);
        updateNodeTask.doUpdate(inputNode, selectedNodeType,
                selectedUwbMode, chboxInitiator.isChecked(),
                chboxFirmwareUpdate.isChecked(), chboxAccelerometer.isChecked(), chboxLedIndication.isChecked(),
                chboxBleEnabled.isChecked(), chboxLocationEngine.isChecked(), !chboxResponsiveMode.isChecked(),
                selectedNewNetworkName, selectedNetworkId,
                etNodeLabel.getText().toString(), selectedUpdateRate, selectedStationaryUpdateRate,
                origPosX, origPosY, origPosZ,
                etPosX.getText().toString(), etPosY.getText().toString(), etPosZ.getText().toString(), appPreferenceAccessor.getLengthUnit(),
                origMasterPosX, origMasterPosY, origMasterPosZ, origMasterAssocId,
                etMasterPosX.getText().toString(), etMasterPosY.getText().toString(), etMasterPosZ.getText().toString(), etMasterAssoc.getText().toString(),
                origSlavePosX, origSlavePosY, origSlavePosZ, origSlaveAssocId,
                etSlavePosX.getText().toString(), etSlavePosY.getText().toString(), etSlavePosZ.getText().toString(), etSlaveAssoc.getText().toString(),
                selectedSlaveMasterSide);
        return true;

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // save the state
        outState.putString(BK_PAUSED, "true");
        outState.putLong(BK_NODE_ID, nodeId);
        outState.putString(BK_SELECTED_NODE_TYPE, selectedNodeType.name());
        outState.putString(BK_SELECTED_UWB_MODE, selectedUwbMode.name());
        outState.putString(BK_SELECTED_SLAVE_MASTER_SIDE, selectedSlaveMasterSide.name());
        if (selectedNewNetworkName != null) {
            outState.putString(BK_SELECTED_NEW_NETWORK_NAME, selectedNewNetworkName);
        } else if (selectedNetworkId != null) {
            outState.putShort(BK_SELECTED_NETWORK_ID, selectedNetworkId);
        }
        if (selectedUpdateRate != null) {
            outState.putString(BK_SELECTED_UPDATE_RATE, selectedUpdateRate.name());
        }
        if (selectedStationaryUpdateRate != null) {
            outState.putString(BK_SELECTED_STATIONARY_UPDATE_RATE, selectedStationaryUpdateRate.name());
        }
        if (rawUpdateRate != null) {
            outState.putInt(BK_RAW_UPDATE_RATE, rawUpdateRate);
        }
        if (origPosX != null) {
            outState.putString(BK_ORIG_POS_X, origPosX);
        }
        if (origPosY != null) {
            outState.putString(BK_ORIG_POS_Y, origPosY);
        }
        if (origPosZ != null) {
            outState.putString(BK_ORIG_POS_Z, origPosZ);
        }
        if (origSlavePosX != null) {
            outState.putString(BK_ORIG_SLAVE_POS_X, origSlavePosX);
        }
        if (origSlavePosY != null) {
            outState.putString(BK_ORIG_SLAVE_POS_Y, origSlavePosY);
        }
        if (origSlavePosZ != null) {
            outState.putString(BK_ORIG_SLAVE_POS_Z, origSlavePosZ);
        }
        if (origSlaveAssocId != null) {
            outState.putString(BK_ORIG_SLAVE_ASSOC, origSlaveAssocId);
        }
        if (origMasterPosX != null) {
            outState.putString(BK_ORIG_MASTER_POS_X, origMasterPosX);
        }
        if (origMasterPosY != null) {
            outState.putString(BK_ORIG_MASTER_POS_Y, origMasterPosY);
        }
        if (origMasterPosZ != null) {
            outState.putString(BK_ORIG_MASTER_POS_Z, origMasterPosZ);
        }
        if (origMasterAssocId != null) {
            outState.putString(BK_ORIG_MASTER_ASSOC, origMasterAssocId);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        InterfaceHub.unregisterHandler(this);
        uiHandler.postDelayed(cancelUpdateRunnable, 200);
    }

    public static Bundle getArgumentsForActiveNetworkNode(long nodeId) {
        Bundle b = new Bundle();
        b.putLong(BK_NODE_ID, nodeId);
        return b;
    }

    private void onNewNetwork(String networkName) {
        selectedNewNetworkName = networkName;
        tvNetworkName.setText(networkName);
        selectedNetworkId = null;
    }

    private void configureNodeNameEditText() {
        etNodeLabel.requestFocus();
        hideKeyboard();
        etNodeLabel.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard();
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) daApp.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etNodeLabel != null) {
            imm.hideSoftInputFromWindow(etNodeLabel.getWindowToken(), 0);
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // IH (interface hub) listening methods
    // Implementation on abstract interfaces for various interfaces: e.g. update rate, node type, etc.
    //

    @Override
    public void onUpdateRatePicked(UpdateRate updateRate, boolean flag) {
        if (flag) {
            // normal/regular
            selectedUpdateRate = updateRate;
            rawUpdateRate = null;
            fillTvUpdateRate();
        } else {
            // stationary
            selectedStationaryUpdateRate = updateRate;
            rawStationaryUpdateRate = null;
            fillTvStationaryUpdateRate();
        }
    }

    @Override
    public void onNodeTypePicked(NodeType nodeType) {
        selectedNodeType = nodeType;
        tvNodeType.setText(Util.nodeTypeString(nodeType, networkNodeManager.getActiveNetwork()));
        adjustNodeTypeSpecificViews();
    }

    @Override
    public void onUwbModePicked(UwbMode uwbMode) {
        selectedUwbMode = uwbMode;
        tvUwbMode.setText(Util.formatUwbMode(uwbMode));
        enableDisableDependentControls();
    }

    @Override
    public void onSlaveMasterSidePicked(SlaveMasterSide slaveMasterSide) {
        selectedSlaveMasterSide = slaveMasterSide;
        tvSlaveMasterSide.setText(Util.formatSlaveMasterSide(slaveMasterSide));
    }

    private void adjustNodeTypeSpecificViews() {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(selectedNodeType);
        }
        // first set the hint
        Short networkId = inputNode.getNetworkId();
        NetworkModel networkModel = networkNodeManager.getNetworks().get(networkId);
        final boolean isAnchor = selectedNodeType == NodeType.ANCHOR;
        final boolean isTag = selectedNodeType == NodeType.TAG;
        final boolean isRanging = networkModel.getNetworkOperationMode() == NetworkOperationMode.RANGING;
        etNodeLabel.setHint(isAnchor ? R.string.anchor_label : R.string.tag_label);
        etNodeLabel.setEnabled(true); // set enabled for all, except for masters (set within conditions below)
        if(isAnchor) {
            for (View v : tagSpecificViews) {
                v.setVisibility(View.GONE);
            }
            for (View v : masterSpecificViews) {
                v.setVisibility(View.GONE);
            }
            if (isRanging) {
                for (View v : anchorSpecificViews) {
                    v.setVisibility(View.GONE);
                }
                for (View v : slaveSpecificViews) {
                    v.setVisibility(View.VISIBLE);
                }
            }
            else {
                for (View v : slaveSpecificViews) {
                    v.setVisibility(View.GONE);
                }
                for (View v : anchorSpecificViews) {
                    v.setVisibility(View.VISIBLE);
                }
            }
        }
        if (isTag) {
            for (View v : anchorSpecificViews) {
                v.setVisibility(View.GONE);
            }
            for (View v : slaveSpecificViews) {
                v.setVisibility(View.GONE);
            }
            if (isRanging) {
                for (View v : tagSpecificViews) {
                    v.setVisibility(View.GONE);
                }
                for (View v : masterSpecificViews) {
                    v.setVisibility(View.VISIBLE);
                }
                etNodeLabel.setEnabled(false);
            }
            else {
                for (View v : masterSpecificViews) {
                    v.setVisibility(View.GONE);
                }
                for (View v : tagSpecificViews) {
                    v.setVisibility(View.VISIBLE);
                }
            }
        }
    }


    @Override
    public void onUpdatePerformed(NetworkNode node) {
        ToastUtil.showToast(daApp.getString(R.string.node_edit_successfully_updated));
        // let the network node manager know
        networkNodeManager.onNodeIntercepted(node);
        // hide the fragment (just go one level up)
        dismiss();
    }


    @Override
    public void onNoChangeDetected() {
        ToastUtil.showToast(daApp.getString(R.string.node_edit_no_change_detected));
        dismiss();
    }

    @Override
    public void onUpdateFailed() {
        showErrorHideProgress(daApp.getString(R.string.node_edit_update_failed));
    }

    @Override
    public void onUpdateStarted() {
        // start rotating the progressFrame
        handleFrames();
    }

    @Override
    public void onNewNetworkName(Short networkId, String networkName) {
        onNewNetwork(networkName);
    }

    @Override
    public void onNewNetworkPicked(String networkName) {
        onNewNetwork(networkName);
    }

    @Override
    public void onNetworkPicked(short networkId) {
        tvNetworkName.setText(networkNodeManager.getNetworks().get(networkId).getNetworkName());
        selectedNetworkId = networkId;
        selectedNewNetworkName = null;
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    private void enableDisableDependentControls() {
        if (this.selectedUwbMode == null) {
            // we do not know - disable the controls for sure
            chboxResponsiveMode.setEnabled(false);
            chboxAccelerometer.setEnabled(false);
            chboxFirmwareUpdate.setEnabled(false);
            chboxLocationEngine.setEnabled(false);
            setUpdateRateEnabled(false);
            setStationaryUpdateRateEnabled(false);
        } else {
            // responsive mode must be modifiable in each case
            chboxResponsiveMode.setEnabled(true);
            // decide based on ACTIVE/OFF
            boolean b = selectedUwbMode == UwbMode.ACTIVE;
            chboxAccelerometer.setEnabled(b);
            chboxLocationEngine.setEnabled(b);
            setUpdateRateEnabled(b);
            onAccelerometerChange();
            chboxFirmwareUpdate.setEnabled(selectedUwbMode != UwbMode.OFF);
        }
    }

    private void onAccelerometerChange() {
        setStationaryUpdateRateEnabled(selectedUwbMode == UwbMode.ACTIVE && chboxAccelerometer.isChecked());
    }
}
