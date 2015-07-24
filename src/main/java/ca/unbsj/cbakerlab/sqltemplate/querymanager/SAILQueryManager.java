package ca.unbsj.cbakerlab.sqltemplate.querymanager;

import java.util.*; 

import java.io.*; 

import javax.xml.parsers.*; 

import org.w3c.dom.*; 

import org.xml.sax.*;

import com.martiansoftware.jsap.*;

import java.sql.*;

import cushion_je.*;

import logic_warehouse_je.*;

import ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.*;

import ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views.*;

/** Shell for the query manager: reads generic answers in the XML format
 *  from System.in and outputs the results (SQL queries with query variable
 *  mappings) in System.out.
 */
public class SAILQueryManager {


    /** Reads generic answers in the XML format
     *  from System.in and outputs the results (SQL queries with query 
     *  variable mappings) in System.out.
     */
    public void generateSQLTemplate(String[] args, String schematicAnswers) throws Exception {


	
	//      Describe options:

    // To avoid setting arguments i.e. args manually from Run -> Run Configurations
    // we set the args "--ecnomical-joins off --table-query-patterns predicate_views.xml"
    args = new String[]{"--output-xml", "off", "--ecnomical-joins", "off", "--table-query-patterns", System.getProperty("java.io.tmpdir").concat("/SQLTemplateDir")+"/"+"predicate_views.xml"};
    //args[0] = "--ecnomical-joins";
    //args[1] = "off";
    //args[2] = "--table-query-patterns";
    //args[3] = "predicate_views.xml";
    	
	JSAP jsap = new JSAP();
	

	FlaggedOption verboseOpt = 
	    new FlaggedOption("verbose")
	    .setLongFlag("verbose")    
	    .setStringParser(JSAP.STRING_PARSER)
	    .setDefault("off")
	    .setRequired(false); 
	    
	jsap.registerParameter(verboseOpt);


	FlaggedOption outputXMLOpt = 
	    new FlaggedOption("output-xml")
	    .setLongFlag("output-xml")    
	    .setStringParser(JSAP.STRING_PARSER)
	    .setDefault("on")
	    .setRequired(false); 
	    
	jsap.registerParameter(outputXMLOpt);


	FlaggedOption outputResultsOpt = 
	    new FlaggedOption("output-results")
	    .setLongFlag("output-results")    
	    .setStringParser(JSAP.STRING_PARSER)
	    .setDefault("on")
	    .setRequired(false); 
	    
	jsap.registerParameter(outputResultsOpt);


	// Switches the economical join optimisation,
	// i.e., the use of SQL EXISTS for reducing redundancy
	// in answers.
	FlaggedOption economicalJoinOpt = 
	    new FlaggedOption("economical-joins")
	    .setLongFlag("economical-joins")    
	    .setStringParser(JSAP.STRING_PARSER)
	    .setDefault("on")
	    .setRequired(false); 
	    
	jsap.registerParameter(economicalJoinOpt);


	// A file containing a mapping from predicates 
	// having views in the DB, to their shorthands
	// used in generic answers.
	FlaggedOption predicateShorthandsOpt = 
	    new FlaggedOption("predicate-shorthands") 
	    .setLongFlag("predicate-shorthands")      
	    .setStringParser(JSAP.STRING_PARSER)
	    .setRequired(false); 


	jsap.registerParameter(predicateShorthandsOpt);


	// A file containing predicate views, i.e., 
	// a mapping from DB abstraction predicates
	// to DB table query patterns, as an XML document.
	FlaggedOption tableQueryPatternsOpt = 
	    new FlaggedOption("table-query-patterns") 
	    .setLongFlag("table-query-patterns") 
	    .setStringParser(JSAP.STRING_PARSER)
	    .setRequired(false); 


	jsap.registerParameter(tableQueryPatternsOpt);
	

	// Type of DBMS to be used ("derby" or "mysql"):
	FlaggedOption dbmsOpt = 
	    new FlaggedOption("dbms") 
	    .setLongFlag("dbms")                                            
	    .setStringParser(JSAP.STRING_PARSER)
	    .setDefault("derby")
	    .setRequired(false); 


	jsap.registerParameter(dbmsOpt);



	// User name to be used for DB connections.
	FlaggedOption userNameOpt = 
	    new FlaggedOption("user") 
	    .setLongFlag("user")                                            
	    .setStringParser(JSAP.STRING_PARSER)
	    .setRequired(false); 


	jsap.registerParameter(userNameOpt);
	

	// User password to be used for DB connections.
	FlaggedOption passwordOpt = 
	    new FlaggedOption("password") 
	    .setLongFlag("password")                                            
	    .setStringParser(JSAP.STRING_PARSER)
	    .setRequired(false); 


	jsap.registerParameter(passwordOpt);
	

	// Database server (is MySQL is used). 
	// Default: "//localhost:3306".
	FlaggedOption serverOpt = 
	    new FlaggedOption("server") 
	    .setLongFlag("server")                                            
	    .setStringParser(JSAP.STRING_PARSER)
	    .setRequired(false); 


	jsap.registerParameter(serverOpt);
	

	// Path to a database to be queried.
	// If it is "off", then no DB will be queried.
	FlaggedOption databaseNameOpt = 
	    new FlaggedOption("database-name") 
	    .setLongFlag("database-name")                                            
	    .setStringParser(JSAP.STRING_PARSER)
	    .setRequired(false); 


	jsap.registerParameter(databaseNameOpt);
	

	
	//      Read the option values:

	
	JSAPResult config = jsap.parse(args);   

	String verbose = config.getString("verbose");
	if (!verbose.equalsIgnoreCase("off") && 
	    !verbose.equalsIgnoreCase("on"))
	    throw new Exception("Parameter --verbose has bad value.");
	

	String outputXML = config.getString("output-xml");
	if (!outputXML.equalsIgnoreCase("off") && 
	    !outputXML.equalsIgnoreCase("on"))
	    throw new Exception("Parameter --output-xml has bad value.");
	
	String outputResults = config.getString("output-results");
	if (!outputResults.equalsIgnoreCase("off") && 
	    !outputResults.equalsIgnoreCase("on"))
	    throw new Exception("Parameter --output-results has bad value.");
	
	String economicalJoins = config.getString("economical-joins");
	if (!economicalJoins.equalsIgnoreCase("off") && 
	    !economicalJoins.equalsIgnoreCase("on"))
	    throw new Exception("Parameter --economical-joins has bad value.");
	
	String[] predicateShorthandFiles = 
	    config.getStringArray("predicate-shorthands");

	String[] tableQueryPatternFiles = 
	    config.getStringArray("table-query-patterns");




	_dbms = config.getString("dbms");
	if (!_dbms.equalsIgnoreCase("derby") && 
	    !_dbms.equalsIgnoreCase("mysql"))
	    throw new Exception("Parameter --dbms has bad value: should be 'derby' or 'mysql'.");
	

	String[] userNames = 
	    config.getStringArray("user");
	
	if (userNames.length > 1)
	    throw 
		new Exception("Parameter --user has more than one value.");
	
	String userName = null;
	if (userNames.length == 1)
	    userName = userNames[0];
		    



	String[] passwords = 
	    config.getStringArray("password");
	
	if (passwords.length > 1)
	    throw 
		new Exception("Parameter --password has more than one value.");
	
	String password = null;
	if (passwords.length == 1)
	    password = passwords[0];
		    


	if (userName != null && password == null)
	    throw 
		new Exception("Password must be specified for the user '" +
			      userName + "'.");

	if (userName != null && password == null)
	    throw 
		new Exception("Password can only be specified for a specific user.");






	String[] servers = 
	    config.getStringArray("server");
	
	if (servers.length > 1)
	    throw 
		new Exception("Parameter --server has more than one value.");
	
	String server = "//localhost:3306"; // default
	if (servers.length == 1)
	    server = servers[0];
       






	String[] databaseNames = 
	    config.getStringArray("database-name");
	
	if (databaseNames.length > 1)
	    throw 
		new Exception("Parameter --database-name has more than one value.");
	
	String databaseName = null;
	if (databaseNames.length == 1)
	    {
		databaseName = databaseNames[0];
		if (databaseName.equals("off"))
		    databaseName = null;
	    };
		    

	// Parse the predicate shorthands:

	TreeMap<String,String> predicateShorthands =
	    new TreeMap<String,String>();

	for (int i = 0; i < predicateShorthandFiles.length; ++i)
	    parsePredicateShorthands(predicateShorthandFiles[i],
				     predicateShorthands);
    


	// Parse the predicate views and test predicate declarations:

	HashMap<String,LinkedList<PredicateView>> predViewMap = 
	    new HashMap<String,LinkedList<PredicateView>>();
	HashMap<String,LinkedList<TestPredicate>> testPredMap = 
	    new HashMap<String,LinkedList<TestPredicate>>();

        ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views.parser.Parser predViewParser =
	    new ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views.parser.Parser();

	for (int i = 0; i < tableQueryPatternFiles.length; ++i)
	    {
		File file = new File(tableQueryPatternFiles[i]);
		
		if (!file.isFile() || !file.canRead())
		    throw 
			new Exception("Cannot open predicate view file " + 
				      tableQueryPatternFiles[i]);
		
		Pair<? extends Collection<PredicateView>,? extends Collection<TestPredicate>>
		    declarations = 
		    predViewParser.parse(file);
		


		// Process predicate views:
		
		for (PredicateView view : declarations.first)
		    {
			LinkedList<PredicateView> viewsByName = 
			    predViewMap.get(view.predicateName());
			
			if (viewsByName == null)
			    {
				viewsByName = new LinkedList<PredicateView>();
				predViewMap.put(view.predicateName(),viewsByName);
			    };

			viewsByName.addLast(view);

		    }; // for (PredicateView view : declarations.first)

		
		// Process test predicates:
		
		
		for (TestPredicate testPredDecl : declarations.second)
		    {
			LinkedList<TestPredicate> testPredByName = 
			    testPredMap.get(testPredDecl.predicateName());
			
			if (testPredByName == null)
			    {
				testPredByName = new LinkedList<TestPredicate>();
				testPredMap.put(testPredDecl.predicateName(),
						testPredByName);
			    };

			testPredByName.addLast(testPredDecl);

		    }; // for (TestPredicate testPredDecl : declarations.second)



	    }; // for (int i = 0; i < tableQueryPatternFiles.length; ++i)
	


	// Connect to the database (if necessary):

	Connection databaseConnection = null;
	Statement statement = null;
	Statement viewStatement = null;
	ResultSet resultSet = null;
	
	if (databaseName != null)
	    {
		// Load the JDBC driver:
		String JDBCDriverName = null;
		if (_dbms.equals("mysql"))
		    {
			JDBCDriverName = "org.gjt.mm.mysql.Driver";
		    }
		else 
		    {
			assert _dbms.equals("derby");
			JDBCDriverName = "org.apache.derby.jdbc.EmbeddedDriver";
		    };


		try 
		    {
			Class.forName(JDBCDriverName).newInstance();
			// Driver loaded.
		    } 
		catch (ClassNotFoundException ex1) 
		    {
			throw 
			    new Exception("Cannot load the JDBC driver " + 
					  JDBCDriverName);
		    } 
		catch (InstantiationException ex2) 
		    {
			throw 
			    new Exception("Cannot instantiate the JDBC driver " + 
					  JDBCDriverName);
		    } 
		catch (IllegalAccessException ex3) 
		    {
			throw 
			    new Exception("Access denied to the JDBC driver " + 
					  JDBCDriverName);
		    };
		

		String protocolPrefix = "jdbc:" + _dbms + ":";

		if (_dbms.equals("mysql"))
		    {
			protocolPrefix += server + "/";
			
			databaseConnection = 
			    DriverManager.getConnection(protocolPrefix + 
							databaseName,
							userName,
							password);
			
		    }
		else
		    {
			assert _dbms.equals("derby");
			
			databaseConnection = 
			    DriverManager.getConnection(protocolPrefix + 
							databaseName);
		    };


		statement = 	
		    databaseConnection.createStatement();
		viewStatement = 	
		    databaseConnection.createStatement();

	    }; // if (database != null)



	// Prepare for parsing the input.

	DocumentBuilderFactory docBuildFactory = 
	    DocumentBuilderFactory.newInstance();

	DocumentBuilder docBuilder = 
	    docBuildFactory.newDocumentBuilder();
	// locate input file for sql generation
	BufferedReader input = 
	    //new BufferedReader(new InputStreamReader(System.in));
		//new BufferedReader(new FileReader(System.getProperty("java.io.tmpdir").concat("/SQLTemplateDir")+"/"+ "schematic_answers.xml"));
        new BufferedReader(new StringReader(schematicAnswers));
	String line;

	String buffer = null;

	boolean insideADocument = false;

	int lineNumber = 1;


	// Main loop: collect the next XML <generic_answer> element
	// line by line in the buffer, parse the buffer, and
	// process the result of parsing.

	int totalNumberOfAnswers = 0;
	int numberOfNewAnswers;
	int genAnsCount = 0;

	while ((line = input.readLine()) != null)
	    {
		if (line.trim().startsWith("<generic_answer>"))
		    {
			if (insideADocument)
			    throw new Exception("Nested <generic_answer>.");

			if (!line.trim().equals("<generic_answer>"))
			    throw new Exception("Non-space characters after <generic_answer>.");
			
			insideADocument = true;
			
			buffer = line;
		    }
		else if (line.trim().startsWith("</generic_answer>"))
		    {
			if (!insideADocument)
			    throw new Exception("</generic_answer> unmatched with <generic_answer>.");
			
			if (!line.trim().equals("</generic_answer>"))
			    throw new Exception("Non-space characters after </generic_answer>.");
			
			insideADocument = false;
			
			buffer = buffer + line;

			// Read a complete document from the buffer string:
			
			StringReader stream = new StringReader(buffer);

			Document doc = docBuilder.parse(new InputSource(stream));
			
			GenericAnswer genAnswer = convertGenericAnswer(doc);
			
			
			++genAnsCount;
			
			if (verbose.equals("on"))
			    {
				System.out.println("Converting generic answer #" + 
						   genAnsCount + ":");
				System.out.println(genAnswer);
			    };

			_SQLGenerator.
			    setEconomicalJoins(economicalJoins.
					       equalsIgnoreCase("on"));

			_SQLGenerator.setShowSteps(verbose.equals("on"));

			
			Collection<SQLQuery> SQLQueries =
			    _SQLGenerator.convert(genAnswer,
						  predViewMap,
						  testPredMap);
			
			if (verbose.equals("on"))
			    System.out.println("Converted.");

			for (SQLQuery q : SQLQueries)
			    {
                    // print the sql template as xml
				if (outputXML.equals("on"))
				    System.out.println(q.toStringAsXML());
                    // print the sql as plain text with reference to the schemtic answers
                if (outputXML.equals("off"))
                    System.out.println(q.toString());
                    // print only the query text
                if (outputXML.equals("off"))
                    System.out.println(q.queryText());



                    if (databaseName != null)
				    {
					// Query the specified DB directly:
					
					
					if (verbose.equals("on"))
					    {
						java.util.Date currentTime = new java.util.Date();
						System.out.println(currentTime);
						System.out.println("Querying the DB...");
					    };
					
					try
					    {
						// Create all necessary auxilliary views:
						for (Pair<String,String> view : q.SQLViews())
						    {
							String statementText = 
							    "DROP VIEW IF EXISTS " + view.first;
							
							viewStatement.execute(statementText);

							statementText = 
							    "CREATE ALGORITHM = MERGE VIEW " + 
							    view.first +
							    " AS " + view.second;

							viewStatement.execute(statementText);
						    };

						resultSet =
						    statement.executeQuery(q.queryText());
					    }
					finally
					    {
						// Drop all created auxilliary views:
						for (Pair<String,String> view : q.SQLViews())
						    {
							String statementText = 
							    "DROP VIEW IF EXISTS " + view.first;
							
							viewStatement.execute(statementText);
						    };
						
					    }; // try


					numberOfNewAnswers = 0;

					while (resultSet.next())
					    {
						++numberOfNewAnswers;
						++totalNumberOfAnswers;

						if (outputResults.equals("on"))
						    {
							for (Map.Entry<String,InputSyntax.Term> varTermPair : 
								 q.queryVarMap().entrySet())
							    {
								String var = varTermPair.getKey();
								InputSyntax.Term term = 
								    varTermPair.getValue();
								
								System.out.println(var + " := " + 
										   instanceAsString(term,
												    resultSet));
							    };
							
							System.out.println("\n");
						    }; // if (outputResults.equals("on"))
					    };
					

					resultSet.close();
					resultSet = null;

					if (verbose.equals("on"))
					    {
						System.out.println("SQL query processed.");
						System.out.println("new:      " + numberOfNewAnswers + " answers.");
						System.out.println("subtotal: " + totalNumberOfAnswers + " answers.");
 					    };


				    }; // if (database != null)

			    }; // for (SQLQuery q : SQLQueries)
			
		    }
		else
		    {
			if (insideADocument)
			    {
				buffer = buffer + line;
			    }
			else if (!line.trim().equals(""))
			    throw new Exception("Nonempty line outside <generic_answer> elements.");
		    };
		
		++lineNumber;

	    }; // while ((line = in.readLine()) != null)

	if (verbose.equals("on"))
	    System.out.println("Total number of answers: " + totalNumberOfAnswers);

	if (databaseConnection != null) 
	    {
		statement.close();
		statement = null;
		databaseConnection.close();
		databaseConnection = null;
	    };

	if (insideADocument) 
	    throw new Exception("Last document was not closed.");
	

    } // void main(String[] args)


    
    /** Converts the generic answer in the (parsed) XML format
     *  into the input representation for the query manager.
     */
    private 
	static GenericAnswer convertGenericAnswer(Document doc) 
	throws Exception {

	Element genAnsElem = doc.getDocumentElement();

	assert genAnsElem.getTagName().equals("generic_answer");


	//          Extract conditions:

	NodeList condElements = 
	    genAnsElem.getElementsByTagName("condition");
	
	// condElements must be a singleton:
	if (condElements.getLength() != 1) 
	    throw new Exception("No or more then one <condition>.");

	Element condElem = (Element)condElements.item(0);

	assert condElem.getTagName().equals("condition");

	LinkedList<Input.Literal> condLiterals = 
	    convertLiterals(condElem.getElementsByTagName("literal"));


	//          Extract the conclusion:

	NodeList conclElements = 
	    genAnsElem.getElementsByTagName("conclusion");

	// condElements must be a singleton:
	if (conclElements.getLength() != 1) 
	    throw new Exception("No or more then one <conclusion>.");
	
	Element conclElem = (Element)conclElements.item(0);
	
	assert conclElem.getTagName().equals("conclusion");

	LinkedList<Input.Literal> conclLiterals = 
	    convertLiterals(conclElem.getElementsByTagName("literal"));
	    
	return new GenericAnswer(condLiterals,conclLiterals);

    } // convertGenericAnswer(Document doc)


