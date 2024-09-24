package io.proj3ct.Spring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Restaurants {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String district;
    private String cuisineType;
    private String concept;
    private String workingHours;
    private String averageCheck;
    private String menuLink;
    private String reviewsLink;
    private String imageUrl; // Field to hold image URL

    // Constructors, getters, and setters

    public Restaurants() {}

    public Restaurants(String name, String address, String district, String cuisineType, String concept,
                       String workingHours, String averageCheck, String menuLink, String reviewsLink, String imageUrl) {
        this.name = name;
        this.address = address;
        this.district = district;
        this.cuisineType = cuisineType;
        this.concept = concept;
        this.workingHours = workingHours;
        this.averageCheck = averageCheck;
        this.menuLink = menuLink;
        this.reviewsLink = reviewsLink;
        this.imageUrl = imageUrl; // Initialize the imageUrl field
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    public String getAverageCheck() {
        return averageCheck;
    }

    public void setAverageCheck(String averageCheck) {
        this.averageCheck = averageCheck;
    }

    public String getMenuLink() {
        return menuLink;
    }

    public void setMenuLink(String menuLink) {
        this.menuLink = menuLink;
    }

    public String getReviewsLink() {
        return reviewsLink;
    }

    public void setReviewsLink(String reviewsLink) {
        this.reviewsLink = reviewsLink;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
