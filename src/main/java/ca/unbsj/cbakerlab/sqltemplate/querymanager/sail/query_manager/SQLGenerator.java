

package ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager;

import java.util.*;

import logic_warehouse_je.*;

import cushion_je.*;

import ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views.*;

/** Converts generic answers into SQL queries. */
public class SQLGenerator {

    public SQLGenerator(Input syntaxFactory) {
	_economicalJoins = true;
	_showSteps = false;
	_syntaxFactory = syntaxFactory;
	_equalityPredicate = _syntaxFactory.createPredicate("equal",2);
    }

    /** Switches the economical join optimisation, ie, the use of SQL EXISTS
     *  for reducing redundancy in answers.
     */
    public void setEconomicalJoins(boolean flag) {
	_economicalJoins = flag;
    }
    
    /** Switches diagnostic output. */
    public void setShowSteps(boolean flag) {
	_showSteps = flag;
    }


    /** Converts the generic answer into SQL queries. 
     *  @param genAnswer
     *  @param predicateViews maps predicate names to lists
     *         of predicate views corresponding to different 
     *         arities
     *  @param testPredicates maps predicate names to lists
     *         of test predicate declarations corresponding to 
     *         different arities
     */
    public 
	Collection<SQLQuery> 
	convert(GenericAnswer genAnswer,
		Map<String,? extends Collection<PredicateView>> predicateViews,
		Map<String,? extends Collection<TestPredicate>> testPredicates)  
	throws Exception {

	assert testPredicates != null;

	_testPredicates = testPredicates;

	// Several SQL queries can be generated if one of the predicates
	// is mapped to a union of queries/tables.


	// Select relevant predicate views. Also, check that all literals 
	// are mapped to views or tests.

	
	HashMap<String,LinkedList<PredicateView>> relevantPredicateViews = 
	    new HashMap<String,LinkedList<PredicateView>>();
	    
	for (InputSyntax.Literal lit : genAnswer.condition())
	    {
		if (relevantPredicateViews.get(lit.predicate().name()) != null)	
		    continue; // already extracted

		PredicateView view = fetchView(lit.predicate().name(),
					       lit.predicateArguments().size(),
					       predicateViews);
		if (view != null)
		    {
			LinkedList<PredicateView> views = 
			    relevantPredicateViews.get(lit.predicate().name());
			if (views == null)
			    {
				views = new LinkedList<PredicateView>();
				relevantPredicateViews.put(lit.predicate().name(),
							    views);
			    };
			if (!views.contains(view)) views.addLast(view);
		    }
		else
		    if (fetchTest(lit.predicate().name(),
				  lit.predicateArguments().size(),
				  testPredicates) == null)
			throw new Exception("Predicate view or test predicate declaration missing for " +
					    lit.predicate().name() + "/" + 
					    lit.predicateArguments().size() +
					    ". Check the predicate view file.");
		
		
	    }; // for (InputSyntax.Literal lit : genAnswer.condition())
	
	

	
	LinkedList<HashMap<String,LinkedList<PredicateView>>> splitRelevantViews =
	    splitUnions(relevantPredicateViews);







	// Collect variables from both sides of the answer:

	Set<InputSyntax.Variable> conditionVariables =
	    new TreeSet<InputSyntax.Variable>();
	for (InputSyntax.Literal lit : genAnswer.condition())
	    conditionVariables.addAll(lit.freeVariables());

	Set<InputSyntax.Variable> conclusionVariables =
	    new TreeSet<InputSyntax.Variable>();
	for (InputSyntax.Literal lit : genAnswer.conclusion())
	    conclusionVariables.addAll(lit.freeVariables());


	// Variables that occur on both sides:
	TreeSet<InputSyntax.Variable> selectionVariables =
	    new TreeSet<InputSyntax.Variable>(conditionVariables);
	selectionVariables.retainAll(conclusionVariables);


	if (conclusionVariables.size() != selectionVariables.size())
	    throw 
		new Exception("Unguarded variables in the answer: this functionality is not yet supported.");


	// All variables that occur in the generic answer:
	TreeSet<InputSyntax.Variable> allVariables =
	    new TreeSet<InputSyntax.Variable>(conditionVariables);
	allVariables.addAll(conclusionVariables);
	    
	HashMap<String,InputSyntax.Term> varMapping = 
	    convertConclusion(genAnswer.conclusion());
	
	
	LinkedList<SQLQuery> result = new LinkedList<SQLQuery>();
		
	
	for (HashMap<String,LinkedList<PredicateView>> predViews :
		 splitRelevantViews)
	    {
		// Will accumulate the auxilliary views that have to be
		// created before the query is computed, and
		// dropped afterwards.

		LinkedList<Pair<String,String>> SQLViews = 
		    new LinkedList<Pair<String,String>>();

		String queryText = 
		    convertCondition(genAnswer.condition(),
				     selectionVariables,
				     allVariables,
				     predViews,
				     SQLViews);
	
		result.addLast(new SQLQuery(queryText,SQLViews,varMapping));		
	    };

	return result;


    } // convert(GenericAnswer genAnswer,






