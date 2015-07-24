
package ca.unbsj.cbakerlab.sqltemplate.querymanager.sail.query_manager;

import java.util.*;

import logic_warehouse_je.*;



/** Input representation of generic answers. */
public class GenericAnswer {

    public GenericAnswer(Collection<? extends InputSyntax.Literal> condition,
			 Collection<? extends InputSyntax.Literal> conclusion) {
	_condition = new LinkedList<InputSyntax.Literal>(condition);
	_conclusion = new LinkedList<InputSyntax.Literal>(conclusion);
    } 

    public Collection<InputSyntax.Literal> condition() {
	return _condition;
    }

    public Collection<InputSyntax.Literal> conclusion() {
	return _conclusion;
    }

    public String toString() {
	String result = "";
	for (InputSyntax.Literal lit : _condition)
	    result += lit + " ";
	result += "|-- ";
	for (InputSyntax.Literal lit : _conclusion)
	    result += lit + " ";
	return result;
    }
    

    private LinkedList<InputSyntax.Literal> _condition;

    private LinkedList<InputSyntax.Literal> _conclusion;
    
} // class GenericAnswer