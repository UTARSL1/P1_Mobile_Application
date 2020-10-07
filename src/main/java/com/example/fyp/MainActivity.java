package com.example.fyp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseCustomLocalModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int LAUNCH_CAMERA_CODE = 500;
    private static int READ_STORAGE_REQUEST_CODE = 0;
    private static int WRITE_REQUEST_CODE = 20;
    private static int CAMERA_CODE = 1000;
    int SCALE_SIZE = 672;
    ViewFlipper viewFlipper, txtFlipper;
    Button analyse_btn, edit_btn, save_btn;
    ImageButton next_btn, previous_btn, setting_btn, doc_btn;
    TextView indicator, demo;
    RelativeLayout info_layout;
    Bitmap view_image;
    Uri image_uri, cropped_uri;
    String[] classes = {"Malignant", "Benign"};
    float CONFIDENCE_THRESHOLD;
    boolean ONLY_SHOW_MAX;
    private InterstitialAd mInterstitialAd;
    private static final String Ad_id = "ca-app-pub-5224279588036591/4085845472";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(getApplicationContext(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(Ad_id);
        final AdRequest.Builder adRequest_builder = new AdRequest.Builder();
        if (!mInterstitialAd.isLoading() && !mInterstitialAd.isLoaded()) {
            mInterstitialAd.loadAd(adRequest_builder.build());
        }

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                Log.d("ADS", "Ad loaded");
            }

            @Override
            public void onAdClosed() {
                mInterstitialAd.loadAd(adRequest_builder.build());
            }
        });

        analyse_btn = findViewById(R.id.analyse_btn);
        edit_btn = findViewById(R.id.edit_btn);
        info_layout = findViewById(R.id.Info);
        viewFlipper = findViewById(R.id.v_flip);
        viewFlipper.setClipToOutline(true);
        txtFlipper = findViewById(R.id.txt_flipper);
        next_btn = findViewById(R.id.left_btn);
        previous_btn = findViewById(R.id.right_btn);
        save_btn = findViewById(R.id.save_btn);
        setting_btn = findViewById(R.id.setting_btn);
        doc_btn = findViewById(R.id.doc_btn);
        indicator = findViewById(R.id.indicator);
        demo = findViewById(R.id.demo);

        final FirebaseModelInterpreter interpreter = create_model_interpreter();
        final FirebaseModelInputOutputOptions inputOutputOption = set_model_input_output();

        PushDownAnim.setPushDownAnimTo(viewFlipper).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDialog();
            }
        });

        PushDownAnim.setPushDownAnimTo(analyse_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = getSharedPreferences("Setting", MODE_PRIVATE);
                ONLY_SHOW_MAX = sharedPreferences.getBoolean("max_show", false);
                CONFIDENCE_THRESHOLD = sharedPreferences.getInt("threshold", 4) / 10.0f;
                try {
                    initiate_detection(view_image, inputOutputOption, interpreter);
                } catch (FirebaseMLException e) {
                    e.printStackTrace();
                }
            }
        });

        edit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start_Crop(image_uri);
            }
        });

        next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewFlipper.setInAnimation(MainActivity.this, R.anim.slide_in_right);
                viewFlipper.setOutAnimation(MainActivity.this, R.anim.slide_out_left);
                txtFlipper.setInAnimation(MainActivity.this, R.anim.slide_in_right);
                txtFlipper.setOutAnimation(MainActivity.this, R.anim.slide_out_left);
                viewFlipper.showNext();
                txtFlipper.showNext();
//                change_layout_color(true);
                update_indicator(viewFlipper, indicator);

            }
        });

        previous_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewFlipper.setInAnimation(MainActivity.this, R.anim.slide_in_left);
                viewFlipper.setOutAnimation(MainActivity.this, R.anim.slide_out_right);
                txtFlipper.setInAnimation(MainActivity.this, R.anim.slide_in_left);
                txtFlipper.setOutAnimation(MainActivity.this, R.anim.slide_out_right);
                viewFlipper.showPrevious();
                txtFlipper.showPrevious();
