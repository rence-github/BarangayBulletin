package com.example.barangaybulletin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdminAdapter extends RecyclerView.Adapter<AnnouncementAdminAdapter.ViewHolder> {

    private List<Announcement> announcementList;
    private AnnouncementActionListener listener;

    public interface AnnouncementActionListener {
        void onAction(Announcement announcement, String action);
    }

    public AnnouncementAdminAdapter(List<Announcement> announcementList, AnnouncementActionListener listener) {
        this.announcementList = announcementList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement announcement = announcementList.get(position);

        // Set announcement content
        holder.tvTitle.setText(announcement.getTitle());
        holder.tvContent.setText(announcement.getContent());

        // Format and set dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        // Posted date (always visible)
        holder.tvPostedDate.setText("Posted: " + fullFormat.format(new Date(announcement.getTimestamp())));

        // Event date (only visible if set)
        if (announcement.hasEventDate()) {
            String eventDateStr = dateFormat.format(new Date(announcement.getEventDate()));
            String eventTimeStr = timeFormat.format(new Date(announcement.getEventDate()));
            holder.tvEventDate.setText("Event: " + eventDateStr + " at " + eventTimeStr);
            holder.tvEventDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvEventDate.setVisibility(View.GONE);
        }

        // Load image if available
        if (announcement.hasImage()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(announcement.getImageUrl())
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        // Set up admin action buttons
        holder.btnEdit.setOnClickListener(v -> listener.onAction(announcement, "edit"));
        holder.btnDelete.setOnClickListener(v -> listener.onAction(announcement, "delete"));
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    public void updateAnnouncements(List<Announcement> newList) {
        announcementList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivImage;
        public TextView tvTitle, tvContent, tvPostedDate, tvEventDate;
        public Button btnEdit, btnDelete;

        public ViewHolder(View view) {
            super(view);
            ivImage = view.findViewById(R.id.iv_image);
            tvTitle = view.findViewById(R.id.tv_title);
            tvContent = view.findViewById(R.id.tv_content);
            tvPostedDate = view.findViewById(R.id.tv_posted_date);
            tvEventDate = view.findViewById(R.id.tv_event_date);
            btnEdit = view.findViewById(R.id.btn_edit);
            btnDelete = view.findViewById(R.id.btn_delete);
        }
    }
}