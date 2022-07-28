package com.rampup.core.models;

import javax.annotation.PostConstruct;

public interface StaticDataSourceModel {
    @PostConstruct
    void init();
}