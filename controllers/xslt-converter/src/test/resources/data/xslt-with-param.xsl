<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:param name="myparam">set internally</xsl:param>
    <xsl:template match="/">
        <xsl:value-of select="$myparam"/>
    </xsl:template>
</xsl:stylesheet>