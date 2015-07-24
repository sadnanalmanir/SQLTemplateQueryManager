
package ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views.parser;


import java.io.*;
import java.math.*;
import java.util.*;
import java.net.*;

import javax.xml.bind.*;
import javax.xml.bind.util.*;
import javax.xml.validation.*;

import cushion_je.*;
import ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views.parser.jaxb.*;
import ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views.*;


/** Parses documents containing predicate views. */
public class Parser {

    

    public Parser() throws java.lang.Exception {

	JAXBContext jc = 
	    JAXBContext.newInstance("ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views.parser.jaxb");
	_unmarshaller = jc.createUnmarshaller();
	

	SchemaFactory schemaFactory = 
	    SchemaFactory.
	    newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);

	//URL schemaURL = ClassLoader.getSystemResource("PredicateViews.xsd");
	URL schemaURI = new File(System.getProperty("java.io.tmpdir").concat("/SQLTemplateDir")+"/"+"PredicateViews.xsd").toURI().toURL();
	//assert schemaURL != null;

	Schema schema = schemaFactory.newSchema(schemaURI);
	if(schema==null)
		System.out.println("null schema");
	else
		System.out.println("Schema found and loaded ->" + schema.toString());

	_unmarshaller.setSchema(schema);	

    } // Parser()


    public 
	Pair<? extends Collection<PredicateView>,? extends Collection<TestPredicate>>
	parse(File file) throws java.lang.Exception {

	
	try
	    {
		PredicateViews root = 
		    (PredicateViews)_unmarshaller.unmarshal(file);

		Pair<LinkedList<PredicateView>,LinkedList<TestPredicate>> 
		    result =
		    new Pair(new LinkedList<PredicateView>(),
			     new LinkedList<TestPredicate>());
		
		
		for (Object decl : root.getTopLevelDeclaration())
		    {
			if (decl instanceof PredicateViews.View)
			    {
				PredicateView viewObj = 
				    convert((PredicateViews.View)decl);
			
				result.first.addLast(viewObj);
			    }
			else if (decl instanceof PredicateViews.Test)
			    {
				TestPredicate testPredObj = 
				    convert((PredicateViews.Test)decl);
				
				result.second.addLast(testPredObj);
			    }
			else
			    throw new Error("Bad element type in sail.query_manager.predicate_views.parser.jaxb.PredicateViews");

		    }; // for (Object decl : root.getTopLevelDeclaration())

		return result;
	    }
	catch (javax.xml.bind.UnmarshalException ex)
	    {
		throw 
		    new java.lang.Exception("Predicate view file cannot be read: " +
					    ex);
	    }



    } // parse(File file)


    
    private PredicateView convert(PredicateViews.View view) {
    
	String predName = view.getPredicateName();
	
	if (predName.equals("")) 
	    throw new Error("Bad predicate name in a predicate view: empty string.");

	int arity = view.getArity().intValue();
	
	if (arity <= 0)
	    throw new Error("Bad arity value in a predicate view: " + arity);

	LinkedList<PredicateView.UnionMember> unionMembers = 
	    new LinkedList<PredicateView.UnionMember>();

	for (Object mem : view.getUnionMember()) 
	    {
		if (mem instanceof PredicateViews.View.Query)
		    {
			unionMembers.
			    addLast(convert((PredicateViews.View.Query)mem));
		    }
		else if (mem instanceof PredicateViews.View.Table)
		    {
			unionMembers.
			    addLast(convert((PredicateViews.View.Table)mem));
		    }
		else 
		    throw new Error("Bad kind of union member: " + mem);

	    }; // for (Object mem : view.getUnionMember()) 
	


	return new PredicateView(predName,arity,unionMembers);

    } // convert(PredicateViews.View view)




    private PredicateView.Query convert(PredicateViews.View.Query query) {
	
	boolean containsNonSpace = false;

	for (byte c : query.getBody().getBytes())
	    if (c != ' ')
		{
		    containsNonSpace = true;
		    break;
		};
	
	if (!containsNonSpace) 
	    throw new Error("Empty query in a predicate view.");

	
	return new PredicateView.Query(query.getBody(),query.getExportedAttribute());

    } // convert(PredicateViews.View.Query query)





    private PredicateView.Table convert(PredicateViews.View.Table table) {
		

	boolean containsNonSpace = false;

	for (byte c : table.getName().getBytes())
	    if (c != ' ')
		{
		    containsNonSpace = true;
		    break;
		};
	
	if (!containsNonSpace) 
	    throw new Error("Empty table name in a predicate view.");


	if (table.getName().contains(" "))
	    throw new Error("Table name in a predicate view contains white space.");
	    
	
	return new PredicateView.Table(table.getName(),table.getExportedAttribute());

    } // convert(PredicateViews.View.Table table)





    
    private TestPredicate convert(PredicateViews.Test testDecl) {
    
	String predName = testDecl.getPredicateName();
	
	if (predName.equals("")) 
	    throw new Error("Bad predicate name in a test predicate declaration: empty string.");

	int arity = testDecl.getArity().intValue();
	
	if (arity <= 0)
	    throw new Error("Bad arity value in a test predicate declaration: " + arity);

	List<JAXBElement<String>> rawTemplate = 
	    testDecl.getTemplateElement();

	LinkedList<String> template = new LinkedList<String>();

	for (JAXBElement<String> el : rawTemplate)
	    if (el.getName().toString().equals("text"))
		{
		    template.addLast(el.getValue());
		}
	    else if (el.getName().toString().equals("parameter"))
		{
		    template.addLast(null);
		}
	    else 
		throw new Error("Bad element in a template: " + el.getName());
	


	return new TestPredicate(predName,arity,template);

    } // convert(PredicateViews.Test testDecl)







    //                   Data:

    private Unmarshaller _unmarshaller;


} // class Parser