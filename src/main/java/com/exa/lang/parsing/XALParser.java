package com.exa.lang.parsing;

import java.io.IOException;
import java.util.Map;

import com.exa.buffer.CharReader;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.expression.parsing.Parser.UnknownIdentifierValidation;
import com.exa.lang.parsing.Computing.EvaluatorSetup;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;

public class XALParser {	
	
	public ObjectValue<XPOperand<?>> parseString(String script) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(cr);
		
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script, EvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(cr, evaluatorSetup, uiv);
		
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script, EvaluatorSetup evaluatorSetup) throws ManagedException {
		return parseString(script, evaluatorSetup, (id, context) -> null);
	}
	
	
	public ObjectValue<XPOperand<?>> parseFile(String script, EvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		CharReader cr;
		try {
			cr = CharReader.forFile(script, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		
		Computing computing = new Computing(cr, evaluatorSetup, uiv);
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseFile(String script, EvaluatorSetup evaluatorSetup) throws ManagedException {
		return parseFile(script, evaluatorSetup, (id, context) -> null);
	}
	
	public ObjectValue<XPOperand<?>> parseFile(String script) throws ManagedException {
		
		CharReader cr;
		try {
			cr = CharReader.forFile(script, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		Computing computing = new Computing(cr);
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> object(String scriptFile, String path, XPEvaluator evaluator) throws ManagedException {
		
		CharReader cr;
		try {
			cr = CharReader.forFile(scriptFile, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		Computing computing = new Computing(cr);
		return computing.object(path, evaluator);
	}
	
	
	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> rootOV, String path, XPEvaluator evaluator) throws ManagedException {
		
		
		return Computing.object(rootOV, path, evaluator);
	}

	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> relativeOV, String path, XPEvaluator evaluator, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {

		return Computing.object(relativeOV, path, evaluator, libOV);
	}
	
	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> ov, XPEvaluator evaluator, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {

		return Computing.object(ov, evaluator, libOV);
	}


}


