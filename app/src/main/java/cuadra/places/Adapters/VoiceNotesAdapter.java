package cuadra.places.Adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cuadra.places.Interfaces.FirebaseAdapterInterface;
import cuadra.places.Models.AudioVoiceNote;
import cuadra.places.R;
import de.hdodenhof.circleimageview.CircleImageView;

import static android.R.drawable.ic_media_play;
import static android.R.drawable.ic_menu_close_clear_cancel;

/**
 * Created by Ruben on 10/12/16.
 */

public class VoiceNotesAdapter extends RecyclerView.Adapter<VoiceNotesAdapter.NoteViewHolder>
{
    private GeoQuery mQuery;
    private List<AudioVoiceNote> mNotes;
    private FirebaseAdapterInterface FirebaseAdapterListener;
    private static String TAG="NotesAdapter";
    private static Context CONTEXT;
    private static Drawable cancelIcon;
    private static Drawable playIcon;
    private static String cacheDir;

    public VoiceNotesAdapter(Context context, final FirebaseAdapterInterface listener, GeoQuery gq, final DatabaseReference ref)
    {
        FirebaseAdapterListener=listener;
        mQuery = gq;
        mNotes= new ArrayList<AudioVoiceNote>();
        CONTEXT = context;
        cancelIcon=  ContextCompat.getDrawable(CONTEXT,ic_menu_close_clear_cancel);
        playIcon = ContextCompat.getDrawable(CONTEXT,ic_media_play);
        cacheDir=CONTEXT.getCacheDir()+"/";

        mQuery.addGeoQueryEventListener(new GeoQueryEventListener()
        {
            @Override
            public void onKeyEntered(String key, final GeoLocation location)
            {
                ref.child(key).addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        mNotes.add(dataSnapshot.getValue(AudioVoiceNote.class));
                        (VoiceNotesAdapter.this).notifyItemInserted(mNotes.size());
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }
            @Override
            public void onKeyExited(String key)
            {
                int i;
                for (i = 0; i<mNotes.size();++i)
                    if (mNotes.get(i).getFileName().startsWith(key))
                        break;
                mNotes.remove(i);
                notifyItemRemoved(i);
            }
            @Override
            public void onKeyMoved(String key, GeoLocation location)
            {Log.d(TAG,"KEY MOVED "+key);}

            @Override
            public void onGeoQueryReady()
            {
                listener.OnPopulate();
            }

            @Override
            public void onGeoQueryError(DatabaseError error)
            {
                Log.d(TAG,"ERROR AL OBTENER DE DB");
            }
        });
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final NoteViewHolder viewHolder, int position)
    {
        final AudioVoiceNote audioVoiceNote = mNotes.get(position);
        final File f = new File(cacheDir+audioVoiceNote.getFileName());
        //Si el file existe poner play, si no existe poner download, de eso depende la funcionalidad del button
        viewHolder.messageTextView.setText(audioVoiceNote.getTitle());
        viewHolder.messengerTextView.setText(audioVoiceNote.getDuration() + "     " +audioVoiceNote.obtainSizeInKb()+"kb");
        if (f.exists())
            viewHolder.playButton.setImageDrawable(playIcon);
        viewHolder.playButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(!f.exists()) //Si no existe, descargarlo
                {
                    Log.d(TAG,"Descargando "+audioVoiceNote.getFileName());
                    viewHolder.playButton.setClickable(false);
                    viewHolder.playButton.setImageDrawable(cancelIcon); //Mame
                    FirebaseAdapterListener.onVoiceNoteDownload(viewHolder, audioVoiceNote.getFileName());
                }
                else    //Si existe hacer play
                {
                    FirebaseAdapterListener.playVoiceNote(viewHolder,f);
                }
            }
        });

        if (audioVoiceNote.getPhotoUrl() == null)
        {
            viewHolder.messengerImageView.setImageDrawable(ContextCompat
                    .getDrawable(CONTEXT, R.drawable.ic_account_circle_black_36dp));
        } else
        {
            Glide.with(CONTEXT).load(audioVoiceNote.getPhotoUrl()).into(viewHolder.messengerImageView);
        }
    }

    @Override
    public int getItemCount()
    {
        return mNotes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder
    {
        public TextView messageTextView;
        public TextView messengerTextView;
        public CircleImageView messengerImageView;
        public ImageButton playButton;

        public NoteViewHolder(View v)
        {
            super(v);
            playButton = (ImageButton) itemView.findViewById(R.id.play_item);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }
}
