package com.drumge.kvo.inner.weak;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.drumge.kvo.api.Kvo;
import com.drumge.kvo.inner.IKvoSource;
import com.drumge.kvo.inner.IKvoTargetProxy;
import com.drumge.kvo.inner.KvoUtils;
import com.drumge.kvo.inner.thread.KvoThread;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by chenrenzhan on 2019/2/2.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WeakSourceTargetSet {
    private static final String TAG = "WeakSourceTargetSet";

    private final Map<WeakSourceWrap, CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>>> mSourceTarget = new ConcurrentHashMap<>();
    private final Map<Reference, String> mTargetTag = new ConcurrentHashMap<>();

    private final ReferenceQueue<Object> mSourceQueue = new ReferenceQueue<>();
    private final ReferenceQueue<IKvoTargetProxy> mTargetQueue = new ReferenceQueue<>();
    private final static Object LOCK = new Object();
    private volatile boolean isExpunging = false;

    public <S> void add(@NonNull IKvoTargetProxy kt, @NonNull S source, String tag) {
        WeakSourceWrap<S> wrap = createWrap(source, tag);
        CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>> targets = mSourceTarget.get(wrap);
        if (targets == null) {
            targets = new CopyOnWriteArrayList<>();
            mSourceTarget.put(wrap, targets);
        }
        if (findTarget(targets, kt) == null) {
            WeakReference<IKvoTargetProxy> weak = new WeakReference<>(kt, mTargetQueue);
            targets.add(weak);
            if (tag != null && tag.length() != 0) {
                mTargetTag.put(weak, tag);
                KvoUtils.addKvoSourceTag(source, tag);
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <S> Map<WeakSourceWrap, List<IKvoTargetProxy>> findWatcher(@NonNull S source) {
        Map<WeakSourceWrap, List<IKvoTargetProxy>> entrySet = null;
        for (Map.Entry<WeakSourceWrap, CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>>> entry : mSourceTarget.entrySet()) {
            WeakSourceWrap w = entry.getKey();
            CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>> targets = entry.getValue();
            // 通知 source 实例的默认tag以及指定 tag 的观察者
            // todo source == w.getSource() ?
            if (source == w.getSource() && (w.getTag().length() == 0 || KvoUtils.containKvoSourceTag(source, w.getTag()))
                    && targets != null && !targets.isEmpty()) {
                List<IKvoTargetProxy> list = getTargetList(targets);
                if (list != null && !list.isEmpty()) {
                    if (entrySet == null) {
                        entrySet = new HashMap<>();
                    }
                    entrySet.put(w, list);
                }
            }
        }

        expungeStale();

        return entrySet;
    }

    public void removeAll(Object target) {
        List<WeakSourceWrap> rmSource = null;
        for (Map.Entry<WeakSourceWrap, CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>>> entry : mSourceTarget.entrySet()) {
            WeakSourceWrap sourceWrap = entry.getKey();
            CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>> targets = entry.getValue();
            if (removeTarget(target, sourceWrap, targets)) {
                if (rmSource == null) {
                    rmSource = new ArrayList<>();
                }
                rmSource.add(sourceWrap);
            }
        }

        removeSourceWrap(rmSource);

        expungeStale();
    }

    public <S> void removeAll(@NonNull Object target, @Nullable S source, String tag) {
        List<WeakSourceWrap> list = findWrap(source, tag);
        if (list == null || list.size() == 0) {
            return;
        }
        List<WeakSourceWrap> rmSource = null;
        for (WeakSourceWrap wrap : list) {
            CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>> targets = mSourceTarget.get(wrap);
            if (removeTarget(target, wrap, targets)) {
                if (rmSource == null) {
                    rmSource = new ArrayList<>();
                }
                rmSource.add(wrap);
            }
        }

        removeSourceWrap(rmSource);

        expungeStale();
    }

    @SuppressWarnings("unchecked")
    private boolean removeTarget(Object target, WeakSourceWrap sourceWrap, CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>> targets) {
        if (targets != null && !targets.isEmpty()) {
            List<WeakReference<IKvoTargetProxy>> rm = null;
            for (WeakReference<IKvoTargetProxy> weak : targets) {
                IKvoTargetProxy proxy = weak.get();
                if (proxy != null) {
                    if (proxy.equalsTarget(target)) {
                        if (rm == null) {
                            rm = new ArrayList<>();
                        }
                        rm.add(weak);
                    }
                }
            }
            if (rm != null && !rm.isEmpty()) {
                for (WeakReference<IKvoTargetProxy> weak : rm) {
                    targets.remove(weak);
                    String tag = mTargetTag.get(weak);
                    if (sourceWrap.getSource() != null && tag != null) {
                        KvoUtils.removeKvoSourceTag(sourceWrap.getSource(), tag);
                    }
                }
                if (targets.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeSourceWrap(List<WeakSourceWrap> rmSource) {
        if (rmSource != null && !rmSource.isEmpty()) {
            for (WeakSourceWrap sourceWrap : rmSource) {
                mSourceTarget.remove(sourceWrap);
                String tag = sourceWrap.tag;
                if (tag != null && tag.length() > 0 && sourceWrap.getSource() != null) {
                    KvoUtils.removeKvoSourceTag(sourceWrap.getSource(), tag);
                }
            }
        }
    }

    @Nullable
    private List<WeakSourceWrap> findWrap(@Nullable Object source, @Nullable String tag) {
        if (source == null && (tag == null || tag.length() == 0)) {
            return null;
        }
        List<WeakSourceWrap> list = new ArrayList<>();
        if (source != null && tag != null && tag.length() > 0) {
            WeakSourceWrap wrap = createWrap(source, tag);
            if (mSourceTarget.containsKey(wrap)) {
                list.add(wrap);
            }
        } else {
            for (WeakSourceWrap w : mSourceTarget.keySet()) {
                if (source == null) {
                    if (tag.equals(w.getTag())) {
                        list.add(w);
                    }
                } else {
                    if (source == w.getSource()) {
                        list.add(w);
                    }
                }
            }
        }

        return list;
    }

    private  <S> WeakSourceWrap<S> createWrap(S source, String tag) {
        return new WeakSourceWrap<>(source, tag == null ? "" : tag, mSourceQueue);
    }

    @Nullable
    private List<IKvoTargetProxy> getTargetList(List<WeakReference<IKvoTargetProxy>> reList) {
        List<IKvoTargetProxy> list = null;
        for (WeakReference<IKvoTargetProxy> re : reList) {
            if (re != null && re.get() != null) {
                if (list == null) {
                    list = new CopyOnWriteArrayList<>();
                }
                list.add(re.get());
            }
        }
        return list;
    }

    @Nullable
    private WeakReference<IKvoTargetProxy> findTarget(CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>> targets, IKvoTargetProxy kt) {
        if (targets == null || targets.isEmpty() || kt == null) {
            return null;
        }

        WeakReference<IKvoTargetProxy> result = null;

        for (WeakReference<IKvoTargetProxy> wkt : targets) {
            if (kt.equals(wkt.get())) {
                result = wkt;
                break;
            }
        }

        expungeStale();

        return result;
    }


    /**
     * Expunges stale source or target from the set.
     */
    private void expungeStale() {
        if (isExpunging) {
            return;
        } else {
            KvoThread.getInstance().workThread(new Runnable() {
                @Override
                public void run() {
                    expungeStaleAsync();
                }
            });
        }
    }

    private void expungeStaleAsync() {
        isExpunging = true;
        List<Reference> sourceStale = getStale(mSourceQueue);
        List<Reference> targetStale = getStale(mTargetQueue);

        removeStale(sourceStale, targetStale);

        isExpunging = false;
    }

    private void removeStale(List<Reference> sourceStale, List<Reference> targetStale) {
        if (sourceStale != null && targetStale != null) {
            synchronized (LOCK) {
                List<WeakSourceWrap> rmSource = new ArrayList<>();
                for (Map.Entry<WeakSourceWrap, CopyOnWriteArrayList<WeakReference<IKvoTargetProxy>>> entry : mSourceTarget.entrySet()) {
                    WeakSourceWrap sourceWrap = entry.getKey();
                    if (sourceWrap == null) {
                        continue;
                    }
                    IKvoSource source = null;
                    if ((source = (IKvoSource) sourceWrap.weakSource.get()) == null || sourceStale.contains(sourceWrap.weakSource)) {
                        rmSource.add(sourceWrap);
                    } else {
                        if (!targetStale.isEmpty()) {
                            List<WeakReference<IKvoTargetProxy>> targetList = entry.getValue();
                            if (targetList != null) {
                                for (Reference ref : targetStale) {
                                    if (targetList.isEmpty()) {
                                        break;
                                    }
                                    targetList.remove(ref);

                                    String tag = null;
                                    if ((tag = mTargetTag.remove(ref)) != null && tag.length() > 0) {
                                        KvoUtils.removeKvoSourceTag(source, tag);
                                    }
                                }
                                if (targetList.isEmpty()) {
                                    rmSource.add(sourceWrap);
                                }
                            }
                        }
                    }
                }

                if (!rmSource.isEmpty()) {
                    for (WeakSourceWrap sourceWrap : rmSource) {
                        mSourceTarget.remove(sourceWrap);

                        String tag = sourceWrap.tag;
                        if (tag != null && tag.length() > 0 && sourceWrap.getSource() != null) {
                            KvoUtils.removeKvoSourceTag(sourceWrap.getSource(), tag);
                        }
                    }
                }
            }
        }
    }

    private List<Reference> getStale(ReferenceQueue queue) {
        List<Reference> stale = null;
        if (queue == null) {
            return null;
        }

        for (Reference x; (x = queue.poll()) != null; ) {
            synchronized (queue) {
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(x);
            }
        }
        return stale;
    }
}
