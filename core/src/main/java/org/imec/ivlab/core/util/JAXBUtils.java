package org.imec.ivlab.core.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imec.ivlab.core.exceptions.TransformationException;

import be.ehealth.technicalconnector.adapter.XmlDateNoTzAdapter;
import be.ehealth.technicalconnector.adapter.XmlTimeNoTzAdapter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;

public class JAXBUtils {
    private final static Logger log = LogManager.getLogger(IOUtils.class);
    public static <T> T unmarshal(Class<T> clazz, String content) throws TransformationException {

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            JAXBElement<T> jaxbElement = (JAXBElement<T>) jaxbUnmarshaller.unmarshal(new StreamSource(new StringReader(content)), clazz);
            return jaxbElement.getValue();

        } catch (JAXBException e) {
            throw new TransformationException("Failed to convert content to class: " + clazz.getName(), e);
        }

    }

    public static String marshal(Object o) throws JAXBException {

        String rootElementName = "";
        if(o==null || o.getClass()==null){
            log.info("The object or objectclass was null. Could not marshall object, returning empty string.");
            return "";
        }
        for(Annotation annotation: o.getClass().getAnnotations()){
            if(annotation.annotationType() == XmlRootElement.class){
                rootElementName = ((XmlRootElement)annotation).name();
                break;
            }
        }

        return marshal(o, rootElementName);

    }

    public static String marshal(Object o, String rootName) throws JAXBException {


        JAXBContext jaxbContext = JAXBContext.newInstance(o.getClass());
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8");

        // Configure the Marshaller to use the custom XmlAdapter for LocalTime
        jaxbMarshaller.setAdapter(new XmlTimeNoTzAdapter());
        jaxbMarshaller.setAdapter(new XmlDateNoTzAdapter());

        StringWriter writer = new StringWriter();

        String nsURI = "";
        for(Annotation annotation: o.getClass().getPackage().getAnnotations()){
            if(annotation.annotationType() == XmlSchema.class){
                nsURI = ((XmlSchema)annotation).namespace();
                break;
            }
        }

        QName qName = new QName(nsURI, rootName);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        JAXBElement<?> root = new JAXBElement(qName, o.getClass(), o);

        jaxbMarshaller.marshal(root, writer);

        return replaceTimeTags(writer.toString());

    }

    // Dirty trick as issue with JAXB adapter
    private static String replaceTimeTags(String xmlString) {
        // Define a regular expression to match <time> tags with a timezone offset
        String regex = "<time>\\s*(\\d+:\\d+:\\d+).*\\s*</time>";
        // Replace matched <time> tags with the content inside the tags
        return xmlString.replaceAll(regex, "<time>$1</time>");
    }


}
