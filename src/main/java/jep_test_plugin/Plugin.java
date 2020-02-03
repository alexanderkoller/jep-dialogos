package jep_test_plugin;

import com.clt.dialogos.plugin.PluginRuntime;
import com.clt.dialogos.plugin.PluginSettings;
import com.clt.diamant.IdMap;
import com.clt.diamant.graph.Node;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Created by timo on 09.10.17.
 */
public class Plugin implements com.clt.dialogos.plugin.Plugin {
    @Override
    public void initialize() {
        Node.registerNodeTypes(com.clt.speech.Resources.getResources().createLocalizedString("IONode"),
                Arrays.asList(JepNode.class));
    }

    @Override
    public String getId() {
        return "jep_test_plugin";
    }

    @Override
    public String getName() {
        return "JEP Test Plugin";
    }

    @Override
    public Icon getIcon() {
        return UIManager.getIcon("FileView.computerIcon");
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public PluginSettings createDefaultSettings() {
        return new PluginSettings() {
            @Override
            public void writeAttributes(XMLWriter out, IdMap uidMap) {
                // nothing to be written
            }

            @Override
            protected void readAttribute(XMLReader r, String name, String value, IdMap uidMap) {
                // nothing to be read
            }

            @Override
            public JComponent createEditor() {
                return new JLabel();
            }

            @Override
            protected PluginRuntime createRuntime(Component parent) {
                return () -> {
                    // no runtime
                };
            }
        };
    }
}
