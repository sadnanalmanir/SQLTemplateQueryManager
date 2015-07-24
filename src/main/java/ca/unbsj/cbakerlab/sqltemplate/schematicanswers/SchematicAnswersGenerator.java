package ca.unbsj.cbakerlab.sqltemplate.schematicanswers;

/**
 * Created by sadnana on 03/07/15.
 */
public class SchematicAnswersGenerator {

    String tptpQuery;

    /**
     * @param tptpQuery
     */
    public SchematicAnswersGenerator(String tptpQuery) {
        super();
        this.tptpQuery = tptpQuery;
    }

    public String generateSchematicAnswers() {

        ExecutionEngine engine = ExecutionEngine.VampirePrime_Schemantic_Answers;
        return engine.run(tptpQuery);

    }
}
