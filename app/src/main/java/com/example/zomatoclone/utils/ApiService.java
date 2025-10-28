package com.example.zomatoclone.utils;

import com.example.zomatoclone.models.Restaurant;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("restaurants")
    Call<List<Restaurant>> getRestaurants();
}
