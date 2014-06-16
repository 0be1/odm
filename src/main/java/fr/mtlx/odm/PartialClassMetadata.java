package fr.mtlx.odm;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import fr.mtlx.odm.filters.Filter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.springframework.util.ReflectionUtils.doWithFields;

public class PartialClassMetadata<T> implements ClassMetadata<T> {

    private final Class<T> persistentClass;

    private ImmutableList<String> objectClassHierarchy;

    private ImmutableSet<String> auxiliaryClasses;

    private Constructor<T> defaultConstructor;

    private Field identifierField;

    private Map<String, AttributeMetadata> attributeMetadataByAttributeName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private Map<String, AttributeMetadata> attributeMetadataByPropertyName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private List<Method> prepersistMethods = Lists.newArrayList();

    private boolean cacheable;

    private boolean strict;

    private Boolean initState = true;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    PartialClassMetadata(final Class<T> persistentClass) {
        this.persistentClass = checkNotNull(persistentClass);
    }

    @Override
    public Field getIdentifier() {
        return identifierField;
    }

    @Override
    public String getIdentifierPropertyName() {
        return getIdentifier() != null ? getIdentifier().getName() : null;
    }

    @Override
    public ImmutableList<String> getObjectClassHierarchy() {
        return objectClassHierarchy;
    }

    @Override
    public ImmutableSet<String> getAuxiliaryClasses() {
        return auxiliaryClasses;
    }

    @Override
    public Constructor<T> getDefaultConstructor() {
        return defaultConstructor;
    }

    @Override
    public String getStructuralClass() {
        return Iterables.getLast(objectClassHierarchy);
    }

    @Override
    public Class<T> getPersistentClass() {
        return this.persistentClass;
    }

    @Override
    public Filter getByExampleFilter() {
        return null;
    }

    @Override
    public AttributeMetadata getAttributeMetadataByAttributeName(
            final String attributeId) {
        return attributeMetadataByAttributeName.get(attributeId);
    }

    @Override
    public AttributeMetadata getAttributeMetadata(
            String propertyName) {
        return attributeMetadataByPropertyName.get(propertyName);
    }

    @Override
    public ImmutableSet<String> getProperties() {
        return ImmutableSet.copyOf(attributeMetadataByPropertyName.keySet());
    }

    @Override
    public Method[] prepersistMethods() {
        return prepersistMethods.toArray(new Method[]{});
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public boolean isStrict() {
        return strict;
    }

    private void initPersistentAttributes(
            final SessionFactoryImpl sessionFactory) {
        final List<AttributeMetadata> attributes = Lists.newArrayList();

        doWithFields(persistentClass, (@Nullable final Field f) -> {
            if (f == null || identifierField.equals(f) || isTransient(f)) {
                return;
            }
            if (attributeMetadataByPropertyName.containsKey(f.getName())) {
                return;
            }
            try {
                attributes.add(new AttributeMetadataFactory(persistentClass, sessionFactory).build(f));
            } catch (MappingException e) {
                throw new IllegalArgumentException(e);
            }
        });

        for(AttributeMetadata metadata : attributes) {
            final String name = metadata.getAttirbuteName();
            if (!(Strings.isNullOrEmpty(name))) {
                if (attributeMetadataByAttributeName.containsKey(name)) {
                    assert attributeMetadataByPropertyName.containsKey(metadata
                            .getPropertyName());

                    log.warn("Attribute {} already declared.",
                            metadata.getAttirbuteName());
                } else {
                    attributeMetadataByAttributeName.put(
                            metadata.getAttirbuteName(), metadata);

                    assert !attributeMetadataByPropertyName.containsKey(metadata
                            .getPropertyName());

                    attributeMetadataByPropertyName.put(metadata.getPropertyName(),
                            metadata);
                }
                for (final String alias : metadata.getAttributeAliases()) {
                    if (Strings.isNullOrEmpty(alias)) {
                        continue;
                    }
                    if (attributeMetadataByAttributeName.containsKey(alias)) {
                        log.warn("Attribute {} already declared.",
                                metadata.getAttirbuteName());
                    } else {
                        attributeMetadataByAttributeName.put(alias, metadata);
                    }
                }
            }
        }
    }

    private boolean isTransient(final Field field) {
        int modifiers = checkNotNull(field).getModifiers();

        return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
    }

    private void findIdentifier() {
        doWithFields(persistentClass, (Field field) -> {
            Id identifier;

            identifier = field.getAnnotation(Id.class);

            if (identifier != null) {
                identifierField = field;
            }
        });
    }

    public Field getIdentifierField() {
        return identifierField;
    }

    public void setIdentifierField(Field identifierField) {
        this.identifierField = identifierField;
    }

    public Map<String, AttributeMetadata> getAttributeMetadataByAttributeName() {
        return attributeMetadataByAttributeName;
    }

    public void setAttributeMetadataByAttributeName(
            Map<String, AttributeMetadata> attributeMetadataByAttributeName) {
        this.attributeMetadataByAttributeName = attributeMetadataByAttributeName;
    }

    public Map<String, AttributeMetadata> getAttributeMetadataByPropertyName() {
        return attributeMetadataByPropertyName;
    }

    public void setAttributeMetadataByPropertyName(
            Map<String, AttributeMetadata> attributeMetadataByPropertyName) {
        this.attributeMetadataByPropertyName = attributeMetadataByPropertyName;
    }

    public List<Method> getPrepersistMethods() {
        return prepersistMethods;
    }

    public void setPrepersistMethods(List<Method> prepersistMethods) {
        this.prepersistMethods = prepersistMethods;
    }

    public void setObjectClassHierarchy(
            ImmutableList<String> objectClassHierarchy) {
        this.objectClassHierarchy = objectClassHierarchy;
    }

    public void setAuxiliaryClasses(ImmutableSet<String> auxiliaryClasses) {
        this.auxiliaryClasses = auxiliaryClasses;
    }

    public void setDefaultConstructor(Constructor<T> defaultConstructor) {
        this.defaultConstructor = defaultConstructor;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    void init(final SessionFactoryImpl sessionFactory) {
        if (initState) {
            synchronized (initState) {
                if (initState) {
                    initState = false;

                    findIdentifier();

                    initPersistentAttributes(sessionFactory);
                }
            }
        }
    }
}
