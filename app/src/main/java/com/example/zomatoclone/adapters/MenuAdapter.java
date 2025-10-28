package com.example.zomatoclone.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.zomatoclone.R;
import com.example.zomatoclone.activities.CartActivity;
import com.example.zomatoclone.models.MenuItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SECTION = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private Context context;
    private List<Object> displayList;
    private Set<Integer> addedItems = new HashSet<>();

    private DatabaseReference userCartRef;
    private String userId, userName, userEmail;

    public MenuAdapter(Context context, List<Object> displayList) {
        this.context = context;
        this.displayList = displayList;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
            userEmail = currentUser.getEmail();
            userName = (currentUser.getDisplayName() != null)
                    ? currentUser.getDisplayName()
                    : "Unknown User";

            userCartRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId)
                    .child("cartItems");
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (displayList.get(position) instanceof String)
                ? VIEW_TYPE_SECTION
                : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SECTION) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_section_header, parent, false);
            return new SectionViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_menu, parent, false);
            return new MenuViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SectionViewHolder) {
            String sectionTitle = (String) displayList.get(position);
            ((SectionViewHolder) holder).textSectionTitle.setText(sectionTitle);
        } else {
            MenuItem item = (MenuItem) displayList.get(position);
            MenuViewHolder menuHolder = (MenuViewHolder) holder;

            menuHolder.textName.setText(item.getName());
            menuHolder.textPrice.setText("₹" + item.getPrice());
            Glide.with(context).load(item.getImage()).into(menuHolder.imageFood);

            // Button state
            if (addedItems.contains(position)) {
                menuHolder.btnAdd.setText("View in Cart");
                menuHolder.btnAdd.setBackgroundResource(R.drawable.btn_added_bg);
            } else {
                menuHolder.btnAdd.setText("Add");
                menuHolder.btnAdd.setBackgroundResource(R.drawable.btn_add_bg);
            }

            // Button click
            menuHolder.btnAdd.setOnClickListener(v -> {
                if (userCartRef == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (addedItems.contains(position)) {
                    // Go to cart
                    Intent intent = new Intent(context, CartActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return;
                }

                // Add to cart
                String itemId = userCartRef.push().getKey();
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());

                Map<String, Object> cartItem = new HashMap<>();
                cartItem.put("itemId", itemId);
                cartItem.put("name", item.getName());
                cartItem.put("price", item.getPrice());
                cartItem.put("image", item.getImage());
                cartItem.put("timestamp", timestamp);

                userCartRef.child(itemId).setValue(cartItem)
                        .addOnSuccessListener(unused -> {
                            addedItems.add(position);
                            menuHolder.btnAdd.setText("View in Cart");
                            menuHolder.btnAdd.setBackgroundResource(R.drawable.btn_added_bg);
                            Toast.makeText(context, item.getName() + " added to cart", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView textSectionTitle;
        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            textSectionTitle = itemView.findViewById(R.id.textSectionTitle);
        }
    }

    public static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView imageFood;
        TextView textName, textPrice, btnAdd;
        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFood = itemView.findViewById(R.id.imageFood);
            textName = itemView.findViewById(R.id.textName);
            textPrice = itemView.findViewById(R.id.textPrice);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}
