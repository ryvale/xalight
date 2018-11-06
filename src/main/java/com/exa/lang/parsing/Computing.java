package com.exa.lang.parsing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exa.buffer.CharReader;
import com.exa.chars.EscapeCharMan;
import com.exa.expression.Type;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.ClassesMan;
import com.exa.expression.eval.MapVariableContext;
import com.exa.expression.eval.XPEvaluator;
import com.exa.expression.eval.XPEvaluator.ContextResolver;
import com.exa.expression.parsing.Parser;
import com.exa.expression.parsing.Parser.UnknownIdentifierValidation;
import com.exa.lexing.ParsingException;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.BooleanValue;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.DecimalValue;
import com.exa.utils.values.IntegerValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.StringValue;
import com.exa.utils.values.Value;

public class Computing {
	
	public static interface EvaluatorSetup {
		
		void setup(XPEvaluator evaluator) throws ManagedException;
	}
	
	public static final String PRTY_NAME = "_name";
	public static final String PRTY_CLASS = "_class";
	public static final String PRTY_TYPE = "_type";
	public static final String PRTY_EXTEND = "_extend";
	public static final String PRTY_OBJECT = "_object";
	public static final String PRTY_PARAMS = "_params";
	public static final String PRTY_CALL_PARAMS = "_call_params";
	public static final String PRTY_CONTEXT = "_context";
	public static final String PRTY_ENTITY = "_entity";
	
	public static final String LIBN_DEFAULT = "references";
	
	public static final String ET_NOW = "now";
	
	public static final String ET_RUNTIME = "runtime";
	
	private ObjectValue<XPOperand<?>> rootObject;
	private Parser xpCompiler;
	private XALLexingRules lexingRules = new XALLexingRules();
	private CharReader charReader;
	
	private List<ObjectValue<XPOperand<?>>> heirsObject = new ArrayList<>();
	
	private TypeSolver typeSolver;
	
	private ContextResolver contextResolver = (vcs, context) -> {
		VariableContext res = vcs.get(context);
		if(res != null) return res;
				
		String parts[] = context.split("[.]");
		StringBuilder sb = new StringBuilder(context);
		for(int i=parts.length -1; i > 0; i--) {
			String part = parts[i];
			sb.delete(sb.length()-part.length()-1, sb.length());
			
			res = vcs.get(sb.toString());
			if(res != null) return res;
		}
		return null;
	};
	
	public Computing(CharReader charReader, ObjectValue<XPOperand<?>> rootObject, VariableContext rootVariableContext, XPEvalautorFactory cclEvaluatorFacory) {
		super();
		this.rootObject = rootObject;
		this.xpCompiler = new Parser(rootVariableContext, contextResolver);
		
		typeSolver = new TypeSolver();
		
		XPEvaluator compEvaluator = xpCompiler.evaluator();
		compEvaluator.getClassesMan().forAllTypeDo((typeName, valueClass) -> {
			if(typeSolver.containsType(typeName)) return;
			
			typeSolver.registerType(typeName, valueClass);
			
		});
		
		this.charReader = charReader;
		//this.cclEvaluatorFacory = cclEvaluatorFacory;
	}
		
	public Computing(CharReader charReader, EvaluatorSetup evSteup, UnknownIdentifierValidation uiv) throws ManagedException {
		this.xpCompiler = new Parser(new MapVariableContext(), contextResolver, uiv);
		typeSolver = new TypeSolver();
		
		XPEvaluator compEvaluator = xpCompiler.evaluator();
		evSteup.setup(compEvaluator);
		
		compEvaluator.getClassesMan().forAllTypeDo((typeName, valueClass) -> {
			if(typeSolver.containsType(typeName)) return;
			
			typeSolver.registerType(typeName, valueClass);
			
		});
		
		this.rootObject = new ObjectValue<>();
		this.charReader = charReader;
		
		//this.cclEvaluatorFacory = new StandardXPEvaluatorFactory(typeSolver, evSteup);
	}
	
	
	public Computing(CharReader charReader) throws ManagedException {
		this(charReader, (evaluator) -> {}, (id, context) -> null); 
	}
	
	public Computing(CharReader charReader, VariableContext vc, XPEvalautorFactory cclEvaluatorFacory) {
		this.xpCompiler = new Parser(vc, contextResolver);
		this.rootObject = new ObjectValue<>();
		this.charReader = charReader;
		//this.cclEvaluatorFacory = cclEvaluatorFacory;
		
		typeSolver = new TypeSolver();
		
		XPEvaluator compEvaluator = xpCompiler.evaluator();
		compEvaluator.getClassesMan().forAllTypeDo((typeName, valueClass) -> {
			if(typeSolver.containsType(typeName)) return;
			
			typeSolver.registerType(typeName, valueClass);
			
		});
	}
	
	
	public void addCustomType(String name, Class<?> valueClass) {
		
	}
	
	public static Map<String, ObjectValue<XPOperand<?>>> getDefaultObjectLib(ObjectValue<XPOperand<?>> rootOV) throws ManagedException {
		Map<String, ObjectValue<XPOperand<?>>> res = new HashMap<>();
		res.put(Computing.LIBN_DEFAULT, rootOV.getAttributAsObjectValue(Computing.LIBN_DEFAULT));
		
		return res;
	}

