package com.exa.lang.parsing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exa.buffer.CharReader;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.expression.parsing.Parser.UnknownIdentifierValidation;
import com.exa.lang.expression.TObjectValue;
import com.exa.lang.expression.XPEvaluatorSetup;
import com.exa.lang.parsing.statements.STIf;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;

public class XALParser {
	public static final TObjectValue T_OBJECT_VALUE = new TObjectValue();
	
	private XALLexingRules lexingRules = new XALLexingRules();
	
	private Map<String, ComputingStatement> statements = new HashMap<>();
	
	public XALParser() {
		statements.put("if", new STIf(this));
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(this, cr);
		
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(this, cr, evaluatorSetup, uiv);
		
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script, XPEvaluatorSetup evaluatorSetup) throws ManagedException {
		return parseString(script, evaluatorSetup, (id, context) -> null);
	}
	
	
	public ObjectValue<XPOperand<?>> parseFile(String script, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		CharReader cr = null;

		try {
			cr = CharReader.forFile(script, false);
			Computing computing = new Computing(this, cr, evaluatorSetup, uiv);
			return computing.execute();
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		finally {
			if(cr != null) try { cr.close(); } catch (Exception e2) { e2.printStackTrace(); }
		}
		
	}
	
	public ObjectValue<XPOperand<?>> parseFile(String script, XPEvaluatorSetup evaluatorSetup) throws ManagedException {
		return parseFile(script, evaluatorSetup, (id, context) -> null);
	}
	
	public ObjectValue<XPOperand<?>> parseFile(String script) throws ManagedException {
		
		CharReader cr = null;
		try {
			cr = CharReader.forFile(script, false);
			Computing computing = new Computing(this, cr);
			return computing.execute();
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		finally {
			if(cr != null) try { cr.close(); } catch (Exception e2) { e2.printStackTrace(); }
		}
		
	}
	
	public ObjectValue<XPOperand<?>> object(String scriptFile, String path, XPEvaluator evaluator, VariableContext entityVC) throws ManagedException {
		return Computing.object(this, parseFile(scriptFile), path, evaluator, entityVC);
	}
	
	
	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> rootOV, String path, XPEvaluator evaluator, VariableContext entityVC) throws ManagedException {
		return Computing.object(this, rootOV, path, evaluator, entityVC);
	}

	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> relativeOV, String path, XPEvaluator evaluator, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		return Computing.object(this, relativeOV, path, evaluator, entityVC, libOV);
	}
	
	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> ov, XPEvaluator evaluator, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		return Computing.object(this, ov, evaluator, entityVC, libOV);
	}

	
	public Computing getComputeObjectFormFile(String script, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		CharReader cr;
		try {
			cr = CharReader.forFile(script, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		
		return new Computing(this, cr, evaluatorSetup, uiv);
	}

	public XALLexingRules getLexingRules() {
		return lexingRules;
	}

	public Map<String, ComputingStatement> getStatements() {
		return statements;
	}

	
}


