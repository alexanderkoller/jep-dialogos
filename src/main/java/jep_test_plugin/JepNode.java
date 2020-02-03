package jep_test_plugin;

import com.clt.diamant.*;
import com.clt.diamant.graph.Graph;
import com.clt.diamant.graph.Node;
import com.clt.diamant.gui.NodePropertiesDialog;
import com.clt.script.exp.Value;
import com.clt.script.exp.values.IntValue;
import com.clt.script.exp.values.StringValue;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import com.sun.jdi.LongValue;
import jep.Jep;
import jep.JepConfig;
import jep.JepException;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JepNode extends Node {
    public static final String SCRIPT_FILENAME = "filename";
    /** variable to store the result in */
    private static final String RESULT_VAR = "resultVar";

    private static Map<String,File> scriptFilenamesToTmpFilenames = new HashMap<>();

    private JepConfig jc = new JepConfig();


    public JepNode() {
        addEdge();
    }


    @Override
    public Node execute(WozInterface comm, InputCenter input, ExecutionLogger logger) {
        List<Slot> variables = getGraph().getVariables();

        try (Jep interp = jc.createSubInterpreter()) {
            setVariables(interp, variables);
            File scriptFile = mkTemporaryFile(getScriptFilename());
            interp.runScript(scriptFile.getAbsolutePath());
        } catch (JepException|IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return getOutEdges().get(0).getTarget();
    }

    private void setVariables(Jep jep, List<? extends AbstractVariable> variables) throws JepException {
        for( AbstractVariable variable : variables ) {
            Object value = variable.getValue();

            if( value instanceof StringValue ) {
                value = ((StringValue) value).getString();
            } else if( value instanceof IntValue ) {
                value = ((IntValue) value).getInt();
            } else if( value instanceof Value ) {
                throw new UnsupportedOperationException("Unsupported DialogOS Value type: " + value.getClass());
            }

            jep.set(variable.getName(), value);
        }
    }

    private File mkTemporaryFile(String scriptFilename) throws IOException {
        File ret = scriptFilenamesToTmpFilenames.get(scriptFilename);

        if( ret == null ) {
            ret = File.createTempFile("dialogos_python_script", ".py");
            ret.deleteOnExit();
            scriptFilenamesToTmpFilenames.put(scriptFilename, ret);

//            System.err.printf("Create new tmp file %s for script name %s ...\n", ret.getAbsolutePath(), scriptFilename);

            InputStream is = JepNode.class.getClassLoader().getResourceAsStream(scriptFilename);
            String script = slurp(new InputStreamReader(is));

            try(PrintWriter w = new PrintWriter(new FileWriter(ret))) {
                w.println(script);
            }
        }

        return ret;
    }

    private String getScriptFilename() {
        return (String) properties.get(SCRIPT_FILENAME);
    }


    @Override
    public void writeVoiceXML(XMLWriter w, IdMap uid_map) throws IOException {

    }

    @Override
    public JComponent createEditorComponent(Map<String, Object> properties) {
        JPanel p = new JPanel();

        JPanel horiz = new JPanel();
        horiz.add(new JLabel("Python script to execute"));
        horiz.add(NodePropertiesDialog.createTextField(properties, SCRIPT_FILENAME));
        p.add(horiz);

        horiz = new JPanel();
        horiz.add(new JLabel("return value to:"));
        horiz.add(NodePropertiesDialog.createComboBox(properties, RESULT_VAR, this.getGraph().getAllVariables(Graph.LOCAL)));
        p.add(horiz);
        
        return p;
    }

    @Override
    protected void writeAttributes(XMLWriter out, IdMap uid_map) {
        super.writeAttributes(out, uid_map);

        Graph.printAtt(out, SCRIPT_FILENAME, getScriptFilename());
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {
        if( SCRIPT_FILENAME.equals(name)) {
            setProperty(SCRIPT_FILENAME, value);
        } else {
            super.readAttribute(r, name, value, uid_map);
        }
    }


    private static String slurp(Reader r) {
        try {
            char[] arr = new char[8 * 1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead;
            while ((numCharsRead = r.read(arr, 0, arr.length)) != -1) {
                buffer.append(arr, 0, numCharsRead);
            }
            r.close();
            String targetString = buffer.toString();
            return targetString;
        } catch(IOException e) {
            return null;
        }
    }
}
