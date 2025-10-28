package com.example.zomatoclone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.zomatoclone.R;
import com.example.zomatoclone.models.CartItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> cartItemList;
    private final Context context;
    private final DatabaseReference cartRef;
    private final OnQuantityChangeListener listener;

    // Interface to notify activity when total needs update
    public interface OnQuantityChangeListener {
        void onQuantityChanged();
    }

    public CartAdapter(List<CartItem> cartItemList, Context context, OnQuantityChangeListener listener) {
        this.cartItemList = cartItemList;
        this.context = context;
        this.listener = listener;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cartRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId)
                .child("cartItems");
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItemList.get(position);

        holder.textName.setText(item.getName());
        holder.textPrice.setText("₹" + (item.getPrice() * item.getQuantity()));
        Glide.with(context).load(item.getImage()).into(holder.imageFood);

        // ✅ Quantity spinner setup (1–10)
        Integer[] quantities = new Integer[]{1,2,3,4,5,6,7,8,9,10};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, quantities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.quantitySpinner.setAdapter(adapter);

        // Set selected quantity
        holder.quantitySpinner.setSelection(item.getQuantity() - 1);

        // Update Firebase and total when changed
        holder.quantitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int newQuantity = (int) parent.getItemAtPosition(pos);
                if (newQuantity != item.getQuantity()) {
                    item.setQuantity(newQuantity);
                    holder.textPrice.setText("₹" + (item.getPrice() * newQuantity));

                    // ✅ Update Firebase
                    cartRef.child(item.getItemId()).child("quantity").setValue(newQuantity);

                    // Notify CartActivity to update total
                    if (listener != null) listener.onQuantityChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // ✅ Delete button logic remains same
        holder.btnDelete.setOnClickListener(v -> {
            cartRef.child(item.getItemId()).removeValue()
                    .addOnSuccessListener(unused -> {
                        cartItemList.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "Item removed", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onQuantityChanged();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageFood, btnDelete;
        TextView textName, textPrice;
        Spinner quantitySpinner;


        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFood = itemView.findViewById(R.id.imageCartItem);
            textName = itemView.findViewById(R.id.textCartItemName);
            textPrice = itemView.findViewById(R.id.textCartItemPrice);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
            quantitySpinner = itemView.findViewById(R.id.quantitySpinner);
        }
    }

}
