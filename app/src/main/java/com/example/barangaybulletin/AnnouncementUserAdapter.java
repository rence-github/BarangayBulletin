package com.example.barangaybulletin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AnnouncementUserAdapter extends RecyclerView.Adapter<AnnouncementUserAdapter.ViewHolder> {

    private static final String TAG = "AnnouncementAdapter";
    private List<Announcement> announcementList;
    private final Executor executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final RequestOptions glideOptions = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image);

    private FavoriteClickListener favoriteClickListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    public interface FavoriteClickListener {
        void onFavoriteUpdated(int position);
    }

    public AnnouncementUserAdapter(List<Announcement> announcementList) {
        this.announcementList = announcementList;
    }

    public void setFavoriteClickListener(FavoriteClickListener listener) {
        this.favoriteClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Announcement announcement = announcementList.get(position);

            // Set text content
            holder.tvTitle.setText(announcement.getTitle());
            holder.tvContent.setText(announcement.getContent());

            // Format dates - do this on a background thread
            executor.execute(() -> {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

                String postedDate = "Posted: " + fullFormat.format(new Date(announcement.getTimestamp()));
                String eventDate = announcement.hasEventDate() ?
                        "Event: " + dateFormat.format(new Date(announcement.getEventDate())) +
                                " at " + timeFormat.format(new Date(announcement.getEventDate())) : null;

                mainHandler.post(() -> {
                    if (holder.getAdapterPosition() == position) {
                        holder.tvPostedDate.setText(postedDate);
                        holder.tvEventDate.setVisibility(eventDate != null ? View.VISIBLE : View.GONE);
                        if (eventDate != null) {
                            holder.tvEventDate.setText(eventDate);
                        }
                    }
                });
            });

            // Set favorite icon
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite);
            int colorRes = announcement.isFavorite() ? R.color.red : R.color.gray;
            holder.ivFavorite.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), colorRes));

            holder.ivFavorite.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    boolean newFavoriteState = !announcement.isFavorite();
                    announcement.setFavorite(newFavoriteState);
                    int newColorRes = newFavoriteState ? R.color.red : R.color.gray;
                    holder.ivFavorite.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), newColorRes));

                    updateFavoriteInFirebase(announcement, newFavoriteState);

                    if (favoriteClickListener != null) {
                        favoriteClickListener.onFavoriteUpdated(adapterPosition);
                    }
                }
            });

            // Handle image display
            if (announcement.hasImage()) {
                holder.ivImage.setVisibility(View.VISIBLE);
                String imageData = announcement.getImageUrl();

                if (imageData.startsWith("data:image")) {
                    final int imageRequestTag = position;
                    holder.ivImage.setTag(imageRequestTag);

                    executor.execute(() -> {
                        try {
                            String base64Image = imageData.split(",")[1];
                            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                            mainHandler.post(() -> {
                                if (holder.ivImage.getTag() != null &&
                                        (int)holder.ivImage.getTag() == imageRequestTag) {
                                    holder.ivImage.setImageBitmap(bitmap);
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error decoding base64 image", e);
                            mainHandler.post(() -> {
                                if (holder.ivImage.getTag() != null &&
                                        (int)holder.ivImage.getTag() == imageRequestTag) {
                                    holder.ivImage.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                } else {
                    Glide.with(holder.itemView.getContext())
                            .load(imageData)
                            .apply(glideOptions)
                            .into(holder.ivImage);
                }
            } else {
                holder.ivImage.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder", e);
        }
    }

    private void updateFavoriteInFirebase(Announcement announcement, boolean isFavorite) {
        String favoriteId = currentUserId + "_" + announcement.getId();

        if (isFavorite) {
            Map<String, Object> favoriteData = new HashMap<>();
            favoriteData.put("announcementId", announcement.getId());
            favoriteData.put("userId", currentUserId);
            favoriteData.put("timestamp", System.currentTimeMillis());

            db.collection("favorites")
                    .document(favoriteId)
                    .set(favoriteData)
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding favorite", e));
        } else {
            db.collection("favorites")
                    .document(favoriteId)
                    .delete()
                    .addOnFailureListener(e -> Log.e(TAG, "Error removing favorite", e));
        }
    }

    @Override
    public int getItemCount() {
        return announcementList != null ? announcementList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivImage, ivFavorite;
        public TextView tvTitle, tvContent, tvPostedDate, tvEventDate;

        public ViewHolder(View view) {
            super(view);
            ivImage = view.findViewById(R.id.iv_image);
            ivFavorite = view.findViewById(R.id.iv_favorite);
            tvTitle = view.findViewById(R.id.tv_title);
            tvContent = view.findViewById(R.id.tv_content);
            tvPostedDate = view.findViewById(R.id.tv_posted_date);
            tvEventDate = view.findViewById(R.id.tv_event_date);
        }
    }
}