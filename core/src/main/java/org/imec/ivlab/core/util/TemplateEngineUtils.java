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

    private static final Logger LOG = LogManager.getLogger(TemplateEngineUtils.class);

    protected static final VelocityEngine VELOCITY_ENGINE;

    /** Thread-safe template cache */
    private static final Map<String, Template> TEMPLATE_CACHE = new ConcurrentHashMap<>();

    private TemplateEngineUtils() {
        throw new UnsupportedOperationException();
    }

    static {
        LOG.info("Init of TemplateEngineUtils");
        VELOCITY_ENGINE = new VelocityEngine();

        // Velocity 2.x property names
        VELOCITY_ENGINE.setProperty("resource.loaders", "classpath");
        VELOCITY_ENGINE.setProperty(
                "resource.loader.classpath.class",
                TemplateEngineUtils.ClasspathResourceLoader.class.getName());
        VELOCITY_ENGINE.setProperty("velocimacro.library.path", "templates/VM_connector_library.vm");
        VELOCITY_ENGINE.setProperty("resource.manager.logwhenfound", "true");

        VELOCITY_ENGINE.init();
    }

    /**
     * Renders the Velocity template at templateLocation with the given context map.
     *
     * `@param` ctx              key/value pairs to expose to the template
     * `@param` templateLocation classpath-relative path to the .vm template
     * `@return` rendered string
     */
    public static String generate(Map<String, Object> ctx, String templateLocation) {
        VelocityContext context = new VelocityContext(ctx);

        Template template = TEMPLATE_CACHE.computeIfAbsent(
                templateLocation,
                location -> VELOCITY_ENGINE.getTemplate(location, StandardCharsets.UTF_8.name()));

        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    // -------------------------------------------------------------------------
    // Custom classpath resource loader (Velocity 2.x API)
    // -------------------------------------------------------------------------

    public static class ClasspathResourceLoader extends ResourceLoader {

        public ClasspathResourceLoader() {
        }

        /**
         * Velocity 2.x calls this with a Map (String as key, Object as values) extracted from
         * the engine configuration.  No additional setup is required here.
         */
        @Override
        public void init(Map<String, Object> configuration) {
            // nothing to initialise
        }

        /**
         * Returns a Readerfor the requested template resource.
         * Velocity 2.x removed getResourceAsStream, this is the replacement.
         */
        @Override
        public Reader getResourceReader(String source, String encoding)
                throws ResourceNotFoundException {

            InputStream is = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(source);

            if (is == null) {
                is = TemplateEngineUtils.class.getResourceAsStream(source);
            }

            if (is == null) {
                throw new ResourceNotFoundException("Resource not found: " + source);
            }

            return new InputStreamReader(is, StandardCharsets.UTF_8);
        }

        /** Templates are loaded from the classpath and never change at runtime. */
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
