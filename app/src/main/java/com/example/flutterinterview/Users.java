package com.example.flutterinterview;

import android.graphics.Bitmap;
import android.net.Uri;

public class Users {

    public String first_name, last_name, email;
    public int id;
    public boolean showMenu;
    public Uri avatar;

    public Users (){

    }

    @Override
    public String toString() {
        return "Users{" +
                "first_name='" + first_name + '\'' +
                ", last_name='" + last_name + '\'' +
                ", email='" + email + '\'' +
                ", id=" + id +
                ", showMenu=" + showMenu +
                ", avatar=" + avatar +
                '}';
    }

    public Users(String first_name, String last_name, Uri avatar, String email, int id, boolean showMenu) {
        this.showMenu = showMenu;
        this.first_name = first_name;
        this.last_name = last_name;
        this.avatar = avatar;
        this.email = email;
        this.id = id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public Uri getAvatar() {
        return avatar;
    }

    public void setAvatar(Uri avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isShowMenu() {
        return showMenu;
    }

    public void setShowMenu(boolean showMenu) {
        this.showMenu = showMenu;
    }
}
