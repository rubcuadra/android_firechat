package cuadra.places.Fragments;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import cuadra.places.Adapters.NotesAdapter;
import cuadra.places.CodelabPreferences;
import cuadra.places.Interfaces.FirebaseAdapterInterface;
import cuadra.places.Models.AudioVoiceNote;
import cuadra.places.R;

import static android.R.drawable.ic_media_pause;
import static android.R.drawable.ic_media_play;
import static android.R.drawable.stat_sys_download;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
import static cuadra.places.Activities.MainActivity.TITLE_LENGTH_LIMIT;

public class FireNotes extends Fragment implements FirebaseAdapterInterface
{
    private static final String BUCKET_REFERENCE = "gs://the-places-youll-go.appspot.com";
    public static final String MESSAGES_CHILD = "VOICE-NOTES";
    private static final String F_TAG = "Notes_Fragment";

    //VARS
    private String mfileName;
    private Context CONTEXT;
    private GeoLocation mLocation;

    //AUDIO
    private static MediaPlayer mPlayer = null;
    private AudioVoiceNote mCurrentVoiceNote; //Objeto que se subira a Firebase

    //INTERACTIONS
    private OnFireNotesFragmentInteractionListener mListener;

    //Views
    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;

    private EditText mNoteTagsEditText;
    private EditText mNoteTitleEditText;


    private ConstraintLayout new_audio_note_layout;
    private Button playButton;
    private Button cancelButton;

    private Drawable playIcon;
    private Drawable downloadIcon;
    private Drawable pauseIcon;

    private NotesAdapter.MessageViewHolder playingViewHolder;   //Item de la lista que esta sonando o null

    //FIREBASE
    private DatabaseReference mFirebaseDatabaseReference;
    private NotesAdapter mFirebaseAdapter;
    private SharedPreferences mSharedPreferences;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseUser mUser;
    private StorageReference mFireStorageRef;
    private DatabaseReference mNewNote;
    private FirebaseStorage mStorageInstance;
    private GeoFire mGeoFire;

    public FireNotes() {}

