package com.drumge.kvo.example;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.drumge.kvo.annotation.KvoWatch;
import com.drumge.kvo.api.Kvo;
import com.drumge.kvo.api.KvoEvent;

public class ExampleActivity2 extends Activity implements View.OnClickListener {
    private static final String TAG = "ExampleActivity";

    private Button mBind;
    private Button mUnbindAll;
    private EditText mExampleTag1Et;
    private EditText mExampleTag2Et;
    private EditText mIndexEt;
    private EditText mCharEt;
    private EditText mNameEt;
    private EditText mBaseClassEt;
    private Button mExampleTag1Btn;
    private Button mExampleTag2Btn;
    private Button mIndexBtn;
    private Button mCharBtn;
    private Button mNameBtn;
    private Button mBaseClassBtn;


    private ExampleSource mExampleSource;
    private ExampleSource mTag1;
    private ExampleSource mTag2;
    private InnerClassExample.InnerStatic innerStatic;

    private SubSource subSource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_activity_main);

        mBind = findViewById(R.id.bind);
        mUnbindAll = findViewById(R.id.unbind_all);
        mBind.setOnClickListener(this);
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

        mBaseClassEt = findViewById(R.id.base_class_et);
        mBaseClassBtn = findViewById(R.id.base_class_btn);
        mBaseClassBtn.setOnClickListener(this);

        mTag1 =  new ExampleSource();
        mTag2 =  new ExampleSource();
        mExampleSource = new ExampleSource();

        mExampleSource.setIndex(3);

        subSource = new SubSource();

        innerStatic = new InnerClassExample.InnerStatic();
    }

    @Override
    public void onClick(View v) {
        if (v == mBind) {
            Kvo.getInstance().bind(this, mExampleSource);
            Kvo.getInstance().bind(this, mTag1, "tag1");
            Kvo.getInstance().bind(this, mTag2, "tag2", false);
            Kvo.getInstance().bind(this, subSource, false);
            Kvo.getInstance().bind(this, innerStatic);
        } else if (v == mUnbindAll) {
            Kvo.getInstance().unbindAll(this);
        } else if (v == mExampleTag1Btn) {
            String text = mExampleTag1Et.getText().toString();
            mTag1.setExample(text);
        } else if (v == mExampleTag2Btn) {
            String text = mExampleTag2Et.getText().toString();
            mTag2.setExample(text);
        } else if (v == mIndexBtn) {
            String text = mIndexEt.getText().toString();
            if (TextUtils.isDigitsOnly(text)) {
                mExampleSource.setIndex(Integer.valueOf(text));
            }
        } else if (v == mCharBtn) {
            String text = mCharEt.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                mExampleSource.setsCharDif(text.charAt(0));
            }
        } else if (v == mNameBtn) {
            String text = mNameEt.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                innerStatic.updateName(text);
            }
        } else if (v == mBaseClassBtn) {
            String text = mBaseClassEt.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                innerStatic.updateName(text);
            }
        }
    }

    @KvoWatch(name = K_ExampleSource.example, tag = "tag1", thread = KvoWatch.Thread.MAIN)
    public void onUpdateExampleTag1(KvoEvent<ExampleSource, String> event) {


        toast(TAG, "onUpdateExampleTag1 oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());
    }


    @KvoWatch(name = K_ExampleSource.example, tag = "tag2", thread = KvoWatch.Thread.MAIN)
    public void onUpdateExampleTag2(KvoEvent<ExampleSource, String> event) {
        toast(TAG, "onUpdateExampleTag2 oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());
    }


    @KvoWatch(name = K_ExampleSource.index)
    public void onUpdateIndex(KvoEvent<ExampleSource, Integer> event) {
        toast(TAG, "onUpdateIndex oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());
    }

    @KvoWatch(name = K_ExampleSource.sChar, thread = KvoWatch.Thread.MAIN)
    public void onUpdateChar(KvoEvent<ExampleSource, Character> event) {
        toast(TAG, "onUpdateChar oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());
    }

    @KvoWatch(name = K_InnerClassExample.InnerStatic.name, thread = KvoWatch.Thread.MAIN)
    public void onUpdateName(KvoEvent<InnerClassExample.InnerStatic, String> event) {
        toast(TAG, "onUpdateName oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());
    }

    @KvoWatch(name = K_BaseSource.baseClass)
    void updateBaseClass(KvoEvent<SubSource, String> event) {
        toast(TAG, "updateBaseClass oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());
    }

    private void toast(String tag, String msg) {
        Toast.makeText(this,  tag + ": " + msg, Toast.LENGTH_SHORT).show();
        Log.i(tag, msg);
    }
}
