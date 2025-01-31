package de.intranda.goobi.plugins;

/**
 * This file is part of a plugin for the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.exceptions.SwapException;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;

@PluginImplementation
public class DisplayMetadataPlugin implements IStepPlugin, IPlugin {

    private static final long serialVersionUID = 8219186039584057484L;

    private static final String PLUGIN_NAME = "intranda_step_displayMetadata";

    private static final Logger logger = Logger.getLogger(DisplayMetadataPlugin.class);

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    private Step step;
    private String returnPath;
    private Process process;
    private List<String> metadataTypes = new ArrayList<>();
    private transient List<MetadataConfiguration> metadata = new ArrayList<>();

    private Map<String, String> metadataMap = new HashMap<>();

    @Override
    public String getTitle() {
        return PLUGIN_NAME;
    }

    @Override
    public void initialize(Step step, String returnPath) {
        String projectName = step.getProzess().getProjekt().getTitel();

        XMLConfiguration xmlConfig = ConfigPlugins.getPluginConfig(PLUGIN_NAME);
        xmlConfig.setExpressionEngine(new XPathExpressionEngine());
        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
        SubnodeConfiguration myconfig = null;

        // order of configuration is:
        // 1.) project name and step name matches
        // 2.) step name matches and project is *
        // 3.) project name matches and step name is *
        // 4.) project name and step name are *
        try {
            myconfig = xmlConfig.configurationAt("//config[./project = '" + projectName + "'][./step = '" + step.getTitel() + "']");
        } catch (IllegalArgumentException e) {
            try {
                myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '" + step.getTitel() + "']");
            } catch (IllegalArgumentException e1) {
                try {
                    myconfig = xmlConfig.configurationAt("//config[./project = '" + projectName + "'][./step = '*']");
                } catch (IllegalArgumentException e2) {
                    myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '*']");
                }
            }
        }

        List<HierarchicalConfiguration> metadataConfiguration = myconfig.configurationsAt("/metadatalist/metadata");
        for (HierarchicalConfiguration sub : metadataConfiguration) {
            String metadataName = sub.getString(".");
            String prefix = sub.getString("./@prefix", "");
            String suffix = sub.getString("./@suffix", "");
            String key = sub.getString("./@key", metadataName);
            metadata.add(new MetadataConfiguration(metadataName, prefix, suffix, key));
            metadataTypes.add(key);
        }

        this.step = step;
        this.returnPath = returnPath;
        process = step.getProzess();
        execute();
    }

    @Override
    public boolean execute() {
        try {
            Fileformat ff = process.readMetadataFile();
            DocStruct logical = ff.getDigitalDocument().getLogicalDocStruct();
            if (logical.getType().isAnchor()) {
                logical = logical.getAllChildren().get(0);
            }

            for (MetadataConfiguration currentMetadata : metadata) {

                MetadataType mdt = process.getRegelsatz().getPreferences().getMetadataTypeByName(currentMetadata.getMetadataName());
                String values = "";
                if (mdt != null) {
                    if (mdt.getIsPerson()) {
                        List<? extends Person> pdl = logical.getAllPersonsByType(mdt);
                        if (pdl != null && !pdl.isEmpty()) {
                            for (Person p : pdl) {
                                if (!values.isEmpty()) {
                                    values = values + "; ";
                                }
                                values = values + p.getDisplayname();

                            }
                        }

                    } else {
                        List<? extends Metadata> mdl = logical.getAllMetadataByType(mdt);
                        if (mdl != null && !mdl.isEmpty()) {
                            for (Metadata md : mdl) {
                                if (!values.isEmpty()) {
                                    values = values + "; ";
                                }
                                values = values + md.getValue();
                            }

                        }
                    }
                    metadataMap.put(currentMetadata.getKey(), currentMetadata.getPrefix() + values + currentMetadata.getSuffix());
                }
            }

        } catch (ReadException | PreferencesException | SwapException | IOException e) {
            logger.error(e);
            return false;
        }

        return true;
    }

    @Override
    public String cancel() {
        return returnPath;
    }

    @Override
    public String finish() {
        return returnPath;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public Step getStep() {
        return step;
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.PART;
    }

    @Override
    public String getPagePath() {
        return "/uii/step_example_full.xhtml";
    }

    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    public void setMetadataMap(Map<String, String> metadataMap) {
        this.metadataMap = metadataMap;
    }

    public List<String> getMetadataTypes() {
        return metadataTypes;
    }
}
