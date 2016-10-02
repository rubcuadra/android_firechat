package cuadra.places.Adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import cuadra.places.Fragments.FireNotes;
import cuadra.places.Fragments.PlaceholderFragment;

/**
 * Created by Ruben on 10/2/16.
 */

public class MainAdapter extends FragmentPagerAdapter
{

    public MainAdapter(FragmentManager fm)
    {
        super(fm);
    }

    @Override
    public Fragment getItem(int position)
    {
        switch(position)
        {
            case 0:
                return PlaceholderFragment.newInstance(1);
            case 1: //EN MEDIO
                return FireNotes.newInstance();
            case 2:
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
            case 0:
                return "Map";
            case 1:
                return "Notes";
            case 2:
                return "Other";
        }
        return null;
    }
}
