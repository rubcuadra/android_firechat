package cuadra.places.Adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import cuadra.places.Fragments.FireNotes;
import cuadra.places.Fragments.PlaceholderFragment;

/**
 * Created by Ruben on 10/2/16.
 */

public class MainAdapter extends FragmentPagerAdapter
{
    public static final int MAP_POSITION = 0;
    public static final int FIRE_NOTES_POSITION = 1;
    public static final int FRAGMENT_POSITION = 2;

    public MainAdapter(FragmentManager fm)
    {
        super(fm);
    }

    @Override
    public Fragment getItem(int position)
    {
        switch(position)
        {
            case MAP_POSITION:
                return PlaceholderFragment.newInstance(1);
            case FIRE_NOTES_POSITION: //EN MEDIO
                return FireNotes.newInstance();
            case FRAGMENT_POSITION:
                return PlaceholderFragment.newInstance(3);
        }
        return null;
    }

    @Override
    public int getCount()
    {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        switch (position)
        {
            case MAP_POSITION:
                return "Map";
            case FIRE_NOTES_POSITION:
                return "Notes";
            case FRAGMENT_POSITION:
                return "Other";
        }
        return null;
    }
}
