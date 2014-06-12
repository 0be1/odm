package fr.mtlx.odm.utils;

public interface Converter<F, T> {
    
    T convert(F object);
}
