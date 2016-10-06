package cuadra.places.Interfaces;

import android.view.View;

import java.io.File;

import cuadra.places.Adapters.NotesAdapter;

/**
 * Created by Ruben on 10/4/16.
 */

public interface FirebaseAdapterInterface
{
    void OnPopulate();
    //void onVoiceNoteDownload(String download_url);
    void onVoiceNoteDownload(NotesAdapter.MessageViewHolder view, String fileName);
    void playVoiceNote(NotesAdapter.MessageViewHolder viewHolder, File f);
}
