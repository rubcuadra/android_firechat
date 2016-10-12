package cuadra.places.Interfaces;

import android.location.Location;
import android.view.View;

import java.io.File;

import cuadra.places.Adapters.VoiceNotesAdapter;
import cuadra.places.Models.AudioVoiceNote;

/**
 * Created by Ruben on 10/4/16.
 */

public interface FirebaseAdapterInterface
{
    void OnPopulate();
    //void onVoiceNoteDownload(String download_url);
    //void onVoiceNoteDownload(NotesAdapter.MessageViewHolder view, String fileName);
    void onVoiceNoteDownload(VoiceNotesAdapter.NoteViewHolder v, String fileName);

    //void playVoiceNote(NotesAdapter.MessageViewHolder viewHolder, File f);
    void playVoiceNote(VoiceNotesAdapter.NoteViewHolder viewHolder, File f);

    void drawPin(String title,Location l);
}
