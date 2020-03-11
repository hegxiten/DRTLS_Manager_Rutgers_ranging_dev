package com.decawave.argomanager.ui.listadapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.annimon.stream.function.Consumer;
import com.decawave.argomanager.R;
import com.decawave.argomanager.components.struct.GeofenceItem;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.util.ToastUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GeofenceSetupGfListAdapter extends RecyclerView.Adapter<GeofenceSetupGfListAdapter.ViewHolder> {

    private final MainActivity mainActivity;
    private final Consumer<Set<Short>> checkedChangedListener;
    private List<GeofenceItem> geofences;
    private Set<Short> checkGeofenceIds;

    public GeofenceSetupGfListAdapter(@NotNull Collection<GeofenceItem> geofenceItems,
                                      @NotNull MainActivity mainActivity,
                                      @NotNull Consumer<Set<Short>> checkedChangedListener) {
        this.mainActivity = mainActivity;
        this.checkedChangedListener = checkedChangedListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.li_geofence_item, parent, false);
        // wrap with view holder
        return new GeofenceListItemHolder(view);
    }

    abstract class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    class GeofenceListItemHolder extends ViewHolder {
        // references to views
        @BindView(R.id.geofenceCardTop)
        View geofenceCardTop;
        @BindView(R.id.geofenceCardContent)
        View geofenceCardContent;
        @BindView(R.id.geofenceCheckbox)
        CheckBox geofenceCheckBox;

        short geofenceItemId;
        String geofenceItemName;

        GeofenceListItemHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this, itemView);
            // set onclick listener
            itemView.findViewById(R.id.cardContent).setOnClickListener(view -> {
                ToastUtil.showToast("geofence item onclick!");
            });
            geofenceCheckBox.setOnCheckedChangeListener((button, isChecked) -> onGeofenceItemChecked(geofenceItemId, isChecked));
        }

        void bind(short geofenceId) {
            this.geofenceItemId = geofenceId;
            geofenceCheckBox.setChecked(checkGeofenceIds.contains(geofenceItemId));
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int pos) {
        if (holder instanceof GeofenceListItemHolder) {
            int p = pos - 1;
            ((GeofenceListItemHolder) holder).bind(geofences.get(p).getId());
        }
    }

    @Override
    public int getItemCount() {
        return geofences.size();
    }

    private void onGeofenceItemChecked(short geofenceId, boolean checked) {
        if (checked) {
            checkGeofenceIds.add(geofenceId);
        } else {
            checkGeofenceIds.remove(geofenceId);
        }
        if (checkedChangedListener != null) {
            checkedChangedListener.accept(checkGeofenceIds);
        }
    }

}