    /** Converts the list of literals in the (parsed) XML format
     *  into a list of instances of Input.Literal.
     */
    private 
	static LinkedList<Input.Literal> convertLiterals(NodeList lits) 
	throws Exception {
	
	LinkedList<Input.Literal> result = 
	    new LinkedList<Input.Literal>();

	for (int i = 0; i < lits.getLength(); ++i)
	    {
		Element litElem = (Element)lits.item(i);
		
		assert litElem.getTagName().equals("literal");
		
		result.addLast(convertLiteral(litElem));
	    };
	
	return result;

    } // convertLiterals(NodeList lits)

    
    /** Converts the literal in the (parsed) XML format
     *  into an instance of Input.Literal.
     */
    private 
	static Input.Literal convertLiteral(Element lit) 
	throws Exception {

	assert lit.getTagName().equals("literal");

	String pol = lit.getAttribute("polarity");
	
	boolean positive = pol.equals("") || pol.equals("pos");
	
	NodeList atomElements = 
	    lit.getElementsByTagName("atom");
	
	if (atomElements.getLength() != 1)
	    throw new Exception("No or more then one <atom> in a <literal>.");
	
	Element atomElem = (Element)atomElements.item(0);
	
	assert atomElem.getTagName().equals("atom");



	// Convert the predicate:

	String predSym = atomElem.getAttribute("predicate");

	assert !predSym.equals("");

	//NodeList argNodes = atomElem.getElementsByTagName("arg");

	NodeList argNodes = atomElem.getChildNodes();


	// Convert predicate arguments:

	LinkedList<InputSyntax.Term> args = convertArguments(argNodes);
	
	int predArity = args.size();

	Input.Predicate pred = 
	    _input.createPredicate(predSym,predArity); // not infix

	return _input.createLiteral(positive,pred,args);

    } // convertLiteral(Element lit)
    


