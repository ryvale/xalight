package com.exa.lang.parsing.statements;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.exa.buffer.CharReader;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.MapVariableContext;
import com.exa.expression.eval.XPEvaluator;
import com.exa.expression.parsing.Parser;
import com.exa.lang.parsing.Computing;
import com.exa.lang.parsing.ComputingStatement;
import com.exa.lang.parsing.XALLexingRules;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class STFor implements ComputingStatement {
	
	private XALParser parser;
	
	
	public STFor(XALParser parser) {
		super();
		this.parser = parser;
	}

	@Override
	public ObjectValue<XPOperand<?>> compileObject(Computing computing, String context) throws ManagedException {
		final CharReader charReader = computing.getCharReader();
		final XALLexingRules lexingRules = computing.getParser().getLexingRules();
		final Parser xpCompiler = computing.getXpCompiler();
		final XPEvaluator evaluator = xpCompiler.evaluator();
		
		Character ch = lexingRules.nextNonBlankChar(charReader);
		if(ch == null || ch != '(') throw new ManagedException("'(' expected after for statement");
		
		String type = lexingRules.nextString(charReader);
		if(type == null) throw new ManagedException("Unexpected end of file in for statement. type or variable expected");
		
		String var;
		if(computing.getTypeSolver().containsType(type)) {
			var = lexingRules.nextNonNullString(charReader);
		}
		else {
			var = type;
			type = "auto";
		}
		if(!lexingRules.isIdentifier(var)) throw new ManagedException("Identifier expected after for statement");
		
		ch = lexingRules.nextForwardNonBlankChar(charReader);
		
		if('i' == ch) {
			String str = lexingRules.nextString(charReader);
			if(str == null || !"in".equals(str)) throw new ManagedException("'in' expected in for statement");
			
			ch = lexingRules.nextNonBlankChar(charReader);
			
			if('[' == ch) {
				ArrayValue<XPOperand<?>> values = computing.readArrayBody(context);
				
				List<Value<?, XPOperand<?>>> lstValues = values.getValue();
				
				if(lstValues.size() == 0) {
					if("auto".equals(type)) throw new ManagedException(String.format("Unable to infer variable %s type in for statement", var));
				}
				else {
					Iterator<Value<?, XPOperand<?>>> it = lstValues.iterator();
					Value<?, XPOperand<?>> vl = it.next();
					
					String vlType = computing.getTypeName(vl);
					
					while(it.hasNext()) {
						vl = it.next();
						if(!vlType.equals(computing.getTypeName(vl))) throw new ManagedException("");
					}
					if("auto".equals(type)) type = vlType;
					else if(!type.equals(vlType)) throw new ManagedException("");
				}
				
				ch = lexingRules.nextNonBlankChar(charReader);
				if(ch == null || ')' != ch) throw new ManagedException("Incorrect syntax in for statement; ')' expected");
				
				VariableContext vc = new MapVariableContext();
				vc.addVariable(var, computing.getTypeSolver().getTypeValueClass(type), null);
				evaluator.pushVariableContext(vc);
				
				Value<?, XPOperand<?>> vlDo = computing.readPropertyValueForObject(context);
				
				evaluator.popVariableContext();
				
				ObjectValue<XPOperand<?>> res = new ObjectValue<>();
				res.setAttribut(Computing.PRTY_CONTEXT, context);
				res.setAttribut(Computing.PRTY_STATEMENT, "for");
				res.setAttribut(Computing.PRTY_TYPE, "in");
				
				res.setAttribut("_var", var);
				
				res.setAttribut("_vartype", type);
				
				res.setAttribut("_values", values);
				
				res.setAttribut("_do", vlDo);
				return res;
			}
			
			throw new ManagedException("Incorrect syntax in for statement");
		}
		
		return null;
	}

	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, XPEvaluator evaluator, VariableContext ovc, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		String type = ov.getAttributAsString(Computing.PRTY_TYPE);
		
		if("in".equals(type)) {
			ArrayValue<XPOperand<?>> values = ov.getRequiredAttributAsArrayValue("_values");
			
			String var = ov.getRequiredAttributAsString("_var");
			String varType = ov.getRequiredAttributAsString("_vartype");
			
			VariableContext vc = new MapVariableContext(ovc);
			vc.addVariable(var, evaluator.getClassesMan().getType(varType).valueClass(), null);
			
			ArrayValue<XPOperand<?>> arRes = new ArrayValue<>();
			
			List<Value<?, XPOperand<?>>> lstValues = values.getValue();
			
			Value<?, XPOperand<?>> vlDo = ov.getRequiredAttribut("_do");
			
			for(Value<?, XPOperand<?>> vl : lstValues) {
				vc.assignContextVariable(var, vl.getValue());
				try {
					Value<?, XPOperand<?>> rawItem = vlDo.clone();
					
					Value<?, XPOperand<?>> item = Computing.value(parser, rawItem, evaluator, vc, libOV);
					
					arRes.add(item);
					
				} catch (CloneNotSupportedException e) {
					throw new ManagedException(e);
				}
			}
			
			ObjectValue<XPOperand<?>> res = new ObjectValue<>();
			
			res.setAttribut(Computing.PRTY_INCORPORATE, arRes);
		}
		return null;
	}

}
