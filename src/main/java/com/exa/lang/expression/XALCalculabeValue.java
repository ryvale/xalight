package com.exa.lang.expression;

import java.util.HashMap;
import java.util.Map;

import com.exa.expression.Type;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.ClassesMan;
import com.exa.expression.eval.XPEvaluator;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.BooleanValue;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.DecimalValue;
import com.exa.utils.values.IntegerValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.StringValue;

public class XALCalculabeValue<T> extends CalculableValue<T,  XPOperand<?>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private XPOperand<T> xp;
	
	private static Map<Type<?>, String> mapTypeManToString = new HashMap<>();
	
	static {
		mapTypeManToString.put(ClassesMan.T_STRING, "string");
		mapTypeManToString.put(ClassesMan.T_INTEGER, "integer");
		mapTypeManToString.put(ClassesMan.T_DOUBLE, "float");
		mapTypeManToString.put(ClassesMan.T_BOOLEAN, "boolean");
		mapTypeManToString.put(ClassesMan.T_DATE, "date");
		//mapTypeManToString.put(ClassesMan.T_OBJECT, "object");
	}
	
	private String evalTime;
	
	private XPEvaluator evaluator;

	protected VariableContext variableContext;

	public XALCalculabeValue(XPOperand<T> xp, String evalTime) {
		super();
		this.xp = xp;
		this.evalTime = evalTime;
	}

	@Override
	public T getValue() {
		try {
			evaluator.pushVariableContext(variableContext);
			T res = xp.value(evaluator);
			evaluator.popVariableContext();
			
			return res;
		} catch (ManagedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setValue(T value) {
		
	}

	@Override
	public StringValue<XPOperand<?>> asStringValue() {
		return null;
	}

	@Override
	public ObjectValue<XPOperand<?>> asObjectValue() {
		return null;
	}

	@Override
	public ArrayValue<XPOperand<?>> asArrayValue() {
		return null;
	}

	@Override
	public BooleanValue<XPOperand<?>> asBooleanValue() {
		return null;
	}

	@Override
	public DecimalValue<XPOperand<?>> asDecimalValue() {
		return null;
	}

	@Override
	public IntegerValue<XPOperand<?>> asIntegerValue() {
		return null;
	}

	@Override
	public CalculableValue<T, XPOperand<?>> asCalculableValue() {
		return this;
	}

	@Override
	public Integer asInteger() throws ManagedException {
		if(xp.type() == ClassesMan.T_INTEGER) {
			
			evaluator.pushVariableContext(variableContext);
			Integer res = ClassesMan.T_INTEGER.valueOrNull(xp.value(evaluator));
			evaluator.popVariableContext();
			
			return res;
		}
		
		throw new ManagedException(String.format("This value should be an integer"));
	}
	
	

	@Override
	public ObjectValue<XPOperand<?>> asRequiredObjectValue() throws ManagedException {
		return null;
	}

	@Override
	public String asRequiredString() throws ManagedException {
		String res = asString();
		if(res == null) throw new ManagedException(String.format("This value should be a non null string"));
		return res;
	}

	@Override
	public Integer asRequiredInteger() throws ManagedException {
		Integer res = asInteger();
		if(res == null) throw new ManagedException(String.format("This value should be a non null integer"));
		return res;
	}

	@Override
	public String asString() throws ManagedException {
		if(xp.type() == ClassesMan.T_STRING) {
			evaluator.pushVariableContext(variableContext);
			String res = ClassesMan.T_STRING.valueOrNull(xp.value(evaluator));
			evaluator.popVariableContext();
			return res;
		}
		throw new ManagedException(String.format("This value should be a string"));
	}
	
	/*@Override
	public Boolean asBoolean() throws ManagedException {
		if(xp.type() == ClassesMan.T_STRING) {
			evaluator.pushVariableContext(variableContext);
			String res = ClassesMan.T_STRING.valueOrNull(xp.value(evaluator));
			evaluator.popVariableContext();
			return res;
		}
		throw new ManagedException(String.format("This value should be a string"));
	}*/

	@Override
	public XALCalculabeValue<T> clone() throws CloneNotSupportedException {
		//ObjectValue<XPOperand<?>> ov = rootObject;
		return new XALCalculabeValue<T>(xp, evalTime);
	}

	@Override
	public String toString() {
		if(xp == null)	return super.toString();
		
		if(xp.isConstant()) {
			try {
				Object v = xp.value(null);
				if(v == null) return "calculable";
				return v.toString();
			} catch (ManagedException e) {
				return "calculable";
			}
		}
		return "calculable";
	}
	
	
	/*private XPEvaluator getEvaluator() throws ManagedException {
		if(evaluator == null) evaluator = evaluatorFactory.create(rootObject, entityContext);
		
		return evaluator;
	}*/


	@Override
	public String typeName() {
		return mapTypeManToString.get(xp.type());
	}


	@Override
	public void setContext(String context) {
		//this.entityContext = context;
	}

	public XPEvaluator getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(XPEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public String getEvalTime() {
		return evalTime;
	}

	@Override
	public Boolean asBoolean() throws ManagedException {
		if(xp.type() == ClassesMan.T_BOOLEAN) {
			evaluator.pushVariableContext(variableContext);
			Boolean res = ClassesMan.T_BOOLEAN.valueOrNull(xp.value(evaluator));
			evaluator.popVariableContext();
			return res;
		}
		
		throw new ManagedException(String.format("This value should be an integer"));
	}

	public VariableContext getVariableContext() {
		return variableContext;
	}

	public void setVariableContext(VariableContext variableContext) {
		this.variableContext = variableContext;
	}

	@Override
	public Boolean asRequiredBoolean() throws ManagedException {
		Boolean res = asBoolean();
		if(res == null) throw new ManagedException(String.format("This value should be a non null integer"));
		return res;
	}
	
	
	
}
