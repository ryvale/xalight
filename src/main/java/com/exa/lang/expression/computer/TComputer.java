package com.exa.lang.expression.computer;

import com.exa.eva.OperatorManager.OMOperandType;
import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.types.TObjectClass;
import com.exa.utils.values.ArrayValue;

public class TComputer  extends TObjectClass<XALComputer, Object> {

	public TComputer() {
		super(null, XALComputer.class, "Computer");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Type<XALComputer> specificType() {
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void initialize() {
		OMMethod<ArrayValue> omAV = new OMMethod<>("sArray", 2, OMOperandType.POST_OPERAND);
		omAV.addOperator(new GetStoredArrayValue());
		methods.put("sArray", new Method<>("sArray", ArrayValue.class, omAV));
	}

}
