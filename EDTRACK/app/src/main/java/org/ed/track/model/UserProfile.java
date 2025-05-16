package org.ed.track.model;

public class UserProfile {
    // common fields
    private String userId;
    private String name;
    private String email;
    private String location;
    private String phone;
    private String role;
    private String imageUrl;

    // teacher specific fields
    private String qualification;
    private String bio;
    private String availableTime;

    // student specific fields
    private String budget;
    private String subject;

    // constructor
    public UserProfile() {
        // Firestore requires empty constructor
    }

    // Getters and setters...


    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getLocation() {
        return location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPhone() {
        return phone;
    }


    // teacher specific getters
    public String getQualification() {
        return qualification;
    }

    public String getBio() {
        return bio;
    }

    public String getAvailableTime() {
        return availableTime;
    }

    // student specific getters
    public String getBudget() {
        return budget;
    }

    public String getSubject() {
        return subject;
    }

}

