package com.example.zomatoclone.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.zomatoclone.R;
import com.example.zomatoclone.adapters.MenuAdapter;
import com.example.zomatoclone.models.MenuItem;
import com.example.zomatoclone.models.Restaurant;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestaurantDetailActivity extends AppCompatActivity {

    private ImageView imageRestaurant;
    private TextView textRestaurantName, textCuisine, textLocation, textRating;
    private RecyclerView recyclerMenu;
    private FloatingActionButton fabMenu;
    private MenuAdapter menuAdapter;

    // Combined list for section headers + items
    private List<Object> displayList = new ArrayList<>();

    // To jump to specific category positions
    private Map<String, Integer> categoryPositions = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);

        imageRestaurant = findViewById(R.id.imageRestaurant);
        textRestaurantName = findViewById(R.id.textRestaurantName);
        textCuisine = findViewById(R.id.textCuisine);
        textLocation = findViewById(R.id.textLocation);
        textRating = findViewById(R.id.textRating);
        recyclerMenu = findViewById(R.id.recyclerMenu);
        fabMenu = findViewById(R.id.fabMenu);

        recyclerMenu.setLayoutManager(new LinearLayoutManager(this));
        menuAdapter = new MenuAdapter(this, displayList);
        recyclerMenu.setAdapter(menuAdapter);

        try {
            String restaurantJson = getIntent().getStringExtra("restaurant_data");
            Restaurant restaurant = new Gson().fromJson(restaurantJson, Restaurant.class);

            if (restaurant != null) {
                updateUI(restaurant);
            } else {
                Toast.makeText(this, "No restaurant data found", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        fabMenu.setOnClickListener(v -> showCategoryMenu());
    }

    private void updateUI(Restaurant restaurant) {
        textRestaurantName.setText(restaurant.getName());
        textCuisine.setText(restaurant.getCuisine());
        textLocation.setText(restaurant.getLocation());
        textRating.setText("⭐ " + restaurant.getRating());

        Glide.with(this)
                .load(restaurant.getImage())
                .placeholder(R.drawable.ic_launcher_background)
                .into(imageRestaurant);

        // Combine menu items with headers
        displayList.clear();
        categoryPositions.clear();

        int position = 0;
        for (Map.Entry<String, List<MenuItem>> entry : restaurant.getMenu().entrySet()) {
            String category = entry.getKey();
            List<MenuItem> items = entry.getValue();

            // Add section header
            displayList.add(category);
            categoryPositions.put(category, position);
            position++;

            // Add all menu items in this category
            displayList.addAll(items);
            position += items.size();
        }

        menuAdapter.notifyDataSetChanged();
    }

    private void showCategoryMenu() {
        PopupMenu popupMenu = new PopupMenu(this, fabMenu);

        // Add categories dynamically
        for (String category : categoryPositions.keySet()) {
            popupMenu.getMenu().add(category);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            String selectedCategory = item.getTitle().toString();
            Integer position = categoryPositions.get(selectedCategory);
            if (position != null) {
                recyclerMenu.smoothScrollToPosition(position);
            }
            return true;
        });

        popupMenu.show();
    }
}
