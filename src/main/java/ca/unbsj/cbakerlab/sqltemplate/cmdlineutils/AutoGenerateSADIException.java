/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.unbsj.cbakerlab.sqltemplate.cmdlineutils;

/**
 *
 * @author sadnana
 */
public class AutoGenerateSADIException extends RuntimeException
{
	
	public AutoGenerateSADIException()
	{
		super();
	}
	
	public AutoGenerateSADIException(String msg)
	{
		super(msg);
	}
	
	public AutoGenerateSADIException(Exception e)
	{
		super(e);
	}
	
	public AutoGenerateSADIException(String msg, Exception e)
	{
		super(msg, e);
	}
}