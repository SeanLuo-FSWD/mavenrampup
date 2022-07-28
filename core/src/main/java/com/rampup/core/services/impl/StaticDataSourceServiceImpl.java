package com.rampup.core.services.impl;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.crx.JcrConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rampup.core.services.DataSourceService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import javax.jcr.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DataSourceService.class, immediate = true, scope = ServiceScope.SINGLETON)
@Slf4j
public class StaticDataSourceServiceImpl implements DataSourceService {
    private static final Logger LOG = LoggerFactory.getLogger(StaticDataSourceServiceImpl.class);

    @Override
    public SlingHttpServletRequest getDataFromSource(@NonNull final SlingHttpServletRequest request) {

        log.error("hellooooo222222");

        LOG.error("hellooooo333333");

        final String path = determineDataSourcePath(request);
        log.debug("Start creating dataSource from <{}>", path);

        final ResourceResolver resolver = request.getResourceResolver();

        try (final InputStream jsonStream = getJsonStreamFromJcr(path, resolver)) {

            if (jsonStream != null) {
                final List<DataSourceValueMap> data = deserializeValueMaps(jsonStream, path);
                final List<Resource> valueMapResourceList = dataToResources(data, resolver);

                // Feed resources into dataSource
                final DataSource dataSource = new SimpleDataSource(valueMapResourceList.iterator());
                request.setAttribute(DataSource.class.getName(), dataSource);
                log.debug("Datasource from <{}> created", path);
            }

        } catch (final IOException e) {
            log.error("Could not close JSON input stream from node <{}>", path, e);
        }

        return request;
    }

    /**
     * Finds the correct data source in the JCR from the information in the request.
     *
     * @param request the request to a component depending on a data source
     * @return the data source
     */
    private String determineDataSourcePath(@NonNull final SlingHttpServletRequest request) {
        final Resource nodeResource = request.getResource();
        final Resource datasourceResource = nodeResource.getChild("datasource");

        if (datasourceResource == null) {
            throw new IllegalArgumentException("No datasource child found for node at " + nodeResource.getPath());
        }

        return "/apps/" + datasourceResource.getResourceType() + "/data.json";
    }

    /**
     * De-serializes JSON objects to a list of value maps.
     *
     * @param jsonStream a JSON stream containing a list of serialized value maps
     * @param path       the path of the JSON file in the JCR
     * @return a list of de-serialized value maps
     */
    private List<DataSourceValueMap> deserializeValueMaps(@NonNull final InputStream jsonStream, @NonNull final String path) {
        List<DataSourceValueMap> data = new ArrayList<>();
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final TypeReference<List<DataSourceValueMap>> type = new TypeReference<List<DataSourceValueMap>>() {
            };
            data = objectMapper.readValue(jsonStream, type);
            log.debug("<{}> values found in json file", data.size());
        } catch (final IOException e) {
            log.error("Unexpected exception while retrieving json values from file <{}>", path, e);
        }
        return data;
    }

    /**
     * Converts a list of value maps to a list of resources.
     *
     * @param data     a list of value maps
     * @param resolver a resource resolver
     * @return a list of resources
     */
    private List<Resource> dataToResources(@NonNull final List<DataSourceValueMap> data, @NonNull final ResourceResolver resolver) {
        return data.stream().map(entry -> {
            final ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
            valueMap.put("value", entry.getValue());
            valueMap.put("text", entry.getText());
            return new ValueMapResource(resolver, new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED, valueMap);
        }).collect(Collectors.toList());
    }

    /**
     * Reads a JSON file in the JCR and reads it as an InputStream.
     *
     * @param path     the path of the JSON file in the JCR
     * @param resolver a resource resolver
     * @return an InputStream containing the contents of the JSON file
     */
    public InputStream getJsonStreamFromJcr(@NonNull final String path, @NonNull final ResourceResolver resolver) {
        final Session session = resolver.adaptTo(Session.class);

        try {
            final Node dataSourceNode = JcrUtils.getNodeIfExists(path, session);
            if (dataSourceNode == null) {
                throw new IllegalArgumentException("Data source node <" + path + "> does not exist");
            }

            final Node jcrContent = dataSourceNode.getNode(JcrConstants.JCR_CONTENT);
            if (jcrContent == null) {
                throw new IllegalArgumentException("Data source node <" + path + "> has no <" + JcrConstants.JCR_DATA + "> child");
            }

            final Property jcrData = jcrContent.getProperty(JcrConstants.JCR_DATA);
            if (jcrData == null) {
                throw new IllegalArgumentException("Node <" + jcrContent.getPath() + "> has no <" + JcrConstants.JCR_DATA + "> property");
            }

            final Binary binary = jcrData.getBinary();
            if (binary == null) {
                throw new IllegalArgumentException("Property <" + JcrConstants.JCR_DATA + "> of node <" + jcrContent.getPath() + "> has no binary data");
            }

            final InputStream jsonStream = binary.getStream();
            if (jsonStream == null) {
                throw new IllegalArgumentException("Could not read dataSource from node " + path);
            }

            return jsonStream;

        } catch (final RepositoryException e) {
            log.error("Could not read JSON from <{}>", path, e);
            return null;
        }
    }

    @Getter
    @Setter
    public static class DataSourceValueMap {
        private String value;
        private String text;
    }
}