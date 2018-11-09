package com.exa.lang.expression;

import com.exa.eva.OperatorManager.OMOperandType;
import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.types.TObjectClass;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class TObjectValue extends TObjectClass<ObjectValue<XPOperand<?>>, Value<?, XPOperand<?>>> {

	public TObjectValue() {
		super(null, ObjectValue.class, "ObjectValue");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void initialize() {
		OMMethod<String> omStr = new OMMethod<>("getString", 2, OMOperandType.POST_OPERAND);
		omStr.addOperator(new MethodGetString());
		methods.put("getString", new Method<>("getString", String.class, omStr));
		
		OMMethod<ObjectValue> omOV = new OMMethod<>("getObject", 2, OMOperandType.POST_OPERAND);
		omOV.addOperator(new MethodGetObject());
		methods.put("getObject", new Method<>("getObject", ObjectValue.class, omOV));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Type<ObjectValue<XPOperand<?>>> specificType() {
		return this;
	}
	
	

}
