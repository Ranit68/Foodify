package com.example.zomatoclone.models;

import java.util.List;
import java.util.Map;

public class Restaurant {
    private String id;
    private String name;
    private String cuisine;
    private String location;
    private double rating;
    private String delivery_time;
    private String image;
    private Map<String, List<MenuItem>> menu;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCuisine() {
        return cuisine;
    }

    public String getLocation() {
        return location;
    }

    public double getRating() {
        return rating;
    }

    public String getDelivery_time() {
        return delivery_time;
    }

    public String getImage() {
        return image;
    }

    public Map<String, List<MenuItem>> getMenu() {
        return menu;
    }

    public void setMenu(Map<String, List<MenuItem>> menu) {
        this.menu = menu;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void setDelivery_time(String delivery_time) {
        this.delivery_time = delivery_time;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
