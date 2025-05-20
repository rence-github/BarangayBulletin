package com.example.barangaybulletin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserHomeFragment extends Fragment implements AnnouncementUserAdapter.FavoriteClickListener {

    private RecyclerView announcementsRecyclerView;
    private AnnouncementUserAdapter adapter;
    private List<Announcement> announcementList;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_home, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        announcementList = new ArrayList<>();

        announcementsRecyclerView = view.findViewById(R.id.rv_announcements);
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AnnouncementUserAdapter(announcementList);
        adapter.setFavoriteClickListener(this);
        announcementsRecyclerView.setAdapter(adapter);
        announcementsRecyclerView.setHasFixedSize(true);

        loadAnnouncements();

        return view;
    }

    private void loadAnnouncements() {
        Log.d("UserHomeFragment", "Loading announcements...");
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d("UserHomeFragment", "Found " + task.getResult().size() + " announcements");
                        announcementList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("UserHomeFragment", "Processing document: " + document.getId());
                            Announcement announcement = document.toObject(Announcement.class);
                            announcement.setId(document.getId());
                            announcement.setFavorite(false);
                            checkIfFavorite(announcement);
                            announcementList.add(announcement);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("UserHomeFragment", "Error loading announcements", task.getException());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Error loading announcements", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkIfFavorite(Announcement announcement) {
        db.collection("favorites")
                .document(currentUserId + "_" + announcement.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    announcement.setFavorite(documentSnapshot.exists());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserHomeFragment", "Error checking favorite status", e);
                });
    }

    @Override
    public void onFavoriteUpdated(int position) {
        // Optional: Handle any UI updates after favorite change
    }
}