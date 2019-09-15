package com.exa.lang.expression;

import com.exa.eva.OperatorManager.OMOperandType;
import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.types.TObjectClass;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.Value;

public class TArrayValue extends TObjectClass<ArrayValue<XPOperand<?>>, Value<?, XPOperand<?>>> {

	public TArrayValue() {
		super(null, ArrayValue.class, "ArrayValue");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Type<ArrayValue<XPOperand<?>>> specificType() {
		return this;
	}

	@Override
	public void initialize() {
		OMMethod<String> omStr = new OMMethod<>("getString", 2, OMOperandType.POST_OPERAND);
		omStr.addOperator(new AVMtdGetString());
		methods.put("getString", new Method<>("getString", String.class, omStr));
	}
	
}