    /** Converts the list of arguments of a function or predicate 
     *  in the (parsed) XML format into a list of instances of Input.Term.
     */
    private 
	static LinkedList<InputSyntax.Term> convertArguments(NodeList args) 
	throws Exception {
	

	LinkedList<InputSyntax.Term> result = 
	    new LinkedList<InputSyntax.Term>();

	for (int i = 0; i < args.getLength(); ++i)
	    {
		if (args.item(i) instanceof Element)
		    {
			Element argElem = (Element)args.item(i);
			
			assert argElem.getTagName().equals("arg");
			
			NodeList contentNodes = argElem.getChildNodes();
			
			Element contentElem = null;
			
			for (int n = 0; n < contentNodes.getLength(); ++n)
			    {
				if (contentNodes.item(n) instanceof Element)
				    {
					
					if (contentElem != null)
					    throw 
						new Exception("More then one element inside an <arg>.");
					
					contentElem = (Element)contentNodes.item(n);
				    };
			    };
		

			if (contentElem == null)
			    throw 
				new Exception("No elements inside an <arg>.");
			
			result.addLast((InputSyntax.Term)convertTerm(contentElem));
		    }; // if (args.item(i) instanceof Element)
	    }; // for (int i = 0; i < args.getLength(); ++i)

	return result;

    } // convertArguments(NodeList args)

