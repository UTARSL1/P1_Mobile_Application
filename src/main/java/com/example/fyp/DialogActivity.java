package com.example.fyp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class DialogActivity extends AppCompatDialogFragment {
    Button camera, upload;
    OnMyDialogResult mDialogResult;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_dialog, null);
        builder.setView(view)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                })
                .setTitle("Choose");

        camera = view.findViewById(R.id.camera);
        upload = view.findViewById(R.id.upload);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDialogResult.finish("Camera");
                dismiss();
            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDialogResult.finish("Upload");
                dismiss();
            }
        });
        return builder.create();

    }

    public void setDialogResult(OnMyDialogResult dialogResult) {
        mDialogResult = dialogResult;
    }

    public interface OnMyDialogResult {
        void finish(String result);
    }
}
