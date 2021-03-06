package com.exa.lang.parsing.statements;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.exa.buffer.CharReader;
import com.exa.expression.Type;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.MapVariableContext;
import com.exa.expression.eval.XPEvaluator;
import com.exa.expression.parsing.Parser;
import com.exa.lang.expression.XALCalculabeValue;
import com.exa.lang.parsing.Computing;
import com.exa.lang.parsing.ComputingStatement;
import com.exa.lang.parsing.XALLexingRules;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class STFor implements ComputingStatement {

	@Override
	public ObjectValue<XPOperand<?>> compileObject(Computing computing, String context) throws ManagedException {
		final CharReader charReader = computing.getCharReader();
		final XALLexingRules lexingRules = computing.getParser().getLexingRules();
		final Parser xpCompiler = computing.getXpCompiler();
		final XPEvaluator evaluator = xpCompiler.evaluator();
		
		Character ch = lexingRules.nextNonBlankChar(charReader);
		if(ch == null || ch != '(') throw new ManagedException(String.format("'(' expected after for statement in context %s", context));
		
		String var = lexingRules.nextString(charReader);
		if(var == null) throw new ManagedException(String.format("Unexpected end of file in for statement in context %s . type or variable expected", context));
		if(!lexingRules.isIdentifier(var)) throw new ManagedException(String.format("Identifier expected after for statement in context %s", context));
		
		String type;
		
		ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == '@') {
			lexingRules.nextNonBlankChar(charReader);
			type = lexingRules.nextNonNullString(charReader);
			
			if(!computing.getTypeSolver().containsType(type)) throw new ManagedException(String.format("Invalid type '%s' in for statement in context '%s'", type, context));
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
		}
		else type = "auto";
		
		Value<?, XPOperand<?>> values;
		String insertion;
		XALCalculabeValue<?> xclName = null;
		
		if('i' == ch) {
			String str = lexingRules.nextNonNullString(charReader);
			if(str == null || !"in".equals(str)) throw new ManagedException(String.format("'in' expected in for statement in context %s", context));
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if('[' == ch) {
				lexingRules.nextNonBlankChar(charReader);
				ArrayValue<XPOperand<?>> vvalues = computing.readArrayBody(context);
				values = vvalues;
				
				List<Value<?, XPOperand<?>>> lstValues = vvalues.getValue();
				
				if(lstValues.size() == 0) {
					if("auto".equals(type)) type = "string"; //throw new ManagedException(String.format("Unable to infer variable %s type in for statement in context %s", var, context));
				}
				else {
					Iterator<Value<?, XPOperand<?>>> it = lstValues.iterator();
					Value<?, XPOperand<?>> vl = it.next();
					
					String vlType = computing.getTypeName(vl);
					
					while(it.hasNext()) {
						vl = it.next();
						if(!vlType.equals(computing.getTypeName(vl))) throw new ManagedException(String.format("Unable to infer variable %s type in for statement in context %s . Somme array items have different types", var, context));
					}
					if("auto".equals(type)) type = vlType;
					else if(!type.equals(vlType)) throw new ManagedException(String.format("The declared variable %s type (%s) in for statement is different from array items type (%s) in context %s.", var, type, vlType, context));
				}
			}
			else {
				values = computing.readExpression(context);
				if(!"ArrayValue".equals(values.typeName())) throw new ManagedException(String.format("The expression in for statement should be an array in context '%s'", context));
				if("auto".equals(type)) type = "string";
			}
			
			ch = lexingRules.nextNonBlankChar(charReader);
			if(ch == null || ')' != ch) throw new ManagedException(String.format("')' expected after for statement in context %s", context));
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			if(ch == null) throw new ManagedException(String.format("Unexpected end of file in for statement after ')' in context %s", context));
			
			Type<?> tp = evaluator.getClassesMan().getType(type);
			if(tp == null) throw new ManagedException(String.format("Non managed type % n for statement in context %s", type, context));
			
			VariableContext vc = new MapVariableContext(evaluator.getCurrentVariableContext());
			vc.addVariable(var, tp.valueClass(), null);
			
			evaluator.pushVariableContext(vc);
			
			ObjectValue<XPOperand<?>> res = new ObjectValue<>();
			
			boolean inArray = context.endsWith("]");
			
			if(ch == '_') {
				str = lexingRules.nextNonNullString(charReader);
				if(!Computing.PRTY_NAME.equals(str)) throw new ManagedException(String.format("Syntax error in for statement after ')' in context %s. Exepected %s instead of %s", context, Computing.PRTY_NAME, str));
			
				ch = lexingRules.nextNonBlankChar(charReader);
				if(ch == null || ch != '=') throw new ManagedException(String.format("Syntax error in for statement after '%s' in context %s. Exepected '='", Computing.PRTY_NAME, context));
				
				XPOperand<?> xpName = xpCompiler.parse(charReader, (lr, cr) -> true, context);
				
				xclName = Computing.calculableFor(xpName, "now");
				
				insertion = inArray ? Computing.VL_ARRAY : Computing.VL_INCORPORATE;
				
				//res.setAttribut(Computing.PRTY_INSERTION, inArray ? Computing.VL_ARRAY : Computing.VL_INCORPORATE);
				//res.setAttribut(Computing.PRTY_NAME, Computing.calculableFor(xpName, "now"));
			}
			else {
				insertion = Computing.VL_ARRAY;
				//res.setAttribut(Computing.PRTY_INSERTION, Computing.VL_ARRAY);
			}
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			if(ch == null) throw new ManagedException(String.format("Unexpected end of file in for statement in context %s. The statement doesn't have body;", context));
			
			Value<?, XPOperand<?>> vlDo = null;
			if(ch == ':') {
				lexingRules.nextNonBlankChar(charReader);
				XPOperand<?> xp = xpCompiler.parse(charReader, (lr, cr) -> true, context);
				vlDo = Computing.calculableFor(xp, "now");
			}
			else if(ch == '{' || ch == '@' || ch == '[' || ch == '=' || ch == '"' || ch == '\'') {
				vlDo = computing.readPropertyValueForObject(context);
			}
			
			
			/*VariableContext vc = new MapVariableContext();
			vc.addVariable(var, computing.getTypeSolver().getTypeValueClass(type), null);
			evaluator.pushVariableContext(vc);*/
			
			evaluator.popVariableContext();
			
			
			if(xclName != null) {
				if(Computing.VL_ARRAY.equals(insertion)) {
					ObjectValue<XPOperand<?>> newVlDo = new ObjectValue<>();
					newVlDo.setAttribut(Computing.PRTY_NAME, xclName);
					newVlDo.setAttribut(Computing.PRTY_VALUE, vlDo);
					vlDo = newVlDo;
					
				}
				else res.setAttribut(Computing.PRTY_NAME, xclName);
				
			}
			else res.setAttribut("_do", vlDo);
			res.setAttribut(Computing.PRTY_CONTEXT, context);
			res.setAttribut(Computing.PRTY_STATEMENT, "for");
			res.setAttribut(Computing.PRTY_TYPE, "in");
			res.setAttribut(Computing.PRTY_INSERTION, insertion);
			
			res.setAttribut("_var", var);
			
			res.setAttribut("_vartype", type);
			
			res.setAttribut("_values", values);
			res.setAttribut("_do", vlDo);
			
			
			
			return res;
			
			//throw new ManagedException("Incorrect syntax in for statement");
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, Computing excetutedComputing, VariableContext ovc, Map<String, ObjectValue<XPOperand<?>>> libOV, String cmd) throws ManagedException {
		String type = ov.getAttributAsString(Computing.PRTY_TYPE);
		
		if("in".equals(type)) {
			Value<?, XPOperand<?>> vlValues = ov.getRequiredAttribut("_values");
		
			ArrayValue<XPOperand<?>> values = vlValues.asArrayValue();
			if(values == null) {
				XALCalculabeValue<?> clValues = (XALCalculabeValue<?>)vlValues.asCalculableValue();
				excetutedComputing.computeCalculableValue(clValues, ovc);
				values = (ArrayValue<XPOperand<?>>) clValues.getValue();
			}
			
			String var = ov.getRequiredAttributAsString("_var");
			
			ArrayValue<XPOperand<?>> arRes = new ArrayValue<>();
			
			List<Value<?, XPOperand<?>>> lstValues = values.getValue();
			
			Value<?, XPOperand<?>> vlDo = ov.getAttribut("_do");
			
			if(ov.getRequiredAttributAsString(Computing.PRTY_INSERTION).equals(Computing.VL_ARRAY))
				for(Value<?, XPOperand<?>> vl : lstValues) {
					VariableContext vc = new MapVariableContext(ovc);
					vc.assignContextVariable(var, vl.getValue());
					//try {
						Value<?, XPOperand<?>> rawItem = vlDo.clone();
						CalculableValue<?, XPOperand<?>> cl = rawItem.asCalculableValue();
						Value<?, XPOperand<?>> item;
						
						//item = excetutedComputing.value(rawItem, vc, libOV);
						if(cl == null) item = excetutedComputing.value(rawItem, vc, libOV);
						else {
							item = excetutedComputing.computeCalculableValue(cl, vc);
						}
						if(item == null) continue;
						
						arRes.add(item);
						
					/*} catch (CloneNotSupportedException e) {
						throw new ManagedException(e);
					}*/
				}
			else {
				Value<?, XPOperand<?>> vlName = ov.getRequiredAttribut(Computing.PRTY_NAME);
				
				for(Value<?, XPOperand<?>> vl : lstValues) {
					VariableContext vc = new MapVariableContext(ovc);
					vc.assignContextVariable(var, vl.getValue());
					//try {
						
						Value<?, XPOperand<?>> item; CalculableValue<?, XPOperand<?>> cl;
						if(vlDo == null) {
							item = null;
						}
						else {
							Value<?, XPOperand<?>> rawItem = vlDo.clone();
							cl = rawItem.asCalculableValue();
							
							if(cl == null) item = excetutedComputing.value(rawItem, vc, libOV);
							else {
								item = excetutedComputing.computeCalculableValue(cl, vc);
							}
							
							if(item == null) continue;
						}
						
						Value<?, XPOperand<?>> rawItemName = vlName.clone();
						
						String propName;
						cl = rawItemName.asCalculableValue();
						
						XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
						if(xalCL.getVariableContext() == null) xalCL.setVariableContext(vc);
						xalCL.setEvaluator(excetutedComputing.getXPEvaluator());
						
						propName = xalCL.asRequiredString();
						
						ObjectValue<XPOperand<?>> ovProp = new ObjectValue<>();
						
						ovProp.setAttribut(propName, item);
						
						arRes.add(ovProp);
						
					/*} catch (CloneNotSupportedException e) {
						throw new ManagedException(e);
					}*/
				}
			}
			ObjectValue<XPOperand<?>> res = new ObjectValue<>();
			res.setAttribut(Computing.PRTY_INSERTION, ov.getAttribut(Computing.PRTY_INSERTION));
			res.setAttribut(Computing.PRTY_CONTEXT, ov.getAttribut(Computing.PRTY_CONTEXT));
			
			res.setAttribut(Computing.PRTY_VALUE, arRes);
			
			return res;
		}
		return null;
	}

}
