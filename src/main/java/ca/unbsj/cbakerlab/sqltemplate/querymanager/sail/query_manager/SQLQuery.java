
package ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager;


import java.util.*; 

import logic_warehouse_je.*;

import cushion_je.*;

/** Representation of SQL queries, obtained by converting generic answers,
 *  combined with the corresponding mappings of original query variables.
 *  The mapping should be used to convert answers to the SQL queries
 *  into answers to the original queries. This is done by instantiating
 *  the variables in the terms in the mapping with the values assigned
 *  to the variables in the answer set.
 */
public class SQLQuery {

    
    public SQLQuery(String queryText,
		    Collection<Pair<String,String>> SQLViews,
		    Map<String,InputSyntax.Term> queryVarMap) 
    {
	_queryText = queryText;

	if (SQLViews == null)
	    SQLViews = new LinkedList<Pair<String,String>>();
	_SQLViews = new LinkedList<Pair<String,String>>(SQLViews);
	_queryVarMap = 
	    new HashMap<String,InputSyntax.Term>(queryVarMap);
    }
		

    public String queryText() { return _queryText; }

    /** Views that have to be defined before the query is computed,
     *  and dropped afterwards; the first element of a pair is the view 
     *  name and the second element is the view query.
     */
    public List<Pair<String,String>> SQLViews() {
	return _SQLViews;
    }
    
    public HashMap<String,InputSyntax.Term> queryVarMap() {
	return _queryVarMap;
    }

    public String toString() {
	String result = _queryText + "|||";
	for (Pair<String,String> p : _SQLViews)
	    {
		result += " VIEW " + p.first + " := " + p.second;
	    };

	return result + " ||| " + _queryVarMap;
    }

    public String toStringAsXML() {
	String result = "<sail_generated_sql_query>\n";
	result += "  <query_text>\n" + _queryText + "\n  </query_text>\n";
	result += "  <sql_views>\n";
	for (Pair<String,String> p : _SQLViews)
	    {
		result += "    <view name=\"" + p.first + "\">\n";
		result += "      " + p.second + "\n";
		result += "    </view>\n";
	    };
	result += "  </sql_views>\n";

	result += "  <var_map>\n";
	result += _queryVarMap + "\n"; // to be refined
	result += "  </var_map>\n";

	return result + "</sail_generated_sql_query>\n";
    } // toStringAsXML()


    private String _queryText;

    private LinkedList<Pair<String,String>> _SQLViews;

    private HashMap<String,InputSyntax.Term> _queryVarMap;

} // class SQLQuery