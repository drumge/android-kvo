package com.drumge.kvo;

import com.drumge.kvo.annotation.KvoBind;
import com.drumge.kvo.annotation.KvoSource;

/**
 * Created by chenrenzhan on 2019/3/14.
 */
@KvoSource
public class KvoFlavorSource {
    private boolean mIsRedShow;


    private void setRedPointShow(boolean isRed){
        if (isRed != mIsRedShow){
            setIsRed(isRed);
        }
    }

    @KvoBind(name = K_KvoFlavorSource.mIsRedShow)
    public void setIsRed(boolean mIsRed) {
        this.mIsRedShow = mIsRed;
    }

    public boolean isRedShow() {
        return mIsRedShow;
    }
}
