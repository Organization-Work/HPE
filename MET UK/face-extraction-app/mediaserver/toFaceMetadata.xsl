<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="text" version="4.0" omit-xml-declaration="yes" encoding="UTF-8" indent="yes"/>
    <xsl:strip-space elements="*" />
	
	<xsl:variable name="separator"><xsl:text>&#9;</xsl:text></xsl:variable>
	<xsl:variable name="delimiter"><xsl:text>&#10;</xsl:text></xsl:variable>
	
    <xsl:template match="/output">
		<xsl:apply-templates select="metadata/session"/>
        <xsl:apply-templates select="results/track/record"/>
    </xsl:template>

    <xsl:template match="record">
		<xsl:apply-templates select="FaceRecognitionAndImage"/>
		<xsl:apply-templates select="FaceRecognitionAndImage/identity"/>
		<xsl:apply-templates select="timestamp"/>
		<xsl:apply-templates select="FaceRecognitionAndImage/face/region"/>
    </xsl:template>

    <xsl:template match="session">
        <xsl:text>Source video:</xsl:text><xsl:value-of select="$separator"/><xsl:value-of select="source"/><xsl:value-of select="$delimiter"/>
    </xsl:template>

    <xsl:template match="FaceRecognitionAndImage">
        <xsl:text>Face id:</xsl:text><xsl:value-of select="$separator"/><xsl:value-of select="id"/><xsl:value-of select="$delimiter"/>
    </xsl:template>
	
    <xsl:template match="identity">
        <xsl:text>Match id:</xsl:text><xsl:value-of select="$separator"/><xsl:value-of select="identifier"/><xsl:value-of select="$delimiter"/>
        <xsl:text>Match confidence:</xsl:text><xsl:value-of select="$separator"/><xsl:value-of select="confidence"/><xsl:value-of select="$delimiter"/>
    </xsl:template>

	<xsl:template match="timestamp">
        <xsl:variable name="startTime" select="startTime/@iso8601"/>
        <xsl:variable name="endTime" select="endTime/@iso8601"/>
        <xsl:text>Start:</xsl:text><xsl:value-of select="$separator"/><xsl:value-of select="substring(translate($startTime, 'TZ', ' '), 12)"/><xsl:value-of select="$delimiter"/>
        <xsl:text>End:</xsl:text><xsl:value-of select="$separator"/><xsl:value-of select="substring(translate($endTime, 'TZ', ' '), 12)"/><xsl:value-of select="$delimiter"/>
    </xsl:template>

	<xsl:template match="region">
        <xsl:text>Width:</xsl:text><xsl:value-of select="$separator"/><xsl:value-of select="width"/><xsl:text> px</xsl:text><xsl:value-of select="$delimiter"/>
        <xsl:text>Height:</xsl:text><xsl:value-of select="$separator"/><xsl:value-of select="height"/><xsl:text> px</xsl:text><xsl:value-of select="$delimiter"/>
    </xsl:template>
	
</xsl:stylesheet>