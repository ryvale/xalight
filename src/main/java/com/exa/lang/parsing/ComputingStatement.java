package com.exa.lang.parsing;

import java.util.Map;

import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public interface ComputingStatement {
	ObjectValue<XPOperand<?>> compileObject(Computing computing, String context) throws ManagedException;
	
	Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, Computing excutedComputing, VariableContext ovc, Map<String, ObjectValue<XPOperand<?>>> libOV, String cmd) throws ManagedException;
	
	//Value<?, XPOperand<?>> import(ObjectValue<XPOperand<?>> ov);
}
