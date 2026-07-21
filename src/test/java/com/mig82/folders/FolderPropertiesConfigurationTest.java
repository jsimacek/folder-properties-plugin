package com.mig82.folders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mig82.folders.properties.FolderProperties;
import com.mig82.folders.properties.StringProperty;
import hudson.util.FormValidation;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.StaplerRequest2;

class FolderPropertiesConfigurationTest {

    @Test
    void preservesAppendSemanticsAndDefaultsToScopedExposure() {
        FolderProperties<?> properties = new FolderProperties<>();

        properties.setProperties(new StringProperty[] {new StringProperty("first", "one")});
        properties.setProperties(new StringProperty[] {new StringProperty("second", "two")});

        assertEquals(2, properties.getProperties().length);
        assertEquals("first", properties.getProperties()[0].getKey());
        assertEquals("second", properties.getProperties()[1].getKey());
        assertFalse(properties.isExposeAtBuildStart());
    }

    @Test
    void returnsNullWhenFolderPropertyIsRemovedFromConfiguration() throws Exception {
        assertNull(new FolderProperties<>().reconfigure((StaplerRequest2) null, null));
    }

    @Test
    void validatesPropertyKeys() {
        StringProperty.DescriptorImpl descriptor = new StringProperty.DescriptorImpl();

        assertEquals(FormValidation.Kind.ERROR, descriptor.doValidate("  ", "value").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doValidate("key", "value").kind);
    }
}
