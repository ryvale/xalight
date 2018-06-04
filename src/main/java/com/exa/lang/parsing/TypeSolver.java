package com.exa.lang.parsing;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.BooleanValue;
import com.exa.utils.values.DecimalValue;
import com.exa.utils.values.IntegerValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.StringValue;

public class TypeSolver {
	private Map<Class<?>, String> valueTypeMapOnTypes = new HashMap<>();
	private Map<String, Class<?>> xalTypeMapToJava = new HashMap<>();
	
	public TypeSolver() {
		valueTypeMapOnTypes.put(StringValue.class, "string");
		valueTypeMapOnTypes.put(IntegerValue.class, "int");
		valueTypeMapOnTypes.put(DecimalValue.class, "float");
		valueTypeMapOnTypes.put(BooleanValue.class, "boolean");
		valueTypeMapOnTypes.put(ObjectValue.class, "object");
		valueTypeMapOnTypes.put(ArrayValue.class, "array");
		
		xalTypeMapToJava.put("string", String.class);
		xalTypeMapToJava.put("int", Integer.class);
		xalTypeMapToJava.put("float", Double.class);
		xalTypeMapToJava.put("boolean", Boolean.class);
		xalTypeMapToJava.put("date", Date.class);
	}
	
	public void registerType(String name, Class<?> valueClass) {
		valueTypeMapOnTypes.put(valueClass, name);
		xalTypeMapToJava.put(name, valueClass);
	}
	
	public String getTypeName(Class<?> valueClass) {
		return valueTypeMapOnTypes.get(valueClass);
	}
	
	public Class<?> getTypeValueClass(String typeName) {
		return xalTypeMapToJava.get(typeName);
	}
	
	public boolean containsType(String name) { return xalTypeMapToJava.keySet().contains(name); }

}
