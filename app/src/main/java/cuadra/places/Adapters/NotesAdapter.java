package cuadra.places.Adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;

import cuadra.places.Models.AudioVoiceNote;
import cuadra.places.Models.FriendlyMessage;
import cuadra.places.Interfaces.FirebaseAdapterInterface;
import cuadra.places.R;
import de.hdodenhof.circleimageview.CircleImageView;

public class NotesAdapter extends FirebaseRecyclerAdapter<AudioVoiceNote,NotesAdapter.MessageViewHolder>
{
    FirebaseAdapterInterface onPopulateListener;
    private Context CONTEXT;

    public NotesAdapter(Context context,FirebaseAdapterInterface listener,Query ref)
    {
        super(AudioVoiceNote.class,R.layout.item_message,MessageViewHolder.class,ref);
        this.CONTEXT = context;
        this.onPopulateListener = listener;
    }

    @Override
    protected void populateViewHolder(MessageViewHolder viewHolder, AudioVoiceNote audioVoiceNote, int position)
    {
        onPopulateListener.OnPopulate();

        viewHolder.messageTextView.setText(audioVoiceNote.getuserName());
        viewHolder.messengerTextView.setText(audioVoiceNote.getDuration() + "     " +audioVoiceNote.getSizeInKb()+"kb");

        if (audioVoiceNote.getPhotoUrl() == null)
        {
            viewHolder.messengerImageView.setImageDrawable(ContextCompat
                    .getDrawable(CONTEXT, R.drawable.ic_account_circle_black_36dp));
        } else
        {
            Glide.with(CONTEXT).load(audioVoiceNote.getPhotoUrl()).into(viewHolder.messengerImageView);
        }
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

