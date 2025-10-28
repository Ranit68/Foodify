package com.example.zomatoclone.models;

import com.example.zomatoclone.models.MenuItem;

import java.util.List;

public class Menu {
    private List<MenuItem> Starters;
    private List<MenuItem> MainCourse;
    private List<MenuItem> Desserts;
    private List<MenuItem> Beverages;

    public List<MenuItem> getStarters() {
        return Starters;
    }

    public List<MenuItem> getMainCourse() {
        return MainCourse;
    }

    public List<MenuItem> getDesserts() {
        return Desserts;
    }

    public List<MenuItem> getBeverages() {
        return Beverages;
    }
}
