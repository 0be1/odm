package fr.mtlx.odm;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import static java.lang.String.format;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.isAbstract;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.persistence.PrePersist;
import static org.springframework.util.ReflectionUtils.doWithMethods;

public class ClassMetadataBuilder {

    public static <T> PartialClassMetadata<T> build(final Class<T> persistentClass) throws MappingException {

        final PartialClassMetadata<T> metadata = new PartialClassMetadata<>(
                persistentClass);

        if (isAbstract(persistentClass.getModifiers())) {
            throw new InstantiationError(format("%s is abstract",
                    persistentClass.getName()));
        }

        Class<? super T> currentClass = persistentClass;

        List<String> classHierarchy = Lists.newArrayList();
        Set<String> auxiliaryClasses = Sets.newHashSet();

        boolean isStrict = false;

        do {
            Entry entry = currentClass.getAnnotation(Entry.class);

            if (entry != null) {
                classHierarchy.addAll(Lists.reverse(Arrays.asList(entry.objectClasses())));

                auxiliaryClasses.addAll(Arrays.asList(entry
                        .auxiliaryObjectClasses()));

                isStrict |= entry.ignoreNonMatched();
            }

            currentClass = currentClass.getSuperclass();
        } while (currentClass != null);

        assert currentClass == null;

        if (classHierarchy.isEmpty()) {
            throw new MappingException(format(
                    "%s is not a persistent class", persistentClass));
        }

        metadata.setObjectClassHierarchy(ImmutableList.copyOf(Lists.reverse(classHierarchy)));

        metadata.setAuxiliaryClasses(ImmutableSet.copyOf(auxiliaryClasses));

        try {
            metadata.setDefaultConstructor(persistentClass
                    .getConstructor(new Class<?>[]{}));
        } catch (SecurityException e) {
            throw new MappingException(e);
        } catch (NoSuchMethodException e) {
            throw new MappingException(format(
                    "no public default constructor found for %s", persistentClass), e);
        }

        metadata.setStrict(isStrict);

        metadata.setCacheable(isCacheable(persistentClass));

        metadata.setPrepersistMethods(persistMethods(persistentClass));

        return metadata;
    }

    private static List<Method> persistMethods(final Class<?> persistentClass) {
        final List<Method> prepersistMethods = Lists.newArrayList();

        doWithMethods(persistentClass, (Method method) -> {
            if (method.getAnnotation(PrePersist.class) != null) {
                prepersistMethods.add(method);
            }
        });

        return prepersistMethods;
    }

    private static boolean isCacheable(final Class<?> persistentClass) {
        return persistentClass.getAnnotation(javax.persistence.Cacheable.class) != null;
    }
}
