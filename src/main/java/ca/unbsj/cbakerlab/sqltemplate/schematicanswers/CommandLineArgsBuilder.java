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
public interface CommandLineArgsBuilder
{
	public String[] buildCommandLine(String[] resourcePaths, String inputFilePath);
}