package com.exa.lang.expression.computer;

import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.Type;

import com.exa.expression.XPOperand;
import com.exa.expression.OMMethod.XPOrtMethod;

import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.Value;

@SuppressWarnings("rawtypes")
public class GetStoredArrayValue extends OMMethod.XPOrtMethod<XALComputer, ArrayValue> {

	public GetStoredArrayValue() {
		super("sArray", 1);
	}

	@Override
	public boolean canManage(XPEvaluator eval, int order, int nbOperands) throws ManagedException {
		return true;
	}

	@Override
	public Type<?> type() {
		return XALParser.T_COMPUTER;
	}

	@Override
	protected XPOrtMethod<XALComputer, ArrayValue>.XPMethodResult createResultOperand(XPOperand<XALComputer> xpObject, Vector<XPOperand<?>> xpParams) {
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public ArrayValue<XPOperand<?>> value(XPEvaluator eval) throws ManagedException {
				XALComputer object = xpObject.value(eval);
				
				String ref = params.get(0).asOPString().value(eval);
				
				Value<?, XPOperand<?>> vl = object.getStored(ref);
				
				if(vl == null) return null;
				
				return vl.asRequiredArrayValue();
			}
		};
	}

}
