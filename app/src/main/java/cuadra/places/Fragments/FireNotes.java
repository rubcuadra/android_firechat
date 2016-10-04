package cuadra.places.Fragments;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.File;
import java.io.IOException;

import cuadra.places.Adapters.NotesAdapter;
import cuadra.places.CodelabPreferences;
import cuadra.places.FriendlyMessage;

import cuadra.places.Interfaces.FirebaseAdapterInterface;
import cuadra.places.R;

import static cuadra.places.Activities.MainActivity.DEFAULT_MSG_LENGTH_LIMIT;

public class FireNotes extends Fragment implements FirebaseAdapterInterface
{
    //private static final String ARG_USERNAME = "param1";
    //private static final String ARG_PHOTO_URL = "param2";

    public static final String MESSAGES_CHILD = "messages";
    private static final String F_TAG = "Notes_Fragment";

    //VARS
    private String mfileName;
    private String mUsername;
    private String mPhotoUrl;

    //AUDIO
    private MediaPlayer mPlayer = null;
    private boolean play=true;

    //INTERACTIONS
    private OnFragmentInteractionListener mListener;

    //Views
    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ConstraintLayout new_audio_note_layout;
    private Button playButton;
    private Button cancelButton;

    //FIREBASE
    private DatabaseReference mFirebaseDatabaseReference;
    private NotesAdapter mFirebaseAdapter;
    private SharedPreferences mSharedPreferences;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseUser mUser;

    public FireNotes() {}

    public static FireNotes newInstance()
    {
        FireNotes fragment = new FireNotes();
        Bundle args = new Bundle();
        //args.putString(ARG_USERNAME, username);
        //args.putString(ARG_PHOTO_URL, photo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //if (getArguments() != null){mUsername = getArguments().getString(ARG_USERNAME);mPhotoUrl = getArguments().getString(ARG_PHOTO_URL);}
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseRemoteConfig=FirebaseRemoteConfig.getInstance();
        mFirebaseDatabaseReference=FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_fire_notes, container, false);

        final Context CONTEXT = view.getContext();
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) view.findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(CONTEXT);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        new_audio_note_layout = (ConstraintLayout) view.findViewById(R.id.audio_constraint_layout);
        playButton = (Button) view.findViewById(R.id.playButton);
        cancelButton = (Button) view.findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                closeAudioLayout();
                deleteFile();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (play)   startPlaying();
                else        stopPlaying();
                play = !play;
            }
        });

        //Start Adapter for Firebase Messages
        mFirebaseAdapter = new NotesAdapter(getContext(),this,mFirebaseDatabaseReference.child(MESSAGES_CHILD));
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
        {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount)
            {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1)))
                {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(CONTEXT);
        //Write Message
        mMessageEditText = (EditText) view.findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});

        mMessageEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                if (charSequence.toString().trim().length() > 0)
                {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        mSendButton = (Button) view.findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(),
                                                                        mUser.getDisplayName(),
                                                                        mUser.getPhotoUrl().toString());
                mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(friendlyMessage);
                mMessageEditText.setText("");
            }
        });
        fetchConfig();
        return view;
    }

    public void OnElementClick()
    {
        if (mListener != null)
        {
            mListener.onFireNotesFragmentInteraction();
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) context;
        } else
        {
            throw new RuntimeException(context.toString()+" must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    public void fetchConfig()
    {
        long cacheExpiration = 3600; //3600 1 hour in seconds

        if (mFirebaseRemoteConfig.getInfo().getConfigSettings()
                .isDeveloperModeEnabled())
        {
            cacheExpiration = 15;
        }

        mFirebaseRemoteConfig.fetch(cacheExpiration).addOnSuccessListener(new OnSuccessListener<Void>()
                {
                    @Override
                    public void onSuccess(Void aVoid)
                    {
                        // Make the fetched config available via
                        // FirebaseRemoteConfig get<type> calls.
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        // There has been an error fetching the config
                        Log.w(F_TAG, "Error fetching config: "+e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }

    private void applyRetrievedLengthLimit()
    {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(CodelabPreferences.FRIENDLY_MSG_LENGTH);
        mMessageEditText.setFilters( new InputFilter[]
                {
                        new InputFilter.LengthFilter(friendly_msg_length.intValue())
                });
        Log.d(F_TAG, "FML is: " + friendly_msg_length);
    }

    public void OnPopulate()
    {hideProgressBar();}
    public void hideProgressBar()
    {mProgressBar.setVisibility(ProgressBar.INVISIBLE);}

    public void setFileName(String newFile)
    {
        mfileName = newFile;
        Log.d(F_TAG,mfileName);
        openAudioLayout();
    }
    protected void openAudioLayout()
    {
        ValueAnimator anim = ValueAnimator.ofInt(new_audio_note_layout.getMeasuredHeightAndState(),ViewGroup.LayoutParams.WRAP_CONTENT);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = new_audio_note_layout.getLayoutParams();
                layoutParams.height = val;
                new_audio_note_layout.setLayoutParams(layoutParams);
            }
        });
        anim.start();
    }
    protected void closeAudioLayout()
    {
        ValueAnimator anim = ValueAnimator.ofInt(new_audio_note_layout.getMeasuredHeightAndState(),1);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = new_audio_note_layout.getLayoutParams();
                layoutParams.height = val;
                new_audio_note_layout.setLayoutParams(layoutParams);
            }
        });
        anim.start();
    }
    private void startPlaying()
    {
        playButton.setText("Stop");

        mPlayer = new MediaPlayer();
        try
        {
            mPlayer.setDataSource(mfileName);
            mPlayer.prepare();
            mPlayer.start();

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer)
                {
                    stopPlaying();
                    play=true;
                }
            });

        } catch (IOException e)
        {
            Log.e(F_TAG, "prepare() failed");
        }
    }
    private void stopPlaying()
    {
        playButton.setText("Play");
        mPlayer.release();
        mPlayer = null;
    }
    private void deleteFile()
    {
        if(mfileName!=null)
        {
            boolean deleted = (new File(mfileName)).delete();
            Log.d(F_TAG,deleted+": Deleted file "+mfileName);
            mfileName=null;
        }
    }
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        deleteFile();
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mPlayer != null)
        {
            mPlayer.release();
            mPlayer = null;
        }
    }

    public interface OnFragmentInteractionListener {void onFireNotesFragmentInteraction();}
}
