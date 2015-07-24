package ca.unbsj.cbakerlab.sqltemplate;

import ca.unbsj.cbakerlab.sqltemplate.querymanager.SAILQueryManager;
import ca.unbsj.cbakerlab.sqltemplate.schematicanswers.SchematicAnswersGenerator;

/**
 * Created by sadnana on 03/07/15.
 */
public class TemplateSQLQueryGenerator {
    public static void main(String[] args) {

        String tptpQuery =

                "include('tohdw_haio_semantic_map.fof.tptp').\n"
                        + "\n"
                        + "% HAI.owl ontology translated into tptp formulas without illegal tptp formula symbols \n"
                        + "include('HAI_no_Illegal_Symbols.ontology.cnf.tptp').\n"
                        + "\n" + "% semantic query" + "\n" +

                        "input_clause(query4patOacisPID,conjecture,\n" + "  [\n"
                        + "   --p_Patient(X),\n"
                        + "   --p_has_patient_identification_number(X,N),\n"
                        + "    ++answer(N)\n" + "  ]).";

        SchematicAnswersGenerator schematicAnswersGenerator = new SchematicAnswersGenerator(tptpQuery);
        String schematicAnswers = schematicAnswersGenerator.generateSchematicAnswers();

        System.out.println(schematicAnswers);
        SAILQueryManager sailQueryManager = new SAILQueryManager();
        try {
            // the args are empty here, the callee method has the assigned arguments
            sailQueryManager.generateSQLTemplate(args, schematicAnswers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
