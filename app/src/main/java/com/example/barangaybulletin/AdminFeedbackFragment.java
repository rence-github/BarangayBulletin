package com.example.barangaybulletin;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AdminFeedbackFragment extends Fragment {

    public AdminFeedbackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_feedback, container, false);
    }

    // Optional: Add this if you need to pass parameters
    public static AdminFeedbackFragment newInstance() {
        return new AdminFeedbackFragment();
    }
}