    /** Converts the database abstraction literals from the condition
     *  into an SQL query.
     *  @param lits 
     *  @param selectionVariables all variables occuring on both
     *         sides of the generic answer, ie, in the database abstraction
     *         literals as well as in the answer literal.
     *  @param allVariables all variables from the generic answer
     *  @param predicateViews maps predicate names to lists
     *         of predicate views corresponding to different 
     *         arities; IMPORTANT: all views must be singletons, ie, they cannot
     *         contain unions (with more than one member)
     *  @param SQLViews accumulates the auxilliary views that have to be
	       created before the query is computed, and dropped afterwards
     */
    private
	String 
	convertCondition(Collection<InputSyntax.Literal> lits,
			 TreeSet<InputSyntax.Variable> selectionVariables,
			 TreeSet<InputSyntax.Variable> allVariables,
			 Map<String,? extends Collection<PredicateView>> predicateViews,
			 LinkedList<Pair<String,String>> SQLViews)  
	throws Exception
    {
	LinkedList<InputSyntax.Literal> DxGenerating = 
	    new LinkedList<InputSyntax.Literal>();

	LinkedList<LinkedList<InputSyntax.Literal>> DxTesting = 
	    new LinkedList<LinkedList<InputSyntax.Literal>>();

	LinkedList<InputSyntax.Literal> Ea = new LinkedList<InputSyntax.Literal>();
	LinkedList<InputSyntax.Literal> Ec = new LinkedList<InputSyntax.Literal>();
	LinkedList<InputSyntax.Literal> Ed = new LinkedList<InputSyntax.Literal>();

	flatten(lits,
		selectionVariables,
		allVariables,
		DxGenerating,
		DxTesting,
		Ea,
		Ec,
		Ed);

	LinkedList<InputSyntax.Literal> Dx =
	    new LinkedList<InputSyntax.Literal>(DxGenerating);

	for (LinkedList<InputSyntax.Literal> component : DxTesting)
	    Dx.addAll(component);
	

	if (_showSteps)
	    {
		System.out.println("\nFlattening results:");
		System.out.println("  Dx = " + Dx);
		System.out.println("     Generating = " + DxGenerating);
		System.out.println("     Testing    = " + DxTesting);
		System.out.println("  Ea = " + Ea);
		System.out.println("  Ec = " + Ec);
		System.out.println("  Ed = " + Ed);
		System.out.println();
	    };
	
	initialiseAuxilliaryMappings(Dx,predicateViews);


	// Will accumulate all y-variables from DxGenerating
	TreeSet<InputSyntax.Variable> yVarsFromGeneratingLits = 
	    new TreeSet<InputSyntax.Variable>();

	for (InputSyntax.Literal lit : DxGenerating)
	    yVarsFromGeneratingLits.addAll(lit.freeVariables());



	//   Form the SELECT part based on Ea:

	String result = "\nSELECT ";
	
	boolean firstAnswerVar = true;

	for (InputSyntax.Literal lit : Ea)
	    {
		if (!firstAnswerVar) result += ",\n       ";
		firstAnswerVar = false;

		String answerVar = 
		    lit.predicateArguments().get(1).variable().name();

		String yVar = 
		    lit.predicateArguments().get(0).variable().name();

		result += tableAliasForYVar(yVar) + 
		    "." + attributeForYVar(yVar) +
		    " AS " + answerVar;

	    }; // for (InputSyntax.Literal lit : Ea)
	

	

	//    Form the FROM part based on DxGenerating:

	result += "\nFROM ";

	boolean firstGenLit = true;

	for (int i = 0; i < Dx.size(); ++i)
	    {
		InputSyntax.Literal lit = Dx.get(i);
		
		if (DxGenerating.contains(lit))
		    {
			assert !isTestLiteral(lit);

			// We do not simply enumerate DxGenerating
			// because the numbering of literals is as in Dx.

			if (!firstGenLit) result += ",\n     ";
			firstGenLit = false;

			String predicate = lit.predicate().name();
			int arity = lit.predicateArguments().size();

			PredicateView view = 
			    fetchView(predicate,arity,predicateViews);
		
			assert view != null;

			// IMPORTANT: all views are singletons, ie, they cannot
			// contain unions (with more than one member)
			assert view.unionMembers().size() == 1;
			
			PredicateView.UnionMember rel = 
			    view.unionMembers().get(0);
			
			String relation;

			if (rel instanceof PredicateView.Query)
			    {
				String SQLViewName = freshSQLViewName();
				String SQLViewBody = ((PredicateView.Query)rel).body();
				SQLViews.addLast(new Pair<String,String>(SQLViewName,
									 SQLViewBody));
				
				relation = SQLViewName;
			    }
			else if (rel instanceof PredicateView.Table)
			    {
				relation = 
				    ((PredicateView.Table)rel).name();
			    }
			else 
			    throw new Error("Bad kind of union member in a predicate view.");

			result += relation +
			    " AS " + _tableAliases[i];
		    };

	    }; // for (int i = 0; i < Dx.size(); ++i)




	// Form the WHERE part:

	LinkedList<String> atomicJoinConditions =  
	    new LinkedList<String>();

	// Join conditions come from 3 sources: Ec, Ed and conditions
	// for linked components from DxTesting. 

	
	// Extract join conditions from Ec:

	for (InputSyntax.Literal lit : Ec)
	    {
		InputSyntax.Variable yVar = 
		    lit.predicateArguments().get(0).variable();

		// Check if the yVar is covered by generating literals.
		// Otherwise, the equality will contribute to
		// the EXISTS condition.
		if (yVarsFromGeneratingLits.contains(yVar))
		    {
			String constant = 
			    lit.predicateArguments().get(1).toString();
		
			String condition = 
			    tableAliasForYVar(yVar.name()) + 
			    "." + attributeForYVar(yVar.name()) + 
			    " = " + constant;

			atomicJoinConditions.addLast(condition);
		    };

	    }; // for (InputSyntax.Literal lit : Ec)
	    



	// Extract join conditions from Ed:
		
	for (InputSyntax.Literal lit : Ed)
	    {
		InputSyntax.Variable yVar1 = 
		    lit.predicateArguments().get(0).variable();
		InputSyntax.Variable yVar2 = 
		    lit.predicateArguments().get(1).variable();
		
		// Check if both variables are covered by generating 
		// literals. Otherwise, the equality will contribute 
		// to the EXISTS condition.
		if (yVarsFromGeneratingLits.contains(yVar1) &&
		    yVarsFromGeneratingLits.contains(yVar2))
		    {
			String condition = 
			    tableAliasForYVar(yVar1.name()) + 
			    "." + attributeForYVar(yVar1.name()) + 
			    " = " +
			    tableAliasForYVar(yVar2.name()) + 
			    "." + attributeForYVar(yVar2.name());
			
			atomicJoinConditions.addLast(condition);
		    };

	    }; // for (InputSyntax.Literal lit : Ed)


	
	for (LinkedList<InputSyntax.Literal> component : DxTesting)
	    {
		assert !component.isEmpty();

		if (containsNonTestLiteral(component))
		    {
			// Will accumulate all y-variables from the component
			TreeSet<InputSyntax.Variable> yVarsFromComponent = 
			    new TreeSet<InputSyntax.Variable>();

			for (InputSyntax.Literal lit : component)
			    yVarsFromComponent.addAll(lit.freeVariables());


			// Form the EXISTS condition corresponding to 
			// the linked component of testing literals:
		
			String existsCondition = "EXISTS( SELECT *\n             FROM ";

			// FROM part:

			boolean firstTestingLit = true;
		
			for (int i = 0; i < Dx.size(); ++i)
			    {
				InputSyntax.Literal lit = Dx.get(i);
			
				if (component.contains(lit))
				    {
					// We do not simply enumerate the component
					// because the numbering of literals is 
					// as in Dx.
				
					if (!isTestLiteral(lit))
					    {
						if (!firstTestingLit) 
						    existsCondition += ",\n                  ";
					
						firstTestingLit = false;
					
						String predicate = lit.predicate().name();
						int arity = lit.predicateArguments().size();
					
						PredicateView view = 
						    fetchView(predicate,arity,predicateViews);
					
						assert view != null;
						// IMPORTANT: all view are0singletons, ie, they cannot
						// contain unions (with more than one member)
						assert view.unionMembers().size() == 1;
						
						PredicateView.UnionMember rel = 
						    view.unionMembers().get(0);
						
						String relation;

						
						if (rel instanceof PredicateView.Query)
						    {
							String SQLViewName = freshSQLViewName();
							String SQLViewBody = ((PredicateView.Query)rel).body();
							SQLViews.addLast(new Pair<String,String>(SQLViewName,
												 SQLViewBody));
				
							relation = SQLViewName;

						    }
						else if (rel instanceof PredicateView.Table)
						    {
							relation = 
							    ((PredicateView.Table)rel).name();
						    }
						else 
						    throw new Error("Bad kind of union member in a predicate view.");
						
						existsCondition += 
						    relation + 
						    " AS " + _tableAliases[i];
					    }; // if (!isTestLiteral(lit))
				    }; // if (component.contains(lit))

			    }; // for (int i = 0; i < Dx.size(); ++i)


		

			// WHERE part of the EXISTS condition:

			LinkedList<String> atomicJoinConditionsForExists =  
			    new LinkedList<String>();
		

			// Join conditions within the EXISTS condition come 
			// from 3 sources: Ec, Ed and test literals in the component.

	
			// Extract join conditions from Ec:

			for (InputSyntax.Literal lit : Ec)
			    {
				InputSyntax.Variable yVar = 
				    lit.predicateArguments().get(0).variable();

				if (yVarsFromComponent.contains(yVar))
				    {
					// The varianle is from this component.

					// Check that the yVar is not covered by 
					// generating literals.
					// Otherwise, the equality must have contributed 
					// to the join conditions above.
					if (!yVarsFromGeneratingLits.contains(yVar))
					    {
						String constant = 
						    lit.predicateArguments().get(1).toString();
					
						String condition = 
						    tableAliasForYVar(yVar.name()) + 
						    "." + attributeForYVar(yVar.name()) + 
						    " = " + constant;
					
						atomicJoinConditionsForExists.addLast(condition);
					    };
				    };
			
			    }; // for (InputSyntax.Literal lit : Ec)
	    


			// Extract join conditions from Ed:
		
			for (InputSyntax.Literal lit : Ed)
			    {
				InputSyntax.Variable yVar1 = 
				    lit.predicateArguments().get(0).variable();
				InputSyntax.Variable yVar2 = 
				    lit.predicateArguments().get(1).variable();
			
				if (yVarsFromComponent.contains(yVar1) ||
				    yVarsFromComponent.contains(yVar2))
				    {
					// At least one of the variable is 
					// from this component.


					// Check that at least one of the variables 
					// is not covered by generating literals. 
					// Otherwise, the equality must have contributed 
					// to the join conditions above.
					if (!yVarsFromGeneratingLits.contains(yVar1) ||
					    !yVarsFromGeneratingLits.contains(yVar2))
					    {
						String condition = 
						    tableAliasForYVar(yVar1.name()) + 
						    "." + attributeForYVar(yVar1.name()) + 
						    " = " +
						    tableAliasForYVar(yVar2.name()) + 
						    "." + attributeForYVar(yVar2.name());
					
						atomicJoinConditionsForExists.addLast(condition);
					    };
				    };
			
			    }; // for (InputSyntax.Literal lit : Ed)
				
		

			// Convert test literals from the component:

			for (InputSyntax.Literal lit : component)
			    {
				if (isTestLiteral(lit))
				    {
					String condition = 
					    testLitToCondition(lit);

					atomicJoinConditionsForExists.
					    addLast(condition);
				    };
				
			    }; // for (InputSyntax.Literal lit : component)
			



			if (!atomicJoinConditionsForExists.isEmpty())
			    {
				existsCondition += "\n             WHERE ";
		
				for (int i = 0; 
				     i < atomicJoinConditionsForExists.size(); 
				     ++i)
				    {
					if (i > 0) existsCondition += "\n               AND ";
					existsCondition += 
					    atomicJoinConditionsForExists.get(i);
				    };
			    }; 

	       
			// EXISTS condition is complete.
		
			atomicJoinConditions.addLast(existsCondition + ")");

		    }
		else // !containsNonTestLiteral(component)
		    {
			// Just 1 test literal:
			
			assert component.size() == 1;
			
			String condition = 
			    testLitToCondition(component.get(0));
			
			atomicJoinConditions.addLast(condition);

		    }; // if (containsNonTestLiteral(component))



	    }; // for (LinkedList<InputSyntax.Literal> component : DxTesting)




	if (!atomicJoinConditions.isEmpty())
	    {
		result += "\nWHERE ";
		
		for (int i = 0; i < atomicJoinConditions.size(); ++i)
		    {
			if (i > 0) result += "\n  AND ";
			result += atomicJoinConditions.get(i);
		    };
	    }; 


	return result;

    } // convertCondition(Collection<InputSyntax.Literal> lits) 






