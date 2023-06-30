package com.exa.lang.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;

import com.exa.expression.OMMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.StringValue;
import com.exa.utils.values.Value;

@SuppressWarnings("rawtypes")
public class OVMtdFieldValuesAsArray extends OMMethod.XPOrtMethod<ObjectValue<XPOperand<?>>, ArrayValue> {
	
	public OVMtdFieldValuesAsArray() {
		super("fieldValuesAsArray", 2);
	}

	public boolean canManage(XPEvaluator eval, int order, int nbOperands) throws ManagedException {
		return true;
	}
	
	@Override
	public Type<?> type() {
		return XALParser.T_ARRAY_VALUE;
	}
	
	@Override
	protected XPOrtMethod<ObjectValue<XPOperand<?>>, ArrayValue>.XPMethodResult createResultOperand(XPOperand<ObjectValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public ArrayValue<XPOperand<?>> value(XPEvaluator eval) throws ManagedException {
				ObjectValue<XPOperand<?>> object = xpObject.value(eval);
				
				final Boolean all = params.get(0).asOPBoolean().value(eval);
				
				final Boolean withNullValue = params.get(1).asOPBoolean().value(eval);
				
				final List<Value<?, XPOperand<?>>> values = new ArrayList<>();
				
				final ArrayValue<XPOperand<?>> res = new ArrayValue<>(values);
				
				final Map<String, Value<?, XPOperand<?>>> fieldValues = object.getValue();
				
				Consumer<String> csm = 
					Boolean.TRUE.equals(all) ? 
						(
							Boolean.TRUE.equals(withNullValue) ?
								k -> {
									
									Object v = fieldValues.get(k).getValue();
									if(v == null) {
										values.add(new StringValue<>(null));
										return;
									}
									
									if(v instanceof List) {
										
										@SuppressWarnings("unchecked")
										List<Value<?, XPOperand<?>>> vlList = (List<Value<?, XPOperand<?>>>)v;
										for(Value<?, XPOperand<?>> iv : vlList) {
											Object v1 = iv.getValue();
											
											values.add(new StringValue<>(v1 == null ? null : v1.toString()));
										}
										return;
									}
									
									values.add(new StringValue<>(v.toString()));  
								} 
								:
								k -> {
									
									Object v = fieldValues.get(k).getValue();
									
									if(v == null) return;
									
									if(v instanceof List) {
										
										@SuppressWarnings("unchecked")
										List<Value<?, XPOperand<?>>> vlList = (List<Value<?, XPOperand<?>>>)v;
										for(Value<?, XPOperand<?>> iv : vlList) {
											Object v1 = iv.getValue();
											
											if(v1 == null) continue;
											
											values.add(new StringValue<>(v1.toString()));
										}
										return;
									}
									
									values.add(new StringValue<>(v.toString()));  
								} 
						)
						: 
						(
							Boolean.TRUE.equals(withNullValue) ?
								k -> {
									if(k.startsWith("_")) return;
									
									Object v = fieldValues.get(k).getValue();
									if(v == null) {
										values.add(new StringValue<>(null));
										return;
									}
									
									if(v instanceof List) {
										
										@SuppressWarnings("unchecked")
										List<Value<?, XPOperand<?>>> vlList = (List<Value<?, XPOperand<?>>>)v;
										for(Value<?, XPOperand<?>> iv : vlList) {
											Object v1 = iv.getValue();
											
											values.add(new StringValue<>(v1 == null ? null : v1.toString()));
										}
										return;
									}
									
									values.add(new StringValue<>(v.toString()));  
									
								}
								:
								
								k -> {
									if(k.startsWith("_")) return;
									
									Object v = fieldValues.get(k).getValue();
									if(v == null) return;
									
									if(v instanceof List) {
										
										@SuppressWarnings("unchecked")
										List<Value<?, XPOperand<?>>> vlList = (List<Value<?, XPOperand<?>>>)v;
										for(Value<?, XPOperand<?>> iv : vlList) {
											Object v1 = iv.getValue();
											
											if(v1 == null) continue;
											
											values.add(new StringValue<>(v1.toString()));
										}
										return;
									}
									
									values.add(new StringValue<>(v.toString()));
								}
						);
				
				Set<String> keys = object.getValue().keySet();
				keys.forEach(csm);
				
				return res;
			}
		};
	}

}
