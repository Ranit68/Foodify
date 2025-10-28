package com.example.zomatoclone.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zomatoclone.R;
import com.example.zomatoclone.adapters.CartAdapter;
import com.example.zomatoclone.models.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnQuantityChangeListener{

    RecyclerView recyclerViewCart;
    Button btnPlaceOrder, selectAddressButton;
    TextView totalItemsText, totalPriceText, selectedAddressText;

    CartAdapter cartAdapter;
    List<CartItem> cartItemList = new ArrayList<>();
    DatabaseReference cartRef, addressRef, orderRef;

    String selectedAddress = "";
    double totalPrice = 0;
    private static final int MAP_REQUEST_CODE = 1010;
    private static final int UPI_PAYMENT_REQUEST_CODE = 123;

    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        totalItemsText = findViewById(R.id.totalItemsText);
        totalPriceText = findViewById(R.id.totalPriceText);
        selectedAddressText = findViewById(R.id.selectedAddressText);
        selectAddressButton = findViewById(R.id.selectAddressButton);

        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cartRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("cartItems");
        addressRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("addresses");
        orderRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("orders");

        cartAdapter = new CartAdapter(cartItemList, this, this);
        recyclerViewCart.setAdapter(cartAdapter);

        fetchCartItems();

        selectAddressButton.setOnClickListener(v -> openAddressSelectionDialog());

        btnPlaceOrder.setOnClickListener(v -> {
            if (selectedAddress.isEmpty()) {
                Toast.makeText(this, "Please select an address first!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (totalPrice <= 0) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            startUPIPayment(totalPrice);
        });
    }

    private void fetchCartItems() {
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartItemList.clear();
                totalPrice = 0;
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    CartItem item = itemSnap.getValue(CartItem.class);
                    if (item != null) {
                        // Default quantity = 1 if not present
                        if (item.getQuantity() == 0) {
                            item.setQuantity(1);
                            cartRef.child(itemSnap.getKey()).child("quantity").setValue(1);
                        }
                        cartItemList.add(item);
                    }
                }
                updateTotalPrice();
                totalItemsText.setText("Total Items: " + cartItemList.size());
                cartAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔄 Updates total when quantity changes
    @Override
    public void onQuantityChanged() {
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        totalPrice = 0;
        for (CartItem item : cartItemList) {
            totalPrice += item.getPrice() * item.getQuantity();
        }
        totalPriceText.setText("Total Price: ₹" + totalPrice);
    }

    private void openAddressSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.address_selection_dialog, null);
        builder.setView(view);

        RadioGroup addressRadioGroup = view.findViewById(R.id.addressRadioGroup);
        Button addAddressButton = view.findViewById(R.id.addAddressButton);

        addressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                addressRadioGroup.removeAllViews();
                for (DataSnapshot addrSnap : snapshot.getChildren()) {
                    String addr = addrSnap.child("fullAddress").getValue(String.class);
                    if (addr != null) {
                        RadioButton rb = new RadioButton(CartActivity.this);
                        rb.setText(addr);
                        rb.setOnClickListener(v -> selectedAddress = addr);
                        addressRadioGroup.addView(rb);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        addAddressButton.setOnClickListener(v -> showAddAddressOptions());

        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedAddressText.setText(selectedAddress.isEmpty() ? "No address selected" : selectedAddress);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddAddressOptions() {
        String[] options = {"Detect Automatically (Map)", "Add Manually"};
        new AlertDialog.Builder(this)
                .setTitle("Add Address")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(CartActivity.this, SelectLocationActivity.class);
                        startActivityForResult(intent, MAP_REQUEST_CODE);
                    } else {
                        openManualAddressDialog();
                    }
                })
                .show();
    }

    private void openManualAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.add_address_dialog, null);
        builder.setView(view);

        EditText name = view.findViewById(R.id.addressName);
        EditText phone = view.findViewById(R.id.addressPhone);
        EditText line1 = view.findViewById(R.id.addressLine1);
        EditText line2 = view.findViewById(R.id.addressLine2);
        EditText pincode = view.findViewById(R.id.addressPincode);
        EditText landmark = view.findViewById(R.id.addressLandmark);
        Button saveButton = view.findViewById(R.id.saveAddressButton);

        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            String fullAddress = name.getText().toString() + ", " + phone.getText().toString() + ", "
                    + line1.getText().toString() + ", " + line2.getText().toString() + ", "
                    + pincode.getText().toString() + ", " + landmark.getText().toString();

            saveAddressToFirebase(fullAddress);
            Toast.makeText(this, "Address Saved!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveAddressToFirebase(String fullAddress) {
        String id = addressRef.push().getKey();
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("fullAddress", fullAddress);
        addressRef.child(id).setValue(map);
    }

    private void startUPIPayment(double amount) {
        String upiId = "yourupiid@oksbi"; // replace with your UPI ID
        String name = "ZomatoClone";
        String note = "Food Order Payment";
        String amountStr = String.format(Locale.getDefault(), "%.2f", amount);

        Uri uri = Uri.parse("upi://pay")
                .buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amountStr)
                .appendQueryParameter("cu", "INR")
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);

        Intent chooser = Intent.createChooser(intent, "Pay with");
        startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String address = data.getStringExtra("address");
            saveAddressToFirebase(address);
        }

        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            handlePaymentResponse(data);
        }
    }

    private void handlePaymentResponse(@Nullable Intent data) {
        if (data != null) {
            String response = data.getStringExtra("response");
            if (response != null && response.toLowerCase().contains("success")) {
                saveOrderToFirebase();
            } else {
                Toast.makeText(this, "Payment Failed or Cancelled", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No Payment Data Received", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrderToFirebase() {
        String orderId = orderRef.push().getKey();
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("address", selectedAddress);
        orderData.put("totalAmount", totalPrice);
        orderData.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        orderData.put("status", "Order Placed");
        orderData.put("items", cartItemList);

        orderRef.child(orderId).setValue(orderData).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Order Confirmed!", Toast.LENGTH_LONG).show();
            cartRef.removeValue();
            cartItemList.clear();
            cartAdapter.notifyDataSetChanged();
            updateTotalPrice();
        });
    }

}