//                change_layout_color(true);
                update_indicator(viewFlipper, indicator);
            }
        });

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageView bitmap_view = (ImageView) viewFlipper.getCurrentView();
                Bitmap bitmap = ((BitmapDrawable) bitmap_view.getDrawable()).getBitmap();
                String pic_name = pic_nameInfo();
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        saveImage(bitmap, pic_name + "_" + System.currentTimeMillis());
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Image Not Saved", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
                }

            }
        });
        setting_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSetting();
            }
        });

        doc_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDoc();
            }
        });

        demo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("android.resource://com.example.fyp/drawable/demo_pic");
                if (CropImage.isReadExternalStoragePermissionsRequired(MainActivity.this, uri)) {
                    image_uri = uri;
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_STORAGE_REQUEST_CODE);
                } else {
                    image_uri = uri;
                    start_Crop(uri);
                }
            }
        });
    }

    private void openDoc() {
        Intent intent = new Intent(this, Document.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void openSetting() {
        Intent intent = new Intent(this, Setting.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageuri = CropImage.getPickImageResultUri(this, data);
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageuri)) {
                image_uri = imageuri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_STORAGE_REQUEST_CODE);
            } else {
                image_uri = imageuri;
                start_Crop(imageuri);
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                assert result != null;
                cropped_uri = result.getUri();
                try {
                    view_image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), cropped_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                view_image = Bitmap.createScaledBitmap(view_image, SCALE_SIZE, SCALE_SIZE, true);
                viewFlipper.removeAllViews();
                ImageView imageView1 = new ImageView(this);
                imageView1.setImageBitmap(view_image);
                viewFlipper.addView(imageView1);
                edit_btn.setVisibility(View.VISIBLE);
                edit_btn.setEnabled(true);
                hide_analyzeBtn(false);
                save_btn.setVisibility(View.INVISIBLE);
                save_btn.setEnabled(false);
                indicator.setVisibility(View.INVISIBLE);

                if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("ADS", "The interstitial wasn't loaded yet.");
                }
            }
        }

        if (requestCode == LAUNCH_CAMERA_CODE && resultCode == Activity.RESULT_OK) {
            byte[] result = data.getByteArrayExtra("result");
            view_image = BitmapFactory.decodeByteArray(result, 0, result.length);
            viewFlipper.removeAllViews();
            ImageView imageView1 = new ImageView(this);
            imageView1.setImageBitmap(view_image);
            viewFlipper.addView(imageView1);
            edit_btn.setVisibility(View.INVISIBLE);
            edit_btn.setEnabled(false);
            hide_analyzeBtn(false);
            save_btn.setVisibility(View.INVISIBLE);
            save_btn.setEnabled(false);
            indicator.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start_Crop(image_uri);
            } else {
                Toast.makeText(this, "Require storage access permission", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == CAMERA_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent camera = new Intent(getApplicationContext(), CameraActivity.class);
                startActivityForResult(camera, LAUNCH_CAMERA_CODE);
            } else {
                Toast.makeText(this, "Require camera permission", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == WRITE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ImageView bitmap_view = (ImageView) viewFlipper.getCurrentView();
                Bitmap bitmap = ((BitmapDrawable) bitmap_view.getDrawable()).getBitmap();
                String pic_name = pic_nameInfo();
                try {
                    saveImage(bitmap, pic_name + "_" + System.currentTimeMillis());
                } catch (IOException e) {
                    Toast.makeText(this, "Error, image not saved", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Require storage access permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void start_Crop(Uri uri) {
        int size = (int) (getResources().getDisplayMetrics().densityDpi * 75 / 160);
        CropImage.activity(uri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setActivityMenuIconColor(Color.DKGRAY)
                .setAutoZoomEnabled(false)
                .setMultiTouchEnabled(false)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setFixAspectRatio(true)
                .setAspectRatio(1, 1)
                .setMinCropWindowSize(size, size)
                .setActivityTitle("Crop")
                .setMinCropResultSize(224, 224)
                .setBorderCornerThickness(1)
                .setAllowFlipping(false)
                .start(this);
    }

    public ByteBuffer get_Input(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        ByteBuffer imgData = ByteBuffer.allocateDirect(224 * 224 * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                int pixelValue = intValues[i * 224 + j];
                imgData.putFloat((((pixelValue >> 16) & 0xFF) - 128.0f) / 128.0f);
                imgData.putFloat((((pixelValue >> 8) & 0xFF) - 128.0f) / 128.0f);
                imgData.putFloat(((pixelValue & 0xFF) - 128.0f) / 128.0f);
            }
        }
        return imgData;
    }

    public FirebaseModelInterpreter create_model_interpreter() {
        FirebaseCustomLocalModel localModel = new FirebaseCustomLocalModel.Builder()
                .setAssetFilePath("detect15k.tflite")
                .build();

        FirebaseModelInterpreter interpreter = null;
        try {
            FirebaseModelInterpreterOptions options = new FirebaseModelInterpreterOptions.Builder(localModel).build();
            interpreter = FirebaseModelInterpreter.getInstance(options);
        } catch (FirebaseMLException e) {
            Log.d("Error", "Build Model Error");
        }
        return interpreter;
    }

    public FirebaseModelInputOutputOptions set_model_input_output() {
        FirebaseModelInputOutputOptions inputOutputOptions =
                null;
        try {
            inputOutputOptions = new FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3})
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 5, 4})
                    .setOutputFormat(1, FirebaseModelDataType.FLOAT32, new int[]{1, 5})
                    .setOutputFormat(2, FirebaseModelDataType.FLOAT32, new int[]{1, 5})
                    .setOutputFormat(3, FirebaseModelDataType.FLOAT32, new int[]{1})
                    .build();
        } catch (FirebaseMLException e) {
            Toast.makeText(this, "Cannot set input output for model", Toast.LENGTH_LONG).show();
        }
        return inputOutputOptions;
    }

    public void initiate_detection(Bitmap bitmap, FirebaseModelInputOutputOptions inputOutputOptions, FirebaseModelInterpreter interpreter) throws FirebaseMLException {
        final Trace myTrace = FirebasePerformance.getInstance().newTrace("inference time");
        myTrace.start();
        ByteBuffer input;
        input = get_Input(bitmap);
        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                .add(input)
                .build();
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Processing");
        dialog.show();
        interpreter.run(inputs, inputOutputOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseModelOutputs>() {
                            @Override
                            public void onSuccess(FirebaseModelOutputs result) {
                                float[][][] boxes_array = result.getOutput(0);
                                float[][] classes_array = result.getOutput(1);
                                float[][] confidence_array = result.getOutput(2);
                                float[] classes = classes_array[0];
                                float[] confidence = confidence_array[0];
                                float[][] box = boxes_array[0];
                                drawRect(view_image, box, classes, confidence, ONLY_SHOW_MAX);
                                dialog.dismiss();
                                myTrace.stop();
                            }
                        })

                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                dialog.dismiss();
                                myTrace.stop();
                                Toast.makeText(MainActivity.this, "Fail to Perform Detection", Toast.LENGTH_LONG).show();
                            }
                        });
        interpreter.close();
    }

    public int getMaxId(float[] confidence_arr) {
        int length = confidence_arr.length;
        float max = (float) 0.0;
        int Id = 0;
        for (int i = 0; i < length; i++) {
            if (confidence_arr[i] > max) {
                max = confidence_arr[i];
                Id = i;
            }
        }
        return Id;
    }

    public void drawRect(final Bitmap bitmap, float[][] box_arr, float[] label_class, float[] confidence_score, final Boolean max) {
        float h = bitmap.getHeight(), w = bitmap.getWidth();
        float y, x, bottom, right;
        String s;
        ArrayList<ImageView> group_view = new ArrayList<>();
        ArrayList<TextView> group_txt = new ArrayList<>();
        int SCALE_SIZE = bitmap.getWidth();
        float ROUND_RADIUS = (float) (SCALE_SIZE / 29.86);
        float STROKE_WIDTH = (float) (SCALE_SIZE / 89.6);
        Paint paint = new Paint();
        Paint txt_paint = new Paint();
        Paint label_paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);
        txt_paint.setColor(getColor(R.color.marble_white));
        float TEXT_SIZE = (float) (SCALE_SIZE / 25.6);
        int TEXT_HEIGHT = SCALE_SIZE / 17;
        txt_paint.setTextSize(TEXT_SIZE);
        txt_paint.setAntiAlias(true);
        label_paint.setStyle(Paint.Style.FILL);
        viewFlipper.removeAllViews();
        txtFlipper.removeAllViews();
        int red = Color.RED;
        int green = Color.GREEN;

        if (max) {
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            int id = getMaxId(confidence_score);
            y = box_arr[id][0] * h;
            x = box_arr[id][1] * w;
            bottom = box_arr[id][2] * h;
            right = box_arr[id][3] * w;
            if (label_class[id] == 0) {
                paint.setColor(red);
                label_paint.setColor(red);
            } else {
                paint.setColor(green);
                label_paint.setColor(green);
            }
            s = classes[(int) label_class[id]] + " " + String.format("%.2f", confidence_score[id] * 100) + "%";
            RectF rectangle = new RectF((int) x, (int) y, (int) right, (int) bottom);
            canvas.drawRoundRect(rectangle, ROUND_RADIUS, ROUND_RADIUS, paint);
            RectF label_rect;
            if (y > 80) {
                label_rect = new RectF(x, y - TEXT_HEIGHT, x + txt_paint.measureText(s), y);
                canvas.drawRect(label_rect, label_paint);
                canvas.drawText(s, x, y - 10, txt_paint);
            } else {
                label_rect = new RectF(x + 10, y, x + 10 + txt_paint.measureText(s), y + TEXT_HEIGHT);
                canvas.drawRect(label_rect, label_paint);
                canvas.drawText(s, x + 10, y + 30, txt_paint);
            }
            ImageView temp_view = new ImageView(this);
            temp_view.setImageBitmap(mutableBitmap);
            TextView output_view = getInfo(label_class[id], confidence_score[id]);
            group_view.add(temp_view);
            group_txt.add(output_view);
        } else {
            for (int i = 0; i < confidence_score.length; i++) {
                Bitmap bitmap_copy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                if (confidence_score[i] >= CONFIDENCE_THRESHOLD) {
                    Canvas canvas = new Canvas(bitmap_copy);
                    y = box_arr[i][0] * h;
                    x = box_arr[i][1] * w;
                    bottom = box_arr[i][2] * h;
                    right = box_arr[i][3] * w;

                    if (label_class[i] == 0) {
                        paint.setColor(red);
                        label_paint.setColor(red);
                    } else {
                        paint.setColor(green);
                        label_paint.setColor(green);
                    }
                    s = classes[(int) label_class[i]] + " " + String.format("%.2f", confidence_score[i] * 100) + "%";
                    RectF rectangle = new RectF((int) x, (int) y, (int) right, (int) bottom);
                    canvas.drawRoundRect(rectangle, ROUND_RADIUS, ROUND_RADIUS, paint);
                    RectF label_rect;
                    if (y > 80) {
                        label_rect = new RectF(x, y - TEXT_HEIGHT, x + txt_paint.measureText(s), y);
                        canvas.drawRect(label_rect, label_paint);
                        canvas.drawText(s, x, y - 10, txt_paint);
                    } else {
                        label_rect = new RectF(x + 10, y, x + 10 + txt_paint.measureText(s), y + TEXT_HEIGHT);
                        canvas.drawRect(label_rect, label_paint);
                        canvas.drawText(s, x + 10, y + 30, txt_paint);
                    }
                    ImageView temp_view = new ImageView(this);
                    temp_view.setImageBitmap(bitmap_copy);
                    TextView output_view = getInfo(label_class[i], confidence_score[i]);
                    group_view.add(temp_view);
                    group_txt.add(output_view);
                }
            }
        }
        if (group_view.size() == 0) {
            ImageView temp_view = show_oriImage(bitmap);
            TextView no_result = show_noResult();
            disable_btn(true);
//                    change_layout_color(false);
            viewFlipper.addView(temp_view);
            txtFlipper.addView(no_result);
        } else {
            for (int i = 0; i < group_view.size(); i++) {
                viewFlipper.addView(group_view.get(i));
                txtFlipper.addView(group_txt.get(i));
            }
            if (group_view.size() == 1 || max) {
                disable_btn(true);
            } else {
                disable_btn(false);
            }
            save_btn.setVisibility(View.VISIBLE);
            save_btn.setEnabled(true);
//                    change_layout_color(true);
            update_indicator(viewFlipper, indicator);
        }
        hide_analyzeBtn(true);
    }


    public TextView getInfo(float classId, float scores) {
        TextView txt_view = new TextView(this);
        if (classId == 0.0) {
            txt_view.setTextColor(getColor(R.color.red));
        } else {
            txt_view.setTextColor(getColor(R.color.start));
        }
        txt_view.setTextSize(19);
        txt_view.setGravity(Gravity.CENTER);
        txt_view.setTypeface(null, Typeface.BOLD);
        String class_type = classes[(int) classId];
        String s = String.format("%.2f", scores * 100);
        String final_str = s + "% is " + class_type;
        txt_view.setText(final_str);
        return txt_view;
    }

    public String pic_nameInfo() {
        TextView temp = (TextView) txtFlipper.getCurrentView();
        String[] info_list = temp.getText().toString().split(" ");
        return info_list[2] + "_" + info_list[0];
    }

    public void disable_btn(boolean True) {
        if (True) {
            next_btn.setVisibility(View.INVISIBLE);
            previous_btn.setVisibility(View.INVISIBLE);
            next_btn.setEnabled(false);
            previous_btn.setEnabled(false);
        } else {
            next_btn.setVisibility(View.VISIBLE);
            previous_btn.setVisibility(View.VISIBLE);
            next_btn.setEnabled(true);
            previous_btn.setEnabled(true);
        }
    }

    public TextView show_noResult() {
        TextView no_result = new TextView(this);
        no_result.setText(R.string.no_result);
        no_result.setTextColor(getColor(R.color.end));
        no_result.setTextSize(19);
        no_result.setGravity(Gravity.CENTER);
        no_result.setTypeface(null, Typeface.BOLD);
        return no_result;
    }

    public ImageView show_oriImage(Bitmap bitmap) {
        ImageView temp_view = new ImageView(this);
        temp_view.setImageBitmap(bitmap);
        return temp_view;
    }

    private void saveImage(Bitmap bitmap, @NonNull String name) throws IOException {
        boolean saved;
        OutputStream fos;
        File image;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/" + "Lession");
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            assert imageUri != null;
            fos = resolver.openOutputStream(imageUri);
            saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            assert fos != null;
            fos.flush();
            fos.close();
        } else {
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
            if (!root.exists()) {
                root.mkdir();
            }
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES).toString() + File.separator + "Lession";

            File file = new File(imagesDir);

            if (!file.exists()) {
                file.mkdir();
            }

            image = new File(imagesDir, name + ".png");
            fos = new FileOutputStream(image);
            saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(image);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }
        if (saved) {
            Toast.makeText(getApplicationContext(), "Image Saved", Toast.LENGTH_SHORT).show();
        }
    }

    public void hide_analyzeBtn(boolean True) {
        if (True) {
            analyse_btn.setEnabled(false);
            analyse_btn.setClickable(false);
            info_layout.setAlpha(1);
            analyse_btn.animate().alpha(0).setDuration(150);
        } else {
            analyse_btn.setEnabled(true);
            analyse_btn.setClickable(true);
            analyse_btn.setElevation(5);
            analyse_btn.setAlpha(1);
            info_layout.setAlpha(0);
        }
    }

    public void update_indicator(ViewFlipper viewFlipper, TextView textView) {
        textView.setVisibility(View.VISIBLE);
        int child = viewFlipper.getChildCount();
        String indicator = (viewFlipper.getDisplayedChild() + 1) + " of " + child;
        textView.setText(indicator);
    }

    public void openDialog() {
        DialogActivity chooser = new DialogActivity();
        chooser.show(getSupportFragmentManager(), "Choose");
        chooser.setDialogResult(new DialogActivity.OnMyDialogResult() {
            @Override
            public void finish(String result) {
                if (result.equals("Camera")) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        Intent camera = new Intent(getApplicationContext(), CameraActivity.class);
                        startActivityForResult(camera, LAUNCH_CAMERA_CODE);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_CODE);
                    }
                } else if (result.equals("Upload")) {
                    Intent gallery = CropImage.getPickImageChooserIntent(MainActivity.this, "upload with", false, false);
                    startActivityForResult(gallery, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
                }
            }
        });
    }

}