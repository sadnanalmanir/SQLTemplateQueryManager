/* query which runs on VP engine, but not from java. It seems that using pipe(|) is now allowed

 * ./vkernel -I /tmp/PSOATransRun -t 10 -m 300000 --elim_def 0 --selection 3 
--config /tmp/PSOATransRun/extensional_predicates.xml --config /tmp/PSOATransRun/answer_predicates.xml 
--max_number_of_answers 1000 --silent on --show_answers_as_xml on /tmp/PSOATransRun/request.tptp 
| java -enableassertions -cp /tmp/PSOATransRun/SAILQueryManager.jar:/tmp/PSOATransRun/JSAP-2.0a.jar:
/tmp/PSOATransRun/LogicWarehouseJE.jar:/tmp/PSOATransRun/CushionJE.jar:/tmp/PSOATransRun/derby.jar:
/tmp/PSOATransRun/mysql-connector-java-5.0.8-bin.jar SAILQueryManager --output-xml on 
--ecnomical-joins off --table-query-patterns /tmp/PSOATransRun/predicate_views.xml
 */
package ca.unbsj.cbakerlab.sqltemplate.cmdlineutils;


/**
 *
 * @author sadnana
 */

import org.apache.commons.exec.*;

import java.io.IOException;
import java.io.OutputStream;

public class ShellUtil {
    public static DefaultExecuteResultHandler resultHandler() {
        return new DefaultExecuteResultHandler();
    }

    private static Executor executor(long timeout) {
        Executor e = new DefaultExecutor();
        e.setWatchdog(new ExecuteWatchdog(timeout));
        return e;
    }

    public static int execute(CommandLine cl, OutputStream out) {
        return execute(cl, out, ExecuteWatchdog.INFINITE_TIMEOUT);
    }

    public static int execute(CommandLine cl, OutputStream out, long timeout) {
        Executor exec = executor(timeout);
        PumpStreamHandler psh = new PumpStreamHandler(out, out);
        exec.setStreamHandler(psh);


        try {
            System.out.println("command# " + cl.toString());

            return exec.execute(cl);
        } catch (ExecuteException e) {
            throw new AutoGenerateSADIException("Execution Error", e);
        } catch (IOException e) {
            throw IOUtil.runtimeIOException(e);
        }
    }

    public static String concat(String... strs) {
        switch (strs.length) {
            case 0:
                return "";
            case 1:
                return strs[0];
            case 2:
                return strs[0].concat(strs[1]);
            default:
                StringBuilder sb = new StringBuilder();
                for (String s : strs) {
                    sb.append(s);
                }
                return sb.toString();
        }
    }

    public static String echo(String s) {
        return "echo ".concat(s);
    }

    public static String padl(String s) {
        return " ".concat(s);
    }

    public static String quote(String s) {
        return concat("\"", s, "\"");
    }

    public static String parenthesize(String s) {
        return concat("(", s, ")");
    }

    public static String rredirect(String s) {
        return "<".concat(s);
    }

    public static String subshell(String s) {
        return concat("(", s, ")");
    }
}
