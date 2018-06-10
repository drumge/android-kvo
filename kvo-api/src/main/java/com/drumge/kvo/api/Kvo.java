package com.drumge.kvo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.drumge.kvo.api.inner.IKvoSource;
import com.drumge.kvo.api.inner.IKvoTargetProxy;
import com.drumge.kvo.api.inner.IKvoTargetCreator;
import com.drumge.kvo.api.log.IKvoLog;
import com.drumge.kvo.api.log.KLog;
import com.drumge.kvo.api.thread.IKvoThread;
import com.drumge.kvo.api.thread.KvoThread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by chenrenzhan on 2018/5/1.
 */

public class Kvo {
    private static final String TAG = "Kvo";
    public static final String INIT_METHOD_NAME = "kvo_init_method_name";

    private final Map<KvoSourceWrap, CopyOnWriteArrayList<IKvoTargetProxy>> mSourceTarget = new ConcurrentHashMap<>();
    private final Map<Class<?>, IKvoTargetCreator> mTargetCreator = new ConcurrentHashMap<>();


    private static class InstanceHolder {
        static Kvo instance = new Kvo();
    }
    public static Kvo getInstance() {
        return Kvo.InstanceHolder.instance;
    }

    private Kvo() {
    }

    public void setLog(IKvoLog log) {
        KLog.init(log);
    }

    public void setThread(IKvoThread thread) {
        KvoThread.getInstance().init(thread);
    }

    public void registerTarget(Class<?> cls, IKvoTargetCreator creator) {
        if (cls != null && creator != null) {
            mTargetCreator.put(cls, creator);
        }
    }

    /**
     * 通知观察者
     * @param source 被观察实体类实例
     * @param name 被观察属性名字 field.name
     * @param oldValue 被观察属性更新前的值
     * @param newValue 被观察属性更新后的值
     */
    public <S, V> void notifyWatcher(@NonNull S source, @NonNull String name, V oldValue, V newValue) {
        if(!KvoUtils.deepEquals(oldValue, newValue)) {
            String tag = getKvoSourceTag(source);
            for (KvoSourceWrap w : mSourceTarget.keySet()) {
                // 通知 source 实例的默认tag以及指定 tag 的观察者
                if (source == w.source && (w.tag.length() == 0 || w.tag.equals(tag))) {
                    notifyWatcher(w, name, oldValue, newValue);
                }
            }
        }
    }

    private <S, V> void notifyWatcher(@NonNull KvoSourceWrap<S> wrap,  @NonNull String name, V oldValue, V newValue) {
        KLog.info(TAG, "notifyWatcher wrap: %s, name: %s, oldValue: %s, newValue: %s", wrap, name, oldValue, newValue);
        CopyOnWriteArrayList<IKvoTargetProxy> targets = mSourceTarget.get(wrap);
        if (targets == null || targets.size() == 0) {
            KLog.error(TAG, "notifyWatcher target is empty with KvoSourceWrap: %s, name: %s, oldValue: %s, newValue: %s", wrap, name, oldValue, newValue);
            return;
        }
        KvoEvent<S, V> event = new KvoEvent<>();
        event.tag = wrap.tag;
        event.source = wrap.source;
        event.oldValue = oldValue;
        event.newValue = newValue;
        for (IKvoTargetProxy kt : targets) {
            kt.notifyWatcher(name, event);
        }
    }

    /**
     * 绑定观察者
     * @param target
     * @param source
     */
    public <S> void bind(@NonNull Object target, @NonNull S source) {
        bind(target, source, "", true);
    }

    /**
     * 绑定观察者
     * @param target
     * @param source
     * @param tag
     */
    public <S> void bind(@NonNull Object target, @NonNull S source, String tag) {
        bind(target, source, tag, true);
    }

    /**
     * 绑定观察者
     * @param target
     * @param source
     * @param notifyWhenBind  绑定时是否通知观察者
     */
    public <S> void bind(@NonNull Object target, @NonNull S source, boolean notifyWhenBind) {
        bind(target, source, "", notifyWhenBind);
    }

    /**
     * 绑定观察者
     * @param target @KvoWatch 修饰观察者所在实例
     * @param source @KvoSource 修饰的被观察对象的实例
     * @param tag 标识指定观察对象的实例
     * @param notifyWhenBind  绑定时是否通知观察者
     */
    public <S> void bind(@NonNull Object target, @NonNull S source, String tag, boolean notifyWhenBind) {
        KLog.info(TAG, "bind target: %s, source: %s, tag: %s, notifyWhenBind: %s", target, source, tag, notifyWhenBind);
        setKvoSourceTag(source, tag);
        KvoSourceWrap<S> wrap = createWrap(source, tag);
        CopyOnWriteArrayList<IKvoTargetProxy> targets = mSourceTarget.get(wrap);
        IKvoTargetProxy kt = createTarget(target);
        if (kt == null) {
            KLog.error(TAG, "bind createTarget is null");
            return;
        }
        if (targets == null) {
            targets = new CopyOnWriteArrayList<>();
            mSourceTarget.put(wrap, targets);
        }
        if (!targets.contains(kt)) {
            targets.add(kt);
        }
        if (notifyWhenBind) {
            // 绑定时通知观察者，初始值 oldValue = null 和 newValue 跟 source 中的值相等，预编译完成初始值赋值
            notifyWatcher(wrap, INIT_METHOD_NAME, null, null);
        }
    }

