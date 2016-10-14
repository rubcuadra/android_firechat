package cuadra.places.Fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import static cuadra.places.Activities.MainActivity.LOCATION_PERMISSIONS;
import static cuadra.places.Activities.MainActivity.PERMISSIONS_LOCATION;
import static cuadra.places.Activities.MainActivity.askPermissions;
import static cuadra.places.Activities.MainActivity.hasPermissions;
import static cuadra.places.Fragments.FireNotes.IN_RADIUS_DISTANCE;
import static cuadra.places.Fragments.FireNotes.NOTES_LOCATIONS_CHILD;

/**
 * Created by Ruben on 10/5/16.
 */

public class CustomMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private static final double CDMX_LAT=19.427;
    private static final double CDMX_LNG=-99.16771;
    public static final int MAP_ZOOM = 16;
    private static final String LOGTAG = "MapFragment";
    private List<Marker> mMarkers;

    private GoogleMap mMap;
    private Context CONTEXT;
    private GeoQuery mGeoQuery;

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
        mMarkers = new ArrayList<Marker>();

        mListener.mapReady(mMap);

        if(hasPermissions(CONTEXT,LOCATION_PERMISSIONS))
        {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(false);
            mMap.getUiSettings().setCompassEnabled(false);
        }
        else
        {
            askPermissions(getActivity(),LOCATION_PERMISSIONS,PERMISSIONS_LOCATION);
        }
        mGeoQuery = (new GeoFire(FirebaseDatabase.getInstance().getReference().child(NOTES_LOCATIONS_CHILD)))
                .queryAtLocation(new GeoLocation
                                (mMap.getCameraPosition().target.latitude,mMap.getCameraPosition().target.latitude),
                        IN_RADIUS_DISTANCE);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(CDMX_LAT,CDMX_LNG),MAP_ZOOM-10));
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener()
        {
            @Override
            public void onCameraIdle()
            {
                mGeoQuery.setCenter(new GeoLocation(mMap.getCameraPosition().target.latitude,mMap.getCameraPosition().target.longitude));
            }
        });

        mGeoQuery.addGeoQueryEventListener(geoListener);

    }
    private GeoQueryEventListener geoListener = new GeoQueryEventListener()
    {
        @Override
        public void onKeyEntered(String key, GeoLocation location)
        {

            Marker m = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(location.latitude,location.longitude))
                                        .title(key)

                                      );
            m.setTag(key);
            mMarkers.add(m);
            //m.getId()
            //m.remove();
        }

        @Override
        public void onKeyExited(String key)
        {
            for (int i=0;i<mMarkers.size();++i)
            {
                if (mMarkers.get(i).getTag().equals(key))
                {
                    mMarkers.get(i).remove();
                    mMarkers.remove(i);
                    break;
                }
            }
        }

        @Override
        public void onKeyMoved(String key, GeoLocation location)
        {

        }

        @Override
        public void onGeoQueryReady()
        {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {}
    };
    public interface OnMapFragmentInteraction
    {
        void mapReady(GoogleMap gmap);
    }
}