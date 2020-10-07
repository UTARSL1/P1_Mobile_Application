package com.example.fyp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.io.ByteArrayOutputStream;

public class CameraActivity extends AppCompatActivity {
    CameraView cameraView;
    ImageView frame_;
    Bitmap crop;
    ImageButton capture, cancel;
    TextView info;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        final byte[][] arr = new byte[1][1];
        frame_ = findViewById(R.id.frame);

        cameraView = findViewById(R.id.cameraView);
        capture = findViewById(R.id.capture);
        cancel = findViewById(R.id.cancel);
        info = findViewById(R.id.info);

        progressDialog = new ProgressDialog(CameraActivity.this);
        progressDialog.setMessage("Processing Image");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        cameraView.setLifecycleOwner(CameraActivity.this);
        cameraView.setAutoFocusResetDelay(1000);
        cameraView.open();

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
                super.onCameraOpened(options);
                frame_.setEnabled(false);

            }

            @Override
            public void onAutoFocusStart(@NonNull PointF point) {
                super.onAutoFocusStart(point);
                frame_.setEnabled(false);

            }


            @Override
            public void onAutoFocusEnd(boolean successful, @NonNull PointF point) {
                super.onAutoFocusEnd(successful, point);
                if (successful) {
                    if (point.x > frame_.getLeft() && point.x < frame_.getRight() && point.y > frame_.getTop() && point.y < frame_.getBottom()) {
                        info.setText(R.string.instruc1);
                        frame_.setEnabled(true);

                    } else {
                        info.setText(R.string.instruc2);
                        frame_.setEnabled(false);
                    }
                } else {
                    info.setText(R.string.instruc2);
                    frame_.setEnabled(false);
                }
            }


            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                cameraView.close();
                arr[0] = result.getData();
                Bitmap bitmap = BitmapFactory.decodeByteArray(arr[0], 0, arr[0].length);
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, cameraView.getWidth(), cameraView.getHeight());
                int frame_size = (int) (frame_.getWidth() - (21.45 * getResources().getDisplayMetrics().densityDpi / 160));
                int x = bitmap.getWidth() / 2 - frame_size / 2;
                int y = bitmap.getHeight() / 2 - frame_size / 2;
                crop = Bitmap.createBitmap(bitmap, x, y, frame_size, frame_size);
//                Log.d("WIDTH"," "+bitmap.getWidth()+" "+crop.getWidth()+" "+cameraView.getWidth());
                bitmap.recycle();
                start_preprocess(crop);

            }
        });


        PushDownAnim.setPushDownAnimTo(capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capture();
            }
        });

        PushDownAnim.setPushDownAnimTo(cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    public void capture() {
        cameraView.takePicture();
        progressDialog.show();
    }

    @Override
    public void finish() {
        cameraView.clearCameraListeners();
        progressDialog.dismiss();
        if (cameraView.isOpened()) {
            cameraView.close();
        }
        super.finish();
        Log.d("CAMERA", "Closed");
    }

    public void start_preprocess(Bitmap img) {
        Preprocess runnable = new Preprocess(img);
        new Thread(runnable).start();
    }

    class Preprocess implements Runnable {
        Bitmap bitmap_img;
        volatile byte[] bytes;

        Preprocess(Bitmap bitmap) {
            this.bitmap_img = bitmap;

        }

        @Override
        public void run() {
            Bitmap bitmap = Bitmap.createScaledBitmap(bitmap_img, 672, 672, true);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            bytes = stream.toByteArray();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", bytes);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            });
            return;
        }
    }

}