    /** Initialises _tableAliases, _yVarToLitMap and 
     *  _yVarToArgNumMap.
     */
    private 
	void 
	initialiseAuxilliaryMappings(LinkedList<InputSyntax.Literal> Dx,
				     Map<String,? extends Collection<PredicateView>> predicateViews) 
    {
	
	_tableAliases = new String[Dx.size()];	    
	_yVarToLitMap = new TreeMap<String,Integer>();
	_yVarToPredMap = new TreeMap<String,InputSyntax.Predicate>();
	_yVarToArgNumMap = new TreeMap<String,Integer>();
	_predicateViews = predicateViews;
	
	
	if (_showSteps)
	    System.out.println("Table aliases:");
	

	for (int i = 0; i < Dx.size(); ++i)
	    {
		InputSyntax.Literal lit = Dx.get(i);
		
		if (!isTestLiteral(lit))
		    {
			_tableAliases[i] = "TABLE" + i;

			if (_showSteps)
			    {
				System.out.println("   " + lit + " --> " + _tableAliases[i]);
			    };
			
			InputSyntax.Predicate predicate = lit.predicate();

			List<InputSyntax.Term> litArgs = lit.predicateArguments();
		
			for (int j = 0; j < litArgs.size(); ++j)
			    {
				String yVarName = 
				    litArgs.get(j).variable().name();
				assert !_yVarToLitMap.containsKey(yVarName);
				_yVarToLitMap.put(yVarName,i);
				assert !_yVarToPredMap.containsKey(yVarName);
				_yVarToPredMap.put(yVarName,predicate);
				assert !_yVarToArgNumMap.containsKey(yVarName);
				_yVarToArgNumMap.put(yVarName,j);
			    };
		    }; // if (!isTestLiteral(lit))
	    }; // for (int i = 0; i < Dx.size(); ++i)

	if (_showSteps) System.out.println();

    } // initialiseAuxilliaryMappings(LinkedList<InputSyntax.Literal> Dx,



