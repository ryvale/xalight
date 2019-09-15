package com.exa.lang.expression;

import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.ClassesMan;
import com.exa.expression.eval.XPEvaluator;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;


public class AVMtdGetString  extends OMMethod.XPOrtMethod<ArrayValue<XPOperand<?>>, String> {

	public AVMtdGetString() {
		super("getString", 1);
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
	protected XPOrtMethod<ArrayValue<XPOperand<?>>, String>.XPMethodResult createResultOperand(XPOperand<ArrayValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public String value(XPEvaluator eval) throws ManagedException {
				Integer idx = params.get(0).asOPInteger().value(eval);
				ArrayValue<XPOperand<?>> object = xpObject.value(eval);
				
				return object.getString(idx);
			}
		};
	}

}
