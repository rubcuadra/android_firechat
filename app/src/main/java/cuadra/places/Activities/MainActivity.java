package cuadra.places.Activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.Map;

import cuadra.places.Adapters.MainAdapter;
import cuadra.places.CodelabPreferences;
import cuadra.places.Fragments.CustomMapFragment;
import cuadra.places.Fragments.FireNotes;
import cuadra.places.Models.AudioVoiceNote;
import cuadra.places.R;

import static android.R.drawable.ic_menu_add;
import static android.R.drawable.ic_menu_compass;
import static cuadra.places.Adapters.MainAdapter.FIRE_NOTES_POSITION;
import static cuadra.places.Adapters.MainAdapter.FRAGMENT_POSITION;
import static cuadra.places.Adapters.MainAdapter.MAP_POSITION;
import static cuadra.places.Adapters.MainAdapter.SECTIONS;

public class MainActivity extends AppCompatActivity implements
        FireNotes.OnFireNotesFragmentInteractionListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener,
        CustomMapFragment.OnMapFragmentInteraction
{
    //CONSTANTS
    private static final String TAG = "MainActivity";
    private static final int REQUEST_INVITE = 1;
    private static final int RECORD_INTENT = 2;
    private static final int PERMISSIONS_RECORD =3;
    public static final int PERMISSIONS_LOCATION =4;

    public static final String[] RECORD_PERMISSIONS =
            {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};
    public static final String[] LOCATION_PERMISSIONS =
            {Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION};
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    public static final int TITLE_LENGTH_LIMIT = 20;

    //Vars
    private MainAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    private int mCurrentFrag;
    private FloatingActionButton fab;
    private Context CONTEXT;
    private Drawable[] mfab_icons;
    private Drawable mAdd_icon;
    private Drawable mSend_icon;
    private Drawable mMenu_icon;
    private GoogleMap gMap;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));//SetToolbar
        CONTEXT = getApplicationContext();
        mSend_icon = ContextCompat.getDrawable(CONTEXT, R.drawable.ic_send_white_24dp);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(onFABClick);
        buildGoogleApiClient();
        setAuth();
        setFirebaseConfigs();
        //Environment.getExternalStorageDirectory().getAbsolutePath()+"/audiorecordtest.3gp";
        /*
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
         */
        setPager();
    }

    private void causeCrash()
    {
        throw new NullPointerException("Fake null pointer exception");
    }

    private void sendInvitation()
    {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        switch (requestCode)
        {
            case REQUEST_INVITE:
                if (resultCode == RESULT_OK)
                {
                    Bundle payload = new Bundle();
                    payload.putString(FirebaseAnalytics.Param.VALUE, "sent");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);
                    // Check how many invitations were sent and log.
                    String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                    Log.d(TAG, "Invitations sent: " + ids.length);
                } else
                {
                    Bundle payload = new Bundle();
                    payload.putString(FirebaseAnalytics.Param.VALUE, "not sent");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);
                    // Sending failed or it was canceled, show failure message to
                    // the user
                    Log.d(TAG, "Failed to send invitation.");
                }
                break;
            case RECORD_INTENT:
                if (resultCode == RESULT_OK)
                {
                    String mFileName = getfilePathFromAudioUri(data.getData());
                    Log.d(TAG,mFileName); //PUEDE SER null pero es raro el caso
                    FireNotes fn = (FireNotes) getFragmentAtPosition(FIRE_NOTES_POSITION);
                    fn.setFileName(mFileName);
                    fn.setLocation(mLastLocation);

                    mfab_icons[FIRE_NOTES_POSITION] = mSend_icon;
                    fab.setOnClickListener(onNextFABClick);
                    changeFABIcon();
                }
                else
                {
                    Bundle payload = new Bundle();
                    payload.putString(FirebaseAnalytics.Param.VALUE, "not recorded");
                    mFirebaseAnalytics.logEvent("Records", payload);
                    //No se guardo la grabacion o eso dijo la otra app
                    Log.d(TAG, "Failed to record audio.");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_tabbed_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        switch (item.getItemId())
        {
            case R.id.invite_menu:
                sendInvitation();
                return true;
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                //mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public void resetFABIcons()
    {
        mfab_icons[MAP_POSITION] = mMenu_icon;
        mfab_icons[FIRE_NOTES_POSITION] = mAdd_icon;
        //mfab_icons[FRAGMENT_POSITION] = mSync_icon
    }
    public void setPager()
    {
        mAdd_icon= ContextCompat.getDrawable(CONTEXT,ic_menu_add);
        mMenu_icon = ContextCompat.getDrawable(CONTEXT,ic_menu_compass);
        //mSync_icon = ContextCompat.getDrawable(CONTEXT,ic_popup_sync);
        mfab_icons = new Drawable[SECTIONS];
        resetFABIcons();
        mCurrentFrag=FIRE_NOTES_POSITION; //FIRE NOTES AS FIRST FRAG
        mSectionsPagerAdapter = new MainAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.setCurrentItem(mCurrentFrag);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageScrollStateChanged(int state) {}
            @Override
            public void onPageSelected(final int position)
            {
                mCurrentFrag=position;
                changeFABIcon();
            }
        });
    }
    public void changeFABIcon()
    {
        ObjectAnimator anim = ObjectAnimator.ofFloat(fab, "rotation", 0f, 360f);
        anim.setDuration(120);
        anim.setRepeatCount(1);
        anim.addListener(new Animator.AnimatorListener()
        {
            @Override
            public void onAnimationStart(Animator animator) {}
            @Override
            public void onAnimationEnd(Animator animator) {}
            @Override
            public void onAnimationCancel(Animator animator) {}
            @Override
            public void onAnimationRepeat(Animator animator)
            {
                fab.setImageDrawable(mfab_icons[mCurrentFrag]);
            }
        });
        anim.start();
    }


    public void setFirebaseConfigs() // Apply config settings and default values.
    {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build();
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(CodelabPreferences.TITLE_LENGTH, TITLE_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
    }

    protected synchronized void buildGoogleApiClient()
    {
        if (hasPermissions(this,LOCATION_PERMISSIONS))
        {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API)
                    .addApi(AppInvite.API)
                    .addApi(LocationServices.API)
                    .addOnConnectionFailedListener(this)
                    .addConnectionCallbacks(this)
                    .build();
        }
        else
        {
            askPermissions(this, LOCATION_PERMISSIONS, PERMISSIONS_LOCATION);
            Toast.makeText(this, "You must give location permissions in order to use this app", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void setAuth()
    {
        //mUsername = ANONYMOUS;
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null)
        {   // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else
        {
            /*mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null)
            {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }*/
        }

    }

    @Override
    public void resetFAB()
    {
        fab.setOnClickListener(onFABClick);
        resetFABIcons();
        changeFABIcon();
    }

    @Override
    public void drawPin(AudioVoiceNote avn)
    {
        if (gMap!=null)
        {
            MarkerOptions mo = new MarkerOptions();
            mo.position(new LatLng(avn.getLatitude(),avn.getLongitude()));
            mo.title(avn.getTitle());
            gMap.addMarker(mo);
        }
    }

    @Override
    public void hideFAB() {fab.hide();}

    @Override
    public void showFAB() {fab.show();}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    public static boolean hasPermissions(Context context, String... permissions)
    {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }
    public static void askPermissions(Activity act,String[] PERMISSIONS,int code)
    {
        ActivityCompat.requestPermissions(act,PERMISSIONS,code);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSIONS_RECORD:
                for (int i = 0; i<permissions.length;++i)
                    Log.d(TAG,permissions[i] + " "+ String.valueOf(grantResults[i]) );
                break;
            case PERMISSIONS_LOCATION:
                for (int i = 0; i<permissions.length;++i)
                    Log.d(TAG,permissions[i] + " "+ String.valueOf(grantResults[i]) );
                break;
        }
    }
    public String getfilePathFromAudioUri(Uri u)
    {
        Cursor cursor = this.getContentResolver().query(u, new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
        if (cursor != null && cursor.getCount() != 0)
        {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        }
        return null;
    }

    public Fragment getFragmentAtPosition(int index)
    {
        return getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.container+":"+index);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mGoogleApiClient == null)
            buildGoogleApiClient();

    }
    protected void onStart()
    {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
    }
    protected void onStop()
    {
        // Disconnecting the client invalidates it.
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        // only stop if it's connected, otherwise we crash
        if (mGoogleApiClient != null)
        {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    // Create an anonymous implementation of OnClickListener
    private View.OnClickListener onFABClick = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            switch (mCurrentFrag)
            {
                case MAP_POSITION:
                    updateMapToCurrentLocation();
                    break;
                case FIRE_NOTES_POSITION: //CALL TO RECORD
                    try
                    {
                        if (mLastLocation!=null)
                        {
                            if (hasPermissions(CONTEXT, RECORD_PERMISSIONS))
                            {
                                Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                                startActivityForResult(intent, RECORD_INTENT);
                            }
                            else
                            {
                                askPermissions(MainActivity.this,RECORD_PERMISSIONS, PERMISSIONS_RECORD);
                            }
                        }
                        else
                        {
                            Toast.makeText(CONTEXT,"Tenemos problemas obteniendo tu location, verifica tu GPS",Toast.LENGTH_SHORT).show();
                        }
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(CONTEXT,"Debes tener instalada alguna app para grabar audio",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case FRAGMENT_POSITION:
                    break;
            }
        }
    };

    private View.OnClickListener onNextFABClick = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            switch (mCurrentFrag)
                {
                    case MAP_POSITION:
                        break;
                    case FIRE_NOTES_POSITION: //CALL TO SEND
                        FireNotes fn = ((FireNotes)getFragmentAtPosition(FIRE_NOTES_POSITION));
                        if (fn.noteIsValid())
                        {
                            fn.sendVoiceNote();
                            fn.closeAudioLayout();
                            resetFAB();
                        }
                        break;
                    case FRAGMENT_POSITION:
                        break;
                }
        }
    };

    public void updateMapToCurrentLocation()
    {
        if (gMap!=null && mLastLocation!=null)
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()),16));
        else
            Log.d(TAG,"gMAP or lastLocation are null");
    }
    @Override
    public void mapReady(GoogleMap gmap)
    {
        this.gMap=gmap;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        Log.d(TAG,"onConnected");

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //mLocationRequest.setSmallestDisplacement(0.1F);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onLocationChanged(Location location)
    {
        mLastLocation = location;
        updateMapToCurrentLocation();
    }
}

