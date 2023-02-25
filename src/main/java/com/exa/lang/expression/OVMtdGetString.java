package com.exa.lang.expression;

import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exa.expression.OMMethod;
import com.exa.expression.OMMethod.XPOrtMethod;
import com.exa.expression.Type;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.ClassesMan;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.Computing;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;

public class OVMtdGetString extends OMMethod.XPOrtMethod<ObjectValue<XPOperand<?>>, String> {

	public OVMtdGetString() {
		super("getString", 1);
	}

	@Override
	public boolean canManage(XPEvaluator eval, int order, int nbOperands) throws ManagedException {
		return true;
	}

	@Override
	public Type<?> type() {
		return ClassesMan.T_STRING;
	}

	@Override
	protected XPOrtMethod<ObjectValue<XPOperand<?>>, String>.XPMethodResult createResultOperand(XPOperand<ObjectValue<XPOperand<?>>> xpObject, Vector<XPOperand<?>> xpParams) {
		
		return new XPMethodResult(xpObject, xpParams) {
			
			@Override
			public String value(XPEvaluator eval) throws ManagedException {
				String fieldName = params.get(0).asOPString().value(eval);
				
				if(Computing.debugOn) {
					final Logger log = LoggerFactory.getLogger(OVMtdGetString.class);
					log.info(String.format("[DEBUG] : Getting '%s' field from ObjectValue", fieldName));
					
				}
				ObjectValue<XPOperand<?>> object = xpObject.value(eval);
				
				return object.getPathAttributAsString(fieldName);
			}
		};
	}

}
