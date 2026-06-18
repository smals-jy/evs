package org.imec.ivlab.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.runtime.resource.loader.ResourceReader;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public final class TemplateEngineUtils {

    private final static Logger LOG = LogManager.getLogger(TemplateEngineUtils.class);
    protected static final VelocityEngine VELOCITY_ENGINE;

    private static Map<String, Template> templateCache = new HashMap<>();

    private TemplateEngineUtils() {
        throw new UnsupportedOperationException();
    }

    public static String generate(Map<String, Object> ctx, String templateLocation) {
        VelocityContext context = new VelocityContext();
        
        // Use forEach instead of Iterator for better readability
        ctx.forEach((key, value) -> context.put(key, value));

        if (!templateCache.containsKey(templateLocation)) {
            templateCache.put(templateLocation, VELOCITY_ENGINE.getTemplate(templateLocation, "UTF-8"));
        }

        Template template = templateCache.get(templateLocation);

        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    static {
        LOG.info("Init of TemplateEngineUtils");
        VELOCITY_ENGINE = new VelocityEngine();
        
        // Updated property names for Velocity 2.4.1
        VELOCITY_ENGINE.setProperty("resource.loaders", "classpath");
        VELOCITY_ENGINE.setProperty("resource.loader.classpath.class", TemplateEngineUtils.ClasspathResourceLoader.class.getName());
        VELOCITY_ENGINE.setProperty("velocimacro.library", "templates/VM_connector_library.vm");
        VELOCITY_ENGINE.setProperty("resource.manager.logwhenfound", "true");
        
        VELOCITY_ENGINE.init();
    }


    public static class ClasspathResourceLoader extends ResourceLoader {
        public ClasspathResourceLoader() {
        }

        @Override
        public void init(java.util.Map<String, Object> configuration) {
            // Configuration initialization for Velocity 2.4.1
        }

        @Override
        public InputStream getResourceStream(String source) throws ResourceNotFoundException {
            return IOUtils.getResourceAsStream(source);
        }

        @Override
        public ResourceReader getResourceReader(String source, String encoding) throws ResourceNotFoundException {
            InputStream inputStream = IOUtils.getResourceAsStream(source);
            if (inputStream == null) {
                throw new ResourceNotFoundException("Resource not found: " + source);
            }
            return new org.apache.velocity.runtime.resource.loader.InputStreamResourceReader(inputStream);
        }

        @Override
        public boolean isSourceModified(Resource resource) {
            return false;
        }

        @Override
        public long getLastModified(Resource resource) {
            return System.currentTimeMillis();
        }
    }
}