    /** Converts the answer literal of the conclusion into the corresponding
     *  query variable mapping.
     */
    private 
	HashMap<String,InputSyntax.Term>
	convertConclusion(Collection<InputSyntax.Literal> lits) 
	throws Exception
    {
	Iterator<InputSyntax.Literal> iter = lits.iterator();
	
	if (!iter.hasNext())
	    throw new Exception("Empty conclusion.");
	
	InputSyntax.Literal lit = iter.next();

	if (iter.hasNext()) 
	    throw 
		new Exception("More than one literal in the conclusion.");
	
	List<InputSyntax.Term> args = lit.predicateArguments();


	HashMap<String,InputSyntax.Term> result = 
	    new HashMap<String,InputSyntax.Term>();

	for (int n = 0; n < args.size(); ++n)
	    {
		String queryVar = "queryVar" + n;
		result.put(queryVar,args.get(n));
	    };


	return result;

    } // convertConclusion(Collection<InputSyntax.Literal> lits) 
    

    /** Flattens the condition literals (see the paper for details);
     *  the results are communicated via the parameters Dx,Ea,Ec and Ed.
     */
    private void flatten(Collection<InputSyntax.Literal> lits,
			 TreeSet<InputSyntax.Variable> selectionVariables,
			 TreeSet<InputSyntax.Variable> allVariables,
			 LinkedList<InputSyntax.Literal> DxGenerating,
			 LinkedList<LinkedList<InputSyntax.Literal>> DxTesting,
			 LinkedList<InputSyntax.Literal> Ea,
			 LinkedList<InputSyntax.Literal> Ec,
			 LinkedList<InputSyntax.Literal> Ed) 
	throws Exception
    {
	assert DxGenerating.isEmpty();
	assert DxTesting.isEmpty();
	assert Ea.isEmpty();
	assert Ec.isEmpty();
	assert Ed.isEmpty();

	
	// Sort the literals into generating and testing:
       
	LinkedList<InputSyntax.Literal> generatingLits = 
	    new LinkedList<InputSyntax.Literal>();
	LinkedList<InputSyntax.Literal> testingViewLits = 
	    new LinkedList<InputSyntax.Literal>();
	LinkedList<InputSyntax.Literal> testLits = 
	    new LinkedList<InputSyntax.Literal>();
	
	separateGeneratingAndTestingLits(lits,
					 generatingLits,
					 testingViewLits,
					 testLits,
					 selectionVariables);

	if (_showSteps)
	    {
		System.out.println("\nSeparation results:");
		System.out.println("   Generating: " + generatingLits);
		System.out.println("   Testing view literals: " + testingViewLits);
		System.out.println("   Test literals: " + testLits);
		System.out.println();
	    };


	Integer freshYVariableIndex = new Integer(0);


	// Accumulates all introduced negative equality literals.
	LinkedList<InputSyntax.Literal> E = 
	    new LinkedList<InputSyntax.Literal>();
	

	// Will accumulate variables from generatingLits:
	TreeSet<InputSyntax.Variable> generatingLitVars =
	    new TreeSet<InputSyntax.Variable>();

	// Will accumulate all y-variables from DxGenerating:
	TreeSet<InputSyntax.Variable> yVarsFromGeneratingLits = 
	    new TreeSet<InputSyntax.Variable>();

	for (InputSyntax.Literal lit : generatingLits)
	    {
		generatingLitVars.addAll(lit.freeVariables());

		InputSyntax.Literal flattenedLit = 
		    flattenViewLit(lit,freshYVariableIndex,allVariables,E);

		DxGenerating.addLast(flattenedLit);
		
		yVarsFromGeneratingLits.addAll(flattenedLit.freeVariables());
	    };



	// We have to mix testing view-literals with test literals 
	// to enable the computation of variable-linked components:
	LinkedList<InputSyntax.Literal> allTestingLits =
	    new LinkedList<InputSyntax.Literal>(testingViewLits);
	allTestingLits.addAll(testLits);
	


	for (LinkedList<InputSyntax.Literal> comp : 
		 linkedComponents(allTestingLits,generatingLitVars))
	    {
		LinkedList<InputSyntax.Literal> DxTestingComponent = 
		    new LinkedList<InputSyntax.Literal>();

		// Flatten the view literals from comp first, because
		// flattening test literals may require some y-variables
		// introduced for view literals from comp (as well as
		// some y-variables introduced for generating literals).

		for (InputSyntax.Literal lit : comp)
		    if (!isTestLiteral(lit))
			DxTestingComponent.addLast(flattenViewLit(lit,
								  freshYVariableIndex,
								  allVariables,
								  E));

		// Now "flatten" the test literals, if any:
		for (InputSyntax.Literal lit : comp)
		    if (isTestLiteral(lit))
			DxTestingComponent.addLast(flattenTestLit(lit,E));
		
		DxTesting.addLast(DxTestingComponent);

	    }; // for (LinkedList<InputSyntax.Literal> comp : 
	


	// Extract Ec:

	ListIterator<InputSyntax.Literal> iter = E.listIterator();

	while (iter.hasNext())
	    {
		InputSyntax.Literal negEq = iter.next();
		
		assert negEq.isNegative();
		assert negEq.predicate().equals(_equalityPredicate);
		assert negEq.predicateArguments().size() == 2;

		InputSyntax.Term y = negEq.predicateArguments().get(0);
		InputSyntax.Term t = negEq.predicateArguments().get(1);

		if (t.isConstant())
		    {
			Ec.addLast(negEq);
			iter.remove();
		    };
		
	    }; // while (iter.hasNext())
	    
	// All y != c are removed from E.


	// Extract Ea:

	TreeSet<InputSyntax.Variable> extractedSelectionVariables =
	    new TreeSet<InputSyntax.Variable>();

	for (InputSyntax.Literal negEq : E)
	    {
		assert negEq.isNegative();
		assert negEq.predicate().equals(_equalityPredicate);
		assert negEq.predicateArguments().size() == 2;

		InputSyntax.Term y = negEq.predicateArguments().get(0);
		
		if (yVarsFromGeneratingLits.contains(y.variable()))
		    {
			InputSyntax.Term t = negEq.predicateArguments().get(1);
		
			assert t.isVariable();

			if (selectionVariables.contains(t.variable()))
			    {
			
				// Check if t.variable() is already in Ea:
				
				if (!extractedSelectionVariables.contains(t.variable()))
				    {
					Ea.addLast(negEq);
					extractedSelectionVariables.add(t.variable());
				    };
			    };
		    };
	    }; // for (InputSyntax.Literal negEq : E)
	
	assert extractedSelectionVariables.size() == selectionVariables.size();



	// Extract Ed, possibly with redundancy:
		
	for (InputSyntax.Literal negEq1 : E)
	    {
		InputSyntax.Term y1 = negEq1.predicateArguments().get(0);
		InputSyntax.Term t1 = negEq1.predicateArguments().get(1);
		for (InputSyntax.Literal negEq2 : E)
		    {
			InputSyntax.Term y2 = negEq2.predicateArguments().get(0);
			InputSyntax.Term t2 = negEq2.predicateArguments().get(1);
						

			if (t1.equals(t2) && !y1.equals(y2))
			    {
				InputSyntax.Literal newNegEq = 
				    _syntaxFactory.createLiteral(false,
								 _equalityPredicate,
								 y1,
								 y2);
				Ed.add(newNegEq);
			    };
		    };
	    }; // for (InputSyntax.Literal negEq1 : E)
	



	// Delete redundancy from Ed:

	LinkedList<InputSyntax.Literal> nonredundantLiterals = 
	    new LinkedList<InputSyntax.Literal>();

	ListIterator<InputSyntax.Literal> listIter = Ed.listIterator();

	while (listIter.hasNext())
	    {
		InputSyntax.Literal negEq = listIter.next();
		
		if (isRedundantWRT(negEq,nonredundantLiterals))
		    {
			listIter.remove();
		    }
		else
		    nonredundantLiterals.addLast(negEq);
	    };


    } // flatten(Collection<InputSyntax.Literal> lits,..)
						 
    

