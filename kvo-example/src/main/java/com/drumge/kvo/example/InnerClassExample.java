package com.drumge.kvo.example;

import com.drumge.kvo.annotation.KvoBind;
import com.drumge.kvo.annotation.KvoIgnore;
import com.drumge.kvo.annotation.KvoSource;

/**
 * Created by chenrenzhan on 2018/6/11.
 */

@KvoSource
public class InnerClassExample {

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    @KvoSource
    public class Inner {
        @KvoIgnore
        private String name;

    }

    @KvoSource(check = true)
    public static class InnerStatic {
        private String name;

        @KvoBind(name = K_InnerClassExample.InnerStatic.name)
        public void updateName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @KvoSource
    class InnerPrivate {
        private String name;

        public void setName(String name) {
            this.name = name;
        }
    }

    @KvoSource
    class InnerPrivateStatic {
        private String name;

        @KvoBind(name = K_InnerClassExample.InnerPrivateStatic.name)
        public void updateName(String name) {
            this.name = name;
        }
    }
}
