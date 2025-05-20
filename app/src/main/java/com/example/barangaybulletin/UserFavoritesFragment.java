package com.example.barangaybulletin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.util.List;

public class UserFavoritesFragment extends Fragment implements AnnouncementUserAdapter.FavoriteClickListener {

    private static final String TAG = "FavoritesFragment";
    private static final int PAGE_SIZE = 10;

    private RecyclerView favoritesRecyclerView;
    private AnnouncementUserAdapter adapter;
    private List<Announcement> favoriteAnnouncements;
    private FirebaseFirestore db;
    private String currentUserId;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_favorites, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favoriteAnnouncements = new ArrayList<>();

        // Initialize views
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyView = view.findViewById(R.id.empty_view);
        favoritesRecyclerView = view.findViewById(R.id.rv_favorites);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        favoritesRecyclerView.setLayoutManager(layoutManager);
        adapter = new AnnouncementUserAdapter(favoriteAnnouncements);
        adapter.setFavoriteClickListener(this);
        favoritesRecyclerView.setAdapter(adapter);

        // Setup pagination scroll listener
        favoritesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        loadMoreFavorites();
                    }
                }
            }
        });

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // Load initial data
        loadFavorites();

        return view;
    }

    private void refreshData() {
        lastVisible = null;
        isLastPage = false;
        favoriteAnnouncements.clear();
        adapter.notifyDataSetChanged();
        loadFavorites();
    }

    private void loadFavorites() {
        if (isLoading) return;

        isLoading = true;
        Log.d(TAG, "Loading favorites...");
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // First query for favorites
        db.collection("favorites")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int count = task.getResult().size();
                        Log.d(TAG, "Found " + count + " favorites");

                        if (count == 0) {
                            // No favorites found
                            handleEmptyFavorites();
                            return;
                        }

                        if (count < PAGE_SIZE) {
                            isLastPage = true;
                        }

                        // Update the last visible item for pagination
                        if (count > 0) {
                            lastVisible = task.getResult().getDocuments().get(count - 1);
                        }

                        // Batch all announcement IDs for a single query
                        List<String> announcementIds = new ArrayList<>();
                        for (QueryDocumentSnapshot favoriteDoc : task.getResult()) {
                            String announcementId = favoriteDoc.getString("announcementId");
                            if (announcementId != null) {
                                announcementIds.add(announcementId);
                            }
                        }

                        // If we have IDs, fetch all announcements in a single batch
                        if (!announcementIds.isEmpty()) {
                            fetchAnnouncementsInBatch(announcementIds);
                        } else {
                            handleEmptyFavorites();
                        }
                    } else {
                        Log.e(TAG, "Error loading favorites", task.getException());
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        showToast("Error loading favorites");
                    }
                });
    }

    private void loadMoreFavorites() {
        if (isLoading || isLastPage || lastVisible == null) return;

        isLoading = true;
        Log.d(TAG, "Loading more favorites...");
        progressBar.setVisibility(View.VISIBLE);

        // Query for the next batch of favorites
        db.collection("favorites")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int count = task.getResult().size();
                        Log.d(TAG, "Found " + count + " more favorites");

                        if (count < PAGE_SIZE) {
                            isLastPage = true;
                        }

                        // Update the last visible item for pagination
                        if (count > 0) {
                            lastVisible = task.getResult().getDocuments().get(count - 1);

                            // Batch all announcement IDs for a single query
                            List<String> announcementIds = new ArrayList<>();
                            for (QueryDocumentSnapshot favoriteDoc : task.getResult()) {
                                String announcementId = favoriteDoc.getString("announcementId");
                                if (announcementId != null) {
                                    announcementIds.add(announcementId);
                                }
                            }

                            // If we have IDs, fetch all announcements in a single batch
                            if (!announcementIds.isEmpty()) {
                                fetchAnnouncementsInBatch(announcementIds);
                            } else {
                                isLoading = false;
                                progressBar.setVisibility(View.GONE);
                            }
                        } else {
                            isLoading = false;
                            progressBar.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Error loading more favorites", task.getException());
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        showToast("Error loading more favorites");
                    }
                });
    }

    private void fetchAnnouncementsInBatch(List<String> announcementIds) {
        // Use the "in" query to get all announcements in one batch
        db.collection("announcements")
                .whereIn("__name__", announcementIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    int positionStart = favoriteAnnouncements.size();
                    int newItems = 0;

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Announcement announcement = document.toObject(Announcement.class);
                        if (announcement != null) {
                            announcement.setId(document.getId());
                            announcement.setFavorite(true);
                            favoriteAnnouncements.add(announcement);
                            newItems++;
                        }
                    }

                    if (newItems > 0) {
                        adapter.notifyItemRangeInserted(positionStart, newItems);
                    }

                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Log.e(TAG, "Error fetching announcements batch", e);
                    showToast("Error loading announcements");
                    updateEmptyView();
                });
    }

    private void handleEmptyFavorites() {
        isLoading = false;
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (favoriteAnnouncements.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            favoritesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            favoritesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFavoriteUpdated(int position) {
        if (position >= 0 && position < favoriteAnnouncements.size()) {
            Announcement announcement = favoriteAnnouncements.get(position);

            // If user unfavorited, remove from the list
            if (!announcement.isFavorite()) {
                favoriteAnnouncements.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmptyView();
            }
        }
    }
}