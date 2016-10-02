package cuadra.places.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import cuadra.places.CodelabPreferences;
import cuadra.places.FriendlyMessage;

import cuadra.places.R;
import de.hdodenhof.circleimageview.CircleImageView;

import static cuadra.places.Activities.tabbed_activity.DEFAULT_MSG_LENGTH_LIMIT;
import static cuadra.places.Activities.tabbed_activity.ARG_MESSAGE_LENGTH;

public class FireNotes extends Fragment
{
    //private static final String ARG_USERNAME = "param1";
    //private static final String ARG_PHOTO_URL = "param2";

    public static final String MESSAGES_CHILD = "messages";
    private static final String F_TAG = "Notes_Fragment";


    private String mUsername;
    private String mPhotoUrl;

    private OnFragmentInteractionListener mListener;

    //Views
    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;

    //FIREBASE
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> mFirebaseAdapter;
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

        //FIREBASE SETUP

        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(FriendlyMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(MESSAGES_CHILD))
        {

            @Override
            protected void populateViewHolder(MessageViewHolder viewHolder, FriendlyMessage friendlyMessage, int position)
            {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.messageTextView.setText(friendlyMessage.getText());
                viewHolder.messengerTextView.setText(friendlyMessage.getName());

                if (friendlyMessage.getPhotoUrl() == null)
                {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat
                                    .getDrawable(CONTEXT,
                                            R.drawable.ic_account_circle_black_36dp));
                } else
                {
                    Glide.with(CONTEXT)
                            .load(friendlyMessage.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }
            }
        };


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

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri)
    {
        if (mListener != null)
        {
            mListener.onFragmentInteraction(uri);
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
        // If developer mode is enabled reduce cacheExpiration to 0 so that
        // each fetch goes to the server. This should not be used in release
        // builds.

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
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(ARG_MESSAGE_LENGTH);
        mMessageEditText.setFilters( new InputFilter[]
                {
                        new InputFilter.LengthFilter(friendly_msg_length.intValue())
                });
        Log.d(F_TAG, "FML is: " + friendly_msg_length);
    }

    //SE COMUNICARA CON MAIN
    public interface OnFragmentInteractionListener
    {
        void onFragmentInteraction(Uri uri);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder
    {
        public TextView messageTextView;
        public TextView messengerTextView;
        public CircleImageView messengerImageView;

        public MessageViewHolder(View v)
        {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }


}