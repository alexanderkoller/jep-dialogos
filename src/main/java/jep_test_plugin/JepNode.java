package jep_test_plugin;

import com.clt.diamant.*;
import com.clt.diamant.graph.Graph;
import com.clt.diamant.graph.Node;
import com.clt.diamant.graph.nodes.AbstractInputNode;
import com.clt.diamant.graph.nodes.NodeExecutionException;
import com.clt.diamant.gui.NodePropertiesDialog;
import com.clt.script.exp.Pattern;
import com.clt.script.exp.patterns.VarPattern;
import com.clt.speech.recognition.*;
import com.clt.srgf.Grammar;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import jep.*;
import org.xml.sax.SAXException;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.io.*;
import java.util.*;

import static com.clt.diamant.graph.nodes.AbstractInputNode.findMatch;

public class JepNode extends Node {
    /** actually, a query expression */
    public static final String FILENAME = "filename";
    /** variable to store the result in */
    private static final String RESULT_VAR = "resultVar";

    private static Map<String,File> scriptFilenamesToTmpFilenames = new HashMap<>();

    private JepConfig jc = new JepConfig();


    public JepNode() {
        addEdge();
    }


    @Override
    public Node execute(WozInterface comm, InputCenter input, ExecutionLogger logger) {
//        InputStream is = JepNode.class.getClassLoader().getResourceAsStream(getFilename());
//        String script = slurp(new InputStreamReader(is));
//
//        System.err.println(script);


        try (Jep interp = jc.createSubInterpreter()) {
            File scriptFile = mkTemporaryFile(getFilename());
            interp.runScript(scriptFile.getAbsolutePath());
//
//
//            interp.exec("from java.lang import System");
//            interp.exec("s = 'Hello World'");
//            interp.exec("System.out.println(s)");
//            interp.exec("print(s)");
//            interp.exec("print(s[1:-1])");
        } catch (JepException|IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return getOutEdges().get(0).getTarget();
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

    private String getFilename() {
        return (String) properties.get(FILENAME);
    }


    @Override
    public void writeVoiceXML(XMLWriter w, IdMap uid_map) throws IOException {

    }

    @Override
    public JComponent createEditorComponent(Map<String, Object> properties) {
        JPanel p = new JPanel();

        JPanel horiz = new JPanel();
        horiz.add(new JLabel("Python script to execute"));
        horiz.add(NodePropertiesDialog.createTextField(properties, FILENAME));
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

        Graph.printAtt(out, FILENAME, getFilename());
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {
        if( FILENAME.equals(name)) {
            setProperty(FILENAME, value);
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
