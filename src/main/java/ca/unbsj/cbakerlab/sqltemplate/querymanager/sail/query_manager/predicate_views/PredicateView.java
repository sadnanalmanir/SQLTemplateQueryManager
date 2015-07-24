
package ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager.predicate_views;

import java.util.*;


/** An instance of this class specifies how an intensional predicate is defined
 *  in SQL. It consists of two parts: an SQL relation-valued expression, 
 *  eg, a table name or a SELECT, and a list of attributes of that expression 
 *  that correspond to the (anonymous positional) arguments of the intensional 
 *  predicate. The order of the attributes is significant: the first one 
 *  corresponds to the first argument of the intensional predicate, the second
 *  one corresponds to the second argument, and so on.
 */
public class PredicateView {

    public static class UnionMember {

	protected UnionMember(Collection<String> exportedAttributes) {
	
	_exportedAttributes = new LinkedList<String>(exportedAttributes);
	}
	
	public List<String> exportedAttributes() { 
	    return _exportedAttributes;
	}
	
   
	private LinkedList<String> _exportedAttributes;

    } // class UnionMember


    public static class Query extends UnionMember {
	
	public Query(String body,Collection<String> exportedAttributes) {
	    super(exportedAttributes);
	    _body = body;
	}

	public String body() { return _body; }

	private String _body;
	
    } // class Query


    public static class Table extends UnionMember {
	
	public Table(String name,Collection<String> exportedAttributes) {
	    super(exportedAttributes);
	    _name = name;
	}

	public String name() { return _name; }

	private String _name;
	
    } // class Table


    




    /** @param predicateName
     *  @param arity > 0
     *  @param unionMembers SQL relation-valued expressions -- SELECTs or 
     *         just table names -- with distinguished attributes that
     *         correspond to arguments of the predicate being defined;
     *         the number of attribues in each union member should 
     *         be equal to <code>arity</code>
     */
    public PredicateView(String predicateName,
			 int arity,
			 Collection<UnionMember> unionMembers) {
	assert predicateName != null;
	assert unionMembers != null;
	assert !unionMembers.isEmpty();
	
	for (UnionMember mem : unionMembers)
	    {
		assert(mem.exportedAttributes().size() == arity);
	    };
	_predicateName = predicateName;
	_arity = arity;
	_unionMembers = new LinkedList(unionMembers);

    } // PredicateView(String relation,


    public String predicateName() { return _predicateName; }
    
    public int arity() { return _arity; }

    public List<UnionMember> unionMembers() {
	return _unionMembers;
    }
    
    
    /** Divides the predicate view into several objects, each containing
     *  just one member of the union in the original object.
     */
    public List<PredicateView> splitUnion() {
	
	LinkedList<PredicateView> result = new LinkedList<PredicateView>();

	for (UnionMember mem : _unionMembers)
	    {
		LinkedList<UnionMember> singleton = 
		    new LinkedList<UnionMember>();
		
		singleton.add(mem);

		result.addLast(new PredicateView(_predicateName,
						 _arity,
						 singleton));
	    };
	
	return result;

    } // splitUnion() 



    public String toString() {

	String result = "";
	
	for (UnionMember mem : _unionMembers)	
	    {
		result += _predicateName + "(";
	
		for (int i = 0; i < _arity; ++i)
		    {
			result += "X" + i;
			if (i + 1 < _arity) result += ",";
		    };
		
		result += ") :- select ";
		
		for (int i = 0; i < mem.exportedAttributes().size(); ++i)
		    {
			result += mem.exportedAttributes().get(i) + " X" + i;
			if (i + 1 < mem.exportedAttributes().size()) result += ", ";
		    };       
		
		result += " from ";
		
		if (mem instanceof Table) 
		    {
			result += ((Table)mem).name();
		    }
		else 
		    result += ((Query)mem).body();
		    
		result += "\n.";
		
	    }; // for (UnionMember mem : _unionMembers)

	return result;

    } // toString()

    
    private String _predicateName;

    private int _arity;

    private LinkedList<UnionMember> _unionMembers;

} // class PredicateView 

