package com.exa.lang.parsing;

import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;

public interface XPEvalautorFactory {
	XPEvaluator create(ObjectValue<XPOperand<?>> rootObject, String context) throws ManagedException;
	void clear();
}
