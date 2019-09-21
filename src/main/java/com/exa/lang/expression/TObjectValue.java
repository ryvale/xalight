package com.exa.lang.expression;

import com.exa.eva.OperatorManager.OMOperandType;
import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.types.TObjectClass;
import com.exa.utils.values.ArrayValue;
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
		omStr.addOperator(new OVMtdGetString());
		methods.put("getString", new Method<>("getString", String.class, omStr));
		
		OMMethod<ObjectValue> omOV = new OMMethod<>("getObject", 2, OMOperandType.POST_OPERAND);
		omOV.addOperator(new OVMtdGetObjectValue());
		methods.put("getObject", new Method<>("getObject", ObjectValue.class, omOV));
		
		OMMethod<ArrayValue> omAV = new OMMethod<>("getArray", 2, OMOperandType.POST_OPERAND);
		omAV.addOperator(new OVMtdGetArrayValue());
		methods.put("getArray", new Method<>("getArray", ArrayValue.class, omAV));
		
		omAV = new OMMethod<>("fieldNamesAsArray", 1, OMOperandType.POST_OPERAND);
		omAV.addOperator(new OVMtdFieldNamesAsArray());
		methods.put("fieldNamesAsArray", new Method<>("fieldNamesAsArray", ArrayValue.class, omAV));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Type<ObjectValue<XPOperand<?>>> specificType() {
		return this;
	}
	
	

}
