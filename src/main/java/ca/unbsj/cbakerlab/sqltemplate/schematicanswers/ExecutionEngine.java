/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package ca.unbsj.cbakerlab.sqltemplate.schematicanswers;

import org.apache.commons.exec.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static ca.unbsj.cbakerlab.sqltemplate.cmdlineutils.IOUtil.*;
import static ca.unbsj.cbakerlab.sqltemplate.cmdlineutils.ShellUtil.execute;

/**
 * @author sadnana
 */
public class ExecutionEngine {
    protected final String[] m_resourcePaths;
    protected final String m_binPath;
    protected final CommandLineArgsBuilder m_commBuilder;
    protected final long m_exeTimeout;
    protected final ResultHandler m_resultHandler;
    //protected File tmpFilePath;

    public ExecutionEngine(CommandLineArgsBuilder cBuilder, ResultHandler resultHandler, long exeTimeout, String binPath, String... resources) {
        ClassLoader loader = this.getClass().getClassLoader();
        if (loader == null)
            System.out.println("class not loaded");
        File binFile = extractFromResource(loader, binPath);
        binFile.setExecutable(true, true);
        m_binPath = binFile.getPath();
        m_resourcePaths = new String[resources.length];

        for (int i = 0; i < resources.length; i++) {
            m_resourcePaths[i] = extractFromResource(loader, resources[i]).getPath();
            System.out.println("resource path " + (i) + " " + m_resourcePaths[i].toString());
        }

        m_commBuilder = cBuilder;
        m_exeTimeout = exeTimeout;
        m_resultHandler = resultHandler;
    }

    public String run(String input) {
        File req = tmpFile("request-", ".tptp");

        writeStringToFile(req, input);

        CommandLine cl = new CommandLine(m_binPath);
        cl.addArguments(m_commBuilder.buildCommandLine(m_resourcePaths, req.getPath()));
        //System.out.println("--"+ cl.toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        execute(cl, out, m_exeTimeout);
        req.delete();

        File schematic_answer = new File(System.getProperty("java.io.tmpdir").concat("/SQLTemplateDir") + "/schematic_answers.xml");
        writeStringToFile(schematic_answer, out.toString());

        return m_resultHandler.parse(out.toString());
    }


    public static ExecutionEngine VampirePrime_Schemantic_Answers = new ExecutionEngine(new CommandLineArgsBuilder() {

        @Override
        public String[] buildCommandLine(String[] resourcePaths,
                                         String inputFilePath) {

            return new String[]{"-I", System.getProperty("java.io.tmpdir").concat("/SQLTemplateDir"), "-t", "10", "-m",
                    "300000", "--elim_def", "0", "--selection", "3",
                    "--config", resourcePaths[0], "--config", resourcePaths[1],
                    "--max_number_of_answers", "1000",
                    "--silent", "on",
                    "--show_answers_as_xml", "on",
                    inputFilePath
            };


        }
    }, ResultHandler.IdentityHandler, 5 * 60 * 1000, "vkernel", "extensional_predicates.xml",
            "answer_predicates.xml",
            "tohdw_haio_semantic_map.fof.tptp",
            "HAI_no_Illegal_Symbols.ontology.cnf.tptp",
            "predicate_views.xml",
            "PredicateViews.xsd");


}