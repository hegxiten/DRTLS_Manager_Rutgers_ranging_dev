package com.decawave.argomanager.ui.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.decawave.argomanager.R;
import com.decawave.argomanager.components.GeofenceManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.struct.GeofenceItem;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.ui.layout.NpaLinearLayoutManager;
import com.decawave.argomanager.ui.listadapter.GeofenceSetupGfListAdapter;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GeofenceSetupFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GeofenceSetupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GeofenceSetupFragment extends DiscoveryProgressAwareFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // adapter + node list
    private GeofenceSetupGfListAdapter adapter;
    private Bundle savedAdapterState;

    public GeofenceSetupFragment() {
        // constructor
        super(FragmentType.GEOFENCE_SETUP);
    }

    @BindView(R.id.geofenceList)
    RecyclerView geofenceListRecyclerView;
    @BindView(R.id.geofenceDetailContentFrame)
    LinearLayout geofenceDetailContentFrame;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_geofence_setup, container, false);
        ButterKnife.bind(this, v);
        // Test Geofence Display in RecyclerView
        ((SimpleItemAnimator) geofenceListRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        geofenceListRecyclerView.setLayoutManager(new NpaLinearLayoutManager(getActivity()));
        geofenceListRecyclerView.setAdapter(adapter);
        return inflater.inflate(R.layout.fragment_geofence_setup, container, false);
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }
}
