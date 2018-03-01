package com.exa.lang.parsing;

import com.exa.expression.TypeMan;
import com.exa.expression.XPOperand;
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
	
	
	public XALCalculabeValue(XPOperand<T> xp) {
		super();
		this.xp = xp;
	}

	@Override
	public T getValue() {
		try {
			return xp.value();
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
		if(xp.type() == TypeMan.INTEGER) return TypeMan.INTEGER.valueOrNull(xp.value());
		
		throw new ManagedException(String.format("This value should be aan integer"));
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
		if(xp.type() == TypeMan.STRING) return TypeMan.STRING.valueOrNull(xp.value());
		throw new ManagedException(String.format("This value should be a string"));
	}

	@Override
	public XALCalculabeValue<T> clone() throws CloneNotSupportedException {
		return new XALCalculabeValue<T>(xp);
	}

}