    /** Does not apply to test literals! */
    private 
	InputSyntax.Literal flattenViewLit(InputSyntax.Literal lit,
					   Integer freshYVariableIndex,
					   TreeSet<InputSyntax.Variable> reservedVariables,
					   LinkedList<InputSyntax.Literal> E) 
	throws Exception
    {
	assert !isTestLiteral(lit);

	LinkedList<InputSyntax.Term> args = new LinkedList<InputSyntax.Term>();
	
	for (InputSyntax.Term arg : lit.predicateArguments())
	    {
		if (!arg.isVariable() && !arg.isConstant())
		    throw new Exception("Compound subterm in a condition literal.");

		InputSyntax.Variable y = 
		    freshYVariable(freshYVariableIndex,reservedVariables);

		InputSyntax.Term alpha = _syntaxFactory.createTerm(y);
		
		E.add(_syntaxFactory.createLiteral(false,
						   _equalityPredicate,
						   alpha,
						   arg));
		
		args.addLast(alpha);
		
	    }; // for (InputSyntax.Term arg : lit.predicateArguments())
	

	return _syntaxFactory.createLiteral(lit.isPositive(),
					    lit.predicate(),
					    args);

    } // flattenViewLit(InputSyntax.Literal lit,..)


    /** This does not really flatten the literal: it just replaces all the x-variables 
     *  with the corresponding y-variables.
     */
    private 
	InputSyntax.Literal flattenTestLit(InputSyntax.Literal lit,
					   LinkedList<InputSyntax.Literal> E) 
	throws Exception
    {
	assert isTestLiteral(lit);

	return _syntaxFactory.instantiate1(lit,extractXToYMap(E));
    }



    /** Extracts a mapping from x-variables to y-variables from the given
     *  set of negated equations on variables and constants; note that
     *  this is a nondeterministic process -- the result depends on 
     *  the order of equations.
     *  @param equations negated equalities of the form y = t, 
     *         where y is a y-variable, and t is either an x-variable
     *         or a constant
     */
    private
	Map<String,InputSyntax.Term> 
	extractXToYMap(LinkedList<InputSyntax.Literal> equations) {

	HashMap<String,InputSyntax.Term> result = 
	    new HashMap<String,InputSyntax.Term>();
	
	for (InputSyntax.Literal lit : equations)
	    {
		InputSyntax.Term t = lit.predicateArguments().get(1);
		assert t.isVariable() || t.isConstant();
		if (t.isVariable())
		    result.put(t.variable().name(),
			       lit.predicateArguments().get(0));
	    };

	return result;

    } // extractXToYMap(LinkedList<InputSyntax.Literal> equations)
		   




