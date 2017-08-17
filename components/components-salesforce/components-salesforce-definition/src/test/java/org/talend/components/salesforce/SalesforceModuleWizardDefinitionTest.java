// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.components.salesforce;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.talend.components.api.wizard.ComponentWizard;
import org.talend.components.salesforce.datastore.SalesforceDatastoreProperties2;
import org.talend.daikon.definition.DefinitionImageType;

/**
 *
 */
public class SalesforceModuleWizardDefinitionTest {

    private final String repoLocation = "___DRI";

    private SalesforceModuleDatasetWizardDefinition definition;
    private SalesforceModuleListProperties properties;

    @Before
    public void setUp() {
        definition = new SalesforceModuleDatasetWizardDefinition();

        properties = new SalesforceModuleListProperties("module");
        properties.init();
    }

    @Test
    public void testSupportsProperties() {
        assertTrue(definition.supportsProperties(SalesforceModuleListProperties.class));
        assertFalse(definition.supportsProperties(SalesforceDatastoreProperties2.class));
    }

    @Test
    public void testCreateWizard() {
        ComponentWizard wizard = definition.createWizard(properties, repoLocation);

        assertThat(wizard, instanceOf(SalesforceModuleDatasetWizard.class));
        assertEquals(definition, wizard.getDefinition());
        assertEquals(repoLocation, wizard.getRepositoryLocation());
    }

    @Test
    public void testImagePath() {
        assertNotNull(definition.getImagePath(DefinitionImageType.TREE_ICON_16X16));
        assertNotNull(definition.getImagePath(DefinitionImageType.WIZARD_BANNER_75X66));
        assertNull(definition.getImagePath(DefinitionImageType.SVG_ICON));
    }
}
