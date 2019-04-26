package com.drumge.kvo;

import com.drumge.kvo.annotation.KvoBind;
import com.drumge.kvo.annotation.KvoSource;

/**
 * Created by chenrenzhan on 2019/4/8.
 */
@KvoSource(check = false)
public abstract class AItemData {
    private Object mChangeFlag;

    public abstract int viewType();

    public void notifyItemDataChange() {
        setChangeFlag(new Object());
    }

    @KvoBind(name = K_AItemData.mChangeFlag)
    private void setChangeFlag(Object object) {
    }
}
