package cuadra.places.Adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;

import java.io.File;

import static android.R.drawable.ic_media_play;
import static android.R.drawable.ic_menu_close_clear_cancel;

import cuadra.places.Models.AudioVoiceNote;
import cuadra.places.Interfaces.FirebaseAdapterInterface;
import cuadra.places.R;
import de.hdodenhof.circleimageview.CircleImageView;

public class NotesAdapter extends FirebaseRecyclerAdapter<AudioVoiceNote,NotesAdapter.MessageViewHolder>
{
    FirebaseAdapterInterface FirebaseAdapterListener;
    private static String TAG="NotesAdapter";
    private static Context CONTEXT;
    private static Drawable cancelIcon;
    private static Drawable playIcon;
    private static String cacheDir;

    public NotesAdapter(Context context,FirebaseAdapterInterface listener,Query ref)
    {
        super(AudioVoiceNote.class,R.layout.item_message,MessageViewHolder.class,ref);
        this.CONTEXT = context;
        this.FirebaseAdapterListener = listener;
        cancelIcon=  ContextCompat.getDrawable(CONTEXT,ic_menu_close_clear_cancel);
        playIcon = ContextCompat.getDrawable(CONTEXT,ic_media_play);
        cacheDir=CONTEXT.getCacheDir()+"/";
    }

    @Override
    protected void populateViewHolder(final MessageViewHolder viewHolder, final AudioVoiceNote audioVoiceNote, int position)
    {
        FirebaseAdapterListener.OnPopulate();

        final File f = new File(cacheDir+audioVoiceNote.getFileName());
        //Si el file existe poner play, si no existe poner download, de eso depende la funcionalidad del button
        viewHolder.messageTextView.setText(audioVoiceNote.getuserName());
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
        FirebaseAdapterListener.drawPin(audioVoiceNote);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder
    {
        public TextView messageTextView;
        public TextView messengerTextView;
        public CircleImageView messengerImageView;
        public ImageButton playButton;

        public MessageViewHolder(View v)
        {
            super(v);
            playButton = (ImageButton) itemView.findViewById(R.id.play_item);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }

}

