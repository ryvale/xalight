package com.exa.lang.expression;

import java.util.List;
import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.ClassesMan;
import com.exa.expression.eval.XPEvaluator;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.Value;

public class AVMtdIndexOf extends OMMethod.XPOrtMethod<ArrayValue<XPOperand<?>>, Integer> {

	public AVMtdIndexOf() {
		super("indexOf", 1);
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
	protected XPOrtMethod<ArrayValue<XPOperand<?>>, Integer>.XPMethodResult createResultOperand(
			XPOperand<ArrayValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public Integer value(XPEvaluator eval) throws ManagedException {
				ArrayValue<XPOperand<?>> object = xpObject.value(eval);
				/*Integer idx = params.get(0).asOPInteger().value(eval);*/
				
				Object o =  params.get(0).value(eval);
				
				List<Value<?, XPOperand<?> >> vlObject = object.getValue();
				
				for(int i=0; i < vlObject.size(); i++) {
					Value<?, XPOperand<?>> vl = vlObject.get(i);
					
					Object oc = vl.getValue();
					if(o == null) {
						if(oc == null) return i;
						continue;
					}
					
					if(o.equals(oc)) return i;
				}
				
				
				return -1;
			}
		};
	}

}
