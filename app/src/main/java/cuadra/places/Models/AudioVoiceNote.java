package cuadra.places.Models;

import com.google.firebase.auth.FirebaseUser;


/**
 * Created by Ruben on 10/4/16.
 */

public class AudioVoiceNote
{
    private String downloadUrl;

    private String uuid;        //Id del usuario que lo subio
    private String userName;    //Nombre de usuario que lo subio
    private String photoUrl;    //Photo of the user that uploaded note

    private String title;
    private String size;
    private String description; //caracteres que describan la nota
    private String duration;
    private String since;
    private String fileName;

    public AudioVoiceNote(){}

    public void setUser(FirebaseUser u)
    {
        photoUrl= String.valueOf(u.getPhotoUrl());
        uuid = u.getUid();
        userName=u.getDisplayName();
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUri) {
        this.downloadUrl = downloadUri;
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
    public String obtainSizeInKb()
    {
        return size.substring(0,size.length()-3);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }
}
