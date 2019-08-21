package de.intranda.goobi.plugins;

public class MetadataConfiguration {

    private String metadataName;
    private String prefix;
    private String suffix;
    private String key;

    public MetadataConfiguration(String metadataName, String prefix, String suffix, String key) {
        this.metadataName = metadataName;
        this.prefix = prefix;
        this.suffix = suffix;
        this.key = key;
    }

    public String getMetadataName() {
        return metadataName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setMetadataName(String metadataName) {
        this.metadataName = metadataName;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
