package com.exa.lang.parsing.statements;

import java.util.Map;

import com.exa.buffer.CharReader;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.expression.parsing.Parser;

import com.exa.lang.expression.XALCalculabeValue;
import com.exa.lang.parsing.Computing;
import com.exa.lang.parsing.ComputingStatement;
import com.exa.lang.parsing.XALLexingRules;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class STIf implements ComputingStatement {
	
	private XALParser parser;
	
	
	public STIf(XALParser parser) {
		super();
		this.parser = parser;
	}

	@Override
	public ObjectValue<XPOperand<?>> compileObject(Computing computing, String context) throws ManagedException {
		final CharReader charReader = computing.getCharReader();
		final XALLexingRules lexingRules = computing.getParser().getLexingRules();
		final Parser xpCompiler = computing.getXpCompiler();
		
		Character ch = lexingRules.nextNonBlankChar(charReader);
		if(ch == null || ch != '(') throw new ManagedException("'(' expected after if statement");
		
		XPOperand<?> xpCondition = xpCompiler.parse(charReader, (lr, cr) -> true, context);
		
		ch = lexingRules.nextNonBlankChar(charReader);
		if(ch == null || ')' != ch) throw new ManagedException("')' expected after if statement");
		
		Value<?, XPOperand<?>> v = computing.readPropertyValueForObject(context);
		ObjectValue<XPOperand<?>> res = v.asObjectValue();
		if(res == null) {
			res = new ObjectValue<>();
			res.setAttribut(Computing.PRTY_THEN, v);
		}
		else {
			if(res.containsAttribut(Computing.PRTY_OBJECT)) {
				ObjectValue<XPOperand<?>> realRes = new ObjectValue<>();
				realRes.setAttribut(Computing.PRTY_THEN, res);
				res = realRes;
			}
		}
		res.setAttribut(Computing.PRTY_STATEMENT, "if");
		res.setAttribut(Computing.PRTY_CONDITION, Computing.calculableFor(xpCondition, "now"));
		
		return res;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, XPEvaluator evaluator, VariableContext ovc, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		Value<?, XPOperand<?>> vlCond = ov.getAttribut(Computing.PRTY_CONDITION);
		
		Boolean res;
		CalculableValue<?, XPOperand<?>> cl = vlCond.asCalculableValue();
		if(cl == null) res = vlCond.asRequiredBoolean();
		else {
			XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
			if(xalCL.getVariableContext() == null) xalCL.setVariableContext(ovc);
			xalCL.setEvaluator(evaluator);
			
			res = xalCL.asRequiredBoolean();
		}
		
		//res = ov.getRequiredAttributAsBoolean(Computing.PRTY_CONDITION);
		
		Map<String, ?> mp = ov.getValue();
		if(res) {
			
			if(mp.containsKey(Computing.PRTY_THEN))
				return Computing.value(parser, ov.getRequiredAttribut(Computing.PRTY_THEN), evaluator, ovc, libOV);
			
			if(mp.containsKey(Computing.PRTY_ELSE)) mp.remove(Computing.PRTY_ELSE);
			return ov;
		}
		
		if(mp.containsKey(Computing.PRTY_ELSE))
			return Computing.value(parser, ov.getRequiredAttribut(Computing.PRTY_ELSE), evaluator, ovc, libOV);
		
		return null;
	}

	
}
