package com.pinedapps.onlinetv;

public class ModelFirebase {
    private String name, gender, logo, video;
    private String id;
    private boolean isFavorite;
    public ModelFirebase(String name, String gender, String logo, String video) {
        this.name = name;
        this.gender = gender;
        this.logo = logo;
        this.video = video;
        this.isFavorite = false;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ModelFirebase() {
    }

    public String getId() {
        return id != null ? id : "";
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }


    public String getVideo() {
        return video != null ? video : "";
    }

    public String getName() {
        return name != null ? name : "";
    }

    public String getGender() {
        return gender != null ? gender : "";
    }

    public String getLogo() {
        return logo != null ? logo : "";
    }
}