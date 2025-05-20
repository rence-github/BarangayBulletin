package com.example.barangaybulletin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeFragment extends Fragment {

    private RecyclerView announcementsRecyclerView;
    private AnnouncementAdminAdapter adapter;
    private List<Announcement> announcementList;
    private FirebaseFirestore db;
    private ListenerRegistration announcementListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        db = FirebaseFirestore.getInstance();
        announcementList = new ArrayList<>();

        initializeRecyclerView(view);
        setupAddButton(view);
        setupAnnouncementsListener();

        return view;
    }

    private void initializeRecyclerView(View view) {
        announcementsRecyclerView = view.findViewById(R.id.rv_announcements);
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AnnouncementAdminAdapter(announcementList, this::onAnnouncementAction);
        announcementsRecyclerView.setAdapter(adapter);
        announcementsRecyclerView.setHasFixedSize(true);
    }

    private void setupAddButton(View view) {
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_announcement);
        fabAdd.setOnClickListener(v -> showAnnouncementDialog(null));
    }

    private void setupAnnouncementsListener() {
        announcementListener = db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showToast("Error loading announcements");
                        return;
                    }

                    announcementList.clear();
                    value.forEach(document -> {
                        Announcement announcement = document.toObject(Announcement.class);
                        announcement.setId(document.getId());
                        announcementList.add(announcement);
                    });
                    adapter.notifyDataSetChanged();
                });
    }

    private void onAnnouncementAction(Announcement announcement, String action) {
        if (announcement == null) return;

        switch (action) {
            case "edit":
                showAnnouncementDialog(announcement);
                break;
            case "delete":
                showDeleteConfirmation(announcement);
                break;
        }
    }

    private void showDeleteConfirmation(Announcement announcement) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Announcement")
                .setMessage("Are you sure you want to delete this announcement?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAnnouncement(announcement))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAnnouncementDialog(Announcement announcement) {
        AnnouncementDialog dialog = new AnnouncementDialog();
        if (announcement != null) {
            Bundle args = new Bundle();
            args.putParcelable("announcement", announcement);
            dialog.setArguments(args);
        }
        dialog.setAnnouncementDialogListener(() -> {
            // This will be called when announcement is saved
            showToast(announcement == null ? "Announcement added" : "Announcement updated");
        });
        dialog.show(getChildFragmentManager(), "AnnouncementDialog");
    }

    private void deleteAnnouncement(Announcement announcement) {
        db.collection("announcements").document(announcement.getId())
                .delete()
                .addOnSuccessListener(aVoid -> showToast("Announcement deleted"))
                .addOnFailureListener(e -> {
                    showToast("Error deleting announcement");
                    e.printStackTrace();
                });
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (announcementListener != null) {
            announcementListener.remove();
        }
    }
}