    /** Generates a fresh y-variable (guaranteed to be distinct from all
     *  variables in <code>reservedVariables</code>); 
     *  <code>index</code> is incremented, possibly several times.
     */
    private InputSyntax.Variable freshYVariable(Integer index,
						Set<InputSyntax.Variable> reservedVariables)
    {
	InputSyntax.Variable result = _syntaxFactory.createVariable("Y" + index);
	
	++index;

	while (reservedVariables.contains(result))
	    {
		result = _syntaxFactory.createVariable("Y" + index);
		++index;
	    };

	reservedVariables.add(result);

	return result;

    } // freshYVariable(int index,..)


    
    /** Checks if the negative equality literal is redundant wrt the specified list
     *  of literals, ie, if the literal's negation is a transitivity consequence
     *  of negations of two literals from the list, or the list simply contains 
     *  an equal literal (modulo the symmetry).
     */
    private boolean isRedundantWRT(InputSyntax.Literal negEq,
				   LinkedList<InputSyntax.Literal> literals) {
	
	// Note that the symmetry of equality has to be taken into account.

	assert negEq.isNegative();
	assert negEq.predicate().equals(_equalityPredicate);
	assert negEq.predicateArguments().size() == 2;
	
	InputSyntax.Term s1 = negEq.predicateArguments().get(0);
	InputSyntax.Term s2 = negEq.predicateArguments().get(1);
	
	for (InputSyntax.Literal negEq1 : literals)
	    {
		InputSyntax.Term t1 = negEq1.predicateArguments().get(0);
		InputSyntax.Term t2 = negEq1.predicateArguments().get(1);
		
		if ((s1.equals(t1) && s2.equals(t2)) ||
		    (s1.equals(t2) && s2.equals(t1)))
		    return true;

		for (InputSyntax.Literal negEq2 : literals)
		    {
			InputSyntax.Term u1 = negEq2.predicateArguments().get(0);
			InputSyntax.Term u2 = negEq2.predicateArguments().get(1);
			
			if ((s1.equals(t1) && 
			     ((s2.equals(u1) && t2.equals(u2)) ||
			      (s2.equals(u2) && t2.equals(u1))))

			    ||
			    
			    (s1.equals(t2) && 
			     ((s2.equals(u1) && t1.equals(u2)) ||
			      (s2.equals(u2) && t1.equals(u1))))
			     
			    ||

			    (s2.equals(t1) &&
			     ((s1.equals(u1) && t2.equals(u2)) ||
			      (s1.equals(u2) && t2.equals(u1))))
		    
			    ||
		    
			    (s2.equals(t2) &&
			     ((s1.equals(u1) && t1.equals(u2)) ||
			      (s1.equals(u2) && t1.equals(u1)))))
			    return true;

		    }; // for (InputSyntax.Literal negEq2 : Ed)
	    }; // for (InputSyntax.Literal negEq1 : Ed)
	
	return false;

    } // isRedundantWRT(InputSyntax.Literal negEq,..)



    /** Heuristically sorts the input literals into generating literals and 
     *  testing literals (the literals are not actually removed from
     *  <code>input</code>);
     *  note that if the economical join optimisation is off, all the input
     *  literals become generating.
     *  
     */
    private 
	void 
	separateGeneratingAndTestingLits(Collection<InputSyntax.Literal> input,
					 LinkedList<InputSyntax.Literal> generatingLits,
					 LinkedList<InputSyntax.Literal> testingViewLits,
					 LinkedList<InputSyntax.Literal> testLits,
					 Collection<InputSyntax.Variable> selectionVariables)
    {
	LinkedList<InputSyntax.Literal> viewLits = 
	    new LinkedList<InputSyntax.Literal>();
	
	for (InputSyntax.Literal lit : input)
	    if (isTestLiteral(lit))
		{
		    testLits.addLast(lit);
		}
	    else 
		viewLits.addLast(lit);
		

	if (!_economicalJoins)
	    {
		// All view literals are identified as generating:
		generatingLits.addAll(viewLits);
		return;
	    };
	

	LinkedList<InputSyntax.Variable> uncoveredAnswerVariables = 
	    new LinkedList<InputSyntax.Variable>(selectionVariables);

	LinkedList<InputSyntax.Variable> allCoveredVariables = 
	    new LinkedList<InputSyntax.Variable>();

	LiteralComparator litComparator = 
	    new LiteralComparator(uncoveredAnswerVariables,
				  allCoveredVariables);
	
	while (!viewLits.isEmpty())
	    {
		if (uncoveredAnswerVariables.isEmpty())
		    {
			// All the remaining literals are testing.
			testingViewLits.addAll(viewLits);
			return;
		    };

		InputSyntax.Literal bestLiteral = 
		    Collections.max(viewLits,litComparator);
		

		// Does it contain uncovered answer variables?
		
		Set<InputSyntax.Variable> vars = 
		    bestLiteral.freeVariables();
		vars.retainAll(uncoveredAnswerVariables);
		
		if (vars.isEmpty())
		    {
			// All the remaining literals are testing.
			testingViewLits.addAll(viewLits);
			return;
		    };
		
		// bestLiteral is generating.

		generatingLits.add(bestLiteral);
		
		viewLits.remove(bestLiteral);
		
		uncoveredAnswerVariables.removeAll(vars);

		allCoveredVariables.addAll(bestLiteral.freeVariables());

	    }; // while (!viewLits.isEmpty())


	// If we are here, all the input literals have been
	// identified as generating.

    } // separateGeneratingAndTestingLits(..)


