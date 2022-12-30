package com.exa.lang.expression;

import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;

import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.eval.ClassesMan;
import com.exa.expression.eval.XPEvaluator;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class OVMtdFieldValuesAsComaSeparated extends OMMethod.XPOrtMethod<ObjectValue<XPOperand<?>>, String> {

	public OVMtdFieldValuesAsComaSeparated() {
		super("fieldValuesAsComaSeparated", 2);
	}

	@Override
	public boolean canManage(XPEvaluator eval, int order, int nbOperands) throws ManagedException {
		return true;
	}

	@Override
	public Type<?> type() {
		return ClassesMan.T_STRING;
	}
	
	@Override
	protected XPOrtMethod<ObjectValue<XPOperand<?>>, String>.XPMethodResult createResultOperand(XPOperand<ObjectValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public String value(XPEvaluator eval) throws ManagedException {
				ObjectValue<XPOperand<?>> object = xpObject.value(eval);
				
				final Boolean all = params.get(0).asOPBoolean().value(eval);
				
				final Boolean withNullValue = params.get(1).asOPBoolean().value(eval);
				
				StringBuilder  sb = new StringBuilder();
				
				final Map<String, Value<?, XPOperand<?>>> fieldValues = object.getValue();
				
				Consumer<String> csm = 
					Boolean.TRUE.equals(all) ? 
						(
							Boolean.TRUE.equals(withNullValue) ?
								k -> {
									
									Object v = fieldValues.get(k).getValue();
									
									sb.append("," + v.toString());
								} 
								:
								k -> {
									
									Object v = fieldValues.get(k).getValue();
									
									if(v == null) return;
									
									sb.append("," + v.toString());  
								} 
						)
						: 
						(
							Boolean.TRUE.equals(withNullValue) ?
								k -> {
									if(k.startsWith("_")) return;
									Object v = fieldValues.get(k).getValue();
									sb.append("," + v.toString());
								}
								:
								
								k -> {
									if(k.startsWith("_")) return;
									
									Object v = fieldValues.get(k).getValue();
									if(v == null) return;
									
									sb.append("," + v.toString());
								}
						);
						
				Set<String> keys = object.getValue().keySet();
				keys.forEach(csm);
				
				return sb.length() > 0 ? sb.substring(1) : null;
				
				
			}
		};
	}

}
