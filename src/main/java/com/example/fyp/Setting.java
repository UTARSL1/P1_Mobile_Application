package com.example.fyp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Setting extends AppCompatActivity {

    SeekBar seekBar;
    Switch max_switch;
    boolean max_show;
    int threshold;
    TextView info_threshold;
    SharedPreferences settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        seekBar = findViewById(R.id.seekBar);
        max_switch = findViewById(R.id.max_switch);
        info_threshold = findViewById(R.id.info_threshold);

        settings = getSharedPreferences("Setting", MODE_PRIVATE);

        max_show = settings.getBoolean("max_show", false);
        threshold = settings.getInt("threshold", 4);

        if (max_show) {
            max_switch.setChecked(true);
        }

        seekBar.setProgress(threshold);
        info_threshold.setText(threshold * 10 + "%");

        max_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    max_show = true;
                    saveBoolSetting("max_show", max_show);
                } else {
                    max_show = false;
                    saveBoolSetting("max_show", max_show);
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                info_threshold.setText(i * 10 + "%");
                saveIntSetting("threshold", i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    public void saveBoolSetting(String s, boolean b) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(s, b);
        editor.apply();
    }

    public void saveIntSetting(String s, int i) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(s, i);
        editor.apply();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        Toast.makeText(this, "Setting saved", Toast.LENGTH_SHORT).show();
    }
}
