package com.ifedorenko.m2e.nexusdev.internal.launch;

import java.io.File;

import org.codehaus.plexus.util.StringUtils;

class VMArguments
{
    public static String quote( File file )
    {
        return StringUtils.quoteAndEscape( file.getAbsolutePath(), '"' );
    }

    public static void append( StringBuilder sb, String str )
    {
        if ( str != null && !"".equals( str.trim() ) )
        {
            sb.append( ' ' ).append( str );
        }
    }

}
