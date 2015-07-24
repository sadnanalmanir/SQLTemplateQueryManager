/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package ca.unbsj.cbakerlab.sqltemplate.schematicanswers;

/**
 *
 * @author sadnana
 */
public abstract class ResultHandler
{
	public static final ResultHandler IdentityHandler = new ResultHandler()
	{
		@Override
		public String parse(String engineOutput)
		{
			return engineOutput;
		}
	};
	
	public abstract String parse(String engineOutput);
}
