package org.ed.track.model;

public class CourseModel {
    private String title;
    private String description;
    private String price;
    private String imageUrl;

    public CourseModel() {

    }
    public CourseModel(String courseName, String description, String price) {
        this.title = courseName;
        this.description = description;
        this.price = price;


    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