    /** Comparator of literals for the selection of generating literals:
     *  bigger literals will have higher priority for selection as generating.
     */
    private static class LiteralComparator 
	implements Comparator<InputSyntax.Literal> {

	public 
	    LiteralComparator(Collection<InputSyntax.Variable> uncoveredAnswerVariables,
			      Collection<InputSyntax.Variable> allCoveredVariables) {
	    _uncoveredAnswerVariables = uncoveredAnswerVariables;
	    _allCoveredVariables = allCoveredVariables;
	}

	/** Heuristic comparison: 1) literals with uncovered answer variables
	 *  are greater than others; 2) literals with more constants are greater 
	 *  than literals with fewer constants; 3) literals with more covered
	 *  variables are greater than literals with fewer such variables;
	 *  4) literals with more uncovered 
	 *  answer variables are greater than literals with fewer such variables.
	 */
	public int compare(InputSyntax.Literal lit1,
			   InputSyntax.Literal lit2) {
	    

	    Set<InputSyntax.Variable> uncoveredVars1 = lit1.freeVariables();
	    Set<InputSyntax.Variable> coveredVars1 = lit1.freeVariables();
	    uncoveredVars1.retainAll(_uncoveredAnswerVariables);
	    coveredVars1.retainAll(_allCoveredVariables);
	    

	    Set<InputSyntax.Variable> uncoveredVars2 = lit2.freeVariables();
	    Set<InputSyntax.Variable> coveredVars2 = lit2.freeVariables();
	    uncoveredVars2.retainAll(_uncoveredAnswerVariables);
	    coveredVars2.retainAll(_allCoveredVariables);

	    
	    if (uncoveredVars1.isEmpty())
		{
		    if (uncoveredVars2.isEmpty())
			return 0; // the result is not important
		    // lit1 has no uncovered variable, lit2 has an uncovered variable
		    return -1;
		}
	    else if (uncoveredVars2.isEmpty())
		{
		    // lit1 has an uncovered variable, lit2 has no uncovered variable
		    return 1;
		};
	    
	    // Both literals contain uncovered answer variables.
	    
	    // Number of constants in lit1:
	    int constCounter1 = 0;
	    for (InputSyntax.Term arg : lit1.predicateArguments())
		if (arg.isConstant()) ++constCounter1;

	    // Number of constants in lit2:
	    int constCounter2 = 0;
	    for (InputSyntax.Term arg : lit2.predicateArguments())
		if (arg.isConstant()) ++constCounter2;
	    
	    if (constCounter1 != constCounter2) 
		return constCounter1 - constCounter2;

	    // Same number of constants.

	  
  
	    // Compare the number of covered variables:
	    
	    if (coveredVars1.size() != coveredVars2.size())
		return coveredVars1.size() - coveredVars2.size();
	    
	    // Same number of covered variables:



	    // Compare the number of uncovered variables:

	    if (uncoveredVars1.size() != uncoveredVars2.size())
		return uncoveredVars1.size() - uncoveredVars2.size();
	    
	    return 0; // The literals are equally good.
	
	} // compare(InputSyntax.Literal lit1,..)

	/** Only if the same object. */
	public boolean equals(Object obj) {
	    return this == obj;
	}
	
	
	private Collection<InputSyntax.Variable> _uncoveredAnswerVariables;

	private Collection<InputSyntax.Variable> _allCoveredVariables;


    } // class LiteralComparator 



    /** Table alias corresponding to the Dx literal containing 
     *  the y-variable.
     */
    private String tableAliasForYVar(String yVar) {

	assert _yVarToLitMap.get(yVar) != null;

	return _tableAliases[_yVarToLitMap.get(yVar)];

    } 

    
    /** Table attribute corresponding to the position of
     *  the y-variable in the corresponding Dx literal.
     */
    private String attributeForYVar(String yVar) {
	
        InputSyntax.Predicate predicate = _yVarToPredMap.get(yVar);
	
	
	PredicateView predView = 
	    fetchView(predicate.name(),predicate.arity(),_predicateViews);

	assert predView != null;

	// IMPORTANT: all views are singletons, ie, they cannot
	// contain unions (with more than one member)
	assert predView.unionMembers().size() == 1;

	return 
	    predView.
	    unionMembers().
	    get(0).
	    exportedAttributes().
	    get(_yVarToArgNumMap.get(yVar));
    } // attributeForYVar(String yVar)


    /** Identifies minimal linked components in <code>lits</code>;
     *  two literals are linked if they share at least one common variable,
     *  or are linked to a common literal; only variables that do not 
     *  occur in <code>nonlinkingVars</code> are considered.
     */
    private 
	static
	LinkedList<LinkedList<InputSyntax.Literal>>
	linkedComponents(LinkedList<InputSyntax.Literal> lits,
			 Set<InputSyntax.Variable> nonlinkingVars) {

	
	LinkedList<LinkedList<InputSyntax.Literal>> res = 
	    new LinkedList<LinkedList<InputSyntax.Literal>>();

	for (InputSyntax.Literal lit : lits)
	    {
		LinkedList<InputSyntax.Literal> newComponent =
		    new LinkedList<InputSyntax.Literal>();
		newComponent.add(lit);

		ListIterator<LinkedList<InputSyntax.Literal>> component =
		    res.listIterator(0);

		while (component.hasNext())
		    {
			LinkedList<InputSyntax.Literal> comp = 
			    component.next();

			if (sharesVariablesWith(lit,comp,nonlinkingVars))
			    {
				component.remove();
				newComponent.addAll(comp);
			    };
		    };
		
		res.add(newComponent);

	    }; // for (InputSyntax.Literal lit : lits)

	return res;

    } // linkedComponents(LinkedList<InputSyntax.Literal> lits,..)


    /** Checks whether <code>lit</code> has a common variable with a literal
     *  from <code>component</code>; only variables that do not 
     *  occur in <code>nonlinkingVars</code> are considered.
     */
    private
	static
	boolean 
	sharesVariablesWith(InputSyntax.Literal lit,
			    LinkedList<InputSyntax.Literal> component,
			    Set<InputSyntax.Variable> nonlinkingVars) {

	Collection<InputSyntax.Variable> litVars = 
	    lit.freeVariables();

	for (InputSyntax.Literal compLit : component)
	    {
		for (InputSyntax.Variable var : compLit.freeVariables())
		    if (!nonlinkingVars.contains(var) && litVars.contains(var))
			return true;
	    };

	return false;
	
    } // sharesVariablesWith(InputSyntax.Literal lit,..)


    
    private 
	PredicateView 
	fetchView(String predName,
		  int arity,
		  Map<String,? extends Collection<PredicateView>> predicateViews) {
	
	Collection<PredicateView> views = predicateViews.get(predName);
		
	if (views == null) return null;

	
	for (PredicateView view : views)
	    if (view.arity() == arity) return view;
	
	return null;

    } // fetchView(String predName,
	



    private 
	TestPredicate 
	fetchTest(String predName,
		  int arity,
		  Map<String,? extends Collection<TestPredicate>> testPredicates) {
	
	Collection<TestPredicate> declarations =
	    testPredicates.get(predName);
		
	if (declarations == null) return null;

	
	for (TestPredicate decl : declarations)
	    if (decl.arity() == arity) return decl;
	
	return null;

    } // fetchTest(String predName,


    private boolean isTestLiteral(InputSyntax.Literal lit) {

	
	return 
	    lit.isNegative() && 
	    fetchTest(lit.predicate().name(),
		      lit.predicate().arity(),
		      _testPredicates) != null;

    } // isTestLiteral(InputSyntax.Literal lit)


