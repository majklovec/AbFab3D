/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2011
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.creator.ui;

// External Imports
import java.util.*;
import java.io.*;

// Internal Imports
import abfab3d.creator.*;

/**
 * Create a user interface for an editor using a standalone Java Application.
 *
 * @author Alan Hudson
 */
public class JavaStandaloneUICreator {
    /** The type of editors */
    private enum Editors {TEXTFIELD, COMBOBOX, FILE_DIALOG};

    private HashMap<Integer,String> indentCache;

    private GeometryKernal kernal;

    public JavaStandaloneUICreator() {
        indentCache = new HashMap<Integer,String>();
    }

    /**
     * Create a user interface for a kernal.
     *
     * @param dir The directory to place the files
     * @param genParams Parameters for generation
     * @param kernal The kernal
     * @param remove The parameters to remove
     */
    public void createInterface(String dir, Map<String,String> genParams, GeometryKernal kernal, Set<String> remove) {
System.out.println("Creating Java Interface");
        this.kernal = kernal;

        try {
            File f = new File(dir);

            if (!f.exists()) {
                System.out.println("Directory does not exist");
                return;
            }

            // TODO: Create path if needed
            FileOutputStream fos = new FileOutputStream(f.toString() + "/" + "Editor.java");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            PrintStream ps = new PrintStream(bos);

            Map<String,Parameter> params = kernal.getParams();

            ps.println("package imageeditor.ui;");  // TODO: Need to add package name
            ps.println("");
            ps.println("import java.util.*;");
            ps.println("import javax.swing.*;");
            ps.println("import java.awt.GridLayout;");
            ps.println("import java.awt.event.*;");
            ps.println("import java.io.*;");
            ps.println("import abfab3d.creator.util.ParameterUtil;");
            ps.println("");
            ps.println("public class Editor extends JFrame implements ActionListener {");

            addGlobalVars(ps, params, remove);

            ps.println("    public static void main(String[] args) {");
            ps.println("        Editor editor = new Editor();");
            ps.println("        editor.launch();");
            ps.println("    }");
            ps.println();
            ps.println("    public void launch() {");
            ps.println("        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);");
            ps.println("        setupUI();");
            ps.println("        pack();");
            ps.println("        setVisible(true);");
            ps.println("    }");
            ps.println("    public void setupUI() {");


            ps.println(indent(8) + "GridLayout layout = new GridLayout(" + (params.size() - remove.size() + 1) + ",3);");
            ps.println(indent(8) + "setLayout(layout);");
            ps.println();

            Iterator<Parameter> itr = params.values().iterator();

            while(itr.hasNext()) {
                Parameter p = itr.next();
                if (remove.contains(p.getName())) {
                    continue;
                }

                addParameterUI(ps, p);
            }

            addGlobalButtons(ps);

            ps.println("    }");

            ps.println();
            addActions(ps,params,remove);

            ps.println("}");

            ps.flush();
            fos.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        System.out.println("Done creating editor");
    }

    /**
     * Add a user interface element for an item.
     *
     * @param ps The stream to print too
     * @param p The parameter
     */
    private void addParameterUI(PrintStream ps, Parameter p) {
System.out.println("Adding param: " + p.getName());
        ps.println(indent(8) + "JLabel " + p.getName() + "Label = new JLabel(\"" + p.getName() + "\");");
        ps.println(indent(8) + "getContentPane().add(" + p.getName() + "Label);");

        switch(getEditor(p)) {
            case COMBOBOX:
                ps.println(indent(8) + "String[] " + p.getName() + "Enums = new String[] {");
                String[] vals = p.getEnumValues();

                ps.print(indent(12));
                for(int i=0; i < vals.length; i++) {
                    ps.print("\"" + vals[i] + "\"");
                    if (i < vals.length - 1) {
                        ps.print(",");
                    }
                }
                ps.println("};");
                ps.println(indent(8) + p.getName() + "Editor = new JComboBox(" + p.getName() + "Enums);");
                break;
            case FILE_DIALOG:
                ps.println(indent(8) + p.getName() + "Dialog = new JFileChooser(new File(\".\"));");
                ps.println(indent(8) + p.getName() + "Button = new JButton(\"Browse\");");
                ps.println(indent(8) + p.getName() + "Button.addActionListener(this);");
            default:
                ps.println(indent(8) + p.getName() + "Editor = new JTextField(\"" + p.getDefaultValue() + "\");");
        }
        ps.println(indent(8) + "getContentPane().add(" + p.getName() + "Editor);");

        // Determine third column content
        switch(getEditor(p)) {
            case FILE_DIALOG:
                ps.println(indent(8) + "getContentPane().add(" + p.getName() + "Button);");
                break;
            default:
                ps.println(indent(8) + "getContentPane().add(new JLabel(\"\"));");
        }

        ps.println();

    }

    /**
     * Add user interface elements for Global Buttons
     *
     * @param ps The stream to print too
     * @param p The parameter
     */
    private void addGlobalButtons(PrintStream ps) {
        ps.println(indent(8) + "submitButton = new JButton(\"Submit\");");
        ps.println(indent(8) + "getContentPane().add(submitButton);");
        ps.println(indent(8) + "submitButton.addActionListener(this);");

        ps.println(indent(8) + "getContentPane().add(new JLabel(\"\"));");

        ps.println(indent(8) + "uploadButton = new JButton(\"Upload\");");
        ps.println(indent(8) + "getContentPane().add(uploadButton);");
        ps.println(indent(8) + "uploadButton.addActionListener(this);");
    }

    private void addActions(PrintStream ps, Map<String,Parameter> params, Set<String> remove) {

        // submit button
        ps.println(indent(4) + "public void actionPerformed(ActionEvent e) {");
        ps.println(indent(8) + "if (e.getSource() == submitButton) {");
        ps.println(indent(12) + "// Get all params to global string vars");

        Iterator<Parameter> itr = params.values().iterator();

        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            switch(getEditor(p)) {
                case TEXTFIELD:
                    ps.println(indent(12) + p.getName() + " = ((JTextField)" + p.getName() + "Editor).getText();");
                    break;
                case FILE_DIALOG:
                    ps.println(indent(12) + p.getName() + " = ((JTextField)" + p.getName() + "Editor).getText();");
                    break;
                case COMBOBOX:
                    ps.println(indent(12) + p.getName() + " = (String) ((JComboBox)" + p.getName() + "Editor).getSelectedItem();");
                    break;
            }

        }

        ps.println();

        // Create Kernal
        String class_name = kernal.getClass().getName();
        ps.println(indent(12) + class_name + " kernal = new " + class_name + "();");

        // Put params into a map

        ps.println(indent(12) + "HashMap<String,String> params = new HashMap<String,String>();");
        itr = params.values().iterator();

        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            ps.println(indent(12) + "params.put(\"" + p.getName() + "\", " + p.getName() + ");");
        }

