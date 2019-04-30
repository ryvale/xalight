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
import com.exa.lexing.ParsingException;
import com.exa.utils.ManagedException;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class STIf implements ComputingStatement {

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
		
		ch = lexingRules.nextNonBlankChar(charReader);
		if(ch == null  || '{' != ch) throw new ManagedException("'{' expected after if statement");
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		
		res.setAttribut(Computing.PRTY_STATEMENT, "if");
		res.setAttribut(Computing.PRTY_CONDITION, Computing.calculableFor(xpCondition, "now"));
		
		computing.readObjectBody(res, context);
		
		Map<String, ?> mp = res.getValue();
		if(!mp.containsKey(Computing.PRTY_THEN) && !mp.containsKey(Computing.PRTY_ELSE)) throw new ParsingException(String.format("'%s' or '%s' expected in if statement", Computing.PRTY_THEN, Computing.PRTY_ELSE));
		
		return res;
	}

	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, XPEvaluator evaluator, VariableContext ovc) throws ManagedException {
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
		
		if(res) return ov.getRequiredAttribut(Computing.PRTY_THEN);
		
		return ov.getRequiredAttribut(Computing.PRTY_ELSE);
	}

	
}
