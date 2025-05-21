package com.example.barangaybulletin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AnnouncementDialog extends DialogFragment {

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int MAX_IMAGE_SIZE_KB = 1024;

    private Announcement announcement;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private AnnouncementDialogListener listener;

    private ImageView ivAnnouncement;
    private Button btnSelectImage;
    private EditText etTitle, etContent, etEventDate, etEventTime;
    private Uri selectedImageUri;
    private Calendar eventCalendar;
    private String encodedImage = "";

    public interface AnnouncementDialogListener {
        void onAnnouncementSaved();
    }

    public void setAnnouncementDialogListener(AnnouncementDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            announcement = getArguments().getParcelable("announcement");
        }
        eventCalendar = Calendar.getInstance();
        // Set default time to 12:00 PM
        eventCalendar.set(Calendar.HOUR_OF_DAY, 12);
        eventCalendar.set(Calendar.MINUTE, 0);
        eventCalendar.set(Calendar.SECOND, 0);
        eventCalendar.set(Calendar.MILLISECOND, 0);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_announcement, null);

        ivAnnouncement = view.findViewById(R.id.iv_announcement);
        btnSelectImage = view.findViewById(R.id.btn_select_image);
        etTitle = view.findViewById(R.id.et_title);
        etContent = view.findViewById(R.id.et_content);
        etEventDate = view.findViewById(R.id.et_event_date);
        etEventTime = view.findViewById(R.id.et_event_time);

        if (announcement != null) {
            etTitle.setText(announcement.getTitle());
            etContent.setText(announcement.getContent());

            if (announcement.hasImage()) {
                if (announcement.getImageUrl().startsWith("data:image")) {
                    encodedImage = announcement.getImageUrl();
                    Glide.with(this)
                            .load(decodeImage(encodedImage))
                            .into(ivAnnouncement);
                } else {
                    Glide.with(this)
                            .load(announcement.getImageUrl())
                            .into(ivAnnouncement);
                }
            }

            if (announcement.hasEventDate()) {
                eventCalendar.setTimeInMillis(announcement.getEventDate());
                updateDateTimeFields();
            } else {
                updateDateTimeFields(); // Show default date/time
            }
        } else {
            updateDateTimeFields(); // Show default date/time for new announcements
        }

        btnSelectImage.setOnClickListener(v -> openImageChooser());

        etEventDate.setOnClickListener(v -> showDatePickerDialog());
        etEventDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePickerDialog();
        });

        etEventTime.setOnClickListener(v -> showTimePickerDialog());
        etEventTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showTimePickerDialog();
        });

        builder.setView(view)
                .setPositiveButton(announcement == null ? "Add" : "Update", null)
                .setNegativeButton("Cancel", (dialog, which) -> dismiss());

        builder.setTitle(announcement == null ? "Add Announcement" : "Edit Announcement");

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    etTitle.setError("Title is required");
                    return;
                }

                if (TextUtils.isEmpty(content)) {
                    etContent.setError("Content is required");
                    return;
                }

                if (selectedImageUri != null) {
                    try {
                        uploadImageToFirebaseStorage(title, content);
                    } catch (Exception e) {
                        encodeImageAndSave(title, content);
                    }
                } else {
                    saveAnnouncement(title, content,
                            announcement != null ? announcement.getImageUrl() : "");
                }
            });
        });

        return dialog;
    }

    private void updateDateTimeFields() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        etEventDate.setText(dateFormat.format(eventCalendar.getTime()));
        etEventTime.setText(timeFormat.format(eventCalendar.getTime()));
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    eventCalendar.set(Calendar.YEAR, year);
                    eventCalendar.set(Calendar.MONTH, month);
                    eventCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeFields();
                },
                eventCalendar.get(Calendar.YEAR),
                eventCalendar.get(Calendar.MONTH),
                eventCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    eventCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    eventCalendar.set(Calendar.MINUTE, minute);
                    eventCalendar.set(Calendar.SECOND, 0);
                    updateDateTimeFields();
                },
                eventCalendar.get(Calendar.HOUR_OF_DAY),
                eventCalendar.get(Calendar.MINUTE),
                false // 12-hour format
        );
        timePickerDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                InputStream inputStream = getActivity().getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();

                if (bitmap != null) {
                    if (bitmap.getByteCount() > MAX_IMAGE_SIZE_KB * 1024) {
                        bitmap = compressImage(bitmap);
                    }
                    ivAnnouncement.setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap compressImage(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        byte[] compressedData = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(compressedData, 0, compressedData.length);
    }

    private void uploadImageToFirebaseStorage(String title, String content) {
        String fileName = "announcements/" + UUID.randomUUID() + ".jpg";
        StorageReference imageRef = storage.getReference(fileName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveAnnouncement(title, content, uri.toString());
                    }).addOnFailureListener(e -> {
                        encodeImageAndSave(title, content);
                    });
                })
                .addOnFailureListener(e -> {
                    encodeImageAndSave(title, content);
                });
    }

    private void encodeImageAndSave(String title, String content) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
                byte[] byteArray = outputStream.toByteArray();
                encodedImage = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
                saveAnnouncement(title, content, encodedImage);
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
            saveAnnouncement(title, content, "");
        }
    }

    private Bitmap decodeImage(String encodedImage) {
        String pureBase64 = encodedImage.substring(encodedImage.indexOf(",") + 1);
        byte[] decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void saveAnnouncement(String title, String content, String imageUrl) {
        if (announcement == null) {
            Announcement newAnnouncement = new Announcement(title, content);
            newAnnouncement.setImageUrl(imageUrl);
            newAnnouncement.setEventDate(eventCalendar.getTimeInMillis());

            db.collection("announcements").add(newAnnouncement)
                    .addOnSuccessListener(documentReference -> {
                        if (listener != null) listener.onAnnouncementSaved();
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error adding announcement", Toast.LENGTH_SHORT).show();
                    });
        } else {
            announcement.setTitle(title);
            announcement.setContent(content);
            announcement.setEventDate(eventCalendar.getTimeInMillis());
            announcement.setImageUrl(imageUrl);

            db.collection("announcements").document(announcement.getId())
                    .set(announcement)
                    .addOnSuccessListener(aVoid -> {
                        if (listener != null) listener.onAnnouncementSaved();
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error updating announcement", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}