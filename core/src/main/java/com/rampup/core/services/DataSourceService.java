package com.rampup.core.services;

import lombok.NonNull;
import org.apache.sling.api.SlingHttpServletRequest;

public interface DataSourceService {
    SlingHttpServletRequest getDataFromSource(@NonNull SlingHttpServletRequest request);
}