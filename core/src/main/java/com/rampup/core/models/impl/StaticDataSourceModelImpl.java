package com.rampup.core.models.impl;

import com.rampup.core.models.StaticDataSourceModel;
import com.rampup.core.services.DataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.osgi.service.component.annotations.Component;

import javax.annotation.PostConstruct;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {StaticDataSourceModel.class}
)
@Component
@Slf4j
public class StaticDataSourceModelImpl implements StaticDataSourceModel {

    @OSGiService
    private DataSourceService dataSourceService;

    @Self
    private SlingHttpServletRequest request;

    @Override
    @PostConstruct
    public void init() {
        request = dataSourceService.getDataFromSource(request);
    }
}