package com.exa.lang.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.OMMethod.XPOrtMethod;

import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;

import com.exa.utils.values.IntegerValue;

import com.exa.utils.values.Value;

@SuppressWarnings("rawtypes")
public class AVMtdIndexArray extends OMMethod.XPOrtMethod<ArrayValue<XPOperand<?>>, ArrayValue> {
	
	public AVMtdIndexArray() {
		super("indexArray", 0);
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
	protected XPOrtMethod<ArrayValue<XPOperand<?>>, ArrayValue>.XPMethodResult createResultOperand(XPOperand<ArrayValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public ArrayValue<XPOperand<?>> value(XPEvaluator eval) throws ManagedException {
				ArrayValue<XPOperand<?>> object = xpObject.value(eval);
				
				List<Value<?, XPOperand<?>>> lst = object.getValue();
				
				
				List<Value<?, XPOperand<?>>> lstRes = new ArrayList<>();
				
				for(int i=0; i<lst.size(); i++) {
					lstRes.add(new IntegerValue<>(i));
				}
				
				return new ArrayValue<>(lstRes);
			}
		};
	}
	
}
