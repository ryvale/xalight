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
		
		ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) throw new ManagedException(String.format("Unexpected end of file in if statement after ')' in context %s", context));
		
		XPOperand<?> xpName = null;
		String str;
		if(ch == '_') {
			str = lexingRules.nextNonNullString(charReader);
			if(!Computing.PRTY_NAME.equals(str)) throw new ManagedException(String.format("Syntax error in if statement after ')' in context %s. Exepected %s instead of %s", context, Computing.PRTY_NAME, str));
			
			ch = lexingRules.nextNonBlankChar(charReader);
			if(ch == null || ch != '=') throw new ManagedException(String.format("Syntax error in if statement after '%s' in context %s. Exepected '='", Computing.PRTY_NAME, context));
			
			xpName = xpCompiler.parse(charReader, (lr, cr) -> true, context);
		}
		
		ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) throw new ManagedException(String.format("Unexpected end of file in if statement in context %s. The statement doesn't have body;", context));
		
		Value<?, XPOperand<?>> v;
		if(ch == ':') {
			lexingRules.nextNonBlankChar(charReader);
			XPOperand<?> xp = xpCompiler.parse(charReader, (lr, cr) -> true, context);
			v = Computing.calculableFor(xp, "now");
		}
		else v = computing.readPropertyValueForObject(context);
		
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
		res.setAttribut(Computing.PRTY_CONTEXT, context);
		res.setAttribut(Computing.PRTY_STATEMENT, "if");
		res.setAttribut(Computing.PRTY_CONDITION, Computing.calculableFor(xpCondition, "now"));
		if(xpName != null) res.setAttribut(Computing.PRTY_NAME, Computing.calculableFor(xpName, "now"));
		
		return res;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, XPEvaluator evaluator, VariableContext ovc, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		Value<?, XPOperand<?>> vlCond = ov.getAttribut(Computing.PRTY_CONDITION);
		
		Boolean cond;
		CalculableValue<?, XPOperand<?>> cl = vlCond.asCalculableValue();
		if(cl == null) cond = vlCond.asRequiredBoolean();
		else {
			XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
			if(xalCL.getVariableContext() == null) xalCL.setVariableContext(ovc);
			xalCL.setEvaluator(evaluator);
			
			cond = xalCL.asRequiredBoolean();
		}
		
		//res = ov.getRequiredAttributAsBoolean(Computing.PRTY_CONDITION);
		Value<?, XPOperand<?>> vlName = ov.getAttribut(Computing.PRTY_NAME);
		ArrayValue<XPOperand<?>> avRes = new ArrayValue<>();
		ObjectValue<XPOperand<?>> finalRes = new ObjectValue<>();
		
		Map<String, ?> mp = ov.getValue();
		if(cond) {
			
			if(mp.containsKey(Computing.PRTY_THEN)) {
				Value<?, XPOperand<?>> res = ov.getRequiredAttribut(Computing.PRTY_THEN);
				
				if(vlName == null) {
				
					ObjectValue<XPOperand<?>> ovRes = res.asObjectValue();
					
					finalRes.setAttribut(Computing.PRTY_INSERTION, ovRes == null ? Computing.VL_VALUE : Computing.VL_INCORPORATE);
					avRes.add(Computing.value(parser, res, evaluator, ovc, libOV));
				}
				else {
					XALCalculabeValue<String> xalCLName = (XALCalculabeValue<String>) vlName;
					if(xalCLName.getVariableContext() == null) xalCLName.setVariableContext(ovc);
					xalCLName.setEvaluator(evaluator);
					
					ObjectValue<XPOperand<?>> ovRes = new ObjectValue<>();
					ovRes.setAttribut(xalCLName.getValue(), Computing.value(parser, res, evaluator, ovc, libOV));
					avRes.add(ovRes);
					finalRes.setAttribut(Computing.PRTY_INSERTION, Computing.VL_INCORPORATE);
				}
			}
			else {
				if(mp.containsKey(Computing.PRTY_ELSE)) mp.remove(Computing.PRTY_ELSE);
				mp.remove(Computing.PRTY_STATEMENT);
				mp.remove(Computing.PRTY_CONDITION);
				finalRes.setAttribut(Computing.PRTY_INSERTION, Computing.VL_INCORPORATE);
				
				if(vlName == null) {
					
					avRes.add(ov);
				}
				else {
					XALCalculabeValue<String> xalCLName = (XALCalculabeValue<String>) vlName;
					if(xalCLName.getVariableContext() == null) xalCLName.setVariableContext(ovc);
					xalCLName.setEvaluator(evaluator);
					
					ObjectValue<XPOperand<?>> ovRes = new ObjectValue<>();
					
					ovRes.setAttribut(xalCLName.getValue(), Computing.value(parser, ov, evaluator, ovc, libOV));
					avRes.add(ovRes);
				}
			}
			
			finalRes.setAttribut(Computing.PRTY_VALUE, avRes);
			
			return finalRes;
		}
		
		if(mp.containsKey(Computing.PRTY_ELSE)) {
			Value<?, XPOperand<?>> res = ov.getRequiredAttribut(Computing.PRTY_ELSE);
			
			if(vlName == null) {
				ObjectValue<XPOperand<?>> ovRes = res.asObjectValue();
				
				finalRes.setAttribut(Computing.PRTY_INSERTION, ovRes == null ? Computing.VL_VALUE : Computing.VL_INCORPORATE);
				
				avRes.add(Computing.value(parser, res, evaluator, ovc, libOV));
			}
			else {
				XALCalculabeValue<String> xalCLName = (XALCalculabeValue<String>) vlName;
				if(xalCLName.getVariableContext() == null) xalCLName.setVariableContext(ovc);
				xalCLName.setEvaluator(evaluator);
				
				ObjectValue<XPOperand<?>> ovRes = new ObjectValue<>();
				ovRes.setAttribut(xalCLName.getValue(), Computing.value(parser, res, evaluator, ovc, libOV));
				avRes.add(ovRes);
				finalRes.setAttribut(Computing.PRTY_INSERTION, Computing.VL_INCORPORATE);
			}
			
			finalRes.setAttribut(Computing.PRTY_VALUE, avRes);
			
			return finalRes;
		}
		
		return null;
	}

	
}
