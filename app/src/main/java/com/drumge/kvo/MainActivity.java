package com.drumge.kvo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.drumge.R;
import com.drumge.kvo.annotation.KvoWatch;
import com.drumge.kvo.api.Kvo;
import com.drumge.kvo.api.KvoEvent;
import com.drumge.kvo.example.ExampleSource;
import com.drumge.kvo.example.ExampleTarget;
import com.drumge.kvo.example.InnerClassExample;
import com.drumge.kvo.example.K_ExampleSource;
import com.drumge.kvo.example.K_InnerClassExample;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "KvoMainActivity";

    private Button mBind;
    private Button mUnbind;
    private Button mUnbindAll;
    private EditText mExampleTag1Et;
    private EditText mExampleTag2Et;
    private EditText mIndexEt;
    private EditText mCharEt;
    private EditText mNameEt;
    private Button mExampleTag1Btn;
    private Button mExampleTag2Btn;
    private Button mIndexBtn;
    private Button mCharBtn;
    private Button mNameBtn;


    private ExampleTarget mExampleTarget;
    private ExampleSource mTag1;
    private ExampleSource mTag2;
    private ExampleSource mTag3;
    private ExampleSource mTag4;
    private InnerClassExample.InnerStatic innerStatic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBind = findViewById(R.id.bind);
        mUnbind = findViewById(R.id.unbind);
        mUnbindAll = findViewById(R.id.unbind_all);
        mBind.setOnClickListener(this);
        mUnbind.setOnClickListener(this);
        mUnbindAll.setOnClickListener(this);

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

        mNameEt = findViewById(R.id.name_et);
        mNameBtn = findViewById(R.id.name_btn);
        mNameBtn.setOnClickListener(this);

        mExampleTarget = new ExampleTarget();
        mTag1 = mExampleTarget.getTag1();
        mTag2 = mExampleTarget.getTag2();
        mTag3 = mExampleTarget.getTag3();
        mTag4 = mExampleTarget.getTag4();

        mTag3.setIndex(new Integer(3));

        Kvo.getInstance().bind(this, mTag3);

        innerStatic = new InnerClassExample.InnerStatic();
    }

    @Override
    public void onClick(View v) {
        if (v == mBind) {
            Kvo.getInstance().bind(MainActivity.this, innerStatic);
            mExampleTarget.bindKvo();
        } else if (v == mUnbind) {
            Kvo.getInstance().unbind(MainActivity.this, innerStatic);
            mExampleTarget.unbindKvo();
        } else if (v == mUnbindAll) {
            mExampleTarget.unbindAll();
        } else if (v == mExampleTag1Btn) {
            String text = mExampleTag1Et.getText().toString();
            mTag1.setExample(text);
        } else if (v == mExampleTag2Btn) {
            String text = mExampleTag2Et.getText().toString();
            mTag2.setExample(text);
        } else if (v == mIndexBtn) {
            String text = mIndexEt.getText().toString();
            if (TextUtils.isDigitsOnly(text)) {
                mTag1.setIndex(Integer.valueOf(text));
                mTag2.setIndex(Integer.valueOf(text) + 1);
                mTag3.setIndex(Integer.valueOf(text) + 2);
                mTag4.setIndex(Integer.valueOf(text) + 3);
            }
        } else if (v == mCharBtn) {
            String text = mCharEt.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                mTag3.setsCharDif(text.charAt(0));
            }
        }  else if (v == mNameBtn) {
            String text = mNameEt.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                innerStatic.updateName(text);
            }
        }
    }

    @KvoWatch(name = K_InnerClassExample.InnerStatic.name, thread = KvoWatch.Thread.WORK)
    public void onUpdateName(KvoEvent<InnerClassExample.InnerStatic, String> event) {
        Log.d(TAG, "onUpdateName oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }

    @KvoWatch(name = K_ExampleSource.index)
    public void onUpdateIndex(KvoEvent<ExampleSource, Integer> event) {
        Log.d(TAG, "onUpdateIndex oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }

}
