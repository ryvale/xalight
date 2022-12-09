package com.exa.lang.expression;

import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.OMMethod.XPOrtMethod;

import com.exa.expression.eval.ClassesMan;
import com.exa.expression.eval.XPEvaluator;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;

public class OVMtdGetInteger extends OMMethod.XPOrtMethod<ObjectValue<XPOperand<?>>, Integer> {

	public OVMtdGetInteger() {
		super("getInteger", 1);
	}
	
	@Override
	public boolean canManage(XPEvaluator eval, int order, int nbOperands) throws ManagedException {
		return true;
	}

	@Override
	public Type<?> type() {
		return ClassesMan.T_INTEGER;
	}
	
	@Override
	protected XPOrtMethod<ObjectValue<XPOperand<?>>, Integer>.XPMethodResult createResultOperand(XPOperand<ObjectValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public Integer value(XPEvaluator eval) throws ManagedException {
				String fieldName = params.get(0).asOPString().value(eval);
				ObjectValue<XPOperand<?>> object = xpObject.value(eval);
				
				return object.getPathAttributAsInteger(fieldName);
			}
		};
	}
}