        ps.println(indent(12) + "Map<String,Object> parsed_params = ParameterUtil.parseParams(kernal.getParams(), params);");

        // Generate Geometry
        ps.println(indent(12) + "try {");
        ps.println(indent(16) + "FileOutputStream fos = new FileOutputStream(\"out.x3db\");");
        ps.println(indent(16) + "BufferedOutputStream bos = new BufferedOutputStream(fos);");
        ps.println(indent(16) + "kernal.generate(parsed_params, bos);");
        ps.println(indent(16) + "fos.close();");
        ps.println(indent(12) + "} catch(IOException ioe) { ioe.printStackTrace(); }");
        ps.println(indent(12) + "System.out.println(\"Model Done\");");

        ps.println(indent(8) + "} else if (e.getSource() == uploadButton) {");
        ps.println(indent(12) + "System.out.println(\"Uploading Model\");");
        //ps.println(indent(8) + "}");


        itr = params.values().iterator();
        boolean more_elses = false;

        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            if (getEditor(p) == Editors.FILE_DIALOG) {
                ps.println(indent(8) + "} else if (e.getSource() == " + p.getName() + "Button) {");

                ps.println(indent(12) + "int returnVal = " + p.getName() + "Dialog" + ".showOpenDialog(this);");

                ps.println(indent(12) + "if (returnVal == JFileChooser.APPROVE_OPTION) {");
                ps.println(indent(16) + "File file = " + p.getName() + "Dialog" + ".getSelectedFile();");
                ps.println(indent(16) + " ((JTextField)" + p.getName() + "Editor).setText(file.toString());");
                ps.println(indent(12) + "}");
                more_elses = true;
            }
        }

        if (more_elses) {
            ps.println(indent(8) + "}");
        }

        ps.println(indent(4) + "}");
    }

    /**
     * Add global variables.
     */
    private void addGlobalVars(PrintStream ps, Map<String,Parameter> params, Set<String> remove) {
            Iterator<Parameter> itr = params.values().iterator();

        ps.println("JButton submitButton;");
        ps.println("JButton uploadButton;");

        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            ps.println(indent(4) + "/** " + p.getDesc() + " Field */");
            ps.println(indent(4) + "protected String " + p.getName() + ";");
            ps.println(indent(4) + "/** " + p.getDesc() + " Editor */");
            ps.println(indent(4) + "protected JComponent " + p.getName() + "Editor;");
            ps.println();

            switch(getEditor(p)) {
                case FILE_DIALOG:
                    ps.println(indent(4) + "protected JButton " + p.getName() + "Button;");
                    ps.println(indent(4) + "protected JFileChooser " + p.getName() + "Dialog;");
                    break;
            }
        }

        ps.println();
    }

    private String indent(int spaces) {
        Integer key = new Integer(spaces);

        String ret_val = indentCache.get(key);

        if (ret_val == null) {
            ret_val = "";
            for(int i=0; i < spaces; i++) {
                ret_val = ret_val + " ";
            }

            indentCache.put(key, ret_val);
        }

        return ret_val;
    }

    /**
     * Get the editor type to use.
     */
    private Editors getEditor(Parameter p) {
        if (p.getEditorType() == Parameter.EditorType.FILE_DIALOG) {
            return Editors.FILE_DIALOG;
        }

        if (p.getDataType() == Parameter.DataType.ENUM) {
            return Editors.COMBOBOX;
        }

        return Editors.TEXTFIELD;
    }
}