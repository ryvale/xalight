package com.exa.lang.expression;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.BooleanValue;
import com.exa.utils.values.DecimalValue;
import com.exa.utils.values.IntegerValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

@SuppressWarnings("rawtypes")
public class AVMtdAddItem extends OMMethod.XPOrtMethod<ArrayValue<XPOperand<?>>, ArrayValue> {
	
	public AVMtdAddItem() {
		super("addItem", 2);
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
			
			@SuppressWarnings("unchecked")
			@Override
			public ArrayValue<XPOperand<?>> value(XPEvaluator eval) throws ManagedException {
				ArrayValue<XPOperand<?>> object = xpObject.value(eval);
				
				Boolean inPlace = params.get(1).asOPBoolean().value(eval);
				
				if(Boolean.FALSE.equals(inPlace)) object = object.clone();
				
				Object objValue = params.get(0).value(eval);
				
				if(objValue instanceof String) object.add((String) objValue);
				else if(objValue instanceof Integer) object.add( new IntegerValue<>((Integer)objValue));
				else if(objValue instanceof Boolean) object.add( new BooleanValue<>((Boolean)objValue));
				else if(objValue instanceof Double) object.add( new DecimalValue<>((Double)objValue));
				else if(objValue instanceof List) object.add( new ArrayValue<>((List<Value<?, XPOperand<?>>>)objValue));
				else if(objValue instanceof Map) object.add( new ObjectValue<>(((Map<String, Value<?, XPOperand<?>>>)objValue)));
				
				
				
				return object;
			}
		};
	}


}
