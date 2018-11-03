package com.exa.lang.parsing;

import com.exa.expression.VariableContext;
import com.exa.expression.eval.XPEvaluator.ContextResolver;

public class XPParser extends com.exa.expression.parsing.Parser {

	public XPParser(VariableContext variableContext, ContextResolver contextResolver) {
		super(variableContext, contextResolver);
	}
	
}
