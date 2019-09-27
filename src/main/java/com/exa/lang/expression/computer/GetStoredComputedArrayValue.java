package com.exa.lang.expression.computer;

import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.OMMethod.XPOrtMethod;

import com.exa.expression.Type;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.MapVariableContext;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;


@SuppressWarnings("rawtypes")
public class GetStoredComputedArrayValue extends OMMethod.XPOrtMethod<XALComputer, ArrayValue> {

	public GetStoredComputedArrayValue() {
		super("scArray", 1);
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
				
				VariableContext vc = new MapVariableContext(object.getComputing().getXPEvaluator().getCurrentVariableContext());
				
				return object.getStoredComputedArrayValue(ref, vc);
			}
		};
	}

}
