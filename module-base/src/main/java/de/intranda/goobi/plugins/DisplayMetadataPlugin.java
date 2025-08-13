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
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.FilesystemHelper;
import de.sub.goobi.helper.NIOFileUtils;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.SwapException;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;

@PluginImplementation
@Log4j2
public class DisplayMetadataPlugin implements IStepPluginVersion2 {

    private static final long serialVersionUID = 6547730934739285127L;

    @Getter
    private String title = "intranda_step_displayMetadata";
    @Getter
    private Step step;

    private Process process;

    private String returnPath;

    @Getter
    private List<String> metadataTypes = new ArrayList<>();

    @Setter
    @Getter
    private Map<String, String> metadataMap = new HashMap<>();

    @Getter
    private transient List<FolderConfiguration> configuredFolder = new ArrayList<>();

    @Getter
    private transient List<MetadataConfiguration> metadata = new ArrayList<>();

    @Getter
    private transient boolean displayMetadata;

    @Getter
    private transient boolean displayContent;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        process = step.getProzess();
        VariableReplacer replacer = new VariableReplacer(null, null, process, step);

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);

        displayContent = myconfig.getBoolean("/folderlist/@displayContent", false);
        if (displayContent) {
            for (HierarchicalConfiguration hc : myconfig.configurationsAt("/folderlist/folder")) {
                // get foldername, get filter

                String foldername = hc.getString("@label");
                Path folderPath = Paths.get(replacer.replace(hc.getString("@path")));
                String filter = hc.getString("@filter", "");

                FolderConfiguration fc = new FolderConfiguration(foldername, folderPath, filter);
                configuredFolder.add(fc);
                // find all files in folder, use filter

                if (StorageProvider.getInstance().isDirectory(folderPath)) {
                    List<Path> content = StorageProvider.getInstance().listFiles(folderPath.toString());
                    for (Path file : content) {
                        if (StringUtils.isBlank(filter) || file.getFileName().toString().matches(filter)) {
                            fc.addFile(file);
                        }
                    }
                }
            }
        }
        displayMetadata = myconfig.getBoolean("/metadatalist/@displayMetadata", true);
        if (displayMetadata) {
            for (HierarchicalConfiguration hc : myconfig.configurationsAt("/metadatalist/metadata")) {
                String metadataName = hc.getString(".");
                String prefix = hc.getString("./@prefix", "");
                String suffix = hc.getString("./@suffix", "");
                String key = hc.getString("./@key", metadataName);
                metadata.add(new MetadataConfiguration(metadataName, prefix, suffix, key));
                metadataTypes.add(key);
            }
        }
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
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.PART;
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String getPagePath() {
        return "/uii/step_example_full.xhtml";
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {
        if (displayMetadata) {
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
                log.error(e);
                return PluginReturnValue.ERROR;
            }
            log.info("DisplayContent step plugin executed");
        }

        if (!displayContent && !displayMetadata) {
            log.error("Nothing configured for display");
            return PluginReturnValue.ERROR;
        }

        return PluginReturnValue.FINISH;
    }

    /**
     * get the size of a file that is listed inside of the configured directory
     * 
     * @param file name of the file to get the size of
     * @return size as String in MB, GB or TB
     */
    public String getFileSize(String file) {
        String result = "-";
        try {
            long fileSize = StorageProvider.getInstance().getFileSize(Paths.get(file));
            result = FilesystemHelper.getFileSizeShort(fileSize);
        } catch (IOException e) {
            log.error(e);
        }
        return result;
    }

    public void downloadFile(String file) {
        Path f = Paths.get(file);
        try (InputStream in = StorageProvider.getInstance().newInputStream(f)) {
            FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
            ExternalContext ec = facesContext.getExternalContext();
            ec.responseReset();
            ec.setResponseContentType(NIOFileUtils.getMimeTypeFromFile(f));
            ec.setResponseHeader("Content-Disposition", "attachment; filename=" + f.getFileName().toString());
            ec.setResponseContentLength((int) StorageProvider.getInstance().getFileSize(f));

            IOUtils.copy(in, ec.getResponseOutputStream());

            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        }
    }
}
