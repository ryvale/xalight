package com.exa.lang.expression;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.expression.params.GlobalParams;
import com.exa.lang.expression.params.TGlobalParams;
import com.exa.utils.ManagedException;
import com.jayway.jsonpath.internal.filter.Evaluator;

public class VIEvaluatorSetup implements XPEvaluatorSetup {
	
	public static class ValueInfo<T> {
		private Class<?> valueClass;
		private T value;
		
		public ValueInfo(Class<?> valueClass, T value) {
			super();
			this.valueClass = valueClass;
			this.value = value;
		}

		public Class<?> valueClass() { return valueClass; }

		public T value() { return value; }
	}
	
	private Map<String, ValueInfo<?>> managedVariables = new HashMap<>();
	
	private Map<String, Object> globalParamsValues = new LinkedHashMap<>();
	
	private GlobalParams globalParams;
	
	
	public VIEvaluatorSetup() {
		globalParams= new GlobalParams(globalParamsValues);
	}

	@Override
	public void setup(XPEvaluator evaluator) throws ManagedException {
		evaluator.getClassesMan().registerClass(new TGlobalParams());
		
		injectVariables(evaluator);
	}

	public void injectVariables(XPEvaluator evaluator) throws ManagedException {
		evaluator.addVariable("params", globalParams.getClass(), globalParams);
		for(String varName : managedVariables.keySet()) {
			ValueInfo<?> value = managedVariables.get(varName);
			if("params".equals(varName)) continue;
			evaluator.assignOrDeclareVariable(varName, value.valueClass, value.value);
		}
	}
	
	public <T>void addVaraiable(String varName, Class<?> valueClass, T value) {
		managedVariables.put(varName, new ValueInfo<T>(valueClass, value));
		globalParamsValues.put(varName, value);
	}
	
	public void disposeVariable(String varName) {
		managedVariables.remove(varName);
		globalParamsValues.remove(varName);
	}
}
