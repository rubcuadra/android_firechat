package cuadra.places.Fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import cuadra.places.R;

/**
 * Created by Ruben on 10/5/16.
 */

public class CustomMapFragment extends SupportMapFragment
{
    private Context CONTEXT;
    private OnMapFragmentReadyCallback mListener;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        Log.d("MAP","ATTACHASDHB");
        CONTEXT=activity;
        if (CONTEXT instanceof OnMapFragmentReadyCallback)
        {
            mListener = (OnMapFragmentReadyCallback) CONTEXT;
            mListener.OnMapFragmentReadyCallback(this);
        } else
        {
            throw new RuntimeException(CONTEXT.toString()+" must implement OnMapFragmentReadyCallback");
        }
    }

    public interface OnMapFragmentReadyCallback
    {
        void OnMapFragmentReadyCallback(CustomMapFragment mp);
    }
}
