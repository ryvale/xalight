package com.exa.lang.parsing.statements;

import java.util.ArrayList;
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
	
	class LoopVariables {
		public final List<Value<?, XPOperand<?>>> values;
		public final String varName;
		private int position;
		
		public final VariableContext vc;
		
		
		
		public LoopVariables(List<Value<?, XPOperand<?>>> values, String varName, VariableContext vc) {
			super();
			this.values = values;
			this.varName = varName;
			this.position = 0;
			this.vc = vc;
			
			if(this.position < values.size())
			vc.assignContextVariable(varName, values.get(this.position).getValue());
		}
		
		boolean next(boolean reinit) {
			if(values.size() <= 0) return false;
			
			if(position >= values.size()) {
				
				if(reinit) {
					this.position = 1;
					vc.assignContextVariable(varName, values.get(0).getValue());
				}
				return false;
			}
			
			Value<?, XPOperand<?>> vl = values.get(this.position++);
			vc.assignContextVariable(varName, vl.getValue());
			return true;
		}
		
		boolean next() { return this.next(true); }
		
		
	}
	
	class NestedLoopMan {
		List<LoopVariables> loopVarList = new ArrayList<>();
		
		private boolean neveUsed = true;
		
		@SuppressWarnings("unchecked")
		void initialize(ObjectValue<XPOperand<?>> stDesc, VariableContext ovc, Computing excetutedComputing) throws ManagedException {
			ArrayValue<XPOperand<?>> loopVars = stDesc.getRequiredAttributAsArrayValue("_loop_vars").clone();
			
			VariableContext parentVC = ovc;
			
			for(Value<?, XPOperand<?>> vlLV : loopVars.getValue()) {
				ObjectValue<XPOperand<?>> ovLoopVars = vlLV.asRequiredObjectValue();
				
				Value<?, XPOperand<?>> vlValues = ovLoopVars.getRequiredAttribut("_values");
				ArrayValue<XPOperand<?>> values = vlValues.asArrayValue();
				if(values == null) {
					XALCalculabeValue<?> clValues = (XALCalculabeValue<?>)vlValues.asCalculableValue();
					excetutedComputing.computeCalculableValue(clValues, ovc);
					values = (ArrayValue<XPOperand<?>>) clValues.getValue();
				}
				
				String varName = ovLoopVars.getRequiredAttributAsString("_var");
				VariableContext vc = new MapVariableContext(parentVC);
				LoopVariables loopV = new LoopVariables(values.getValue(), varName, vc);
				
				loopVarList.add(loopV);
				
				parentVC = vc;
			}
		}
		
		VariableContext getVariableContext() { return loopVarList.size() > 0 ? loopVarList.get(loopVarList.size() - 1 ).vc : null; }
		
		
		boolean next() {
			
			if(neveUsed) {
				if(loopVarList.size() == 0) return false;
				
				boolean res = loopVarList.get(loopVarList.size() - 1).next();
				
				for(int i=loopVarList.size() - 2; i>=0; i--) {
					LoopVariables lv = loopVarList.get(i);
					lv.next();
				}
				
				neveUsed = false;
				
				return res;
			}
			for(int i=loopVarList.size() - 1; i>=0; i--) {
				LoopVariables lv = loopVarList.get(i);
				
				if(lv.next()) return true;
				
			}
			return false;
		}
		
		VariableContext cloneVC() {
			
			Iterator<LoopVariables> it = loopVarList.iterator();
			if(!it.hasNext()) return null;
			
			
			LoopVariables lv = it.next();
			
			VariableContext vc = STFor.this.cloneVC(lv.vc, lv.vc.getParent());
			
			
			VariableContext parentVC = vc;
			while(it.hasNext()) {
				lv = it.next();
				
				vc = STFor.this.cloneVC(lv.vc, parentVC);
				//vc.setParent(parentVC);
				
				parentVC = vc;
			}
			
			return vc;
		}
	}

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
		
		ObjectValue<XPOperand<?>> loopVars = new ObjectValue<>();
		loopVars.setAttribut(Computing.PRTY_STATEMENT, "for");
		loopVars.setAttribut("_var", var);
		
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
		
		//String nestedStatement = null;
		ArrayValue<XPOperand<?>> nestedloopVars = new ArrayValue<>();
		
		nestedloopVars.add(loopVars);
		
		
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
			
			
			loopVars.setAttribut("_vartype", type);
			loopVars.setAttribut("_values", values);
			loopVars.setAttribut(Computing.PRTY_TYPE, "in");
			
			
			
			VariableContext vc = new MapVariableContext(evaluator.getCurrentVariableContext());
			vc.addVariable(var, tp.valueClass(), null);
			
			evaluator.pushVariableContext(vc);
			
			ObjectValue<XPOperand<?>> res = new ObjectValue<>();
			
			boolean inArray = context.endsWith("[");
			
			if(ch == '_') {
				str = lexingRules.nextNonNullString(charReader);
				if(!Computing.PRTY_NAME.equals(str)) throw new ManagedException(String.format("Syntax error in for statement after ')' in context %s. Exepected %s instead of %s", context, Computing.PRTY_NAME, str));
			
				ch = lexingRules.nextNonBlankChar(charReader);
				if(ch == null || ch != '=') throw new ManagedException(String.format("Syntax error in for statement after '%s' in context %s. Exepected '='", Computing.PRTY_NAME, context));
				
				XPOperand<?> xpName = xpCompiler.parse(charReader, (lr, cr) -> true, context);
				
				xclName = Computing.calculableFor(xpName, "now");
				
				insertion = inArray ? Computing.VL_ARRAY : Computing.VL_INCORPORATE;
				
			}
			else {
				insertion = Computing.VL_ARRAY;

			}
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			if(ch == null) throw new ManagedException(String.format("Unexpected end of file in for statement in context %s. The statement doesn't have body;", context));
			
			Value<?, XPOperand<?>> vlDo = null;
			if(ch == ':') {
				lexingRules.nextNonBlankChar(charReader);
				XPOperand<?> xp = xpCompiler.parse(charReader, (lr, cr) -> true, context);
				vlDo = Computing.calculableFor(xp, "now");
				res.setAttribut("_do", vlDo);
			}
			else if(ch == '{' || ch == '@' || ch == '[' || ch == '=' || ch == '"' || ch == '\'') {
				vlDo = computing.readPropertyValueForObject(context);
				res.setAttribut("_do", vlDo);
			}
			else if(ch == '*') {
				lexingRules.nextNonBlankChar(charReader);
				ObjectValue<XPOperand<?>> ovDo = computing.readStatement(context);
				
				vlDo = ovDo;
				
				if(xclName == null) xclName = (XALCalculabeValue<?>) ovDo.getAttribut(Computing.PRTY_NAME);
				
				if("for".equals(ovDo.getRequiredAttributAsString(Computing.PRTY_STATEMENT))) {
					insertion = ovDo.getRequiredAttributAsString(Computing.PRTY_INSERTION);
					
					ArrayValue<XPOperand<?>> innerNestedloopVars = ovDo.getAttributAsArrayValue("_loop_vars");
					if(innerNestedloopVars == null)
						throw new ManagedException(String.format("Missing '_loop_vars' property in inner for loop (Context : %s).", context));
					
					for(Value<?, XPOperand<?>> vllv : innerNestedloopVars.getValue()) {
						nestedloopVars.add(vllv);
					}
					
					res.setAttribut("_do", ovDo.getAttribut("_do"));
					
				}
				else {
					res.setAttribut("_do", vlDo);
				}
				
				
			}
			
			/*VariableContext vc = new MapVariableContext();
			vc.addVariable(var, computing.getTypeSolver().getTypeValueClass(type), null);
			evaluator.pushVariableContext(vc);*/
			
			evaluator.popVariableContext();
			
			
			if(xclName != null) {
				/*if(Computing.VL_ARRAY.equals(insertion)) {
					ObjectValue<XPOperand<?>> newVlDo = new ObjectValue<>();
					newVlDo.setAttribut(Computing.PRTY_NAME, xclName);
					newVlDo.setAttribut(Computing.PRTY_VALUE, vlDo);
					vlDo = newVlDo;
					
				}
				else*/ res.setAttribut(Computing.PRTY_NAME, xclName);
				
			}
			
			loopVars.setAttribut(Computing.PRTY_CONTEXT, context);
			res.setAttribut(Computing.PRTY_STATEMENT, "for");
			
			
			
			res.setAttribut(Computing.PRTY_INSERTION, insertion);
			res.setAttribut("_loop_vars", nestedloopVars);
			
			
			return res;
			
			//throw new ManagedException("Incorrect syntax in for statement");
		}
		
		return null;
	}
	
	private VariableContext cloneVC(VariableContext vc, VariableContext parentVC) {
		
		VariableContext res = new MapVariableContext(parentVC);
		
		vc.visitAll( 
			(n, v) -> {
				try {
					res.addVariable(n, v.valueClass(), v.value());
				} catch (ManagedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		);
		
		return res;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, Computing excetutedComputing, VariableContext ovc, Map<String, ObjectValue<XPOperand<?>>> libOV, String cmd) throws ManagedException {

		
		NestedLoopMan nlm = new NestedLoopMan();
		
		nlm.initialize(ov, ovc, excetutedComputing);
		
		Value<?, XPOperand<?>> vlDo = ov.getAttribut("_do");
		
		ArrayValue<XPOperand<?>> arRes = new ArrayValue<>();
		
		//VariableContext vc = nlm.getVariableContext();
		
		if(ov.getRequiredAttributAsString(Computing.PRTY_INSERTION).equals(Computing.VL_ARRAY)) {
			
			Value<?, XPOperand<?>> vlName = ov.getAttribut(Computing.PRTY_NAME);
			
			
			if(vlName == null) {
				while(nlm.next()) {
					VariableContext iVC = nlm.cloneVC();
					Value<?, XPOperand<?>> rawItem = vlDo.clone();
					CalculableValue<?, XPOperand<?>> cl = rawItem.asCalculableValue();
					Value<?, XPOperand<?>> item;
					
					//item = excetutedComputing.value(rawItem, vc, libOV);
					if(cl == null) item = excetutedComputing.value(rawItem, iVC, libOV);
					else {
						item = excetutedComputing.computeCalculableValue(cl, iVC);
					}
					
					arRes.add(item);
				}
			}
			else {
				Value<?, XPOperand<?>> item; CalculableValue<?, XPOperand<?>> clName = vlName.asCalculableValue();
				
				if(clName == null) {
					 
					while(nlm.next()) {
						VariableContext iVC = nlm.cloneVC();
						Value<?, XPOperand<?>> rawItem = vlDo.clone();
						CalculableValue<?, XPOperand<?>> cl = rawItem.asCalculableValue();
						//Value<?, XPOperand<?>> item;
						
						//item = excetutedComputing.value(rawItem, vc, libOV);
						if(cl == null) item = excetutedComputing.value(rawItem, iVC, libOV);
						else {
							item = excetutedComputing.computeCalculableValue(cl, iVC);
						}
						
						ObjectValue<XPOperand<?>> ovItem = item.asObjectValue();
						if(ovItem != null && !ovItem.containsAttribut(Computing.PRTY_NAME)) ovItem.setAttribut(Computing.PRTY_NAME, vlName.asRequiredString());
						arRes.add(item);
					}
				}
				else {
					while(nlm.next()) {
						VariableContext iVC = nlm.cloneVC();
						
						
						Value<?, XPOperand<?>> rawItem = vlDo == null ? new ObjectValue<>() :  vlDo.clone();
						CalculableValue<?, XPOperand<?>> cl = rawItem.asCalculableValue();
						//Value<?, XPOperand<?>> item;
						
						//item = excetutedComputing.value(rawItem, vc, libOV);
						if(cl == null) item = excetutedComputing.value(rawItem, iVC, libOV);
						else {
							item = excetutedComputing.computeCalculableValue(cl, iVC);
						}
						
						ObjectValue<XPOperand<?>> ovItem = item.asObjectValue();
						if(ovItem != null && !ovItem.containsAttribut(Computing.PRTY_NAME)) {
							CalculableValue<?, XPOperand<?>> clNameCl = clName.clone();
							
							XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) clNameCl;
							if(xalCL.getVariableContext() == null) xalCL.setVariableContext(iVC);
							xalCL.setEvaluator(excetutedComputing.getXPEvaluator());
							
							ovItem.setAttribut(Computing.PRTY_NAME, xalCL.asRequiredString());
						}
						arRes.add(item);
					}
				}
				
			}
		
			
		}
		
		else {
			
			Value<?, XPOperand<?>> vlName = ov.getRequiredAttribut(Computing.PRTY_NAME);
			
			while(nlm.next()) {
				VariableContext iVC = nlm.cloneVC();
				
				Value<?, XPOperand<?>> item; CalculableValue<?, XPOperand<?>> cl;
				
				
				if(vlDo == null) {
					item = null;
				}
				else {
					Value<?, XPOperand<?>> rawItem = vlDo.clone();
					cl = rawItem.asCalculableValue();
					
					if(cl == null) item = excetutedComputing.value(rawItem, iVC, libOV);
					else {
						item = excetutedComputing.computeCalculableValue(cl, iVC);
					}
					
					if(item == null) continue;
				}
					
				Value<?, XPOperand<?>> rawItemName = vlName.clone();
				
				String propName;
				cl = rawItemName.asCalculableValue();
				
				XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
				if(xalCL.getVariableContext() == null) xalCL.setVariableContext(iVC);
				xalCL.setEvaluator(excetutedComputing.getXPEvaluator());
				
				propName = xalCL.asRequiredString();
				
				ObjectValue<XPOperand<?>> ovProp = new ObjectValue<>();
				
				ovProp.setAttribut(propName, item);
				
				arRes.add(ovProp);
			}
		}
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		res.setAttribut(Computing.PRTY_INSERTION, ov.getAttribut(Computing.PRTY_INSERTION));
		res.setAttribut(Computing.PRTY_CONTEXT, ov.getAttribut(Computing.PRTY_CONTEXT));
		
		res.setAttribut(Computing.PRTY_VALUE, arRes);
		
		return res;
		
	}

}
