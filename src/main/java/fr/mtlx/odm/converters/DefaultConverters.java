package fr.mtlx.odm.converters;

import java.lang.reflect.Type;
import java.util.Date;

import javax.naming.Name;
import javax.naming.ldap.LdapName;

import com.google.common.collect.ImmutableMap;

import fr.mtlx.odm.attributes.LabeledURI;

public final class DefaultConverters {

    public static final ImmutableMap<String, Converter> defaultSyntaxConverters = new ImmutableMap.Builder<String, Converter>()
            .put("1.3.6.1.4.1.1466.115.121.1.7", new BooleanConverter())
            .put("1.3.6.1.4.1.1466.115.121.1.27", new IntegerConverter())
            .put("1.3.6.1.4.1.1466.115.121.1.36", new LongConverter())
            .put("1.3.6.1.4.1.1466.115.121.1.15",
                    new DirectoryStringConverter())
            .put("1.3.6.1.4.1.1466.115.121.1.5", new BinaryStringConverter())
            .put("1.3.6.1.4.1.1466.115.121.1.24", new TimeConverter())
            .put("1.3.6.1.4.1.1466.115.121.1.12",
                    new DistinguishedNameConverter()).build();

    public static final ImmutableMap<Type, Converter> defaultAttributeConverters = new ImmutableMap.Builder<Type, Converter>()
            .put(LabeledURI.class, new LabeledURIConverter())
            .put(Integer.TYPE, new IdentityConverter<>(Integer.TYPE))
            .put(Integer.class, new IdentityConverter<>(Integer.class))
            .put(Short.TYPE, new IdentityConverter<>(Short.TYPE))
            .put(Short.class, new IdentityConverter<>(Short.class))
            .put(Byte.TYPE, new IdentityConverter<>(Byte.TYPE))
            .put(Byte.class, new IdentityConverter<>(Byte.TYPE))
            .put(Boolean.TYPE, new IdentityConverter<>(Boolean.TYPE))
            .put(Boolean.class, new IdentityConverter<>(Boolean.class))
            .put(String.class, new IdentityConverter<>(String.class))
            .put(byte[].class, new IdentityConverter<>(byte[].class))
            .put(Name.class, new IdentityConverter<>(Name.class))
            .put(LdapName.class, new IdentityConverter<>(LdapName.class))
            .put(Date.class, new IdentityConverter<>(Date.class))
            .put(Long.TYPE, new IdentityConverter<>(Long.TYPE))
            .put(Long.class, new IdentityConverter<>(Long.class)).build();

    public static final ImmutableMap<Type, String> defaultSyntaxes = new ImmutableMap.Builder<Type, String>()
            .put(Integer.TYPE, "1.3.6.1.4.1.1466.115.121.1.27")
            .put(Integer.class, "1.3.6.1.4.1.1466.115.121.1.27")
            .put(Short.TYPE, "1.3.6.1.4.1.1466.115.121.1.27")
            .put(Short.class, "1.3.6.1.4.1.1466.115.121.1.27")
            .put(Byte.TYPE, "1.3.6.1.4.1.1466.115.121.1.27")
            .put(Byte.class, "1.3.6.1.4.1.1466.115.121.1.27")
            .put(Boolean.TYPE, "1.3.6.1.4.1.1466.115.121.1.7")
            .put(Boolean.class, "1.3.6.1.4.1.1466.115.121.1.7")
            .put(String.class, "1.3.6.1.4.1.1466.115.121.1.15")
            .put(byte[].class, "1.3.6.1.4.1.1466.115.121.1.5")
            .put(Name.class, "1.3.6.1.4.1.1466.115.121.1.12")
            .put(LdapName.class, "1.3.6.1.4.1.1466.115.121.1.12")
            .put(Date.class, "1.3.6.1.4.1.1466.115.121.1.24")
            .put(Long.TYPE, "1.3.6.1.4.1.1466.115.121.1.36")
            .put(Long.class, "1.3.6.1.4.1.1466.115.121.1.36").build();
}
