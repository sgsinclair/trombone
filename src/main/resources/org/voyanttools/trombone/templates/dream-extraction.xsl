<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="normalised">
        <xsl:value-of select="@orig"/>
    </xsl:template>
    <xsl:template match="ref[@type='oclc']">
        <ref>
            <xsl:attribute name="target">
                <xsl:text>http://www.worldcat.org/oclc/</xsl:text>
                <xsl:value-of select="node()"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="@type"/>
            </xsl:attribute>
            <xsl:value-of select="."/>
        </ref>
        <xsl:if test="@type='oclc'"></xsl:if>
    </xsl:template>
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>