    /**
     * 解绑观察者
     * @param target
     * @param source
     */
    public <S> void unbind(@NonNull Object target, @NonNull S source) {
        doUnbind(target, source, "");
    }

    /**
     * 解绑观察者
     * @param target @KvoWatch 修饰观察者所在实例
     * @param tag 标识指定观察对象的实例
     *
     */
    public <S> void unbind(@NonNull Object target, @NonNull S source, String tag) {
        doUnbind(target, source, tag);
    }

    /**
     * 解绑 target 下的所有观察者
     * @param target @KvoWatch 修饰观察者所在实例
     *
     */
    public void unbindAll(@NonNull Object target) {
        KLog.info(TAG, "unbindAll target: %s", target);
        IKvoTargetProxy kt = createTarget(target);
        List<KvoSourceWrap> rmList = new ArrayList<>();
        for (Map.Entry<KvoSourceWrap, CopyOnWriteArrayList<IKvoTargetProxy>> st : mSourceTarget.entrySet()) {
            CopyOnWriteArrayList<IKvoTargetProxy> tps = st.getValue();
            if (tps.contains(kt)) {
                tps.remove(kt);
            }
            if (tps.isEmpty()) {
                rmList.add(st.getKey());
            }
        }
        for (KvoSourceWrap ksw : rmList) {
            mSourceTarget.remove(ksw);
        }
    }

    private  <S> void doUnbind(@NonNull Object target, @Nullable S source, String tag) {
        KLog.info(TAG, "doUnbind target: %s, source: %s, tag: %s", target, source, tag);
        List<KvoSourceWrap> list = findWrap(source, tag);
        if (list == null || list.size() == 0) {
            return;
        }
        IKvoTargetProxy kt = createTarget(target);
        for (KvoSourceWrap wrap : list) {
            removeTarget(wrap, kt);
        }
    }

    @Nullable
    private IKvoTargetProxy createTarget(Object target) {
        KLog.debug(TAG, "createTarget mTargetCreator: %s", mTargetCreator);
        if (target == null) {
            KLog.error(TAG, "createTarget target is null");
            return null;
        }

        Class cls = target.getClass();
        IKvoTargetCreator creator = mTargetCreator.get(cls);
        if (creator != null) {
            return creator.createTarget(target);
        }
        KLog.error(TAG, "createTarget creator is null, cls: %s", cls);
        return null;
    }

    public void mainThread(@NonNull Runnable task) {
        KvoThread.getInstance().mainThread(task);
    }

    public void mainThread(@NonNull Runnable task, long delay) {
        KvoThread.getInstance().mainThread(task, delay);
    }

    public void workThread(@NonNull Runnable task) {
        KvoThread.getInstance().workThread(task);
    }

    public void workThread(@NonNull Runnable task, long delay) {
        KvoThread.getInstance().workThread(task, delay);
    }

    private <S> KvoSourceWrap<S> createWrap(S source, String tag) {
        return new KvoSourceWrap<>(source, tag == null ? "" : tag);
    }

    @Nullable
    private List<KvoSourceWrap> findWrap(@Nullable Object source, @Nullable String tag) {
        if (source == null && (tag == null || tag.length() == 0)) {
            return null;
        }
        List<KvoSourceWrap> list = new ArrayList<>();
        if (source != null && tag != null && tag.length() > 0) {
            KvoSourceWrap wrap = createWrap(source, tag);
            if (mSourceTarget.containsKey(wrap)) {
                list.add(wrap);
            } else {
                return null;
            }
        }
        for (KvoSourceWrap w : mSourceTarget.keySet()) {
            if (source == null) {
                if (tag.equals(w.tag)) {
                    list.add(w);
                }
            } else {
                if (source == w.source) {
                    list.add(w);
                }
            }
        }
        return list;
    }

    @Nullable
    private boolean removeTarget(KvoSourceWrap wrap, IKvoTargetProxy target) {
        if (!mSourceTarget.containsKey(wrap)) {
            return false;
        }
        CopyOnWriteArrayList<IKvoTargetProxy> targets = mSourceTarget.get(wrap);
        boolean re = targets.remove(target);
        if (targets.isEmpty()) {
            mSourceTarget.remove(wrap);
        }
        return re;
    }


    private <S> void setKvoSourceTag(S source, String tag) {
        if (tag == null || tag.length() == 0) {
            return;
        }
        _setKvoSourceTag(source, tag);
    }

    private <S> String getKvoSourceTag(S source) {
        String tag = "";
        if (source == null) {
            return tag;
        }
        tag = _getKvoSourceTag(source);
        return tag == null ? "" : tag;
    }

    private void _setKvoSourceTag(Object source, String tag) {
        try {
            IKvoSource s = (IKvoSource) source;
            s._setKvoSourceTag(tag);
        } catch (Exception e) {
            KLog.error(TAG, e);
        }
    }

    private String _getKvoSourceTag(Object source) {
        try{
            IKvoSource s = (IKvoSource) source;
            return s._getKvoSourceTag();
        } catch (Exception e) {
            KLog.error(TAG, e);
        }
        return "";
    }
}
