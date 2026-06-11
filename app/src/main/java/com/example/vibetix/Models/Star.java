package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class Star implements Serializable {
    @PropertyName("star_id")
    private String starId;

    @PropertyName("stage_name")
    private String stageName;

    @PropertyName("real_name")
    private String realName;

    @PropertyName("slug")
    private String slug;

    @PropertyName("avatar_url")
    private String avatarUrl;

    @PropertyName("cover_url")
    private String coverUrl;

    @PropertyName("bio")
    private String bio;

    @PropertyName("genre")
    private String genre;

    @PropertyName("nationality")
    private String nationality;

    @PropertyName("social_spotify")
    private String socialSpotify;

    @PropertyName("social_youtube")
    private String socialYoutube;

    @PropertyName("social_instagram")
    private String socialInstagram;

    @PropertyName("social_facebook")
    private String socialFacebook;

    @PropertyName("social_tiktok")
    private String socialTiktok;

    @PropertyName("follower_count")
    private int followerCount = 0;

    @PropertyName("is_verified")
    private boolean isVerified = false;

    @PropertyName("is_active")
    private boolean isActive = true;

    @PropertyName("created_at")
    private com.google.firebase.Timestamp createdAt;

    @PropertyName("updated_at")
    private com.google.firebase.Timestamp updatedAt;

    public Star() {}

    @PropertyName("star_id")
    public String getStarId() { return starId; }
    @PropertyName("star_id")
    public void setStarId(String starId) { this.starId = starId; }

    @PropertyName("stage_name")
    public String getStageName() { return stageName; }
    @PropertyName("stage_name")
    public void setStageName(String stageName) { this.stageName = stageName; }

    @PropertyName("real_name")
    public String getRealName() { return realName; }
    @PropertyName("real_name")
    public void setRealName(String realName) { this.realName = realName; }

    @PropertyName("slug")
    public String getSlug() { return slug; }
    @PropertyName("slug")
    public void setSlug(String slug) { this.slug = slug; }

    @PropertyName("avatar_url")
    public String getAvatarUrl() { return avatarUrl; }
    @PropertyName("avatar_url")
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    @PropertyName("cover_url")
    public String getCoverUrl() { return coverUrl; }
    @PropertyName("cover_url")
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    @PropertyName("bio")
    public String getBio() { return bio; }
    @PropertyName("bio")
    public void setBio(String bio) { this.bio = bio; }

    @PropertyName("genre")
    public String getGenre() { return genre; }
    @PropertyName("genre")
    public void setGenre(String genre) { this.genre = genre; }

    @PropertyName("nationality")
    public String getNationality() { return nationality; }
    @PropertyName("nationality")
    public void setNationality(String nationality) { this.nationality = nationality; }

    @PropertyName("social_spotify")
    public String getSocialSpotify() { return socialSpotify; }
    @PropertyName("social_spotify")
    public void setSocialSpotify(String socialSpotify) { this.socialSpotify = socialSpotify; }

    @PropertyName("social_youtube")
    public String getSocialYoutube() { return socialYoutube; }
    @PropertyName("social_youtube")
    public void setSocialYoutube(String socialYoutube) { this.socialYoutube = socialYoutube; }

    @PropertyName("social_instagram")
    public String getSocialInstagram() { return socialInstagram; }
    @PropertyName("social_instagram")
    public void setSocialInstagram(String socialInstagram) { this.socialInstagram = socialInstagram; }

    @PropertyName("social_facebook")
    public String getSocialFacebook() { return socialFacebook; }
    @PropertyName("social_facebook")
    public void setSocialFacebook(String socialFacebook) { this.socialFacebook = socialFacebook; }

    @PropertyName("social_tiktok")
    public String getSocialTiktok() { return socialTiktok; }
    @PropertyName("social_tiktok")
    public void setSocialTiktok(String socialTiktok) { this.socialTiktok = socialTiktok; }

    @PropertyName("follower_count")
    public int getFollowerCount() { return followerCount; }
    @PropertyName("follower_count")
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }

    @PropertyName("is_verified")
    public boolean isVerified() { return isVerified; }
    @PropertyName("is_verified")
    public void setVerified(boolean verified) { isVerified = verified; }

    @PropertyName("is_active")
    public boolean isActive() { return isActive; }
    @PropertyName("is_active")
    public void setActive(boolean active) { isActive = active; }

    @PropertyName("created_at")
    public com.google.firebase.Timestamp getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(com.google.firebase.Timestamp createdAt) { this.createdAt = createdAt; }

    @PropertyName("updated_at")
    public com.google.firebase.Timestamp getUpdatedAt() { return updatedAt; }
    @PropertyName("updated_at")
    public void setUpdatedAt(com.google.firebase.Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
