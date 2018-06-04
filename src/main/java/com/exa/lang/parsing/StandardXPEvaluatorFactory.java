package com.exa.lang.parsing;

import java.util.Map;

import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.MapVariableContext;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.Computing.EvaluatorSetup;
import com.exa.utils.ManagedException;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class StandardXPEvaluatorFactory implements XPEvalautorFactory {
	/**
	 * 
	 */
	private final TypeSolver typeSolver;

	private XPEvaluator evaluator;
	
	private EvaluatorSetup evaluatorSetup;
	
	public StandardXPEvaluatorFactory(TypeSolver typeSolver, EvaluatorSetup evaluatorSetup) {
		super();
		this.typeSolver = typeSolver;
		this.evaluatorSetup = evaluatorSetup;
	}
	
	public StandardXPEvaluatorFactory(TypeSolver typeSolver) {
		this(typeSolver, (evaluator) -> {} );
	}

	@Override
	public XPEvaluator create(ObjectValue<XPOperand<?>> rootObject, String entityContext) throws ManagedException {
		
		if(entityContext == null) {
			if(evaluator == null) {
				evaluator = new XPEvaluator();
				evaluatorSetup.setup(evaluator);
			}
			return evaluator;
		}
		
		evaluator = new XPEvaluator();
		evaluatorSetup.setup(evaluator);
		
		ObjectValue<XPOperand<?>> ovContexts = rootObject.getPathAttributAsObjecValue(entityContext + "."+Computing.PRTY_CALL_PARAMS);
		
		if(ovContexts == null) return evaluator;
		
		Map<String, Value<?, XPOperand<?>>> mpContxts = ovContexts.getValue();
		for(String evalContext : mpContxts.keySet()) {
			VariableContext vc = new MapVariableContext();
			evaluator.addVariableContext(vc, evalContext, evaluator.getDefaultVariableContext());
			
			Value<?, XPOperand<?>> vlContxt = mpContxts.get(evalContext);
			ObjectValue<XPOperand<?>> ovContxt = vlContxt.asRequiredObjectValue();
			Map<String, Value<?, XPOperand<?>>> mpParams = ovContxt.getValue();
			
			for(String var : mpParams.keySet()) {
				
				Value<?, XPOperand<?>> vlParamValue = mpParams.get(var);
				
				try {
					vlParamValue = vlParamValue.clone();
				} catch (CloneNotSupportedException e) {
					throw new ManagedException(e);
				}
				CalculableValue<?, XPOperand<?>> clParamValue = vlParamValue.asCalculableValue();
				
				Class<?> valueClass = null;
				Object value = null; 
				if(clParamValue == null) value = vlParamValue.getValue();
				else {
					clParamValue.setContext(null);
					value = clParamValue.getValue();
					valueClass = typeSolver.getTypeValueClass(clParamValue.typeName());
				}
				
				vc.addVariable(var, valueClass, value);
			}
		}
		
		return evaluator;
		
	}
	
	public void clear() { evaluator = null; }
	
}