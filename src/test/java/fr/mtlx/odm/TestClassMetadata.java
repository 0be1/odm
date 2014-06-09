/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.mtlx.odm;

import javax.persistence.Cacheable;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author alex
 */
public class TestClassMetadata {

    @Cacheable
    @Entry(objectClasses = {"class0", "class1"}, auxiliaryObjectClasses = {"aux1", "aux2"})
    static class TestClass1 {
        public TestClass1() {}
    }

    @Entry(objectClasses = "class2", auxiliaryObjectClasses = {"aux2", "aux3"})
    static class TestClass2 extends TestClass1 {
        public TestClass2() {}

    }

    private ClassMetadata<TestClass1> metadata1;
    private ClassMetadata<TestClass2> metadata2;

    @Before
    public void init() throws MappingException {
        metadata1 = ClassMetadataBuilder.build(TestClass1.class);
        
        metadata2 = ClassMetadataBuilder.build(TestClass2.class);
    }

    @Test
    public void persistentClass() {

        assertThat(metadata1.getPersistentClass(), equalTo(TestClass1.class));

        assertThat(metadata2.getPersistentClass(), equalTo(TestClass2.class));

    }

    @Test
    public void isCacheable() {

        assertTrue(metadata1.isCacheable());

        assertFalse(metadata2.isCacheable());
    }

    @Test
    public void structuralClass() {

        assertThat(metadata1.getStructuralClass(), equalToIgnoringCase("class1"));

        assertThat(metadata2.getStructuralClass(), equalToIgnoringCase("class2"));

    }

    @Test
    public void auxiliaryClasses() {

        assertThat(metadata1.getAuxiliaryClasses(), containsInAnyOrder(new String[]{"aux1", "aux2"}));
        
        assertThat(metadata2.getAuxiliaryClasses(), containsInAnyOrder(new String[]{"aux1", "aux2", "aux3"}));

    }
    
    @Test
    public void objectClassHierarchy() {

        assertThat(metadata1.getObjectClassHierarchy(), contains(new String[]{"class0", "class1"}));
        
        assertThat(metadata2.getObjectClassHierarchy(), contains(new String[]{"class0","class1", "class2", }));

    }
}
