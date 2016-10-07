package cuadra.places.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import cuadra.places.R;

import static cuadra.places.Activities.MainActivity.LOCATION_PERMISSIONS;
import static cuadra.places.Activities.MainActivity.PERMISSIONS_LOCATION;
import static cuadra.places.Activities.MainActivity.askPermissions;
import static cuadra.places.Activities.MainActivity.hasPermissions;

/**
 * Created by Ruben on 10/5/16.
 */

public class CustomMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private static final String LOGTAG = "MapFragment";
    private GoogleMap mMap;
    private Context CONTEXT;
    private Location mLocation;

    //INTERACTIONS
    private OnMapFragmentInteraction mListener;

    public CustomMapFragment() {}

    public static CustomMapFragment newInstance()
    {
        CustomMapFragment fragment = new CustomMapFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        CONTEXT = activity;
        if (activity instanceof OnMapFragmentInteraction)
        {
            mListener = (OnMapFragmentInteraction) CONTEXT;
        } else
        {
            throw new RuntimeException(CONTEXT.toString()+" must implement OnMapFragmentInteraction");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener=null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        mListener.mapReady(mMap);

        if(hasPermissions(CONTEXT,LOCATION_PERMISSIONS))
        {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.setMyLocationEnabled(true);
            /*
                LatLng sydney = new LatLng(19, -99.262761);
                mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Santa Fe"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                Log.v(LOGTAG, "Added Santa Fe");
             */
        }
        else
        {
            askPermissions(getActivity(),LOCATION_PERMISSIONS,PERMISSIONS_LOCATION);
        }
    }
    public interface OnMapFragmentInteraction
    {
        void mapReady(GoogleMap gmap);
    }
}