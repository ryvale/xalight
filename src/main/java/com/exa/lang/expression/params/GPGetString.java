package com.exa.lang.expression.params;

import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.ClassesMan;
import com.exa.expression.eval.XPEvaluator;
import com.exa.utils.ManagedException;

public class GPGetString extends OMMethod.XPOrtMethod<GlobalParams, String> {

	public GPGetString() {
		super("getString", 2);
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
	protected XPOrtMethod<GlobalParams, String>.XPMethodResult createResultOperand(XPOperand<GlobalParams> xpGP, Vector<XPOperand<?>> xpFieldName) {
		return new XPMethodResult(xpGP, xpFieldName) {
			
			@Override
			public String value(XPEvaluator eval) throws ManagedException {
				String fieldName = xpFieldName.get(0).asOPString().value(eval);
				GlobalParams gp = xpGP.value(eval);
				
				return gp.getString(fieldName);
			}
		};
	}

}
