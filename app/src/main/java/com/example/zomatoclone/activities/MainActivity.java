package com.example.zomatoclone.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zomatoclone.R;
import com.example.zomatoclone.adapters.RestaurantAdapter;
import com.example.zomatoclone.models.MenuItem;
import com.example.zomatoclone.models.Restaurant;
import com.example.zomatoclone.utils.ApiService;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    RestaurantAdapter adapter;
    List<Restaurant> allRestaurants = new ArrayList<>();
    LinearLayout categoryLayout;
    EditText searchEditText;
    SwitchCompat vegSwitch;
    TextView userHeader; // for showing username and address

    String selectedCategory = "All";
    DatabaseReference userRef;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        categoryLayout = findViewById(R.id.categoryLayout);
        searchEditText = findViewById(R.id.searchEditText);
        vegSwitch = findViewById(R.id.vegSwitch);
        userHeader = findViewById(R.id.userHeader); // Add this TextView in XML (top of screen)

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RestaurantAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Firebase user setup
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        loadUserInfo();
        loadRestaurants();
        setupCategories();
        setupSearchAndFilter();
    }

    private void loadUserInfo() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String address = "";

                // Get first address if exists
                DataSnapshot addresses = snapshot.child("addresses");
                if (addresses.exists()) {
                    for (DataSnapshot addrSnap : addresses.getChildren()) {
                        address = addrSnap.child("fullAddress").getValue(String.class);
                        break;
                    }
                }

                if (name == null) name = "User";
                if (address == null || address.isEmpty())
                    address = "Add your delivery address";

                // Truncate address to one line if too long
                if (address.length() > 33)
                    address = address.substring(0, 33) + "...";

                userHeader.setText(String.format(Locale.getDefault(),
                        "👋 Hi, %s\nDelivering to: %s", name, address));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_ERROR", "Error fetching user info: " + error.getMessage());
            }
        });
    }

    private void loadRestaurants() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://zomato-api-eeio.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getRestaurants().enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(Call<List<Restaurant>> call, Response<List<Restaurant>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRestaurants = response.body();
                    filterData();
                }
            }

            @Override
            public void onFailure(Call<List<Restaurant>> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void setupCategories() {
        String[] categories = {"All", "Biryani", "Pizza", "Cake", "Paneer", "Burger", "Ice Cream"};
        int[] categoryImages = {
                R.drawable.all,
                R.drawable.biriyani,
                R.drawable.pizza,
                R.drawable.cake,
                R.drawable.paneer,
                R.drawable.burger,
                R.drawable.icecream,
        };
        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];

            // Inflate the custom category layout
            MaterialCardView categoryItem = (MaterialCardView) getLayoutInflater()
                    .inflate(R.layout.item_category, categoryLayout, false);

            ImageView categoryImage = categoryItem.findViewById(R.id.categoryImage);
            TextView categoryName = categoryItem.findViewById(R.id.categoryName);

            categoryImage.setImageResource(categoryImages[i]);
            categoryName.setText(category);

            categoryItem.setOnClickListener(v -> {
                clearChipSelection();
                categoryItem.setSelected(true);
                selectedCategory = category;
                filterData();
            });

            categoryLayout.addView(categoryItem);
        }
    }

    private void clearChipSelection() {
        for (int i = 0; i < categoryLayout.getChildCount(); i++) {
            categoryLayout.getChildAt(i).setSelected(false);
        }
    }

    private void setupSearchAndFilter() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData();
            }
        });

        vegSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> filterData());
    }

    private void filterData() {
        String query = searchEditText.getText().toString().toLowerCase();
        boolean vegOnly = vegSwitch.isChecked();

        if (!vegOnly && query.isEmpty() && selectedCategory.equals("All")) {
            adapter.setList(new ArrayList<>(allRestaurants));
            adapter.notifyDataSetChanged();
            return;
        }

        List<Restaurant> filteredList = new ArrayList<>();

        for (Restaurant res : allRestaurants) {
            if (res.getMenu() == null) continue;

            Map<String, List<MenuItem>> filteredMenu = new HashMap<>();
            boolean restaurantMatches = res.getName().toLowerCase().contains(query);

            for (Map.Entry<String, List<MenuItem>> entry : res.getMenu().entrySet()) {
                List<MenuItem> filteredItems = new ArrayList<>();

                for (MenuItem item : entry.getValue()) {
                    if (item.getName() == null) continue;
                    String itemName = item.getName().toLowerCase();
                    boolean itemIsVeg = item.isVeg();

                    boolean matchesVeg = !vegOnly || itemIsVeg;
                    boolean matchesSearch = itemName.contains(query) || restaurantMatches;
                    boolean matchesCategory = selectedCategory.equals("All")
                            || itemName.contains(selectedCategory.toLowerCase());

                    if (matchesVeg && matchesSearch && matchesCategory) {
                        filteredItems.add(item);
                    }
                }

                if (!filteredItems.isEmpty()) {
                    filteredMenu.put(entry.getKey(), filteredItems);
                }
            }

            if (!filteredMenu.isEmpty()) {
                Restaurant filteredRes = new Restaurant();
                filteredRes.setName(res.getName());
                filteredRes.setImage(res.getImage());
                filteredRes.setMenu(filteredMenu);
                filteredList.add(filteredRes);
            }
        }

        adapter.setList(filteredList);
        adapter.notifyDataSetChanged();
    }
}
