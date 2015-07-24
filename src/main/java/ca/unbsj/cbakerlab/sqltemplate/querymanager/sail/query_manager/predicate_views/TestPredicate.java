
package ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views;

import java.util.*;


/** An instance of this class specifies how a predicate is mapped to 
 *  an SQL test expression ("conditional").
 */
public class TestPredicate {


    /** @param predicateName
     *  @param arity > 0
     *  @param template template of an SQL test expression; parameters of the template
     *         are represented as null elements in the collection; fpr example,
     *         the template "(PARAM > PARAM)" will be represented by
     *         [ "(", null, " > ", null, ")" ]; note that the number of parameters 
     *         (nulls in the collection) must coincide with <code>arity</code>
     */
    public TestPredicate(String predicateName,
			 int arity,
			 Collection<String> template) {
	
	assert predicateName != null;
	assert arity > 0;
	assert template != null;
	assert countNulls(template) == arity;

	_predicateName = predicateName;
	_arity = arity;
	_template = new LinkedList<String>(template);
	

    } // TestPredicate(String predicateName,
	

    public String predicateName() { return _predicateName; }
    
    public int arity() { return _arity; }		 
    
    /** Instantiates the template of an SQL test, associated with this object, 
     *  by replacing the parameters with the provided values.		
     *  @param parameters nonnull strings; parameters.size() must be equal 
     *         to this.arity() 
     */
    public String instantiateTemplate(Collection<String> parameters) {

	assert parameters != null;
	assert parameters.size() == arity();

	String result = "";
	
	Iterator<String> param = parameters.iterator();

	for (String el : _template)
	    if (el == null)
		{
		    result += param.next();
		}
	    else 
		result += el;


	return result;
	
    } // instantiateTemplate(Collection<String> parameters)


    
    public String toString() {

	String result = _predicateName + "(";
	
	LinkedList<String> parameters = new LinkedList<String>();
	
	for (int i = 0; i < _arity; ++i)
	    {
		result += "X" + i;
		if (i + 1 < _arity) result += ",";
		parameters.addLast("X" + i);
	    };
	
	result += ") :- " + instantiateTemplate(parameters);

	return result;

    } // toString()


    

    private int countNulls(Collection c) {
	int result = 0;
	for (Object obj : c)			
	    if (obj == null) ++result;
	return result;
    }

    
    private String _predicateName;

    private int _arity;
    
    private LinkedList<String> _template;
    
} // class TestPredicate