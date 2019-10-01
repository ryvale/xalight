package com.exa.lang.expression.computer;

import java.util.HashMap;
import java.util.Map;

import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.lang.parsing.Computing;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class XALComputer {
	
	private Computing computing;
	
	private Map<String, Value<?, XPOperand<?>>> store =  new HashMap<>();

	public XALComputer(Computing computing) {
		super();
		this.computing = computing;
	}
	
	public ObjectValue<XPOperand<?>> getStoredComputedObjectValue(String ref, VariableContext entityVC) throws ManagedException {
		Value<?, XPOperand<?>> vl = store.get(ref);
		
		if(vl != null) {
			vl = computing.object(ref, entityVC);
			
			store.put(ref, vl);
		}
		
		return vl.asObjectValue();
	}
	
	public ArrayValue<XPOperand<?>> getStoredComputedArrayValue(String ref, VariableContext entityVC) throws ManagedException {
		Value<?, XPOperand<?>> vl = store.get(ref);
		
		if(vl != null) {
			vl = computing.array(ref, entityVC);
			
			store.put(ref, vl);
		}
		
		return vl.asArrayValue();
	}
	
	public Value<?, XPOperand<?>> getStored(String ref) {
		return store.get(ref);
	}
	
	public void remove(String ref) {
		this.store.remove(ref);
	}

	public Computing getComputing() {
		return computing;
	}
	
	public void store(String ref, String name, VariableContext vc) throws ManagedException {
		Value<?, XPOperand<?>> svl = computing.value(ref, vc);
		
		store.put(name, svl);
	}
	
}
