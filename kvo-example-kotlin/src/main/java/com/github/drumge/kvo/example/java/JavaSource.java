package com.github.drumge.kvo.example.java;


import com.drumge.kvo.annotation.KvoBind;
import com.drumge.kvo.annotation.KvoIgnore;
import com.drumge.kvo.annotation.KvoSource;

/**
 * Created by chenrenzhan on 2018/5/3.
 */

@KvoSource
public class JavaSource {
    @KvoIgnore
    public int aa;
    private String example;
    private Integer index = 0;
    private long time;
    private short mShort;
    private byte mByte;
    private int mInt;
    private float mFloat;
    private double mDouble;
    private boolean mBoolean;
    private char sChar;

    public void setExample(String example) {
        this.example = example;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setMShort(short mShort) {
        this.mShort = mShort;
    }

    public void setMByte(byte mByte) {
        this.mByte = mByte;
    }

    public void setMInt(int mInt) {
        this.mInt = mInt;
    }

    public void setMFloat(float mFloat) {
        this.mFloat = mFloat;
    }

    public void setMDouble(double mDouble) {
        this.mDouble = mDouble;
    }

    @KvoBind(name = K_JavaSource.mBoolean)
    public void setmBooleanHH(boolean mBoolean) {
        this.mBoolean = mBoolean;
    }

    @KvoBind(name = K_JavaSource.sChar)
    public void setsCharDif(char sChar) {
        this.sChar = sChar;
    }
}
