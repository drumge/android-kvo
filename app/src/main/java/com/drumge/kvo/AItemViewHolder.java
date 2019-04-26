package com.drumge.kvo;

import com.drumge.kvo.annotation.KvoWatch;
import com.drumge.kvo.api.Kvo;
import com.drumge.kvo.api.KvoEvent;

/**
 * Created by chenrenzhan on 2019/4/8.
 */
public abstract class AItemViewHolder<T extends AItemData> {

    private T mItemData;


    public T getItemData() {
        return mItemData;
    }

    public void bindData(T data) {
        if (mItemData != null && mItemData != data) {
            Kvo.getInstance().unbind(this, mItemData);
        } else if (mItemData != data) {
            mItemData = data;
            Kvo.getInstance().bind(this, mItemData, false);
        }

        onBindView(mItemData);
    }

    @KvoWatch(name = K_AItemData.mChangeFlag, thread = KvoWatch.Thread.MAIN)
    void onItemDataChanged(KvoEvent<AItemData, Object> event) {
        onItemDataChanged(mItemData);
    }

    /**
     * 绑定数据时调用
     * 子类重写该方法更新 ui
     * @param data
     */
    protected void onBindView(T data) {

    }

    /**
     * itemData 局部数据变更时调用，可做局部更新刷新ui
     * @param data
     */
    protected void onItemDataChanged(T data) {

    }

}