    private 
	boolean 
	containsNonTestLiteral(Collection<InputSyntax.Literal> lits) {

	for (InputSyntax.Literal lit : lits)
	    if (!isTestLiteral(lit))
		return true;
	
	return false;

    } // containsNonTestLiteral(Collection<InputSyntax.Literal> lits)



    private String testLitToCondition(InputSyntax.Literal testLiteral) 
    throws Exception {

	assert isTestLiteral(testLiteral);
	
	TestPredicate decl = 
	    fetchTest(testLiteral.predicate().name(),
		      testLiteral.predicate().arity(),
		      _testPredicates);
	
	LinkedList<String> templateParameters = new LinkedList<String>();

	for (InputSyntax.Term arg : testLiteral.predicateArguments())
	    if (arg.isVariable())
		{
		    // Y-variable. Has to be mapped to a table's attribute:
		    
		    templateParameters.
			addLast(tableAliasForYVar(arg.variable().name()) + 
				"." + attributeForYVar(arg.variable().name()));

		}
	    else if (arg.isConstant())
		{
		    templateParameters.addLast(arg.toString());
		}
	    else 
		throw 
		    new Exception("Operations in test literal are not yet supported.");

	return decl.instantiateTemplate(templateParameters);

    } // testLitToCondition(InputSyntax.Literal testLiteral)


    /** Generates all combinations of union members of all participating views;
     *  note that the map <code>predicateViews</code> becomes empty as a result.
     *  @param predicateViews nonempty
     */
    private
	LinkedList<HashMap<String,LinkedList<PredicateView>>> 
	splitUnions(HashMap<String,LinkedList<PredicateView>> predicateViews) {
		
	assert !predicateViews.isEmpty();
	
	Map.Entry<String,LinkedList<PredicateView>> firstEntry = 
	    predicateViews.entrySet().iterator().next();
	
	predicateViews.remove(firstEntry.getKey());
	
	LinkedList<LinkedList<PredicateView>> combinations = 
	    splitUnions(firstEntry.getValue());
	
	LinkedList<HashMap<String,LinkedList<PredicateView>>> result = 
	    new LinkedList<HashMap<String,LinkedList<PredicateView>>>();
	
	if (predicateViews.isEmpty())
	    {
		// Recursion base. 	

		for (LinkedList<PredicateView> comb : combinations)
		    {
			HashMap<String,LinkedList<PredicateView>> newMap = 
			    new HashMap<String,LinkedList<PredicateView>>();
			newMap.put(firstEntry.getKey(),comb);
			result.addLast(newMap);
		    };
	    }
	else
	    {
		// Recursively process the remaining views:
		
		LinkedList<HashMap<String,LinkedList<PredicateView>>> base =
		    splitUnions(predicateViews);
		
		for (HashMap<String,LinkedList<PredicateView>> baseEl : base)
		    for (LinkedList<PredicateView> comb : combinations)
			{
			    HashMap<String,LinkedList<PredicateView>> newMap = 
				(HashMap<String,LinkedList<PredicateView>>)baseEl.clone();
			    newMap.put(firstEntry.getKey(),comb);
			    result.addLast(newMap);
			};
		
	    }; // if (predicateViews.isEmpty())
		
		
	return result;
		
    } // splitUnions(HashMap<String,LinkedList<PredicateView>> predicateViews)
    


    
    /** Generates all combinations of union members of all participating views;
     *  note that the list <code>views</code> becomes empty as a result.
     *  @param views nonempty
     */
    private
	LinkedList<LinkedList<PredicateView>> 
	splitUnions(LinkedList<PredicateView> views) {
	
	assert !views.isEmpty();

	PredicateView first = views.removeFirst();
		
	List<PredicateView> members = first.splitUnion();
		
	LinkedList<LinkedList<PredicateView>> result = 
	    new LinkedList<LinkedList<PredicateView>>();
		
	if (views.isEmpty())
	    {
		// Recursion base. 

		for (PredicateView mem : members)
		    {
			LinkedList<PredicateView> singleton = 
			    new LinkedList<PredicateView>();
			singleton.add(mem);
			result.addLast(singleton);
		    };
	    }
	else 
	    {
		// Recursively process the remaining views:
		
		LinkedList<LinkedList<PredicateView>> base = 
		    splitUnions(views);

		// Multiply members by the elements of base,	
		// i.e., every member should be added to every element
		// of base.
		
		for (LinkedList<PredicateView> baseEl : base)
		    for (PredicateView mem : members)
			{
			   LinkedList<PredicateView> newEl = 
			       (LinkedList<PredicateView>)baseEl.clone();
			   newEl.addFirst(mem);
			   result.addLast(newEl);
			};		

	    }; // if (views.isEmpty())

	return result;

    } // splitUnions(LinkedList<PredicateView> views)

   
    /** Generates a fresh auxilliary SQL view name; this is generally unsafe
     *  becase, theoretically, there is a possibility of a clash with a table
     *  name.
     */
    private static String freshSQLViewName() {
	return "SAIL_SQL_GENERATOR_AUXILLIARY_VIEW_" + _nextSQLViewNumber++;
    }





    //               Data:


    private static long _nextSQLViewNumber;


    private Input _syntaxFactory;

    /** Switches the economical join optimisation, ie, the use of SQL EXISTS
     *  for reducing redundancy in answers.
     */
    private boolean _economicalJoins;

    /** Switches diagnostic output. */
    private boolean _showSteps;

    private InputSyntax.Predicate _equalityPredicate;
    

    // Some auxilliary mappings:
    
    /** Maps a number of a literal in Dx into the corresponding
     *  table alias.
     */
    private String[] _tableAliases;
    

    /** Maps a y-variable to the number of the literal containing it
     *  in Dx.
     */
    private TreeMap<String,Integer> _yVarToLitMap;

    /** Maps a y-variable to the predicate of the literal containing it
     *  in Dx.
     */
    private TreeMap<String,InputSyntax.Predicate> _yVarToPredMap;

    /** Maps a y-variable to the number of the argument
     *  of the literal containing it.	
     */
    private TreeMap<String,Integer> _yVarToArgNumMap;
    
    private Map<String,? extends Collection<PredicateView>> _predicateViews;


    /** Maps predicate names to lists of test predicate declarations corresponding 
     *  to different arities.
     */
    private Map<String,? extends Collection<TestPredicate>> _testPredicates;


} // class SQLGenerator