package com.exa.lang.parsing;

import java.io.IOException;

import com.exa.buffer.CharReader;
import com.exa.expression.XPOperand;
import com.exa.lang.parsing.Computing.EvaluatorSetup;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;

public class Parser {	
	
	public ObjectValue<XPOperand<?>> parseString(String script) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(cr, new Computing.StandardXPEvaluatorFactory());
		
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script, EvaluatorSetup evaluatorSetup) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(cr, evaluatorSetup);
		
		return computing.execute();
	}
	
	
	public ObjectValue<XPOperand<?>> parseFile(String script, EvaluatorSetup evaluatorSetup) throws ManagedException {
		CharReader cr;
		try {
			cr = CharReader.forFile(script, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		
		Computing computing = new Computing(cr, evaluatorSetup);
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseFile(String script) throws ManagedException {
		
		CharReader cr;
		try {
			cr = CharReader.forFile(script, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		Computing computing = new Computing(cr, new Computing.StandardXPEvaluatorFactory());
		return computing.execute();
	}
	
	


}


