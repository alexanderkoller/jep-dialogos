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
import com.clt.xml.XMLWriter;
import jep.Interpreter;
import jep.JepException;
import jep.SharedInterpreter;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.io.IOException;
import java.util.*;

import static com.clt.diamant.graph.nodes.AbstractInputNode.findMatch;

public class JepNode extends Node {
    /** actually, a query expression */
    public static final String QUERY = "queryExp";
    /** variable to store the result in */
    private static final String RESULT_VAR = "resultVar";



    public JepNode() {
        addEdge();
    }

    @Override
    public Node execute(WozInterface comm, InputCenter input, ExecutionLogger logger) {
        try (Interpreter interp = new SharedInterpreter()) {
            interp.exec("from java.lang import System");
            interp.exec("s = 'Hello World'");
            interp.exec("System.out.println(s)");
            interp.exec("print(s)");
            interp.exec("print(s[1:-1])");
        } catch (JepException e) {
            e.printStackTrace();
        }

        return getOutEdges().get(0).getTarget();
    }

    @Override
    public void writeVoiceXML(XMLWriter w, IdMap uid_map) throws IOException {

    }

    @Override
    public JComponent createEditorComponent(Map<String, Object> properties) {
        JPanel p = new JPanel();

        JPanel horiz = new JPanel();
        horiz.add(new JLabel("Python expression"));
        horiz.add(NodePropertiesDialog.createTextField(properties, QUERY));
        p.add(horiz);

        horiz = new JPanel();
        horiz.add(new JLabel("return value to:"));
        horiz.add(NodePropertiesDialog.createComboBox(properties, RESULT_VAR, this.getGraph().getAllVariables(Graph.LOCAL)));
        p.add(horiz);
        
        return p;
    }
}
