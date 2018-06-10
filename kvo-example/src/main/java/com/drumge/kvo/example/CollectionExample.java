package com.drumge.kvo.example;

import com.drumge.kvo.annotation.KvoSource;

import java.util.List;

/**
 * Created by chenrenzhan on 2018/6/10.
 */

@KvoSource
public class CollectionExample {

    private List<ExampleSource> sourceList;

    public void setSourceList(List<ExampleSource> sourceList) {
        this.sourceList = sourceList;
    }
}
