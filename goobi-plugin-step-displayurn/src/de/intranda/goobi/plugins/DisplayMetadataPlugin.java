package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class DisplayMetadataPlugin implements IStepPlugin, IPlugin {

    private static final String PLUGIN_NAME = "DisplayMetadataPlugin";

    private static final Logger logger = Logger.getLogger(DisplayMetadataPlugin.class);

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    private Step step;
    private String returnPath;
    private Process process;
    private List<String> metadataTypes = new ArrayList<String>();
    private List<MetadataConfiguration> metadata = new ArrayList<MetadataConfiguration>();


    private Map<String, String> metadataMap = new HashMap<>();

    @Override
    public String getTitle() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return PLUGIN_NAME;
    }

    @Override
    public void initialize(Step step, String returnPath) {
        int numberOfMetadata = ConfigPlugins.getPluginConfig(this).getList("metadatalist.metadata").size();
        for (int i = 0;  i < numberOfMetadata; i++) {
            String metadataName = ConfigPlugins.getPluginConfig(this).getString("metadatalist.metadata(" + i + ")");
            String prefix = ConfigPlugins.getPluginConfig(this).getString("metadatalist.metadata(" + i + ")[@prefix]", "");
            String suffix = ConfigPlugins.getPluginConfig(this).getString("metadatalist.metadata(" + i + ")[@suffix]", "");
            metadata.add(new MetadataConfiguration(metadataName, prefix, suffix));
            metadataTypes.add(metadataName);
        }
//        metadataTypes = ConfigPlugins.getPluginConfig(this).getList("metadatalist.metadata");

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
                    metadataMap.put(mdt.getName(), currentMetadata.getPrefix() + values + currentMetadata.getSuffix());
                }
            }

        } catch (ReadException | PreferencesException | SwapException | DAOException | WriteException | IOException | InterruptedException e) {
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
        return null;
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
