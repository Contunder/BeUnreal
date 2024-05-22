package com.supinfo.beunreal.Login.Retrofit;


import com.supinfo.beunreal.Login.Gateway.RegisterDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitAPI {

    @POST("users")
    Call<RegisterDto> createPost(@Body RegisterDto registerDto);
}
