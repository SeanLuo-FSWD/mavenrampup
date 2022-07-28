package com.rampup.core.services;

import lombok.NonNull;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import java.io.InputStream;

public interface DataSourceService {
    SlingHttpServletRequest getDataFromSource(@NonNull SlingHttpServletRequest request);

    InputStream getJsonStreamFromJcr(@NonNull String path, @NonNull ResourceResolver resolver);
}