package com.example.zomatoclone.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.zomatoclone.R;
import com.example.zomatoclone.activities.RestaurantDetailActivity;
import com.example.zomatoclone.models.Restaurant;
import com.google.gson.Gson;

import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    private Context context;
    private List<Restaurant> list;

    public RestaurantAdapter(Context context, List<Restaurant> list) {
        this.context = context;
        this.list = list;
    }
    public void setList(List<Restaurant> newList) {
        this.list = newList;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Restaurant restaurant = list.get(position);

        holder.name.setText(restaurant.getName());
        holder.rating.setText(String.format("%.1f", restaurant.getRating()));
        holder.delevTime.setText(restaurant.getDelivery_time());
        holder.cuisine.setText(restaurant.getCuisine());

        Glide.with(context)
                .load(restaurant.getImage())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RestaurantDetailActivity.class);
            intent.putExtra("restaurant_data", new Gson().toJson(restaurant));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, cuisine;
        ImageView image;
        TextView rating;
        TextView delevTime;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.restaurantName);
            image = itemView.findViewById(R.id.restaurantImage);
            rating = itemView.findViewById(R.id.rating);
            delevTime = itemView.findViewById(R.id.deliveryTime);
            cuisine = itemView.findViewById(R.id.cuisine);
        }
    }
}
