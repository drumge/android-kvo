package com.drumge.kvo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.drumge.R;
import com.drumge.kvo.example.ExampleActivity;
import com.github.drumge.kvo.example.kotlin.KvoKotlinActivity;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "KvoMainActivity";

    private Button mJavaExample;
    private Button mKtExample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mJavaExample = findViewById(R.id.java_activity);
        mKtExample = findViewById(R.id.kt_activity);

        mJavaExample.setOnClickListener(this);
        mKtExample.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mJavaExample) {
            Intent intent = new Intent(this, ExampleActivity.class);
            startActivity(intent);
        } else if (v == mKtExample) {
            Intent intent = new Intent(this, KvoKotlinActivity.class);
            startActivity(intent);
        }
    }


}
