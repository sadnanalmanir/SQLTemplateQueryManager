/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.unbsj.cbakerlab.sqltemplate.cmdlineutils;

import java.io.*;

/**
 *
 * @author sadnana
 */
public class IOUtil {
  private final static File tempDir;
	
	static
	{
		//String tempDirPath = System.getProperty("java.io.tmpdir").concat("/PSOATransRun");
        String tempDirPath = System.getProperty("java.io.tmpdir").concat("/SQLTemplateDir");
		tempDir = new File(tempDirPath);
		tempDir.mkdir();
		//tempDir.deleteOnExit();
	}
	
	public static File tmpFile(String prefix)
	{
		return tmpFile(prefix, null);
	}
	
	public static File tmpFile(String prefix, String suffix)
	{
		try
		{
			File tmpFile = File.createTempFile(prefix, suffix, tempDir);
			//tmpFile.deleteOnExit();
			return tmpFile;
		}
		catch (IOException e)
		{
			throw runtimeIOException(e);
		}
	}
	
	public static void writeStringToFile(File f, String content)
	{
		PrintWriter w = writer(f);
		w.print(content);
		w.close();
	}
	
	public static File tmpFileWithContent(String prefix, String suffix, String content)
	{
		File f = tmpFile(prefix, suffix);
		writeStringToFile(f, content);
		return f;
	}
	
	public static File extractFromResource(ClassLoader loader, String resource)
	{
		File f = new File(tempDir, resource);
		InputStream in = loader.getResourceAsStream(resource);
                if(in == null)
                    System.out.println("cannot get resource : "+resource);
		
		try
		{
			try
			{
				FileOutputStream out = new FileOutputStream(f, false);
				try
				{
					byte[] buffer = new byte[4096];
					int len;
					
					while ((len = in.read(buffer)) != -1)
					{
						out.write(buffer, 0, len);
					}
					
					//f.deleteOnExit();
					return f;
				}
				finally
				{
					out.close();
                                        
				}
			}
			finally
			{
				in.close();
			}
		}
		catch (IOException e)
		{
			throw runtimeIOException(e);
		}
	}
		
	public static PrintWriter writer(File f) {
		try
		{
			return new PrintWriter(f);
		}
		catch (IOException e)
		{
			throw runtimeIOException(e);
		}
	}
	
	public static AutoGenerateSADIException runtimeIOException(IOException e)
	{
		return new AutoGenerateSADIException("I/O Error", e);
	}
	
	public static void print(PrintStream output, Object... objs)
	{
		for (Object obj : objs) {
			System.out.print(obj);
		}
	}
	
	public static void print(Object... objs)
	{
		print(System.out, objs);
	}
	
	public static void println(PrintStream output, Object... objs)
	{
		print(output, objs);
		output.println();
	}
	
	public static void println(Object... objs)
	{
		println(System.out, objs);
	}
	
	public static void printErr(Object... objs)
	{
		print(System.err, objs);
	}
	
	public static void printErrln(Object... objs)
	{
		println(System.err, objs);
	}
	
	public static void printErrlnAndExit(Object... objs)
	{
		println(System.err, objs);
		System.exit(1);
	}
}
