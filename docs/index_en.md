---
title: Display of metadata and folder contents in a task
identifier: intranda_step_displayMetadata
published: true
description: This is the technical documentation for the Goobi plug-in for displaying any metadata and folder contents in a workflow task.
keywords:
    - Goobi workflow
    - Plugin
    - Step Plugin
---
## Introduction
‌This documentation describes the installation, configuration and use of a plug-in to display metadata and folder contents in a workflow step. The plugin can display any metadata and folder contents in one step. The configuration of prefixes and suffixes is also possible.


## Installation 
‌To use the plugin, the two artifacts must be copied to the following locations:

```bash
/opt/digiverso/goobi/plugins/step/plugin_intranda_step_displayMetadata-base.jar
/opt/digiverso/goobi/plugins/GUI/plugin_intranda_step_displayMetadata-gui.jar
```

‌The configuration of the plugin is expected at the following path:

```bash
/opt/digiverso/goobi/config/plugin_intranda_step_displayMetadata.xml
```


## Overview and functionality
In Goobi, the plugin in the workflow must then be configured. To do this, you must select `intranda_step_displayContentAndMetadata` as the step plug-in in the step configuration.

![Configuration of the step](screen1.png)

If the step is then opened after successful configuration, all metadata and folder contents - if available in the process - are displayed:

![Integrated plugin in the user interface](screen2.png)


## Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config_plugin>
    <config>
        <project>*</project>
        <step>*</step>
        <metadatalist displayMetadata="true">
            <metadata>Author</metadata>
            <metadata>TitleDocMain</metadata>
            <metadata>_urn</metadata>
            <metadata prefix="http://svdmzgoobiweb01.klassik-stiftung.de/viewer/image/" suffix="/1/" key="url">CatalogIDDigital</metadata>
        </metadatalist>
        <folderlist displayContents="true">
            <folder label="Thesis" path="(folder.media)" filter=".*tif" />
            <folder label="Plagiatsprüfungsprotokoll" path="(folder.media)" filter=".*pdf" />
            <folder label="Beilagen" path="{folder.attachments}" />
        </folderlist>
    </config>
</config_plugin>
```

### Metadata
In `metadatalist`, several metadata can be configured for display, additionally a prefix and a suffix can be displayed. The `key` attribute is used for the translation of the labels of the metadata. Rendering of the metadatalist can be disabled by setting the attribute `displayMetadata` to `false`. When ommiting this attribute, `true` will be assumed.

### Folder contents
In `folderlist` several folders can be configured to make their contents appear in a list for download.  In order for the folder listing to be rendered the attribute `displayContents` needs to be set to `true`.
The attribute `label` sets the display label for the folder. The attribute `path` expects a folder from the images folder of the processes, that is configured in `goobi_config.properties`. E.g. a folder that is configured by the line `process.folder.images.attachments={processtitle}_attachments` in the `goobi_config.properties` should be entered here as `{folder.attachments}`.  
The attribute `filter` can hold a pattern against which the file names will be matched for filtering out the files to be displayed. Typically this would be filename extensions for filtering by filetype.