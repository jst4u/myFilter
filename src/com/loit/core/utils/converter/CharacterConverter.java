package com.loit.core.utils.converter;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;

public final class CharacterConverter
    implements Converter
{

    private Object defaultValue;
    private boolean useDefault;

    public CharacterConverter()
    {
        defaultValue = null;
        defaultValue = null;
        useDefault = false;
    }

    public CharacterConverter(Object defaultValue)
    {
        this.defaultValue = null;
        this.defaultValue = defaultValue;
        useDefault = true;
    }

    public Object convert(Class type, Object value)
    {
        if(value == null || "".equals(value.toString().trim()))
            return defaultValue;
        if(value instanceof Character)
            return value;
        try
        {
            return new Character(value.toString().charAt(0));
        }
        catch(Exception e)
        {
            if(useDefault)
                return defaultValue;
            else
                throw new ConversionException(e);
        }
    }
}
