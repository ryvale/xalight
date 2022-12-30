package com.exa.lang.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;

import com.exa.expression.OMMethod;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.StringValue;
import com.exa.utils.values.Value;

@SuppressWarnings("rawtypes")
public class OVMtdFieldNamesAsArray extends OMMethod.XPOrtMethod<ObjectValue<XPOperand<?>>, ArrayValue> {

	public OVMtdFieldNamesAsArray() {
		super("fieldNamesAsArray", 1);
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
				
				Boolean all = params.get(0).asOPBoolean().value(eval);
				
				List<Value<?, XPOperand<?>>> values = new ArrayList<>();
				
				ArrayValue<XPOperand<?>> res = new ArrayValue<>(values);
				
				Set<String> keys = object.getValue().keySet();
				
				
				Consumer<String> csm = 
					Boolean.TRUE.equals(all) ? 
						v -> values.add(new StringValue<>(v))
						: 
						v -> {if(v.startsWith("_")) return; values.add(new StringValue<>(v));  } ;
				
				keys.forEach(csm);
				
				
				return res;
			}
		};
	}

}
