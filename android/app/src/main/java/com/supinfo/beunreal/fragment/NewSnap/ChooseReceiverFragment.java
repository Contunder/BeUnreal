package com.supinfo.beunreal.fragment.NewSnap;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.supinfo.beunreal.Adapter.ReceiverAdapter;
import com.supinfo.beunreal.MainActivity;
import com.supinfo.beunreal.Objects.UserObject;
import com.supinfo.beunreal.R;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This Fragment appears after the user takes a camera shot.
 * It is responsible for letting the user chose what users to send the image to.
 */
public class ChooseReceiverFragment extends Fragment implements View.OnClickListener {

    private RecyclerView.Adapter mAdapter;

    private View view;

    public static ChooseReceiverFragment newInstance() {
        return new ChooseReceiverFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_choose_receiver, container, false);

        initializeObjects();
        listenForData();

        return view;
    }

    /**
     * Initialize UI elements
     */
    private void initializeObjects() {
        FloatingActionButton mSend = view.findViewById(R.id.send);
        mSend.setOnClickListener(this);

        RecyclerView mRecyclerView = view.findViewById(R.id.recyclerView);
        mRecyclerView.setNestedScrollingEnabled(true);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ReceiverAdapter(results, getActivity());
        mRecyclerView.setAdapter(mAdapter);
    }


    private ArrayList<UserObject> results = new ArrayList<>();

    /**
     * Fetches the info for the users the current user is following
     */
    private void listenForData() {
        if (getActivity() == null)
            return;
        for (int i = 0; i < ((MainActivity) getActivity()).listFollowing.size(); i++) {
            DatabaseReference usersDb = FirebaseDatabase.getInstance().getReference().child("users").child(((MainActivity) getActivity()).listFollowing.get(i));
            usersDb.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    UserObject mUser = new UserObject();
                    mUser.parseData(dataSnapshot);

                    if (!results.contains(mUser)) {
                        results.add(mUser);
                        mAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    /**
     * Saves the image that was taken in the previous fragment to the database.
     * Stores first in the storage and only then does it create an entry for this post
     * in the database.
     */
    private void saveToStories() {
        ((MainActivity) requireActivity()).showProgressDialog("sending...");
        Bitmap bitmap = ((MainActivity) requireActivity()).getBitmapToSend();

        final DatabaseReference userStoryDb = FirebaseDatabase.getInstance().getReference().child("users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child("story");
        //creates an id for the post
        final String key = userStoryDb.push().getKey();

        final StorageReference filePath;

        if (key != null) {
            filePath = FirebaseStorage.getInstance().getReference().child("captures").child(key);
        } else {
            return;
        }


        //Getting the image ready for the upload, some compression is made
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] dataToUpload = baos.toByteArray();
        UploadTask uploadTask = filePath.putBytes(dataToUpload);


        uploadTask.addOnSuccessListener(taskSnapshot -> filePath.getDownloadUrl().addOnSuccessListener(uri -> {
            Long currentTimestamp = System.currentTimeMillis();
            Long endTimestamp = currentTimestamp + (24 * 60 * 60 * 1000);

            CheckBox mStory = view.findViewById(R.id.story);
            //if user wants to save to story then save it
            if (mStory.isChecked()) {
                Map<String, Object> mapToUpload = new HashMap<>();
                mapToUpload.put("imageUrl", uri.toString());
                mapToUpload.put("timestampBeg", currentTimestamp);
                mapToUpload.put("timestampEnd", endTimestamp);
                userStoryDb.child(key).setValue(mapToUpload);
            }
            //loop through the checked users and send the image to them
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).getReceive()) {
                    DatabaseReference userDb = FirebaseDatabase.getInstance().getReference().child("users").child(results.get(i).getId()).child("received").child(FirebaseAuth.getInstance().getUid());
                    Map<String, Object> mapToUpload = new HashMap<>();
                    mapToUpload.put("imageUrl", uri.toString());
                    mapToUpload.put("timestampBeg", currentTimestamp);
                    mapToUpload.put("timestampEnd", endTimestamp);
                    userDb.child(key).setValue(mapToUpload);
                }
            }
            ((MainActivity) requireActivity()).clearBackStack();
        }));


        uploadTask.addOnFailureListener(e -> ((MainActivity) requireActivity()).clearBackStack());

    }

    /**
     * handles onClick events
     *
     * @param v - view that was clicked
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.send) {
            saveToStories();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
