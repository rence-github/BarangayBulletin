package com.example.barangaybulletin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserHomeFragment extends Fragment implements AnnouncementUserAdapter.FavoriteClickListener {

    private static final String TAG = "UserHomeFragment";
    private static final int PAGE_SIZE = 10; // Number of announcements to load per page

    private RecyclerView announcementsRecyclerView;
    private AnnouncementUserAdapter adapter;
    private List<Announcement> announcementList;
    private FirebaseFirestore db;
    private String currentUserId;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Map<String, Boolean> favoritesMap = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_home, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        announcementList = new ArrayList<>();

        // Initialize views
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        announcementsRecyclerView = view.findViewById(R.id.rv_announcements);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        announcementsRecyclerView.setLayoutManager(layoutManager);
        adapter = new AnnouncementUserAdapter(announcementList);
        adapter.setFavoriteClickListener(this);
        announcementsRecyclerView.setAdapter(adapter);

        // Setup pagination scroll listener
        announcementsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadMoreAnnouncements();
                    }
                }
            }
        });

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // Load data
        loadFavorites();

        return view;
    }

    private void refreshData() {
        lastVisible = null;
        isLastPage = false;
        announcementList.clear();
        adapter.notifyDataSetChanged();
        loadFavorites();
    }

    private void loadFavorites() {
        Log.d(TAG, "Loading favorites...");
        progressBar.setVisibility(View.VISIBLE);

        // First, load all user favorites to a map for quick checking
        db.collection("favorites")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d(TAG, "Found " + task.getResult().size() + " favorites");
                        favoritesMap.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String announcementId = document.getString("announcementId");
                            if (announcementId != null) {
                                favoritesMap.put(announcementId, true);
                            }
                        }

                        // After loading favorites, load announcements
                        loadAnnouncements();
                    } else {
                        Log.e(TAG, "Error loading favorites", task.getException());
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        showToast("Error loading favorites");
                    }
                });
    }

    private void loadAnnouncements() {
        if (isLoading) return;

        isLoading = true;
        Log.d(TAG, "Loading first page of announcements...");

        Query query = db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        executeAnnouncementsQuery(query);
    }

    private void loadMoreAnnouncements() {
        if (isLoading || isLastPage || lastVisible == null) return;

        isLoading = true;
        Log.d(TAG, "Loading more announcements...");
        progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE);

        executeAnnouncementsQuery(query);
    }

    private void executeAnnouncementsQuery(Query query) {
        query.get().addOnCompleteListener(task -> {
            isLoading = false;
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (task.isSuccessful() && task.getResult() != null) {
                int newItems = task.getResult().size();
                Log.d(TAG, "Found " + newItems + " announcements");

                if (newItems < PAGE_SIZE) {
                    isLastPage = true;
                }

                if (newItems > 0) {
                    // Update the last visible item for pagination
                    lastVisible = task.getResult().getDocuments().get(newItems - 1);

                    // Process the results
                    List<Announcement> newAnnouncements = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Announcement announcement = document.toObject(Announcement.class);
                        announcement.setId(document.getId());

                        // Check if this announcement is a favorite
                        boolean isFavorite = favoritesMap.containsKey(announcement.getId());
                        announcement.setFavorite(isFavorite);

                        newAnnouncements.add(announcement);
                    }

                    // Add to the list and notify adapter
                    int positionStart = announcementList.size();
                    announcementList.addAll(newAnnouncements);
                    adapter.notifyItemRangeInserted(positionStart, newAnnouncements.size());
                }
            } else {
                Log.e(TAG, "Error loading announcements", task.getException());
                showToast("Error loading announcements");
            }
        });
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFavoriteUpdated(int position) {
        if (position >= 0 && position < announcementList.size()) {
            Announcement announcement = announcementList.get(position);

            // Update our local favorites map
            if (announcement.isFavorite()) {
                favoritesMap.put(announcement.getId(), true);
            } else {
                favoritesMap.remove(announcement.getId());
            }
        }
    }
}