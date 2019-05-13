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
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class STName implements ComputingStatement {
	
	private XALParser parser;
	
	public STName(XALParser parser) {
		super();
		this.parser = parser;
	}
	
	@Override
	public ObjectValue<XPOperand<?>> compileObject(Computing computing, String context) throws ManagedException {
		final CharReader charReader = computing.getCharReader();
		final XALLexingRules lexingRules = computing.getParser().getLexingRules();
		final Parser xpCompiler = computing.getXpCompiler();
		
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) throw new ManagedException(String.format("Unexpected end of file. '\"', ''', '=' expected after name statement in context %s", context));
		if(ch != '"' && ch != '\'' && ch != '=') throw new ManagedException(String.format("'\"', ''', '=' expected after name statement in context %s", context));
		
		Value<?, XPOperand<?>> vlName;
		
		if(ch == '"' || ch == '\'') {
			//lexingRules.nextNonBlankChar(charReader);
			vlName = computing.readString(ch.toString());
		}
		else {
			lexingRules.nextNonBlankChar(charReader);
			XPOperand<?> xpName = xpCompiler.parse(charReader, (lr, cr) -> true, context);
			
			if(!"string".equals(xpName.type().typeName())) throw new ManagedException(String.format("The expression should have a string type after name statement in context %s", context));
			
			vlName = Computing.calculableFor(xpName, "now");
		}
		
		ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) throw new ManagedException(String.format("Unexpected end of file in name statement in context %s. The statement doesn't have body;", context));
		
		Value<?, XPOperand<?>> vlValue;
		if(ch == ':') {
			lexingRules.nextNonBlankChar(charReader);
			XPOperand<?> xp = xpCompiler.parse(charReader, (lr, cr) -> true, context);
			vlValue = Computing.calculableFor(xp, "now");
		}
		else vlValue = computing.readPropertyValueForObject(context);
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		
		//res.setAttribut(Computing.PRTY_INSERTION, Computing.VL_INCORPORATE);
		res.setAttribut(Computing.PRTY_STATEMENT, "name");
		res.setAttribut(Computing.PRTY_NAME, vlName);
		res.setAttribut(Computing.PRTY_CONTEXT, context);
		res.setAttribut(Computing.PRTY_VALUE, vlValue);
		
		return res;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, XPEvaluator evaluator, VariableContext ovc, Map<String, ObjectValue<XPOperand<?>>> libOV, String cmd) throws ManagedException {
		//
		Value<?, XPOperand<?>> vlName = ov.getRequiredAttribut(Computing.PRTY_NAME);
		
		CalculableValue<?, XPOperand<?>> cl =  vlName.asCalculableValue();
		String name;
		if(cl == null) name= vlName.asRequiredString();
		else {
			XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
			if(xalCL.getVariableContext() == null) xalCL.setVariableContext(ovc);
			xalCL.setEvaluator(evaluator);
			
			name = xalCL.asString();
		}
		
		Value<?, XPOperand<?>> vlValue = ov.getRequiredAttribut(Computing.PRTY_VALUE);
		cl = vlValue.asCalculableValue();
		if(cl != null) {
			XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
			if(xalCL.getVariableContext() == null) xalCL.setVariableContext(ovc);
			xalCL.setEvaluator(evaluator);
		}
		
		ObjectValue<XPOperand<?>> finalRes = new ObjectValue<>();
		finalRes.setAttribut(Computing.PRTY_INSERTION, Computing.VL_INCORPORATE);
		
		finalRes.setAttribut(Computing.PRTY_CONTEXT, ov.getAttribut(Computing.PRTY_CONTEXT));
		ArrayValue<XPOperand<?>> avRes = new ArrayValue<>();
		
		ObjectValue<XPOperand<?>> ovIncorporate = new ObjectValue<>();
		ovIncorporate.setAttribut(name, vlValue);
		avRes.add(ovIncorporate);
		
		finalRes.setAttribut(Computing.PRTY_VALUE, avRes);
		
		return finalRes;
	}

}
