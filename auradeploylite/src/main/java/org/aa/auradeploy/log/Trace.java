package org.aa.auradeploy.log;

/**
 * @author Jatin
 *
 * Copyright (C) 2006  Apartech Ltd. Jatin Bhadra

 */
public class Trace {
	
	public static void log (String logString,Class sourceClass){
		
		System.out.println( sourceClass.getName() + " " + logString);
	
	}

}