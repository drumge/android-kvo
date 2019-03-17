# Android KVO

### 简介

* KVO， 即 Key-Value Observing 的缩写，当指定的实例的属性被修改后，该实例该属性的指定观察者可以收到通知。简单的说就是每次指定的被观察的实例的属性被修改后，KVO 就会自动通知相应的观察者。 
可以使用 KVO 来检测对象属性的变化、快速做出响应，这能够为开发强交互、响应式应用以及实现视图和模型的双向绑定时提供大量的帮助。 

* 相信 iOS 开发的同学会很熟悉，KVO 的思想本身是 Object-C 语言的一种特性，但是据说在 iOS 开发过程中比较麻烦并且还容易出现内存泄漏隐患而不被经常使用。  

* Android KVO 参考了 iOS 中的思想使用在 Android 系统上实现一套属性改变自动通知的框架，结合预编译手段实现了简单使用注解就可以很方便的使用 KVO。
Java 中内存自动回收机制，使用 KVO 不用担心 iOS 中出现循环引用之类的内存泄漏。

### 使用

 KVO 使用了 gradle 插件实现预编译，依赖了另一个 [easy-gradle-plugin](https://github.com/drumge/easy-gradle-plugin.git)  gradle 插件，实现更简单的使用自定义 gradle 插件，如需有兴趣移步了解。如果只想了解 KVO 的使用，不关心实现原理可忽略 [easy-gradle-plugin](https://github.com/drumge/easy-gradle-plugin.git) 这部分。
#### 1. build.gradle 构建脚本配置
* 在项目根目录下的 build.gradle 中对应的块中添加以下下配置信息
```groovy
buildscript {
    repositories {
	    maven{ url uri('https://oss.sonatype.org/content/groups/staging')}
    }
    dependencies {
        classpath "com.github.drumge:easy-plugin:0.2.4"
        classpath "com.github.drumge:kvo-plugin:0.2.18"
    }
}
allprojects {
    repositories {
        maven{ url uri('https://oss.sonatype.org/content/groups/staging')} 
    }
}
```
 * 在 application module(as默认叫app) 下的 build.gradle 中添加以下配置信息
``` groovy 
dependencies {
	implementation "com.github.drumge:kvo-api:0.2.9"
}

import com.drumge.kvo.plugin.KvoPlugin
import com.drumge.kvo.plugin.KvoTransform
apply plugin: 'com.drumge.easy.plugin'
easy_plugin {
    enable = true
    plugins{
        kvo {
            plugin = new KvoPlugin(project)
            transform = new KvoTransform(project)
        }
    }
}
```
* 在使用 KVO 的 module 的 build.gradle 中添加
```groovy
dependencies {
    annotationProcessor "com.github.drumge:kvo-compiler:0.2.5"
}
```
#### 2. KVO 初始化
出于轻量化的考虑，并不实现自己的日志库和线程池，而是提供了接口提供使用中设置自己的日志实现和线程池。这种方式对使用者来说，可以有效的控制线程池的使用和线程总数的控制。
* 初始化线程池（必须）
实现 ` com.drumge.kvo.api.thread.IKvoThread ` 接口，并通过调用 ` Kvo.getInstance().setThread(new KvoThread()) ` 初始化。

* 初始化日志（非必须）
实现 ` com.drumge.kvo.api.log.IKvoLog ` 接口，并通过调用 ` Kvo.getInstance().setLog(new KvoLog()) ` 初始化。

* 最后给一个简单的初始化例子, 然后在使用前调用 ` KvoInit.initKvo() ` 即可，初始化不会耗时，建议在 Application onCreate 中初始化
```java
public class KvoInit {

    public static void initKvo() {
        Kvo.getInstance().setLog(new KvoLog());
        Kvo.getInstance().setThread(new KvoThread());
    }

    private static class KvoThread implements IKvoThread {
        Handler mMainHandler = new Handler(Looper.getMainLooper());
        // 线程池可换成项目中统一的线程池，应用中只应存在一个全局统一的线程池，不建议创建多个线程池；
        private ScheduledExecutorService mThreadPool = Executors.newSingleThreadScheduledExecutor();

        @Override
        public void mainThread(@NonNull Runnable runnable) {
            mMainHandler.post(runnable);
        }

        @Override
        public void mainThread(@NonNull Runnable runnable, long l) {
            mMainHandler.postDelayed(runnable, l);
        }

        @Override
        public void workThread(@NonNull Runnable runnable) {
            mThreadPool.schedule(runnable, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public void workThread(@NonNull Runnable runnable, long l) {
            mThreadPool.schedule(runnable, l, TimeUnit.MILLISECONDS);
        }
    }

    private static class KvoLog implements IKvoLog {

        @Override
        public void debug(Object o, String s, Object... objects) {
            Log.d(String.valueOf(o), String.format(s, objects));
        }

        @Override
        public void info(Object o, String s, Object... objects) {
            Log.i(String.valueOf(o), String.format(s, objects));
        }

        @Override
        public void warn(Object o, String s, Object... objects) {
            Log.w(String.valueOf(o), String.format(s, objects));
        }

        @Override
        public void error(Object o, String s, Object... objects) {
            Log.e(String.valueOf(o), String.format(s, objects));
        }

        @Override
        public void error(Object o, Throwable throwable) {
            Log.e(String.valueOf(o), Log.getStackTraceString(throwable));
        }
    }
}
```

#### 3. 接口说明
前提：使用 KVO 被观察的属性，必须通过 set 相关的方法类改变属性的值才会被 KVO 自动通知到观察者。其中改变属性的方法可以使用注解指定属性。  
*注意：KVO 的使用有一些限制，不符合的规范的会在编译期间检查报错，在编译报错时可仔细阅读相关的报错信息，然后检查使用是否规范。*
* **@KvoSource(check = true)**
修饰需要被观察的属性所在的类，其中参数 check 默认值为 true， 表示会检查 @KvoSource 修饰的类中只允许存在 private 私有的属性， 否则将在编译期间报错，不能编译通过。如果需要非 private 的属性存在，可以设置 check = false, 这样子会跳过非 private 属性的检查。    
另外` com.github.drumge:kvo-compiler ` 会为每一个 @KvoSource 修饰的类的 private 类型的属性生成一个对应的 key， 方便 @KvoBind 或者 @KvoWatch 时指定绑定的属性。生成规则，@KvoSource 类对应生成一个 K_classname 的 interface 并包含对应的 private 属性，比如 ` @KvoSource public class Example{ private String name;} ` 会生成  ` public interface K_Example { String name = "name"; } `。
* **@KvoIgnore**
修饰属性，当使用了 @KvoSource(check=true) 时，类中不能存在非 private 类型的属性 ，除上边设置 check = false 之外，还可以使用 @KvoIgnore 忽略指定的属性非 private 检查。
* **@KvoBind(name = K_Example.name)**
修饰方法，绑定改变属性的方法和指定的属性，其中 ‘` name = K_Example.name `’ name 的值指定绑定的属性。@KvoBind 绑定并不是必须的，如果不使用 @KvoBind， 默认会关联属性的set方法， 其中方法名为 set+属性名(首字母大写)， 例如 属性 ` private String name; ` 对应默认的绑定的修改属性的方法是 ` public void setName(String name){} `。
* **@KvoWatch**
修饰方法，被修饰的方法不能为 private 类型。指定观察的属性，当指定的属性的值修改时，KVO 会自动调用 @KvoWatch 的方法达到通知观察者的目的。
@KvoWatch 中有三个属性，name(必须)，tag(可选)， thread(可选)。 
**name** 为指定需要观察的属性；
**thread** 指定观察者被调用的线程，比如需要 UI 更新时可指定 ` thread = KvoWatch.Thread.MAIN `；
**tag** 是为了解决需要观察同一个对象但是不同的实例的相同属性，这个时候就需要使用 tag 来区分不同的实例，比如一个页面中需要观察两个用户的信息中的nickname属性变化并更新 UI，用户信息对象 UserInfo，两个用户信息实例分别是 myUserInfo, otherUserInfo, 可以使用 ` @KvoWatch(name = K_UserInfo.nickname, tag = "my-info", thread = KvoWatch.Thread.MAIN) ` 来修饰更新 myUserInfo 的昵称的方法， 使用` @KvoWatch(name = K_UserInfo.nickname, tag = "other-info", thread = KvoWatch.Thread.MAIN) ` 来修饰更新 otherUserInfo 的昵称的方法。另外使用 ` Kvo#bind(Object, S, String) ` 绑定观察的实例时需要指定 tag。

* **KvoEvent<S, V>**
` @KvoWatch ` 修饰的方法的参数必须是 ` KvoEvent<S, V> ` ， 其中 S 是观察的对象类（@KvoSource）的类型， V 为属性的类型，例如
 ```java
 @KvoWatch(name = K_UseInfo.name, tag = "my-info", thread = KvoWatch.Thread.MAIN)
 public void onMyInfo(KvoEvent<UseInfo, String> event) { }
 ```

* **Kvo**  
最后再了解 Kvo 类中的绑定和解绑方法就可以使用 KVO了。
*注意：Kvo 必须是使用者自己调用 bind 并 unbind, 不然可能存在内存泄漏。*
```java 
/**
 * 绑定观察者
 * @param target @KvoWatch 修饰观察者所在实例
 * @param source @KvoSource 修饰的被观察对象的实例
 * @param tag 标识指定观察对象的实例
 * @param notifyWhenBind  绑定时是否通知观察者
 */
public <S> void bind(@NonNull Object target, @NonNull S source, String tag, boolean notifyWhenBind);
```
```java
/**
  * 解绑观察者
  * @param target @KvoWatch 修饰观察者所在实例
  * @param tag 标识指定观察对象的实例
  */
 public <S> void unbind(@NonNull Object target, @NonNull S source, String tag);
```
```java
 /**
  * 解绑 target 下的所有观察者
  * @param target @KvoWatch 修饰观察者所在实例
  */
 public void unbindAll(@NonNull Object target)
```

#### 4. example
可以到 [github KVO 项目中的 example module](https://github.com/drumge/kvo.git) 上看详细 example 使用。
被观察的属性：
```java 
@KvoSource(check = true)
public class ExampleSource {
    @KvoIgnore
    public int aa;
    private String example;
    private Integer index;
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

    @KvoBind(name = K_ExampleSource.mBoolean)
    public void setmBooleanHH(boolean mBoolean) {
        this.mBoolean = mBoolean;
    }

    @KvoBind(name = K_ExampleSource.sChar)
    public void setsCharDif(char sChar) {
        this.sChar = sChar;
    }
}
```

观察属性
```java
public class ExampleTarget {
    private static String TAG = "ExampleTarget";

    ExampleSource tag1;
    ExampleSource tag2;
    ExampleSource tag3;

    public ExampleTarget() {
        tag1 = new ExampleSource();
        tag2 = new ExampleSource();
        tag3 = new ExampleSource();
    }

    public ExampleSource getTag1() {
        return tag1;
    }

    public ExampleSource getTag2() {
        return tag2;
    }

    public ExampleSource getTag3() {
        return tag3;
    }

    public void bindKvo() {
        Kvo.getInstance().bind(this, tag1, "tag1", false);
        Kvo.getInstance().bind(this, tag2, "tag2");
        Kvo.getInstance().bind(this, tag3);
    }

    public void unbindKvo() {
        Kvo.getInstance().unbind(this, tag1);
        Kvo.getInstance().unbind(this, tag2);
        Kvo.getInstance().unbind(this, tag3);
    }

    public void unbindAll() {
        Kvo.getInstance().unbindAll(this);
    }

    @KvoWatch(name = K_ExampleSource.example, tag = "tag1", thread = KvoWatch.Thread.MAIN)
    public void onUpdateExampleTag1(KvoEvent<ExampleSource, String> event) {
        Log.d(TAG, "onUpdateExampleTag1 oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());
    }

    @KvoWatch(name = K_ExampleSource.example, tag = "tag2", thread = KvoWatch.Thread.WORK)
    public void onUpdateExampleTag2(KvoEvent<ExampleSource, String> event) {
        Log.d(TAG, "onUpdateExampleTag2 oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }

    @KvoWatch(name = K_ExampleSource.index)
    public void onUpdateIndex(KvoEvent<ExampleSource, Integer> event) {
        Log.d(TAG, "onUpdateIndex oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }

    @KvoWatch(name = K_ExampleSource.sChar)
    public void onUpdateChat(KvoEvent<ExampleSource, Character> event) {
        Log.d(TAG, "onUpdateChat oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }
}
```


# todo list
* Kvo 引用改用弱引用，避免使用者绑定之后忘记解绑导致内存泄漏；使用弱引用之后不能使用匿名类，同时检查匿名类的使用，编译期间报错。
* 绑定指定属性，解绑指定属性
* @KvoWatch 绑定时通知的顺序
* 继承关系，支持子类接收父类的@KvoWatch通知