    public static FireNotes newInstance()
    {
        FireNotes fragment = new FireNotes();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //if (getArguments() != null){mUsername = getArguments().getString(ARG_USERNAME);mPhotoUrl = getArguments().getString(ARG_PHOTO_URL);}
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        //Firebase Stuff
        mFirebaseRemoteConfig=FirebaseRemoteConfig.getInstance();
        mFirebaseDatabaseReference=FirebaseDatabase.getInstance().getReference();
        mStorageInstance = FirebaseStorage.getInstance();
        mFireStorageRef = mStorageInstance.getReferenceFromUrl(BUCKET_REFERENCE).child(MESSAGES_CHILD);
        mNewNote=mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        mCurrentVoiceNote = new AudioVoiceNote();
        mCurrentVoiceNote.setUser(mUser);
        mGeoFire = new GeoFire(mFirebaseDatabaseReference.child("GEOFIRE"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_fire_notes, container, false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) view.findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(CONTEXT);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        new_audio_note_layout = (ConstraintLayout) view.findViewById(R.id.audio_constraint_layout);
        playButton = (Button) view.findViewById(R.id.playButton);
        cancelButton = (Button) view.findViewById(R.id.cancelButton);

        playIcon =  ContextCompat.getDrawable(CONTEXT, ic_media_play);
        downloadIcon = ContextCompat.getDrawable(CONTEXT, stat_sys_download);
        pauseIcon = ContextCompat.getDrawable(CONTEXT, ic_media_pause);

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                closeAudioLayout();
                deleteFile();
                if (mListener!=null)
                    mListener.resetFAB();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                stopPlaying();
                if (playingViewHolder==null && mPlayer!=null)//Esta sonando y es esta voice, pausarla
                {}
                else //Darle Play
                {
                    playButton.setText("Stop");
                    startPlaying(mfileName);
                }
            }
        });

        mMessageRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState)
            {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == SCROLL_STATE_IDLE && mListener!=null)
                    mListener.showFAB();
                else
                    mListener.hideFAB();
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(CONTEXT);

        mNoteTagsEditText = (EditText) view.findViewById(R.id.TagsEditText);
        mNoteTitleEditText = (EditText) view.findViewById(R.id.noteTitle);

        mNoteTitleEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.TITLE_LENGTH, TITLE_LENGTH_LIMIT))});

        fetchConfig();
        return view;
    }

    public void onVoiceNoteDownload(final NotesAdapter.MessageViewHolder view, String fileToDownload)
    {
        StorageReference gsReference = mStorageInstance.getReferenceFromUrl(BUCKET_REFERENCE+"/"+MESSAGES_CHILD+"/"+fileToDownload);
        try
        {

            final File localFile = new File(CONTEXT.getCacheDir()+"/"+fileToDownload);
            Log.d(F_TAG,"Nota descargada en: "+localFile.getAbsolutePath());
            gsReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
            {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                {
                    view.playButton.setImageDrawable(playIcon);
                    view.playButton.setClickable(true);
                }
            }).addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception exception)
                {
                    Toast.makeText(CONTEXT,"Ups try again",Toast.LENGTH_SHORT);
                    view.playButton.setImageDrawable(downloadIcon);
                    localFile.delete();
                    view.playButton.setClickable(true);
                }
            });

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void playVoiceNote(NotesAdapter.MessageViewHolder viewHolder, File f)
    {
        if (viewHolder!= playingViewHolder) //Vamos a darle play
        {
            stopPlaying();
            viewHolder.playButton.setImageDrawable(pauseIcon);
            startPlaying(f.getAbsolutePath());
            playingViewHolder=viewHolder;
        }
        else  //Lo volvieron a presionar, osea pausar
        {
            stopPlaying();
        }
    }

    @Override
    public void drawPin(AudioVoiceNote vn)
    {
        if (mListener!=null)
            mListener.drawPin(vn);
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        CONTEXT=context;
        if (context instanceof OnFireNotesFragmentInteractionListener)
        {
            mListener = (OnFireNotesFragmentInteractionListener) context;
        } else
        {
            throw new RuntimeException(context.toString()+" must implement OnFireNotesFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    public void resetFirebaseRefs()
    {
        mNewNote=mNewNote.getParent();
        mFireStorageRef=mFireStorageRef.getParent();
    }

    public void sendVoiceNote()
    {
        try
        {
            //Existe el caso en el que la location es null??
            InputStream stream = new FileInputStream(new File(mfileName));
            String extension = mfileName.substring( mfileName.lastIndexOf(".")+1 );
            mNewNote= mNewNote.push();
            String newFName = mNewNote.getKey()+"."+extension;
            mFireStorageRef = mFireStorageRef.child(newFName);
            mCurrentVoiceNote.setFileName(newFName);
            mCurrentVoiceNote.setTitle(mNoteTitleEditText.getText().toString());
            mNoteTitleEditText.setText("");

            UploadTask uploadTask = mFireStorageRef.putStream(stream);
            uploadTask.addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception exception)
                {
                    Log.d(F_TAG,"Failed to upload");
                    resetFirebaseRefs();
                    deleteFile();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                {
                    mCurrentVoiceNote.setDownloadUrl(String.valueOf(taskSnapshot.getDownloadUrl()));
                    mCurrentVoiceNote.setSize(String.valueOf(taskSnapshot.getMetadata().getSizeBytes()));
                    mGeoFire.setLocation(mNewNote.getKey(),mLocation);
                    //Si falla en subir la mGeoFire que borre el registro del storage y ya no sube la mNewNote
                    mNewNote.setValue(mCurrentVoiceNote);
                    resetFirebaseRefs();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot)
                {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    System.out.println("Upload is " + progress + "% done");
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot)
                {
                    System.out.println("Upload is paused");
                }
            });

        } catch (FileNotFoundException e)
        {
            Log.d(F_TAG,"File not found "+mfileName);
            mfileName=null;
            e.printStackTrace();
        }
    }

    public void OnPopulate()
    {
        hideProgressBar();
    }

    public void hideProgressBar()
    {
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    public void setFileName(String newFile)
    {
        mfileName = newFile;
        Log.d(F_TAG,newFile);
        openAudioLayout();
        mCurrentVoiceNote.setDurationFromInt( getFileDuration() );
    }
    private int getFileDuration()
    {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(mfileName);
        int duration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        mmr.release();
        return duration;
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
    public void closeAudioLayout()
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
    private void startPlaying(final String fName)
    {
        mPlayer = new MediaPlayer();
        try
        {
            mPlayer.setDataSource(fName);
            Log.d(F_TAG,fName);
            mPlayer.prepare();
            mPlayer.start();

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer)
                {
                    stopPlaying();
                }
            });

        } catch (IOException e)
        {
            Log.e(F_TAG, "prepare() failed");
        }
    }
    private void stopPlaying()
    {
        if (mPlayer!=null)
        {
            mPlayer.release();
            mPlayer = null;
        }
        if (playingViewHolder!=null) //Paramos un item
        {
            playingViewHolder.playButton.setImageDrawable(playIcon);
            playingViewHolder=null;
        }
        else                        //Paramos la grabacion actual
        {
            playButton.setText("Play");
        }
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
        stopPlaying();
    }
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        // If there's an upload in progress, save the reference so you can query it later

        if (mFireStorageRef != null)
        {
            outState.putString("reference", mFireStorageRef.toString());
            outState.putString("register",  mNewNote.toString());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState!=null)
        {
            // If there was an upload in progress, get its reference and create a new StorageReference
            final String stringRef = savedInstanceState.getString("reference");
            if (stringRef == null) return;

            mFireStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);
            mNewNote = FirebaseDatabase.getInstance().getReferenceFromUrl(savedInstanceState.getString("register"));

            // Find all UploadTasks under this StorageReference (in this example, there should be one)
            List tasks = mFireStorageRef.getActiveUploadTasks();
            if (tasks.size() > 0)
            {
                // Get the task monitoring the upload
                UploadTask task = (UploadTask)tasks.get(0);

                // Add new listeners to the task using an Activity scope
                task.addOnSuccessListener(
                        new OnSuccessListener<UploadTask.TaskSnapshot>()
                        {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                            {
                                mCurrentVoiceNote.setDownloadUrl(String.valueOf(taskSnapshot.getDownloadUrl()));
                                mGeoFire.setLocation(mNewNote.getKey(),mLocation);
                                //Igual si falla en subir que borre el storage
                                mNewNote.setValue(mCurrentVoiceNote);
                                resetFirebaseRefs();
                            }
                        }).addOnFailureListener(new OnFailureListener()
                        {
                            @Override
                            public void onFailure(@NonNull Exception exception)
                            {
                                Log.d(F_TAG,"Failed to upload");
                                resetFirebaseRefs();
                                deleteFile();
                            }
                        });
            }
        }
    }

    public interface OnFireNotesFragmentInteractionListener
    {
        void resetFAB();
        void drawPin(AudioVoiceNote avn);
        void hideFAB();
        void showFAB();
    }

    public void setLocation(Location loc)
    {
        mLocation = new GeoLocation(loc.getLatitude(),loc.getLongitude());
    }
    public void startAdapter(Location loc)
    {
        Log.d(F_TAG,"Starting with location"+loc.toString());
        //Start Adapter for Firebase Messages
        GeoQuery mq = mGeoFire.queryAtLocation(new GeoLocation(loc.getLatitude(), loc.getLongitude()),60);
        mq.addGeoQueryEventListener(new GeoQueryEventListener()
        {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onKeyExited(String key) {
                System.out.println(String.format("Key %s is no longer in the search area", key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                System.out.println("All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                System.err.println("There was an error with this query: " + error);
            }
        });

        mFirebaseAdapter = new NotesAdapter(getContext(),this,  mFirebaseDatabaseReference.child(MESSAGES_CHILD));

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
        {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount)
            {
                super.onItemRangeInserted(positionStart, itemCount);
                int audioVoiceNotes = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (audioVoiceNotes - 1) &&
                                lastVisiblePosition == (positionStart - 1)))
                {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    }

    public boolean noteIsValid()
    {
        boolean valid;
        valid = mNoteTitleEditText.getText().toString().trim().length()>0;
        if (!valid) //Titulo valido
            mNoteTitleEditText.setError("Titulo invalido");
        return valid;
    }

    public void fetchConfig()
    {
        long cacheExpiration = 3600; //3600 1 hour in seconds

        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled())
        {
            cacheExpiration = 10;
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
        Long title_length = mFirebaseRemoteConfig.getLong(CodelabPreferences.TITLE_LENGTH);
        mNoteTitleEditText.setFilters( new InputFilter[]
                {
                        new InputFilter.LengthFilter(title_length.intValue())
                });
        Log.d(F_TAG, "FML is: " + title_length);
    }
}
