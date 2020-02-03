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
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A node that calls a Python script via JEP.
 * The Python script is run in a context where all local and global DialogOS
 * and Groovy variables have been made accessible as Python variables (with their
 * current values within DialogOS). You can also specify that the value of one
 * Python variable is copied into a DialogOS variable after the script ends.
 * <p>
 *
 * Limitations:
 * <ul>
 *     <li>Only variables of type int and string can be copied in either direction between DialogOS and the Python script.</li>
 *     <li>Only a single variable can be copied from Python to DialogOS. The name of this variable must be
 *     the same in Python and DialogOS. The variable must be a local DialogOS variable.</li>
 * </ul>
 *
 */
public class JepNode extends Node {
    public static final String SCRIPT_FILENAME = "filename";
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
            setVariables(interp, getGraph().getAllVariables(Graph.GLOBAL));
            setVariables(interp, getGraph().getAllVariables(Graph.LOCAL));
            setVariables(interp, getGraph().getGroovyVariables());

            File scriptFile = mkTemporaryFile(getScriptFilename());
            interp.runScript(scriptFile.getAbsolutePath());

            setVariableFromJep(interp, getResultVariable());
        } catch (JepException|IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return getOutEdges().get(0).getTarget();
    }

    private void setVariableFromJep(Jep jep, Slot dialogosVariable) throws JepException {
        if( dialogosVariable == null || dialogosVariable.getName() == null ) {
            return;
        }


        Object ret = jep.getValue(dialogosVariable.getName());

        if( ret == null ) {
            throw new RuntimeException("JEP returned null value for variable name " + dialogosVariable.getName());
        } else if( ret instanceof String ) {
            dialogosVariable.setValue(new StringValue((String) ret));
        } else if( ret instanceof Integer ) {
            dialogosVariable.setValue(new IntValue((Integer) ret));
        } else {
            throw new UnsupportedOperationException("Unsupported Python type: " + ret.getClass());
        }
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

    private Slot getResultVariable() {
        return (Slot) properties.get(RESULT_VAR);
    }


    @Override
    public void writeVoiceXML(XMLWriter w, IdMap uid_map) throws IOException {

    }

    @Override
    public JComponent createEditorComponent(Map<String, Object> properties) {
        JPanel p = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        p.setLayout(gbl);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;

        JPanel horiz = new JPanel();
        horiz.add(new JLabel("Python script to execute"));
        horiz.add(NodePropertiesDialog.createTextField(properties, SCRIPT_FILENAME));
        horiz.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.gridy = 0;
        p.add(horiz, c);

        horiz = new JPanel();
        horiz.setLayout(new FlowLayout(FlowLayout.LEADING));
        horiz.add(new JLabel("Copy this Python variable to DialogOS"));
        horiz.add(NodePropertiesDialog.createComboBox(properties, RESULT_VAR, this.getGraph().getAllVariables(Graph.LOCAL)));
        horiz.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.gridy = 1;
        p.add(horiz, c);

        c.gridy = 2;
        c.weighty = 1;
        p.add(Box.createVerticalGlue(), c);
        
        return p;
    }

    @Override
    protected void writeAttributes(XMLWriter out, IdMap uid_map) {
        super.writeAttributes(out, uid_map);

        Graph.printAtt(out, SCRIPT_FILENAME, getScriptFilename());

        if( getResultVariable() != null ) {
            Graph.printAtt(out, RESULT_VAR, getResultVariable().getName());
        }
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {
        if( SCRIPT_FILENAME.equals(name)) {
            setProperty(SCRIPT_FILENAME, value);
        } else if( RESULT_VAR.equals(name)) {
            Slot slot = findSlot(value);

            if( slot == null ) {
                throw new SAXException("Variable not defined in DialogOS: " + value);
            } else {
                setProperty(RESULT_VAR, slot);
            }
        } else {
            super.readAttribute(r, name, value, uid_map);
        }
    }

    private Slot findSlot(String name) {
        List<Slot> allLocalSlots = getGraph().getAllVariables(Graph.LOCAL);
        for( Slot x : allLocalSlots ) {
            if( name.equals(x.getName())) {
                return x;
            }
        }

        return null;
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
