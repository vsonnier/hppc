package com.carrotsearch.hppc.generator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Template options for velocity directives in templates.
 */
public class TemplateOptions
{
    public Type ktype;
    public Type vtype;

    public boolean doNotGenerateKType = false;
    public boolean doNotGenerateVType = false;

    public File sourceFile;

    public TemplateOptions(final Type ktype)
    {
        this(ktype, null);
    }

    public TemplateOptions(final Type ktype, final Type vtype)
    {
        this.ktype = ktype;
        this.vtype = vtype;
    }

    public boolean isKTypePrimitive()
    {
        return ktype != Type.GENERIC;
    }

    public boolean isKTypeNumeric()
    {
        return (ktype != Type.GENERIC && ktype != Type.BOOLEAN);
    }

    public boolean isKTypeBoolean()
    {
        return (ktype == Type.BOOLEAN);
    }

    public boolean isVTypePrimitive()
    {
        return getVType() != Type.GENERIC;
    }

    public boolean isVTypeNumeric()
    {
        return (getVType() != Type.GENERIC && getVType() != Type.BOOLEAN);
    }

    public boolean isVTypeBoolean()
    {
        return (vtype == Type.BOOLEAN);
    }

    public boolean isKTypeGeneric()
    {
        return ktype == Type.GENERIC;
    }

    public boolean isVTypeGeneric()
    {
        return getVType() == Type.GENERIC;
    }


    public boolean isAllGeneric()
    {
        return isKTypeGeneric() && isVTypeGeneric();
    }

    public boolean isAnyPrimitive()
    {
        return isKTypePrimitive() || isVTypePrimitive();
    }

    public boolean isAnyGeneric()
    {
        return isKTypeGeneric() || (hasVType() && isVTypeGeneric());
    }

    public boolean hasVType()
    {
        return vtype != null;
    }

    public Type getKType()
    {
        return ktype;
    }

    public Type getVType()
    {
        if (vtype == null) throw new RuntimeException("VType is null.");
        return vtype;
    }

    public void doNotGenerateKType(final String notGeneratingType)
    {
        this.doNotGenerateKType = (Type.valueOf(notGeneratingType) == this.ktype);
    }

    public void doNotGenerateVType(final String notGeneratingType)
    {
        if (this.vtype != null)
        {
            this.doNotGenerateVType = (Type.valueOf(notGeneratingType) == this.vtype);
        }
    }

    public boolean isDoNotGenerateKType()
    {
        return this.doNotGenerateKType;
    }

    public boolean isDoNotGenerateVType()
    {
        return this.doNotGenerateVType;
    }

    /**
     * Returns the current time in ISO format.
     */
    public String getTimeNow()
    {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        return format.format(new Date());
    }

    public String getSourceFile()
    {
        return sourceFile.getName();
    }

    public String getGeneratedAnnotation()
    {
        return "@javax.annotation.Generated(date = \"" +
                getTimeNow() + "\", value = \"HPPC generated from: " +
                sourceFile.getName() + "\")";
    }
}