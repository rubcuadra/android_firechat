package cuadra.places.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
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
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import cuadra.places.Adapters.MainAdapter;
import cuadra.places.CodelabPreferences;
import cuadra.places.Fragments.FireNotes;
import cuadra.places.R;

import static android.R.drawable.presence_audio_busy;

public class MainActivity extends AppCompatActivity implements
        FireNotes.OnFragmentInteractionListener,
        GoogleApiClient.OnConnectionFailedListener

{
    private static final String TAG = "MainActivity";
    private static final int REQUEST_INVITE = 1;
    private static final int RECORD_INTENT = 2;
    private static final int PERMISSIONS_ALL=3;

    private static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};


    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;

    private MainAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private GoogleApiClient mGoogleApiClient;
    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;

    //RECORDER
    private static String mFileName = null;
    private MediaRecorder mRecorder = null;
    private MediaPlayer   mPlayer = null;

    private FloatingActionButton fab_play;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));//SetToolbar
        setGoogleAPI();
        setAuth();
        setFirebaseConfigs();
        setFAB(); //FloatingActionButton
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
                    mFileName=getfilePathFromAudioUri(data.getData());
                    Log.d(TAG,mFileName); //PUEDE SER null pero es raro el caso
                    fab_play.setVisibility(View.VISIBLE);
                }
                else
                {

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
            case R.id.crash_menu:
                FirebaseCrash.logcat(Log.ERROR, TAG, "crash caused");
                causeCrash();
                return true;
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

    public void setPager()
    {
        mSectionsPagerAdapter = new MainAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.setCurrentItem(1); //De enmedio
    }
    public void setFirebaseConfigs() // Apply config settings and default values.
    {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build();
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
    }
    public void setGoogleAPI()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .addApi(AppInvite.API)
                .build();
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
    public void onFragmentInteraction(Uri uri)
    {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    //RECORD
    private void onRecord(boolean start)
    {
        if (start)
        {
            startRecording();
        } else
        {
            stopRecording();
        }
    }

    private void onPlay(boolean start)
    {
        if (start)
        {
            startPlaying();
        } else
        {
            stopPlaying();
        }
    }

    private void startPlaying()
    {
        mPlayer = new MediaPlayer();
        try
        {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e)
        {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying()
    {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording()
    {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try
        {
            mRecorder.prepare();
        } catch (IOException e)
        {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording()
    {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    public void setFAB()
    {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/audiorecordtest.3gp";

        Log.d(TAG,mFileName);
        final FloatingActionButton fab_record = (FloatingActionButton) findViewById(R.id.fab);
        fab_play= (FloatingActionButton) findViewById(R.id.fab_temporal);

        fab_record.setOnClickListener(new View.OnClickListener()
        {
            boolean mStartRecording = true;
            @Override
            public void onClick(View view)
            {
                if (hasPermissions( getApplicationContext()  ,PERMISSIONS) )
                {
                    Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                    startActivityForResult(intent, RECORD_INTENT);
                    //onRecord(mStartRecording);
                    //Snackbar.make(view, mStartRecording ? "Starting to record" : "Stopping record", Snackbar.LENGTH_LONG)
                    //        .setAction("Action", null).show();
                    //mStartRecording = !mStartRecording;
                }
                else
                {
                    askPermissions();
                }
            }
        });

        fab_play.setOnClickListener(new View.OnClickListener()
        {
            boolean mStartPlaying = true;

            @Override
            public void onClick(View view)
            {

                onPlay(mStartPlaying);
                Snackbar.make(view, mStartPlaying ? "Starting to play" : "Stopping note", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mStartPlaying = !mStartPlaying;
            }
        });
        fab_play.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mRecorder != null)
        {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
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
    public void askPermissions()
    {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_ALL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSIONS_ALL:
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
}

