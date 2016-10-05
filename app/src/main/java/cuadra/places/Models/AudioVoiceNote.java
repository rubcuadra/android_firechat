package cuadra.places.Models;

import android.net.Uri;

import com.google.firebase.auth.FirebaseUser;


/**
 * Created by Ruben on 10/4/16.
 */

public class AudioVoiceNote
{
    private String downloadUri;

    private String uuid;        //Id del usuario que lo subio
    private String userName;    //Nombre de usuario que lo subio
    private String photoUrl;    //Photo of the user that uploaded note

    private String title;
    private String size;
    private String description; //caracteres que describan la nota
    private String duration;
    private String since;

    public AudioVoiceNote(String downloadUri, String uuid, String title, String size,String userName,String description,
                          String photoUrl, String duration,String since)
    {
        this.downloadUri = downloadUri;
        this.uuid = uuid;
        this.title = title;
        this.size = size;
        this.userName =userName;
        this.description=description;
        this.photoUrl=photoUrl;
        this.duration = duration;
        this.since = since;

    }
    public AudioVoiceNote(){}

    public void setUser(FirebaseUser u)
    {
        photoUrl= String.valueOf(u.getPhotoUrl());
        uuid = u.getUid();
        userName=u.getDisplayName();
    }

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

    public String getuserName() {
        return userName;
    }

    public void setuserName(String userName) {
        this.userName = userName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setDurationFromInt(int duration_in_mili_seconds)
    {
        int seconds = duration_in_mili_seconds/1000;
        int minutes = seconds/60;
        seconds -= minutes*60;
        duration="";
        duration+= minutes<10?"0"+minutes:minutes;
        duration+= ":";
        duration+= seconds<10?"0"+seconds:seconds;
    }
    public String getSizeInKb()
    {
        return size.substring(0,size.length()-3);
    }
}
