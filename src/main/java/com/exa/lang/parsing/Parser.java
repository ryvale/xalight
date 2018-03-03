package com.exa.lang.parsing;

import java.io.IOException;

import com.exa.buffer.CharReader;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;

public class Parser {
	
	public ObjectValue<XPOperand<?>> parse(CharReader cr, VariableContext vc, XPEvalautorFactory clcEvaluator) throws ManagedException {
		Computing computing = new Computing(cr, vc, clcEvaluator);
		return computing.execute();
	}
	
	
	public ObjectValue<XPOperand<?>> parseString(String script) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(cr, new Computing.StandardXPEvaluatorFactory());
		
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


