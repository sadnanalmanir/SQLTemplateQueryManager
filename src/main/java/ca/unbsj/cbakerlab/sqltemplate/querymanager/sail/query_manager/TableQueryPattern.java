
package ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager;

import java.util.*;


/** Representation of special patterns that can be used to specify
 *  a relation between a database abstraction predicate and a table;
 *  a TableQueryPattern essentially represents a small query to the DB
 *  whose result defines the DB abstraction predicate.
 */
public class TableQueryPattern {

    public TableQueryPattern(String tableName,
			     Collection<String> exportedAttributes,
			     Map<String,String> attributeValueConstraints)
    {
	_tableName = tableName;
	_exportedAttributes = new LinkedList<String>(exportedAttributes);
	_attributeValueConstraints = 
	    new TreeMap<String,String>(attributeValueConstraints);
    }

    
    public String tableName() { return _tableName; }
    
    public List<String> exportedAttributes() { 
	return _exportedAttributes;
    }

    public Map<String,String> attributeValueConstraints() {
	return _attributeValueConstraints;
    }

    public String toString() {
	String result = _tableName + "(";
	Iterator<String> iter1 = _exportedAttributes.iterator();
	if (iter1.hasNext())
	    {
		result += iter1.next();
		while (iter1.hasNext())
		    result += ", " + iter1.next();
	    };

	Iterator<Map.Entry<String,String>> iter2 = 
	    _attributeValueConstraints.entrySet().iterator();
	
	if (iter2.hasNext())
	    {
		Map.Entry<String,String> entry = iter2.next();

		result += " | " + entry.getKey() + "=" + entry.getValue();
	
		while (iter2.hasNext())
		    result += ", " + entry.getKey() + "=" + entry.getValue();
	    };

	return result + ")";

    } // toString()

    private String _tableName;
    
    private LinkedList<String> _exportedAttributes;

    private TreeMap<String,String> _attributeValueConstraints;

} // class TableQueryPattern