    /** Converts the term in the (parsed) XML format
     *  into an instance of Input.Term.
     */
    private 
	static Input.Term convertTerm(Element term) 
	throws Exception {
	
	if (term.getTagName().equals("var"))
	    {
		String varSym = term.getAttribute("sym");

		assert !varSym.equals("");

		Input.Variable var = _input.createVariable(varSym);
		
		return _input.createTerm(var);
	    }
	else if (term.getTagName().equals("const")) 
	    {
		String constSym = term.getAttribute("sym");

		assert !constSym.equals("");

		Input.FunctionalSymbol con = 
		    _input.createFunctionalSymbol(constSym,0);
		
		return _input.createTerm(con);
	
	    }
	else if (term.getTagName().equals("compound_term")) 
	    {
		String funcSym = term.getAttribute("sym");

		assert !funcSym.equals("");

		NodeList argNodes = 
		    term.getElementsByTagName("arg");
		
		int funcArity = argNodes.getLength();
		
		Input.FunctionalSymbol func = 
		    _input.createFunctionalSymbol(funcSym,funcArity);
		
		LinkedList<InputSyntax.Term> args = convertArguments(argNodes);

		return _input.createTerm(func,args);

	    }
	else
	    throw 
		new Exception("<" + term.getTagName() + "> where a term is expected.");

    } // convertTerm(Element term)


