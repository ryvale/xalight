package com.exa.lang.expression;

import com.exa.expression.eval.XPEvaluator;
import com.exa.utils.ManagedException;

public interface XPEvaluatorSetup {
	void setup(XPEvaluator evaluator) throws ManagedException;
}