	public ObjectValue<XPOperand<?>> execute() throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch.charValue() == ':') {
			lexingRules.nextNonBlankChar(charReader);
			
			String type = lexingRules.nextNonNullString(charReader);
			rootObject.setAttribut(PRTY_TYPE, type);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if(ch == null) return rootObject;
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after type value."));
			lexingRules.nextNonBlankChar(charReader);
		}
		
		ch = null;
		
		do {
			String propertyName;
			
			try {
				propertyName = lexingRules.nextRequiredPropertyName(charReader);
			}
			catch(LexingException e) {
				if(e.isRealParsingException()) throw e;
				
				if(e.getParsingString() == null) {
					if(ch == null) return rootObject;
				}
				throw e;
			}
			
			Value<?, XPOperand<?>> propertyValue = readPropertyValueForObject(propertyName);
			
			rootObject.setAttribut(propertyName, propertyValue);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			
			if(ch == null) break;
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after property value."));
			
			charReader.nextChar();
		}
		while(true);
		
		Value<?, XPOperand<?>> vlReferences = rootObject.getAttribut("references");
		
		if(vlReferences == null) return rootObject;
		
		ObjectValue<XPOperand<?>> references = vlReferences.asObjectValue();
		
		if(references == null) return rootObject;
		
		for(ObjectValue<XPOperand<?>> ov : heirsObject) {
		
			Set<String> cyclicRefs = new HashSet<>();
			
			String entity = ov.getAttributAsString(PRTY_ENTITY);
			
			getObjectInheritance(ov, references, cyclicRefs, entity);
		}
	
		
		return rootObject;
	}
	
	public ObjectValue<XPOperand<?>> object(String path, XPEvaluator evaluator) throws ManagedException {
		ObjectValue<XPOperand<?>> rootOV = execute();
		
		return object(rootOV, path, evaluator);
	}
	
	public static ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> rootOV, String path, XPEvaluator evaluator) throws ManagedException {
		ObjectValue<XPOperand<?>> res = rootOV;
		
		String parts[] = path.split("[.]");
		
		VariableContext lastVC = null;
		
		StringBuilder sbVCName = new StringBuilder();
		for(int i=0; i<parts.length; ++i) {
			String part = parts[i];
			
			if(i>0) sbVCName.append(".").append(part);
			else sbVCName.append(part);
			
			res = res.getRequiredAttributAsObjectValue(part);
			
			ObjectValue<XPOperand<?>> ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			if(ovCallParams == null) continue;
			do {
				lastVC = new MapVariableContext();
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				addParamsValueInContext(evaluator, lastVC, mpParams);
				
				evaluator.pushVariableContext(lastVC);
				
				ObjectValue<XPOperand<?>> ovMother = rootOV.getPathAttributAsObjecValue(motherClass);
				
				Map<String, Value<?, XPOperand<?>>> mpRes = res.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, res, evaluator);
				
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
				
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			} while(true);
		}
		
		try {
			res = computeAllCalculable(res, evaluator, Computing.getDefaultObjectLib(rootOV));
		} catch (CloneNotSupportedException e) {
			throw new ManagedException(e);
		}
		
		return res;
	}
	
	public static ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> relativeOV, String path, XPEvaluator evaluator, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		ObjectValue<XPOperand<?>> res = relativeOV;
		
		String parts[] = path.split("[.]");
		
		VariableContext lastVC = null;
		
		StringBuilder sbVCName = new StringBuilder();
		for(int i=0; i<parts.length; ++i) {
			String part = parts[i];
			
			if(i>0) sbVCName.append(".").append(part);
			else sbVCName.append(part);
			
			res = res.getRequiredAttributAsObjectValue(part);
			
			ObjectValue<XPOperand<?>> ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			if(ovCallParams == null) continue;
			do {
				lastVC = new MapVariableContext();
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				addParamsValueInContext(evaluator, lastVC, mpParams);
				
				evaluator.pushVariableContext(lastVC);
				
				String fqnParts[] = motherClass.split("[.]");

				
				ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
				
				ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
				
				Map<String, Value<?, XPOperand<?>>> mpRes = res.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, res, evaluator);
				
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
				
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			} while(true);
		}
		
		try {
			res = computeAllCalculable(res, evaluator, libOV);
		} catch (CloneNotSupportedException e) {
			throw new ManagedException(e);
		}
		
		return res;
	}
	
	public static ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> ovEntity, XPEvaluator evaluator, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		ObjectValue<XPOperand<?>> res = ovEntity;
		ObjectValue<XPOperand<?>> ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
		if(ovCallParams == null) return res;
		
		do {
			VariableContext lastVC = new MapVariableContext();
			
			Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
			
			Iterator<String> strIt = mpCallParams.keySet().iterator();
			String motherClass = strIt.next();
			
			ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
			Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
			
			addParamsValueInContext(evaluator, lastVC, mpParams);
			
			evaluator.pushVariableContext(lastVC);
			
			String fqnParts[] = motherClass.split("[.]");
	
			
			ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
			
			ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
			
			Map<String, Value<?, XPOperand<?>>> mpRes = ovEntity.getValue();
			mpRes.remove(PRTY_CALL_PARAMS);
			
			mergeInheritedObject(ovMother, ovEntity, evaluator);
			if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
		
			ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
		} while(true);
		try {
			res = computeAllCalculable(res, evaluator, libOV);
		} catch (CloneNotSupportedException e) {
			throw new ManagedException(e);
		}
		
		return res;
	}
	
	@SuppressWarnings("unchecked")
	private static ObjectValue<XPOperand<?>> computeAllCalculable(ObjectValue<XPOperand<?>> ovEntity, XPEvaluator evaluator, Map<String, ObjectValue<XPOperand<?>>> libOV) throws CloneNotSupportedException, ManagedException {
		ObjectValue<XPOperand<?>> res = ovEntity;
		
		ObjectValue<XPOperand<?>> ovCallParams = ovEntity.getAttributAsObjectValue(PRTY_CALL_PARAMS);
		if(ovCallParams != null) {
			do {
				VariableContext lastVC = new MapVariableContext();
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				addParamsValueInContext(evaluator, lastVC, mpParams);
				
				evaluator.pushVariableContext(lastVC);
				
				String fqnParts[] = motherClass.split("[.]");
		
				
				ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
				
				ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
				
				Map<String, Value<?, XPOperand<?>>> mpRes = ovEntity.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, ovEntity, evaluator);
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
			
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			} while(true);
		}
		
		Map<String, Value<?, XPOperand<?>>> mp = res.getValue();
		
		for(String propertyName : mp.keySet()) {
			Value<?, XPOperand<?>> vl=mp.get(propertyName);
			
			
			ObjectValue<XPOperand<?>> vov = vl.asObjectValue();
			if(vov != null) {
				
				mp.put(propertyName, computeAllCalculable(vov, evaluator, libOV));
				continue;
			}
			
			CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
			
			if(cl == null) continue;
			
			if(ET_RUNTIME.equals(cl.getEvalTime())) {
				XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
				xalCL.setEvaluator(evaluator);
				continue;
			}
			
			if("string".equals(cl.typeName())) {
				XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
				xalCL.setEvaluator(evaluator);

				mp.put(propertyName, new StringValue<>(xalCL.getValue()));
				continue;
			}
			
			if("integer".equals(cl.typeName())) {
				XALCalculabeValue<Integer> xalCL = (XALCalculabeValue<Integer>) cl;
				xalCL.setEvaluator(evaluator);
				
				mp.put(propertyName, new IntegerValue<>(xalCL.getValue()));
				continue;
			}
			
			if("float".equals(cl.typeName())) {
				XALCalculabeValue<Double> xalCL = (XALCalculabeValue<Double>) cl;
				xalCL.setEvaluator(evaluator);
				
				mp.put(propertyName, new DecimalValue<>(xalCL.getValue()));
				continue;
			}
						
			if("boolean".equals(cl.typeName())) {
				XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
				xalCL.setEvaluator(evaluator);
				
				mp.put(propertyName, new BooleanValue<>(xalCL.getValue()));
				continue;
			}
		}
		
		return res;
		
	}
	
	@SuppressWarnings("unchecked")
	private static void addParamsValueInContext(XPEvaluator evaluator, VariableContext vc, Map<String, Value<?, XPOperand<?>>> mpParams) throws ManagedException {
		for(String paramName : mpParams.keySet()) {
			Value<?, XPOperand<?>> vl = mpParams.get(paramName);
			
			CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
			if(cl != null) {
				
				if("string".equals(cl.typeName())) {
					XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
					xalCL.setEvaluator(evaluator);
					vc.addVariable(paramName, String.class, xalCL.getValue());
					continue;
				}
				
				if("integer".equals(cl.typeName())) {
					XALCalculabeValue<Integer> xalCL = (XALCalculabeValue<Integer>) cl;
					xalCL.setEvaluator(evaluator);
					vc.addVariable(paramName, Integer.class, xalCL.getValue());
					continue;
				}
				
				if("float".equals(cl.typeName())) {
					XALCalculabeValue<Double> xalCL = (XALCalculabeValue<Double>) cl;
					xalCL.setEvaluator(evaluator);
					vc.addVariable(paramName, Double.class, xalCL.getValue());
					continue;
				}
				
				if("date".equals(cl.typeName())) {
					XALCalculabeValue<Date> xalCL = (XALCalculabeValue<Date>) cl;
					xalCL.setEvaluator(evaluator);
					vc.addVariable(paramName, Date.class, xalCL.getValue());
					continue;
				}
				
				if("boolean".equals(cl.typeName())) {
					XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
					xalCL.setEvaluator(evaluator);
					vc.addVariable(paramName, Boolean.class, xalCL.getValue());
					continue;
				}
				
				continue;
			}
			
			StringValue<XPOperand<?>> stVl = vl.asStringValue();
			if(stVl != null) {
				vc.addVariable(paramName, String.class, stVl.getValue());
				continue;
			}
			
			IntegerValue<XPOperand<?>> itVl = vl.asIntegerValue();
			if(itVl != null) {
				vc.addVariable(paramName, Integer.class, itVl.getValue());
				continue;
			}
			
			BooleanValue<XPOperand<?>> blVl = vl.asBooleanValue();
			if(blVl != null) {
				vc.addVariable(paramName, Boolean.class, blVl.getValue());
				continue;
			}
			
			DecimalValue<XPOperand<?>> dcVl = vl.asDecimalValue();
			if(dcVl != null) {
				vc.addVariable(paramName, Double.class, dcVl.getValue());
				continue;
			}
			
			ObjectValue<XPOperand<?>> obVl = vl.asObjectValue();
			if(obVl != null) {
				vc.addVariable(paramName, Object.class, obVl.getValue());
				continue;
			}
		}
	}
	
	private void getObjectInheritance(ObjectValue<XPOperand<?>> dst, ObjectValue<XPOperand<?>> references,  Set<String> cyclicRefs, String entity) throws ManagedException {
		
		String objectClass = dst.getAttributAsString(PRTY_OBJECT);
		
		if(objectClass == null) return;
		
		if(cyclicRefs.contains(objectClass)) throw new ManagedException(String.format("Cyclic reference in inheritance for %s. ( %s )", dst, cyclicRefs.toString()));
		
		ObjectValue<XPOperand<?>> src = references.getAttributAsObjectValue(objectClass);
		if(src == null) return;
		
		cyclicRefs.add(objectClass);
		if(src.getAttribut(PRTY_OBJECT) != null) getObjectInheritance(src, references, cyclicRefs, entity);
		
		Map<String, Value<?, XPOperand<?>>> mpDst = dst.getValue();
		
		mergeObject(src, dst, objectClass, entity);
		
		mpDst.remove(PRTY_OBJECT);
	}
	
	public Value<?, XPOperand<?>> readPropertyValueForObject(String context) throws ManagedException {
		//variableContextAnyWay(context);
		Value<?, XPOperand<?>> res = _readPropertyValueForObjectWithOutDec(context);
		if(res != null) return res;
		Character ch = lexingRules.nextForwardChar(charReader);
		
		
		if('(' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			
			return readObjectWithDeclarationParam(context);
		}
		
		throw new ParsingException(String.format("Unexpected error near %s", ch.toString()));
	}
	
	private ObjectValue<XPOperand<?>> readObjectWithDeclarationParam(String context) throws ManagedException {
		XPEvaluator evaluator = xpCompiler.evaluator();
		VariableContext vc = new MapVariableContext();
		evaluator.pushVariableContext(vc);
		
		ObjectValue<XPOperand<?>> params = readFunctionParamsDeclaration(vc);
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		res.setAttribut(PRTY_PARAMS, params);
		
		Character ch = lexingRules.nextForwardRequiredNonBlankChar(charReader);
		if('{' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			
			res = readObjectBody(res, context);
			evaluator.popVariableContext();
			return res;
		}
		
		if('@' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			res = readObjectByClass(res, context);
			evaluator.popVariableContext();
			return res;
		}
		
		if(',' == ch) {
			evaluator.popVariableContext();
			return res;
		}
		
		throw new ParsingException(String.format("'{', ',' or '@' expected after declarion params. Unexpected %S", ch.toString()));
	}
	
	private Value<?, XPOperand<?>> readFunctionCallParams(String context) throws ManagedException {
		
		Character ch = lexingRules.nextForwardRequiredNonBlankChar(charReader);
		if('#' == ch) {
			ObjectValue<XPOperand<?>> ovRes = new ObjectValue<>();
			lexingRules.nextNonBlankChar(charReader);
			do {
				String propertyName = lexingRules.nextRequiredPropertyName(charReader);
				
				Value<?, XPOperand<?>> propetyValue = _readPropertyValueForObjectWithOutDec(context);
				
				ch = lexingRules.nextForwardRequiredNonBlankChar(charReader);
				
				//String newContext = context + "." + propertyName;
				
				ovRes.setAttribut(propertyName, propetyValue);
				
				if(')' == ch) {
					lexingRules.nextNonBlankChar(charReader);
					break;
				}
				
				if(',' != ch) throw new ManagedException(String.format("Unexpected char %s while reading function call params", ch.toString()));
				
			} while(true);
			
			return ovRes;
		}
		
		ArrayValue<XPOperand<?>> avRes = new ArrayValue<>();
		
		ch = lexingRules.nextForwardRequiredNonBlankChar(charReader);
		if(')' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			return avRes;
		}
		
		do {
			//xpCompiler xpCompiler = new xpCompiler(getVariablesContext(context));
			XPOperand<?> xp = xpCompiler.parse(charReader, (lr, charReader) -> true, context);
			
			Value<?, XPOperand<?>> propetyValue = calculableFor(xp, "now");
			
			ch = lexingRules.nextForwardRequiredNonBlankChar(charReader);
			
			avRes.add(propetyValue);
			
			if(')' == ch) {
				lexingRules.nextNonBlankChar(charReader);
				break;
			}
			
			if(',' != ch) throw new ManagedException(String.format("Unexpected char %s while reading function call params", ch.toString()));
			
			lexingRules.nextNonBlankChar(charReader);
		} while(true);
		
		return avRes;
	}

	private XALCalculabeValue<?> calculableFor(XPOperand<?> xp, String evalTime) {
		Type<?> type = xp.type();
		
		if(type == ClassesMan.T_STRING) return new XALCalculabeValue<>(ClassesMan.T_STRING.valueOrNull(xp), evalTime);
		
		if(type == ClassesMan.T_INTEGER) return new XALCalculabeValue<>(ClassesMan.T_INTEGER.valueOrNull(xp), evalTime);
		
		if(type == ClassesMan.T_BOOLEAN) return new XALCalculabeValue<>(ClassesMan.T_BOOLEAN.valueOrNull(xp), evalTime);
		
		if(type == ClassesMan.T_DOUBLE) return new XALCalculabeValue<>(ClassesMan.T_DOUBLE.valueOrNull(xp), evalTime);
		
		if(type == ClassesMan.T_DATE) return new XALCalculabeValue<>(ClassesMan.T_DATE.valueOrNull(xp), evalTime);
		
		return new XALCalculabeValue<>(xp, evalTime);
	}
	
	/*private VariableContext variableContextAnyWay(String context) throws ManagedException {
		String varContextName  = getVariableContextName(context);
		XPEvaluator evaluator = xpCompiler.evaluator();
		VariableContext res = evaluator.getVariableContext(varContextName);
		if(res == null) {
			res = new MapVariableContext();
			evaluator.addVariableContext(res, varContextName, evaluator.getDefaultVariableContext());
		}
		
		return res;
	}
	*/
	
	/*private VariableContext variableContextAnyWay(String context) throws ManagedException {
		
		XPEvaluator evaluator = xpCompiler.evaluator();
		VariableContext res = new MapVariableContext();
		evaluator.pushVariableContext(res);
		
		
		return res;
	}
	*/
	
	public static String getVariableContextName(String context) {
		String parts[] = context.split("[.]");
		return parts.length > 1 ? parts[0] + "_" + parts[1] : parts[0];
	}
	
	public Value<?, XPOperand<?>> readObjectBody(String context) throws ManagedException {
		return readObjectBody(new ObjectValue<>(), context);
	}
	
	public ObjectValue<XPOperand<?>> readObjectBody(ObjectValue<XPOperand<?>> ov, String context) throws ManagedException {
		//ObjectValue ov = new ObjectValue();
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if('}' == ch.charValue()) {
			charReader.nextChar();
			return ov;
		}
		
		ch = null;
		do {
			String propertyName;
			
			try {
				propertyName = lexingRules.nextRequiredPropertyName(charReader);
			}
			catch(LexingException e) {
				if(e.isRealParsingException()) throw e;
				
				if(e.getParsingString() == null) {
					if(ch == null) throw new ParsingException(String.format("Unexpected end of file. Expecting '}' or property"));
					throw e;
				}
				if("}".equals(e.getParsingString())) return ov;
				throw new ParsingException(String.format("Error near %s . '}' or property expected.", e.getParsingString()));
			}
			
			String newContext = context + "." + propertyName;
			
			Value<?, XPOperand<?>> propertyValue = readPropertyValueForObject(newContext);
			
			String parts[] = context.split("[.]");
			if(parts.length == 1) {
				ObjectValue<XPOperand<?>> ovEntity = propertyValue.asObjectValue();
				if(ovEntity != null) {
					ovEntity.setAttribut(PRTY_ENTITY, newContext);
				}
			}
			
			ov.setAttribut(propertyName, propertyValue);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file after property %s reading", propertyName));
			
			if('}' == ch.charValue())  { charReader.nextChar();	break;	}
			
			if(ch != ',') 
				throw new ParsingException(String.format("',' expected after property value."));
			
			lexingRules.nextNonBlankChar(charReader);
		}
		while(true);
		
		return ov;
	}
	
	private ObjectValue<XPOperand<?>> readFunctionParamsDeclaration(VariableContext vc) throws ManagedException {
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		
		//VariableContext vc = variableContextAnyWay(context);
		
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(')' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			return res;
		}
		
		do {
			String propertyName = lexingRules.nextRequiredPropertyName(charReader);
			String type  = null;
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			if(ch == null) throw new ManagedException(String.format("Unexpected end of file while reading declaration params"));
			
			if('@' == ch) {
				lexingRules.nextNonBlankChar(charReader);
				
				type = lexingRules.nextNonNullString(charReader);
				
				if(!typeSolver.containsType(type)) throw new ManagedException(String.format("Unknown %s while expecting type after @", type));
				
				
				ch = lexingRules.nextForwardNonBlankChar(charReader);
				if(ch == null) throw new ManagedException(String.format("Unexpected end of file while reading declaration params"));
			}
			
			if(type == null) type = "string";
			res.setAttribut(propertyName, type);
			
			Class<?> valueClass = typeSolver.getTypeValueClass(type);
			vc.addVariable(propertyName, valueClass, null);
			
			
			if(')' == ch) {
				lexingRules.nextNonBlankChar(charReader);
				break;
			}
			
			if(',' != ch) throw new ManagedException(String.format("Unexpected char %s while reading declaration params", ch.toString()));
			
			lexingRules.nextNonBlankChar(charReader);
		}
		while(true);
		
		return res;
	}
	
	private Value<?, XPOperand<?>> _readPropertyValueForObjectWithOutDec(String context) throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		
		if(ch == null) return new BooleanValue<XPOperand<?>>(Boolean.TRUE);
		
		if(ch == '{') {
			lexingRules.nextNonBlankChar(charReader);
			return readObjectBody(context);
		}
		
		if(ch == '[') { 
			lexingRules.nextNonBlankChar(charReader); 
			return readArrayBody(context);
		}
		
		if(ch == ',') return new BooleanValue<XPOperand<?>>(Boolean.TRUE);
		
		if(ch == '\'' || ch == '\"') return readString(ch.toString());
				
		if(XALLexingRules.NUMERIC_DIGITS.indexOf(ch) >= 0) return readNumeric();
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) >= 0) {
			String str = lexingRules.nextNonNullString(charReader);
			
			if("true".equals(str)) return new BooleanValue<>(Boolean.TRUE);
			
			if("false".equals(str)) return new BooleanValue<>(Boolean.FALSE);
			
			if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
			
			ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
			ov.setAttribut(PRTY_NAME, str);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if(ch == '{') {
				lexingRules.nextNonBlankChar(charReader);
				return readObjectBody(ov, context);
			}
			
			if('@' == ch) {
				lexingRules.nextNonBlankChar(charReader);
				
				readObjectByClass(ov, context);
				
				return ov;
			}
			
			return ov;
		}
		
		if('@' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			return readObjectByClass(context);
		}
		
		if('=' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			XPOperand<?> xp; String evalTime;
			
			ch = lexingRules.nextForwardChar(charReader);
			if(ch == '>') {
				charReader.nextChar();
				evalTime = "runtime";
			}
			else evalTime = "now";
			xp = xpCompiler.parse(charReader, (lr, charReader) -> true, context);
			
			return calculableFor(xp, evalTime);
		}
		
		
		return null;
	}
	
	private ObjectValue<XPOperand<?>> readObjectByClass(String context) throws ManagedException {
		return readObjectByClass(new ObjectValue<XPOperand<?>>(), context);
	}
	
	private Value<? extends Number, XPOperand<?>> readNumeric() throws ManagedException {
		String str = lexingRules.nextNonNullString(charReader);
		
		if(!lexingRules.isInteger(str, true)) throw new ParsingException(String.format("%s is not not numeric", str));
		
		if(XALLexingRules.EXTENDED_NUMERIC_TERMINATION.indexOf(str.charAt(str.length() - 1)) >= 0) return new DecimalValue<XPOperand<?>>(Double.parseDouble(str));
		
		Character ch = lexingRules.nextForwardChar(charReader);
		
		if(ch == null) return new IntegerValue<XPOperand<?>>(Integer.parseInt(str));
		
		if(ch == '.') {
			charReader.nextChar();
			
			ch = lexingRules.nextForwardChar(charReader);
			if(ch == null || XALLexingRules.NUMERIC_DIGITS.indexOf(ch) < 0) return new DecimalValue<XPOperand<?>>(Double.parseDouble(str));
			
			String str2 = lexingRules.nextNonNullString(charReader);
			
			if(!lexingRules.isInteger(str, true)) throw new ParsingException(String.format("%s is not not numeric", str));
			
			return new DecimalValue<XPOperand<?>>(Double.parseDouble(str+"."+str2));
		}
		
		return new IntegerValue<XPOperand<?>>(Integer.parseInt(str));
	}
	
	private StringValue<XPOperand<?>> readString(String end) throws ManagedException {
		String str = readStringReturnString(end);
		
		return new StringValue<XPOperand<?>>(str);
	}
	
	private String readStringReturnString(String end) throws ManagedException {
		String str = lexingRules.nextNonNullString(charReader);
		
		if(!str.endsWith(end)) throw new ManagedException(String.format("%s is not a valid string", str));
		
		StringBuilder sb = new StringBuilder(str.substring(1, str.length()-1));
		EscapeCharMan.STANDARD.normalized(sb);
		return sb.toString();
	}
	
	public Value<?, XPOperand<?>> readArrayBody(String context) throws ManagedException {
		ArrayValue<XPOperand<?>> av = new ArrayValue<>();
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(']' == ch.charValue()) {
			charReader.nextChar();
			return av;
		}
		
		ch = null;
		
		int i = 0;
		do {
			Value<?, XPOperand<?>> item = readArrayItem(context);
			
			ObjectValue<?> ovItem = item.asObjectValue();
			if(ovItem != null) {
				ovItem.setAttribut(PRTY_ENTITY, context + "[" + i + "]");
			}
			
			av.add(item);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file reading array item"));
			
			if(']' == ch.charValue())  { charReader.nextChar();	break;	}
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after property value."));
			
			lexingRules.nextNonBlankChar(charReader);
			i++;
			
		} while(true);
		
		return av;
	}
	
	private Value<?, XPOperand<?>> readArrayItem(String context) throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		
		if(ch == null) throw new ParsingException(String.format("Unexpected end of file while reading array item"));
		
		if(ch == '{') {
			lexingRules.nextNonBlankChar(charReader);
			return readObjectBody(context);
		}
		
		if(ch == '[') { 
			lexingRules.nextNonBlankChar(charReader);
			readArrayBody(context);
		}
		
		if(ch == ',') return new BooleanValue<>(Boolean.TRUE);
		
		if(ch == '\'' || ch == '\"') return readString(ch.toString());
		
		if(XALLexingRules.NUMERIC_DIGITS.indexOf(ch) >= 0) return readNumeric();
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) >= 0) {
			String str = lexingRules.nextNonNullString(charReader);
			
			if("true".equals(str)) return new BooleanValue<>(Boolean.TRUE);
			
			if("false".equals(str)) return new BooleanValue<>(Boolean.FALSE);
			
			if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
			
			ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
			ov.setAttribut("_name", str);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if(ch == '{') {
				lexingRules.nextNonBlankChar(charReader);
				return readObjectBody(ov, context);
			}
			
			if('@' == ch) {
				charReader.nextChar();
				return readObjectByClass(ov, context);
			}
			
			
			return ov;
		}
		
		if('@' == ch) {
			charReader.nextChar();
			return readObjectByClass(context);
		}
		
		if('=' == ch) {
			charReader.nextChar();
			
			ch = lexingRules.nextForwardChar(charReader);
			
			XPOperand<?> xp; String evalTime;
			if('>' == ch) {
				charReader.nextChar();
				evalTime = "runtime";
			}
			else evalTime = "now";
			
			xp = xpCompiler.parse(charReader, (lr, charReader) -> true, context);
			
			return calculableFor(xp, evalTime);
		}
		
		throw new ParsingException(String.format("Unexpected error near %s", ch.toString()));
	}
	
	private ObjectValue<XPOperand<?>> readObjectByClass(ObjectValue<XPOperand<?>> ov, String context) throws ManagedException {
		Character ch = lexingRules.nextForwardRequiredNonBlankChar(charReader);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) throw new ParsingException(String.format("Error near %s .", ch.toString()));
		
		String motherClass = lexingRules.nextNonNullString(charReader);
		if(!lexingRules.isIdentifier(motherClass)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", motherClass));
		
		ov.setAttribut(PRTY_OBJECT, motherClass);
		heirsObject.add(ov);
		
		ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) return ov;
		
		if('(' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			Value<?, XPOperand<?>> params = readFunctionCallParams(context);
			
			ObjectValue<XPOperand<?>> ovParams = ov.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			if(ovParams == null) {
				ovParams = new ObjectValue<>();
				ov.setAttribut(PRTY_CALL_PARAMS, ovParams);
			}
			
			ovParams.setAttribut("references."+motherClass, params);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			if(ch == null) return ov;
		}
	
		if('{' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			return readObjectBody(ov, context);
		}
		
		return ov;
	}
	
	private void mergeObject(ObjectValue<XPOperand<?>> src, ObjectValue<XPOperand<?>> dst, String objectClass, String entity) throws ManagedException {
		Map<String, Value<?, XPOperand<?>>> mpSrc = src.getValue();
		Map<String, Value<?, XPOperand<?>>> mpDst = dst.getValue();
		
		checkParameters(mpSrc, mpDst);
		
	}
	
	private static void mergeInheritedObject(ObjectValue<XPOperand<?>> src, ObjectValue<XPOperand<?>> dst, XPEvaluator evaluator) throws ManagedException {
		Map<String, Value<?, XPOperand<?>>> mpSrc = src.getValue();
		Map<String, Value<?, XPOperand<?>>> mpDst = dst.getValue();
		
		for(String v : mpSrc.keySet()) {
			if(PRTY_PARAMS.equals(v)) continue;
			
			Value<?, XPOperand<?>> vlv = dst.getAttribut(v);
			if(vlv != null) {
				ObjectValue<XPOperand<?>> dstOVAttribut = vlv.asObjectValue();
				if(dstOVAttribut == null || dstOVAttribut.getAttribut(PRTY_CALL_PARAMS) != null) {
					continue;
				}
				
				vlv = src.getAttribut(v);
				if(vlv == null) continue;
				
				ObjectValue<XPOperand<?>> srcOVAttribut = vlv.asObjectValue();
				if(srcOVAttribut == null) continue;
				
				
				mergeInheritedObject(srcOVAttribut, dstOVAttribut, evaluator);
				continue;
			}
			
			vlv = src.getAttribut(v);
			if(vlv == null) continue;
			
			/*if(PRTY_CALL_PARAMS.equals(v)) {
				try {
					vlv = vlv.clone();
					Map<String, Value<?, XPOperand<?>>> mpCallParams = vlv.asRequiredObjectValue().getValue();
					
					Iterator<Value<?, XPOperand<?>>> it = mpCallParams.values().iterator();
					
					ArrayValue<XPOperand<?>> aparams = it.next().asArrayValue();
					
					List<Value<?, XPOperand<?>>> paramItems = aparams.getValue();
					
					for(int i=0; i<paramItems.size(); ++i) {
						CalculableValue<?, XPOperand<?>> cl = paramItems.get(i).asCalculableValue();
						if(cl == null) continue;
						
						if("string".equals(cl.typeName())) {
							XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
							xalCL.setEvaluator(evaluator);
							
							paramItems.set(i, new StringValue<>(xalCL.getValue()));
							continue;
						}
						
						if("integer".equals(cl.typeName())) {
							XALCalculabeValue<Integer> xalCL = (XALCalculabeValue<Integer>) cl;
							xalCL.setEvaluator(evaluator);
							paramItems.set(i, new IntegerValue<>(xalCL.getValue()));
							continue;
						}
						
						if("float".equals(cl.typeName())) {
							XALCalculabeValue<Double> xalCL = (XALCalculabeValue<Double>) cl;
							xalCL.setEvaluator(evaluator);
							paramItems.set(i, new DecimalValue<>(xalCL.getValue()));
							continue;
						}
												
						if("boolean".equals(cl.typeName())) {
							XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
							xalCL.setEvaluator(evaluator);
							paramItems.set(i, new BooleanValue<>(xalCL.getValue()));
							continue;
						}
						
					}
					
					ObjectValue<XPOperand<?>> oparams = it.next().asObjectValue();
					
					Map<String, Value<?, XPOperand<?>>> mpop = oparams.getValue();
					
					for(String paramName : mpop.keySet()) {
						CalculableValue<?, XPOperand<?>> cl = mpop.get(paramName).asCalculableValue();
						
						if(cl == null) continue;
						
						if("string".equals(cl.typeName())) {
							XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
							xalCL.setEvaluator(evaluator);

							mpop.put(paramName, new StringValue<>(xalCL.getValue()));
							continue;
						}
						
						if("integer".equals(cl.typeName())) {
							XALCalculabeValue<Integer> xalCL = (XALCalculabeValue<Integer>) cl;
							xalCL.setEvaluator(evaluator);
							
							mpop.put(paramName, new IntegerValue<>(xalCL.getValue()));
							continue;
						}
						
						if("float".equals(cl.typeName())) {
							XALCalculabeValue<Double> xalCL = (XALCalculabeValue<Double>) cl;
							xalCL.setEvaluator(evaluator);
							
							mpop.put(paramName, new DecimalValue<>(xalCL.getValue()));
							continue;
						}
						
						
						if("boolean".equals(cl.typeName())) {
							XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
							xalCL.setEvaluator(evaluator);
							
							mpop.put(paramName, new BooleanValue<>(xalCL.getValue()));
							continue;
						}
					}
					
					
				} catch (CloneNotSupportedException e) {
					throw new ManagedException(e);
				}
				
				
			}*/
			
			
			mpDst.put(v, vlv);
			
		}
	}
	
	private void checkParameters(Map<String, Value<?, XPOperand<?>>> mpOvSrc, Map<String, Value<?, XPOperand<?>>> mpOvDst) throws ManagedException {
		Value<?, XPOperand<?>> src = mpOvSrc.get(PRTY_PARAMS);
		
		Value<?, XPOperand<?>> dstContexts = mpOvDst.get(PRTY_CALL_PARAMS);
		
		if(src == null && dstContexts == null) return;
		
		ObjectValue<XPOperand<?>> ovDstContexts = dstContexts.asRequiredObjectValue();
	
		
		String motherClass = mpOvDst.get(PRTY_OBJECT).asRequiredString();
		Iterator<Value<?, XPOperand<?>>> it = ovDstContexts.getValue().values().iterator();
		Value<?, XPOperand<?>> dst = it.hasNext() ? it.next() : null;
		
		//Value<?, XPOperand<?>> dst = ovDstContexts.getRequiredAttribut("references_" + motherClass);
		
		if(src == null || dst == null) throw new ManagedException(String.format("The number arguments does'nt match."));
		
		//ArrayValue<XPOperand<?>> avSrc = src.asArrayValue();
		
		ObjectValue<XPOperand<?>> ovSrc = src.asObjectValue();
		
		if(ovSrc == null) throw new ManagedException(String.format("The called source arguments are invalid."));
		
		ArrayValue<XPOperand<?>> avDst = dst.asArrayValue();
		
		ObjectValue<XPOperand<?>> ovDst = dst.asObjectValue();
		
		if(avDst == null && ovDst == null) throw new ManagedException(String.format("The caller destination arguments are invalid."));
		
		Map<String, Value<?, XPOperand<?>>> mpSrc = ovSrc.getValue();
		
		int nbSrc = mpSrc.keySet().size();
		if(avDst == null) {
			Map<String, Value<?, XPOperand<?>>> mpDst = ovDst.getValue();
			int nbDst = mpDst.keySet().size();
			
			if(nbSrc != nbDst) throw new ManagedException(String.format("The number of source argument ( %s ) is different than the number of destination on ( %s )", nbSrc, nbDst));
			
			for(String v : mpSrc.keySet()) {
				Value<?, XPOperand<?>> vlDstPrm = mpDst.get(v);
				if(vlDstPrm == null) throw new ManagedException(String.format("The parameter %s is not defined", v));
				
				Value<?, XPOperand<?>> vlSrcPrm = mpSrc.get(v);
				
				String srcType = vlSrcPrm.asString();
				
				CalculableValue<?, XPOperand<?>> clDstPrm = vlDstPrm.asCalculableValue();
				
				String dstType = clDstPrm == null ? typeSolver.getTypeName(vlDstPrm.getClass()) : clDstPrm.typeName();
				
				if(dstType == null || srcType.equals(dstType)) continue;
				
				throw new ManagedException(String.format("Type mismatch %s %s", srcType, dstType));
				
			}
			
			return;
		}
		
		ovDst = new ObjectValue<>();
		List<Value<?, XPOperand<?>>> lsDst = avDst.getValue();
		int nbDst = lsDst.size();
		if(nbSrc != nbDst) throw new ManagedException(String.format("The number of source argument ( %s ) is different than the number of destination on ( %s )", nbSrc, nbDst));
		
		int i= 0;
		for(String v : mpSrc.keySet()) {
			Value<?, XPOperand<?>> vlDstPrm = lsDst.get(i);
			if(vlDstPrm == null) throw new ManagedException(String.format("The %s th parameter is not defined", i));
			
			Value<?, XPOperand<?>> vlSrcPrm = mpSrc.get(v);
			
			String srcType = vlSrcPrm.asString();
			
			CalculableValue<?, XPOperand<?>> clDstPrm = vlDstPrm.asCalculableValue();
			
			String dstType = clDstPrm == null ? typeSolver.getTypeName(vlDstPrm.getClass()) : clDstPrm.typeName();
			
			//String dstType = valueTypeMapOnTypes.get(vlDstPrm.getClass());
			
			if(dstType == null || srcType.equals(dstType)) { ovDst.setAttribut(v, vlDstPrm);  i++; continue;}
			
			throw new ManagedException(String.format("Type mismatch %s %s", srcType, dstType));
			
		}
		
		ovDstContexts.setAttribut("references_" + motherClass, ovDst);
		//mpOvDst.put(PRTY_CALL_PARAMS, ovDst);
	}

}
