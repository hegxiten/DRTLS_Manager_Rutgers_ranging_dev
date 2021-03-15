package com.decawave.argomanager.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.decawave.argo.api.struct.SlaveMasterSide;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.layout.NpaLinearLayoutManager;
import com.decawave.argomanager.util.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.kryl.android.appcompat.dialogs.AlertDialog;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;
import eu.kryl.android.common.log.ComponentLog;

/** Dialog to pick the side of master/slave devices */
public class SlaveMasterSidePickerDialogFragment extends DialogFragment {
    public static final ComponentLog log = new ComponentLog(SlaveMasterSidePickerDialogFragment.class);

    private static final String FRAGMENT_TAG = "slavemastersidepicker";

    private static final String BK_SELECTED_SLAVE_MASTER_SIDE = "selected";

    // ***************************
    // * INPUT
    // ***************************

    @SuppressWarnings("NullableProblems")
    @NotNull
    private SlaveMasterSide[] slaveMasterSides;

    @Nullable
    private SlaveMasterSide selectedSide;

    // ***************************
    // * OTHER
    // ***************************

    private AlertDialog dlg;
    private Adapter adapter;

    // ***************************
    // * CONSTRUCTOR
    // ***************************

    public SlaveMasterSidePickerDialogFragment() {
        slaveMasterSides = SlaveMasterSide.values();
    }

    public static Bundle getArgsForSlaveMasterSide(SlaveMasterSide slaveMasterSide) {
        Bundle b = new Bundle();
        if (slaveMasterSide != null) {
            b.putString(BK_SELECTED_SLAVE_MASTER_SIDE, slaveMasterSide.name());
        }
        return b;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (selectedSide != null) {
            bundle.putString(BK_SELECTED_SLAVE_MASTER_SIDE, selectedSide.name());
        }
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle b = bundle;
        if (b == null) {
            b = getArguments();
        }
        if (b != null) {
            if (b.containsKey(BK_SELECTED_SLAVE_MASTER_SIDE)) {
                setSelectedSlaveMasterSide(SlaveMasterSide.valueOf(b.getString(BK_SELECTED_SLAVE_MASTER_SIDE)));
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        adapter = new Adapter();
        @SuppressLint("InflateParams")
        final View content = LayoutInflater.from(getActivity()).inflate(R.layout.dlg_item_picker, null);
        final RecyclerView recyclerView = (RecyclerView) content.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        final RecyclerView.LayoutManager layoutManager = new NpaLinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        builder.setView(content);
        builder.setRemoveTopPadding(true);

        dlg = builder.create();

        return dlg;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static void showDialog(FragmentManager fm, @Nullable SlaveMasterSide selectedSlaveMasterSide) {
        final SlaveMasterSidePickerDialogFragment f = new SlaveMasterSidePickerDialogFragment();
        if (selectedSlaveMasterSide != null) {
            f.setArguments(getArgsForSlaveMasterSide(selectedSlaveMasterSide));
        }
        f.show(fm, SlaveMasterSidePickerDialogFragment.FRAGMENT_TAG);
    }

    // ***************************
    // * INNER CLASSES
    // ***************************

    /**
     * Adapter
     */
    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View v = inflater.inflate(R.layout.li_dlg_slave_master_side_picker, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.bind(slaveMasterSides[position]);
        }

        @Override
        public int getItemCount() {
            return slaveMasterSides.length;
        }

    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setSelectedSlaveMasterSide(@Nullable SlaveMasterSide slaveMasterSide) {
        boolean b = this.selectedSide != slaveMasterSide;
        this.selectedSide = slaveMasterSide;
        return b;
    }

    /**
     * View holder
     */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.radio)
        RadioButton rb;
        @BindView(R.id.tvSlaveMasterSide)
        TextView tvSlaveMasterSide;
        //
        private View container;
        // data bean
        SlaveMasterSide slaveMasterSide;

        ViewHolder(View v) {
            super(v);
            // extract references to visual elements
            ButterKnife.bind(this, v);
            // set up the listener
            v.setOnClickListener(this);
            container = v;
        }

        @Override
        public void onClick(View view) {
            SlaveMasterSide newSlaveMasterSide = (SlaveMasterSide) view.getTag();
            if (selectedSide != newSlaveMasterSide) {
                // broadcast and dismiss
                InterfaceHub.getHandlerHub(SlaveMasterSidePickerDialogFragment.IhCallback.class).onSlaveMasterSidePicked(newSlaveMasterSide);
                dismiss();
            }
        }

        void bind(SlaveMasterSide slaveMasterSide) {
            // assign data bean
            this.slaveMasterSide = slaveMasterSide;
            // set up visual elements content
            this.tvSlaveMasterSide.setText(Util.slaveMasterSideString(this.slaveMasterSide));
            // toggle radio button
            rb.setChecked(selectedSide == this.slaveMasterSide);
            //
            container.setTag(this.slaveMasterSide);
        }
    }

    /**
     * Interface for UI callback
     */
    public interface IhCallback extends InterfaceHubHandler {

        void onSlaveMasterSidePicked(SlaveMasterSide slaveMasterSide);

    }


}
