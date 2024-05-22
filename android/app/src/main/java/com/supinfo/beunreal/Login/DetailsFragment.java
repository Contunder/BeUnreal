package com.supinfo.beunreal.Login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.supinfo.beunreal.Login.Gateway.RegisterDto;
import com.supinfo.beunreal.Login.Retrofit.RetrofitAPI;
import com.supinfo.beunreal.R;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Fragment responsible for registering a new user
 */
public class DetailsFragment extends Fragment implements View.OnClickListener {

    private EditText mName;
    private EditText mEmail;
    private EditText mPassword;

    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null)
            view = inflater.inflate(R.layout.fragment_registration_details, container, false);
        else
            container.removeView(view);


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeObjects();
    }


    /**
     * Called any time the user clicks the register button.
     * Does some checks to see if the user is valid and only then does it create the user
     */
    private void register() throws IOException {
        if (mName.getText().length() == 0) {
            mName.setError("please fill this field");
            return;
        }
        if (mEmail.getText().length() == 0) {
            mEmail.setError("please fill this field");
            return;
        }
        if (mPassword.getText().length() == 0) {
            mPassword.setError("please fill this field");
            return;
        }
        if (mPassword.getText().length() < 6) {
            mPassword.setError("password must have at least 6 characters");
            return;
        }


        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();
        final String name = mName.getText().toString();

        postData(name, email, password);

    }

    /**
     * Initializes the UI elements
     */
    private void initializeObjects() {
        mName = view.findViewById(R.id.name);
        mEmail = view.findViewById(R.id.email);
        mPassword = view.findViewById(R.id.password);
        Button mRegister = view.findViewById(R.id.register);

        mRegister.setOnClickListener(this);
    }

    /**
     * Handles onClick events
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.register) {
            try {
                register();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void postData(String name, String email, String password) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://127.0.0.1:8080/api/auth/register/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);

        RegisterDto registerDto = new RegisterDto(name, email, password);

        Call<RegisterDto> call = retrofitAPI.createPost(registerDto);

        call.enqueue(new Callback<RegisterDto>() {
            @Override
            public void onResponse(Call<RegisterDto> call, Response<RegisterDto> response) {
                RegisterDto responseFromAPI = response.body();
                String responseString = "Response Code : " + response.code() + "\nName : " + responseFromAPI.getName() + "\n" + "Email : " + responseFromAPI.getEmail();
                mName.setText(responseString);
            }

            @Override
            public void onFailure(Call<RegisterDto> call, Throwable t) {

                mName.setError("Error found is : " + t.getMessage());
            }
        });
    }

}