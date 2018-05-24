package com.drumge.kvo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.drumge.R;
import com.drumge.kvo.example.ExampleSource;
import com.drumge.kvo.example.ExampleTarget;

public class MainActivity extends Activity implements View.OnClickListener {
    private Button mBind;
    private Button mUnbind;
    private EditText mExampleTag1Et;
    private EditText mExampleTag2Et;
    private EditText mIndexEt;
    private EditText mCharEt;
    private Button mExampleTag1Btn;
    private Button mExampleTag2Btn;
    private Button mIndexBtn;
    private Button mCharBtn;


    private ExampleTarget mExampleTarget;
    private ExampleSource mTag1;
    private ExampleSource mTag2;
    private ExampleSource mTag3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBind = findViewById(R.id.bind);
        mUnbind = findViewById(R.id.unbind);
        mBind.setOnClickListener(this);
        mUnbind.setOnClickListener(this);

        mExampleTag1Et = findViewById(R.id.example_tag1);
        mExampleTag1Btn = findViewById(R.id.example_change_tag1);
        mExampleTag1Btn.setOnClickListener(this);

        mExampleTag2Et = findViewById(R.id.example_tag2);
        mExampleTag2Btn = findViewById(R.id.example_change_tag2);
        mExampleTag2Btn.setOnClickListener(this);

        mIndexEt = findViewById(R.id.index_et);
        mIndexBtn = findViewById(R.id.index_btn);
        mIndexBtn.setOnClickListener(this);

        mCharEt = findViewById(R.id.char_et);
        mCharBtn = findViewById(R.id.char_btn);
        mCharBtn.setOnClickListener(this);

        mExampleTarget = new ExampleTarget();
        mTag1 = mExampleTarget.getTag1();
        mTag2 = mExampleTarget.getTag2();
        mTag3 = mExampleTarget.getTag3();


    }

    @Override
    public void onClick(View v) {
        if (v == mBind) {
            mExampleTarget.bindKvo();
        } else if (v == mUnbind) {
            mExampleTarget.unbindKvo();
        } else if (v == mExampleTag1Btn) {
            String text = mExampleTag1Et.getText().toString();
            mTag1.setExample(text);
        } else if (v == mExampleTag2Btn) {
            String text = mExampleTag2Et.getText().toString();
            mTag2.setExample(text);
        } else if (v == mIndexBtn) {
            String text = mIndexEt.getText().toString();
            if (TextUtils.isDigitsOnly(text)) {
                mTag3.setIndex(Integer.valueOf(text));
            }
        } else if (v == mCharBtn) {
            String text = mCharEt.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                mTag3.setsChar(text.charAt(0));
            }
        }
    }
}
