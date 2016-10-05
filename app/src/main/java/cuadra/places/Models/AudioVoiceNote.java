package cuadra.places.Models;

import android.net.Uri;


/**
 * Created by Ruben on 10/4/16.
 */

public class AudioVoiceNote
{
    private String downloadUri;
    private String uuid;
    private String title;
    private String size;

    public AudioVoiceNote(String downloadUri, String uuid, String title, String size)
    {
        this.downloadUri = downloadUri;
        this.uuid = uuid;
        this.title = title;
        this.size = size;
    }
    public AudioVoiceNote(){}

    public String getDownloadUri() {
        return downloadUri;
    }

    public void setDownloadUri(String downloadUri) {
        this.downloadUri = downloadUri;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}
