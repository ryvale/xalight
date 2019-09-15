package com.exa.lang.expression;

import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.ObjectValue;

@SuppressWarnings("rawtypes")
public class OVMtdGetArrayValue extends OMMethod.XPOrtMethod<ObjectValue<XPOperand<?>>, ArrayValue> {

	public OVMtdGetArrayValue() {
		super("getArray", 1);
	}

	@Override
	public boolean canManage(XPEvaluator eval, int order, int nbOperands) throws ManagedException {
		return true;
	}

	@Override
	public Type<?> type() {
		return XALParser.T_ARRAY_VALUE;
	}

	@Override
	protected XPOrtMethod<ObjectValue<XPOperand<?>>, ArrayValue>.XPMethodResult createResultOperand(XPOperand<ObjectValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public ArrayValue<XPOperand<?>> value(XPEvaluator eval) throws ManagedException {
				String fieldName = params.get(0).asOPString().value(eval);
				ObjectValue<XPOperand<?>> object = xpObject.value(eval);
				
				return object.getPathAttributAsArrayValue(fieldName);
			}
		};
	}

	/*@Override
	protected XPOrtMethod<ArrayValue<XPOperand<?>>, ArrayValue>.XPMethodResult createResultOperand(
			XPOperand<ArrayValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		// TODO Auto-generated method stub
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public ArrayValue<XPOperand<?>> value(XPEvaluator eval) throws ManagedException {
				String fieldName = params.get(0).asOPString().value(eval);
				ObjectValue<XPOperand<?>> object = xpObject.value(eval);
				
				return object.getAttributByPathAsObjectValue(fieldName);
			}
		};
	}*/
	
	

}