    /*
      Sample contents of a table query pattern file:

<predicateViews>
     

  <view predicateName="db_universities" arity="2">
    <query>universities</query>
    <exportedAttribute>code</exportedAttribute>
    <exportedAttribute>name</exportedAttribute>
  </view>

  <view predicateName="db_departments" arity="2">
    <query>departments</query>
    <exportedAttribute>code</exportedAttribute>
    <exportedAttribute>name</exportedAttribute>
  </view>

</predicateViews>

    */



    /*
      Sample contents of a predicate shorthand file:

      <predicateShorthands>

         <entry short="db_abs_iUniversity" long="http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#University"/>

         <entry short="db_abs_iteacherOf" long="http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#teacherOf"/>
      
      </predicateShorthands>
    */
    /** Parses the file as a mapping from full predicate names which
     *  have views in the DB to DB abstraction predicate names 
     *  used in generic answers.
     */
    private
	static
	void 
	parsePredicateShorthands(String XMLFileName,
				 Map<String,String> result) 
	throws Exception
    {

	File file = new File(XMLFileName);
	
	if (!file.isFile() || !file.canRead())
	    throw 
		new Exception("Cannot open predicate shorthand file " + 
			      XMLFileName);
       
	DocumentBuilderFactory docBuildFactory = 
	    DocumentBuilderFactory.newInstance();

	DocumentBuilder docBuilder = 
	    docBuildFactory.newDocumentBuilder();
	
	Document doc = docBuilder.parse(file);


	Element rootElem = doc.getDocumentElement();

	if (!rootElem.getTagName().equals("predicateShorthands"))
	    throw 
		new Exception("<predicateShorthands> expected as " +
			      "the root element in predicate shorthand " +
			      "file " + XMLFileName);

	NodeList entryElems =
	    rootElem.getElementsByTagName("entry");
	
	for (int i = 0; i < entryElems.getLength(); ++i)
	    {
		Element entryElem = (Element)entryElems.item(i);
		
		String shorthand = entryElem.getAttribute("short");
		if (shorthand.equals(""))
		    throw 
			new Exception("Missing attribute 'short' in an <entry>."); 

		String predicate = entryElem.getAttribute("long");
		if (predicate.equals(""))
		    throw 
			new Exception("Missing attribute 'long' in an <entry>."); 
		
		result.put(predicate,shorthand);

	    }; // for (int i = 0; i < entryElems.getLength(); ++i)
	


    } // parsePredicateShorthands(String XMLFileName,




    /** Instantiates the term using the current position in the result set as a substitution on
     *  term variables, and returns the string representation of the instance.
     */
    private static String instanceAsString(InputSyntax.Term term,ResultSet map) 
	throws Exception {

	if (term.isVariable())
	    {
		try 
		    {
			return map.getString(term.variable().name());
		    }
		catch (SQLException ex)
		    {
			throw new Exception("Cannot instantiate variable " +  
					    term.variable().name());
		    }
	    }
	else if (term.isConstant()) 
	    {
		return term.functionalSymbol().name();
	    }
	else // compound term
	    {
		String result = term.functionalSymbol() + "(";
		
		for (int n = 0; n < term.arguments().size(); ++n)
		    {
			if (n > 0) result += ",";
			result += instanceAsString(term.arguments().get(n),map);
		    };
		
		return result + ")";
	    }

    } // instanceAsString(InputSyntax.Term term,ResultSet map) 





    /** DBMS type, can be 'derby' (default) or 'mysql'. */
    private static String _dbms = "derby";

    /** Factory for all datastructures for the internal representation
     *  of generic solutions.
     */
    private static Input _input = new Input();

    private static SQLGenerator _SQLGenerator = new SQLGenerator(_input);
	

} // class SAILQueryManager