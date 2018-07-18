<?xml version="1.0"?>
<!DOCTYPE stylesheet [
    <!ENTITY nbsp "&#160;">
]>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:common="http://exslt.org/common"
    xmlns:autn="http://schemas.autonomy.com/aci/"
    xmlns:saxon="http://saxon.sf.net/"
    extension-element-prefixes="common"
    exclude-result-prefixes="autn saxon"
        >
    <xsl:output method="html" indent="yes"/>
    
    <xsl:template match="autn:content">
        <xsl:choose>
            <xsl:when test="element()">
                <ul>
                <xsl:apply-templates mode="content"/>  
                </ul>  
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="."/>
            </xsl:otherwise>
        </xsl:choose>
        
        
    </xsl:template>
    <xsl:template match="node()">
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="element()" mode="content">
        <xsl:choose>
            <xsl:when test="element()">
                <xsl:apply-templates mode="content"/>
            </xsl:when>
            <xsl:otherwise>
                <li><b><xsl:value-of select="name(.)"/>: </b><xsl:value-of select="."/></li>
            </xsl:otherwise>
        </xsl:choose>
        
        
    </xsl:template>
     
</xsl:stylesheet>
