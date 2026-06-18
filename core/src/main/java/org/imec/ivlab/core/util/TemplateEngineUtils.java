package org.imec.ivlab.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TemplateEngineUtils {

    private final static Logger LOG = LogManager.getLogger(TemplateEngineUtils.class);
    protected static final VelocityEngine VELOCITY_ENGINE;

    // Fixed: Thread-safe cache to prevent ConcurrentModificationException in production
    private static final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    private TemplateEngineUtils() {
        throw new UnsupportedOperationException();
    }

    public static String generate(Map<String, Object> ctx, String templateLocation) {
        // Fixed: Pass map directly to the constructor for efficiency
        VelocityContext context = new VelocityContext(ctx);

        // Fixed: Atomic computeIfAbsent guarantees thread safety
        Template template = templateCache.computeIfAbsent(templateLocation, location -> 
            VELOCITY_ENGINE.getTemplate(location, "UTF-8")
        );

        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    static {
        LOG.info("Init of TemplateEngineUtils");
        VELOCITY_ENGINE = new VelocityEngine();
        
        // Configuration property names for Velocity 2.x
        VELOCITY_ENGINE.setProperty("resource.loaders", "classpath");
        VELOCITY_ENGINE.setProperty("resource.loader.classpath.class", TemplateEngineUtils.ClasspathResourceLoader.class.getName());
        VELOCITY_ENGINE.setProperty("velocimacro.library.path", "templates/VM_connector_library.vm");
        VELOCITY_ENGINE.setProperty("resource.manager.logwhenfound", "true");
        
        VELOCITY_ENGINE.init();
    }

    public static class ClasspathResourceLoader extends ResourceLoader {
        public ClasspathResourceLoader() {
        }

        @Override
        public void init(Map<String, Object> configuration) {
            // ExtProperties maps directly to Map<String, Object> here
        }

        // FIXED: Official Velocity 2.4.1 signature (returns standard java.io.Reader)
        @Override
        public Reader getResourceReader(String source, String encoding) throws ResourceNotFoundException {
            // Fixed: Replaced non-standard IOUtils call with native ClassLoader routing
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(source);
            if (inputStream == null) {
                inputStream = TemplateEngineUtils.class.getResourceAsStream(source);
            }
            
            if (inputStream == null) {
                throw new ResourceNotFoundException("Resource not found: " + source);
            }
            
            // Return a standard Java Reader wrapped with the proper encoding
            return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        }

        @Override
        public boolean isSourceModified(Resource resource) {
            return false;
        }

        @Override
        public long getLastModified(Resource resource) {
            return 0L;
        }
    }
}
