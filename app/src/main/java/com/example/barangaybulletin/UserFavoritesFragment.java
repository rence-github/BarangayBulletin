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

public class UserFavoritesFragment extends Fragment {

    private static final String TAG = "FavoritesFragment";
    private RecyclerView favoritesRecyclerView;
    private AnnouncementUserAdapter adapter;
    private List<Announcement> favoriteAnnouncements = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_favorites, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        favoritesRecyclerView = view.findViewById(R.id.rv_favorites);
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AnnouncementUserAdapter(favoriteAnnouncements);
        favoritesRecyclerView.setAdapter(adapter);

        loadFavorites();

        return view;
    }

    private void loadFavorites() {
        Log.d(TAG, "Loading favorites...");
        db.collection("favorites")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d(TAG, "Found " + task.getResult().size() + " favorites");
                        favoriteAnnouncements.clear();
                        for (QueryDocumentSnapshot favoriteDoc : task.getResult()) {
                            String announcementId = favoriteDoc.getString("announcementId");
                            if (announcementId != null) {
                                fetchAnnouncementDetails(announcementId);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error loading favorites", task.getException());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Error loading favorites", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchAnnouncementDetails(String announcementId) {
        db.collection("announcements")
                .document(announcementId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Announcement announcement = documentSnapshot.toObject(Announcement.class);
                        if (announcement != null) {
                            announcement.setId(documentSnapshot.getId());
                            announcement.setFavorite(true);
                            favoriteAnnouncements.add(announcement);
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching announcement details", e);
                });
    }
}