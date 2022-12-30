package com.exa.lang.expression;

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

public class OVMtdFieldNamesAsComaSeparated extends OMMethod.XPOrtMethod<ObjectValue<XPOperand<?>>, String> {

	public OVMtdFieldNamesAsComaSeparated() {
		super("fieldNamesAsComaSeparated", 1);
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
				
				Boolean all = params.get(0).asOPBoolean().value(eval);
				
				StringBuilder  sb = new StringBuilder();
				
				Set<String> keys = object.getValue().keySet();
				
				Consumer<String> csm = 
					Boolean.TRUE.equals(all) ? 
						v -> sb.append("," + v)
						: 
						v -> {
							if(v.startsWith("_")) return; 
							sb.append("," + v);  
						} ;
				
				keys.forEach(csm);
				
				return sb.length() > 0 ? sb.substring(1) : null;
				
				
			}
		};
	}

}
