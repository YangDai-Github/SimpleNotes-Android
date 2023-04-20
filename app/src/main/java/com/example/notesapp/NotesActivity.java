package com.example.notesapp;

import static com.example.notesapp.FileUtils.FileSaveToInside;
import static com.example.notesapp.FileUtils.deleteSingleFile;
import static com.example.notesapp.FileUtils.getRealPathFromURI;
import static com.example.notesapp.PermissionUtils.verifyStoragePermissions;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notesapp.Models.Notes;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.elevation.SurfaceColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class NotesActivity extends AppCompatActivity {

    LinearLayout linearLayout;
    EditText editText_title, editText_notes;
    TextView textView_date;
    ImageView imageButton, imageButton1;
    Notes notes;
    String images = "";
    String dateStr;
    List<String> paths = new ArrayList<>();
    long id;
    int imageId = 0;
    boolean is_old_note = false;
    Uri uri;
    ActivityResultLauncher<Intent> intentActivityResultLauncher;

    private void initView() {
        linearLayout = findViewById(R.id.linear);
        textView_date = findViewById(R.id.date);
        editText_title = findViewById(R.id.edit_text_title);
        editText_notes = findViewById(R.id.edit_text_notes);
        imageButton = findViewById(R.id.imageButton);
        imageButton1 = findViewById(R.id.imageButton1);
    }

    private void initViewEvents() {
        imageButton.setOnClickListener(v -> {
            editText_title.clearFocus();
            editText_notes.clearFocus();
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                intent.setType("image/*");
                intentActivityResultLauncher.launch(intent);
            } else {
                intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                intentActivityResultLauncher.launch(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.stayout, R.anim.out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.song_detail_toolbar_menu_share) {
            String title = editText_title.getText().toString();
            String description = editText_notes.getText().toString();
            if (description.isEmpty()) {
                Toast.makeText(NotesActivity.this, getString(R.string.toadd), Toast.LENGTH_SHORT).show();
                return false;
            }
            notes.setTitle(title);
            notes.setNotes(description);
            notes.setDate(dateStr);
            StringBuilder temp = new StringBuilder();
            for (int i = 0; i < paths.size(); ++i) {
                temp.append(paths.get(i)).append(" ");
            }
            notes.setImage(temp.toString());
            Intent intent = new Intent();
            intent.putExtra("note", notes);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else if (item.getItemId() == android.R.id.home) {
            if (!is_old_note) {
                for (int i = 0; i < paths.size(); ++i) {
                    deleteSingleFile(paths.get(i));
                }
            }
            finish();
        } else if (item.getItemId() == R.id.time) {
            openCalendar();
        }
        return super.onOptionsItemSelected(item);
    }

    private void openCalendar() {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Uri.parse("content://com.android.calendar/events"))
                .putExtra("title", editText_title.getText().toString())
                .putExtra("description", editText_notes.getText().toString());
        startActivity(intent);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));
        setContentView(R.layout.activity_notes);
        verifyStoragePermissions(this);
        initView();
        initViewEvents();

        intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            //此处是跳转的result回调方法
            new Thread(() -> {
                Bitmap bitmap;
                if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
                    uri = result.getData().getData();
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                    Date date = new Date();
                    String path = getRealPathFromURI(uri, this);
                    bitmap = decodeSampledBitmap(path);
                    paths.add(FileSaveToInside(this, format.format(date), bitmap));
                    runOnUiThread(() -> {
                        ImageView imageView = new ImageView(NotesActivity.this);
                        imageView.setId(imageId++);
                        imageView.setLayoutParams(imageButton1.getLayoutParams());
                        imageView.setImageBitmap(bitmap);
                        imageView.setPadding(8, 0, 8, 0);
                        imageView.setOnClickListener(v -> bigImageLoader(((BitmapDrawable) imageView.getDrawable()).getBitmap()));
                        imageView.setOnLongClickListener(v -> {
                            PopupMenu popupMenu = new PopupMenu(this, imageView);
                            popupMenu.setOnMenuItemClickListener(item -> {
                                if (item.getItemId() == R.id.del) {
                                    int id = imageView.getId();
                                    linearLayout.removeView(imageView);
                                    deleteSingleFile(paths.get(id));
                                    paths.remove(id);
                                    return true;
                                }
                                return false;
                            });
                            popupMenu.inflate(R.menu.pop_up_delete);
                            popupMenu.show();
                            return false;
                        });
                        linearLayout.addView(imageView, linearLayout.getChildCount() - 2);
                    });
                }
            }).start();
        });

        is_old_note = true;
        notes = new Notes();
        if (null != getIntent().getSerializableExtra("old_note")) {
            notes = (Notes) getIntent().getSerializableExtra("old_note");
            editText_title.setText(notes.getTitle());
            editText_notes.setText(notes.getNotes());
            dateStr = notes.getDate();
            textView_date.setText(getString(R.string.create) + dateStr);
            images = notes.getImage();
            if (!images.isEmpty()) {
                List<String> listArr = Arrays.asList(images.trim().split(" "));
                paths = new ArrayList<>(listArr);
                for (int i = 0; i < paths.size(); ++i) {
                    Bitmap bitmap = BitmapFactory.decodeFile(paths.get(i));
                    ImageView imageView = new ImageView(NotesActivity.this);
                    imageView.setId(imageId++);
                    //Glide.with(this).asBitmap().load(paths.get(i)).into(imageView);
                    imageView.setLayoutParams(imageButton1.getLayoutParams());
                    imageView.setImageBitmap(bitmap);
                    imageView.setPadding(8, 0, 8, 0);
                    imageView.setOnClickListener(v -> bigImageLoader(((BitmapDrawable) imageView.getDrawable()).getBitmap()));
                    imageView.setOnLongClickListener(v -> {
                        PopupMenu popupMenu = new PopupMenu(this, imageView);
                        popupMenu.setOnMenuItemClickListener(item -> {
                            if (item.getItemId() == R.id.del) {
                                int id = imageView.getId();
                                linearLayout.removeViewAt(id);
                                deleteSingleFile(paths.get(id));
                                paths.remove(id);
                                return true;
                            }
                            return false;
                        });
                        popupMenu.inflate(R.menu.pop_up_delete);
                        popupMenu.show();
                        return false;
                    });
                    linearLayout.addView(imageView, linearLayout.getChildCount() - 2);
                }
            }
            id = notes.getID();
        } else {
            is_old_note = false;
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("EEE, yyyy-MM-dd hh:mm a");
            Date date = new Date();
            dateStr = format.format(date);
            textView_date.setText(getString(R.string.create) + dateStr);
        }
    }


    private Bitmap decodeSampledBitmap(String path) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        // Calculate inSampleSize
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;     // 屏幕宽度（像素）
        int height = metric.heightPixels;   // 屏幕高度（像素）
        options.inSampleSize = calculateInSampleSize(options, width, height);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            inSampleSize *= 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void bigImageLoader(Bitmap bitmap) {
        final Dialog dialog = new Dialog(this);
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        dialog.setContentView(image);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        image.setOnClickListener(v -> dialog.cancel());
    }
}