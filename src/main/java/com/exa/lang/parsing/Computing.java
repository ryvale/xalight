package com.exa.lang.parsing;

import java.io.IOException;
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
import com.exa.expression.parsing.Parser;
import com.exa.expression.parsing.Parser.UnknownIdentifierValidation;

import com.exa.lang.expression.XALCalculabeValue;
import com.exa.lang.expression.XPEvaluatorSetup;
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
		
	class XPParser extends Parser {

		public XPParser(UnknownIdentifierValidation unknownIdValidation) {
			this(new MapVariableContext()/*, XPEvaluator.CR_DEFAULT*/, unknownIdValidation);
		}

		public XPParser(VariableContext variableContext/*, ContextResolver contextResolver*/, UnknownIdentifierValidation unknownIdValidation) {
			super(variableContext/*, contextResolver*/, unknownIdValidation);
			
			classesMan.registerClass(XALParser.T_OBJECT_VALUE);
		}

		public XPParser(VariableContext variableContext/*, ContextResolver contextResolver*/) {
			this(variableContext/*, contextResolver*/, (id, context) -> null);
		}
		
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
	public static final String PRTY_STATEMENT = "_statement";
	public static final String PRTY_CONDITION = "_condition";
	public static final String PRTY_THEN = "_then";
	public static final String PRTY_ELSE = "_else";
	
	public static final String LIBN_DEFAULT = "references";
	
	public static final String ET_NOW = "now";
	
	public static final String ET_RUNTIME = "runtime";
	
	private ObjectValue<XPOperand<?>> rootObject;
	private Parser xpCompiler;
	
	private XALParser parser;
	//private XALLexingRules lexingRules /* = new XALLexingRules()*/;
	private CharReader charReader;
	
	private List<ObjectValue<XPOperand<?>>> heirsObject = new ArrayList<>();
	
	private TypeSolver typeSolver;
	
	public Computing(XALParser parser, CharReader charReader, ObjectValue<XPOperand<?>> rootObject, VariableContext rootVariableContext, XPEvalautorFactory cclEvaluatorFacory) {
		super();
		this.parser = parser;
		this.rootObject = rootObject;
		this.xpCompiler = new XPParser(rootVariableContext/*, contextResolver*/);
		
		typeSolver = new TypeSolver();
		
		XPEvaluator compEvaluator = xpCompiler.evaluator();
		compEvaluator.getClassesMan().forAllTypeDo((typeName, valueClass) -> {
			if(typeSolver.containsType(typeName)) return;
			
			typeSolver.registerType(typeName, valueClass);
			
		});
		
		this.charReader = charReader;
	}
		
	public Computing(XALParser parser, CharReader charReader, XPEvaluatorSetup evSteup, UnknownIdentifierValidation uiv) throws ManagedException {
		this.parser = parser;
		
		this.xpCompiler = new XPParser(new MapVariableContext()/*, contextResolver*/, uiv);
		typeSolver = new TypeSolver();
		
		XPEvaluator compEvaluator = xpCompiler.evaluator();
		evSteup.setup(compEvaluator);
		
		compEvaluator.getClassesMan().forAllTypeDo((typeName, valueClass) -> {
			if(typeSolver.containsType(typeName)) return;
			
			typeSolver.registerType(typeName, valueClass);
			
		});
		
		this.rootObject = new ObjectValue<>();
		this.charReader = charReader;
	}
	
	public Computing(XALParser pareser, CharReader charReader) throws ManagedException {
		this(pareser, charReader, (evaluator) -> {}, (id, context) -> null); 
	}
	
	public Computing(CharReader charReader, VariableContext vc, XPEvalautorFactory cclEvaluatorFacory) {
		this.xpCompiler = new XPParser(vc);
		this.rootObject = new ObjectValue<>();
		this.charReader = charReader;
		
		typeSolver = new TypeSolver();
		
		XPEvaluator compEvaluator = xpCompiler.evaluator();
		compEvaluator.getClassesMan().forAllTypeDo((typeName, valueClass) -> {
			if(typeSolver.containsType(typeName)) return;
			
			typeSolver.registerType(typeName, valueClass);
			
		});
	}
	
	public void closeCharReader() throws IOException {
		this.charReader.close();
	}
	
	public void addCustomType(String name, Class<?> valueClass) {
		
	}
	
	public static Map<String, ObjectValue<XPOperand<?>>> getDefaultObjectLib(ObjectValue<XPOperand<?>> rootOV) throws ManagedException {
		Map<String, ObjectValue<XPOperand<?>>> res = new HashMap<>();
		res.put(Computing.LIBN_DEFAULT, rootOV.getAttributAsObjectValue(Computing.LIBN_DEFAULT));
		
		return res;
	}

	public ObjectValue<XPOperand<?>> execute() throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
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
	
	/*public ObjectValue<XPOperand<?>> object(String path, XPEvaluator evaluator) throws ManagedException {
		ObjectValue<XPOperand<?>> rootOV = execute();
		
		return object(rootOV, path, evaluator);
	}*/
	
	public static ObjectValue<XPOperand<?>> object(XALParser parser, ObjectValue<XPOperand<?>> rootOV, String path, XPEvaluator evaluator, VariableContext entityVC) throws ManagedException {
		return object(parser, rootOV.getAttributByPathAsObjectValue(path), evaluator, entityVC, Computing.getDefaultObjectLib(rootOV));
	}
	
	public static ObjectValue<XPOperand<?>> object(XALParser parser, ObjectValue<XPOperand<?>> relativeOV, String path, XPEvaluator evaluator, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		/*ObjectValue<XPOperand<?>> res = relativeOV;
		
		String parts[] = path.split("[.]");
		
		VariableContext lastVC = null;
		
		VariableContext parentVC = evaluator.getCurrentVariableContext();
		
		VariableContext entityParentVC = null;

		for(int i=0; i<parts.length-1; ++i) {
			String part = parts[i];
			
			res = res.getRequiredAttributAsObjectValue(part);
			
			ObjectValue<XPOperand<?>> ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			if(ovCallParams == null) continue;
			boolean firtVC = true;
			do {
				lastVC = new MapVariableContext(evaluator.getCurrentVariableContext());
				if(firtVC) {
					entityParentVC = lastVC;
					firtVC = false;
				}
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				addParamsValueInContext(evaluator, parentVC, lastVC, mpParams);
				
				evaluator.pushVariableContext(lastVC);
				
				String fqnParts[] = motherClass.split("[.]");

				ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
				
				ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
				
				Map<String, Value<?, XPOperand<?>>> mpRes = res.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, res, evaluator, lastVC);
				
				parentVC = lastVC;
				
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
				
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			} while(true);
		}
		
		if(entityParentVC != null) entityVC.setParent(entityParentVC);*/
		
		return object(parser, relativeOV.getRequiredAttributAsObjectValue(path), evaluator, entityVC, libOV);
	}
	
	public static Value<?, XPOperand<?>> value(XALParser parser, Value<?, XPOperand<?>> vlEntity, XPEvaluator evaluator, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		
		ObjectValue<XPOperand<?>> ovEntity = vlEntity.asObjectValue();
		if(ovEntity == null) {
			return vlEntity;
		}
		Map<String, Value<?, XPOperand<?>>> mpProperties = ovEntity.getValue();
		
		if(mpProperties.containsKey(PRTY_STATEMENT)) {
			ComputingStatement cs = parser.getStatements().get(ovEntity.getRequiredAttributAsString(PRTY_STATEMENT));
			
			Value<?, XPOperand<?>> vl = cs.translate(ovEntity, evaluator, entityVC, libOV);
			if(vl == null) return null;
			ovEntity = vl.asObjectValue();
			
			if(ovEntity == null) return vl;
		}
		
		ObjectValue<XPOperand<?>> res = ovEntity;
		
		ObjectValue<XPOperand<?>> ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
		
		if(ovCallParams == null) {
			computeCalculabe(parser, ovEntity, evaluator, entityVC, libOV);
		} else {
			VariableContext parentVC = entityVC;
			
			do {
				VariableContext lastVC = new MapVariableContext(parentVC);
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				addParamsValueInContext(evaluator, parentVC, lastVC, mpParams);
				
				String fqnParts[] = motherClass.split("[.]");
		
				
				ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
				
				ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
				
				Map<String, Value<?, XPOperand<?>>> mpRes = ovEntity.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, ovEntity, evaluator, lastVC);
				
				computeCalculabe(parser, ovEntity, evaluator, lastVC, libOV);
				
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
			
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
				
				parentVC = lastVC;
			} while(true);
			//evaluator.popVariableContext();
		}
		
		
		return res;
	}
	
	
	public Value<?, XPOperand<?>> value(Value<?, XPOperand<?>> vlEntity, XPEvaluator evaluator, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		return value(parser, vlEntity, evaluator, entityVC, libOV);
	}
	
	public static ObjectValue<XPOperand<?>> object(XALParser parser, ObjectValue<XPOperand<?>> ovEntity, XPEvaluator evaluator, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		Map<String, Value<?, XPOperand<?>>> mpProperties = ovEntity.getValue();
		
		if(mpProperties.containsKey(PRTY_STATEMENT)) {
			ComputingStatement cs = parser.getStatements().get(ovEntity.getRequiredAttributAsString(PRTY_STATEMENT));
			
			Value<?, XPOperand<?>> vl = cs.translate(ovEntity, evaluator, entityVC, libOV);
			if(vl == null) return null;
			ovEntity = vl.asRequiredObjectValue();
		}
		
		ObjectValue<XPOperand<?>> res = ovEntity;
		
		ObjectValue<XPOperand<?>> ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
		
		if(ovCallParams == null) {
			computeCalculabe(parser, ovEntity, evaluator, entityVC, libOV);
		} else {
			VariableContext parentVC = entityVC;
			
			do {
				VariableContext lastVC = new MapVariableContext(parentVC);
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				addParamsValueInContext(evaluator, parentVC, lastVC, mpParams);
				
				String fqnParts[] = motherClass.split("[.]");
		
				
				ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
				
				ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
				
				Map<String, Value<?, XPOperand<?>>> mpRes = ovEntity.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, ovEntity, evaluator, lastVC);
				
				computeCalculabe(parser, ovEntity, evaluator, lastVC, libOV);
				
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
			
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
				
				parentVC = lastVC;
			} while(true);
			//evaluator.popVariableContext();
		}
		
		
		return res;
	}
	
	@SuppressWarnings("unchecked")
	public static void computeCalculabe(XALParser parser, ObjectValue<XPOperand<?>> ovEntity, XPEvaluator evaluator, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		Map<String, Value<?, XPOperand<?>>> mp = ovEntity.getValue();
		
		List<String> propeertiesTodelete = new ArrayList<>();
		for(String propertyName : mp.keySet()) {
			Value<?, XPOperand<?>> vl=mp.get(propertyName);
			
			ObjectValue<XPOperand<?>> vov = vl.asObjectValue();
			if(vov != null) {

				vl = value(parser, vov, evaluator, entityVC, libOV);
				if(vl == null) {
					propeertiesTodelete.add(propertyName);
					continue;
				}
				
				CalculableValue<?, XPOperand<?>> cli = vl.asCalculableValue();
				if(cli == null) {
					mp.put(propertyName, vl);
					continue;
				}
			}
			
			CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
			
			if(cl == null) continue;
			
			if(ET_RUNTIME.equals(cl.getEvalTime())) {
				XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
				if(xalCL.getVariableContext() == null) xalCL.setVariableContext(entityVC);
				xalCL.setEvaluator(evaluator);
				continue;
			}
			
			if("string".equals(cl.typeName())) {
				XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
				if(xalCL.getVariableContext() == null) xalCL.setVariableContext(entityVC);
				xalCL.setEvaluator(evaluator);

				mp.put(propertyName, new StringValue<>(xalCL.getValue()));
				continue;
			}
			
			if("integer".equals(cl.typeName())) {
				XALCalculabeValue<Integer> xalCL = (XALCalculabeValue<Integer>) cl;
				if(xalCL.getVariableContext() == null) xalCL.setVariableContext(entityVC);
				xalCL.setEvaluator(evaluator);
				
				mp.put(propertyName, new IntegerValue<>(xalCL.getValue()));
				continue;
			}
			
			if("float".equals(cl.typeName())) {
				XALCalculabeValue<Double> xalCL = (XALCalculabeValue<Double>) cl;
				if(xalCL.getVariableContext() == null) xalCL.setVariableContext(entityVC);
				xalCL.setEvaluator(evaluator);
				
				mp.put(propertyName, new DecimalValue<>(xalCL.getValue()));
				continue;
			}
						
			if("boolean".equals(cl.typeName())) {
				XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
				if(xalCL.getVariableContext() == null) xalCL.setVariableContext(entityVC);
				xalCL.setEvaluator(evaluator);
				
				mp.put(propertyName, new BooleanValue<>(xalCL.getValue()));
				continue;
			}
		}
		
		for(String p : propeertiesTodelete) {
			mp.remove(p);
		}

	}
	
	@SuppressWarnings("unchecked")
	public static ArrayValue<XPOperand<?>> array(XALParser parser, ArrayValue<XPOperand<?>> avSrc, XPEvaluator evaluator, VariableContext arrayVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		ArrayValue<XPOperand<?>> res = avSrc;
		
		List<Value<?, XPOperand<?>>> lSrc = avSrc.getValue();
		
		for(int i=0; i< lSrc.size(); ++i) {
			Value<?,  XPOperand<?>> vl = lSrc.get(i);
			ObjectValue<XPOperand<?>> ov = vl.asObjectValue();
			if(ov == null) {
				CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
				
				if(cl == null) {
					ArrayValue<XPOperand<?>> av = vl.asArrayValue();
					if(av == null) continue;
					
					lSrc.set(i, array(parser, av, evaluator, arrayVC, libOV));
					continue;
				}
				
				if(ET_RUNTIME.equals(cl.getEvalTime())) {
					XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
					xalCL.setVariableContext(arrayVC);
					xalCL.setEvaluator(evaluator);
					continue;
				}
				
				if("string".equals(cl.typeName())) {
					XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
					xalCL.setVariableContext(arrayVC);
					xalCL.setEvaluator(evaluator);

					lSrc.set(i, new StringValue<>(xalCL.getValue()));
					continue;
				}
				
				if("integer".equals(cl.typeName())) {
					XALCalculabeValue<Integer> xalCL = (XALCalculabeValue<Integer>) cl;
					xalCL.setVariableContext(arrayVC);
					xalCL.setEvaluator(evaluator);
					
					lSrc.set(i, new IntegerValue<>(xalCL.getValue()));
					continue;
				}
				
				if("float".equals(cl.typeName())) {
					XALCalculabeValue<Double> xalCL = (XALCalculabeValue<Double>) cl;
					xalCL.setVariableContext(arrayVC);
					xalCL.setEvaluator(evaluator);
					
					lSrc.set(i, new DecimalValue<>(xalCL.getValue()));
					continue;
				}
							
				if("boolean".equals(cl.typeName())) {
					XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
					xalCL.setVariableContext(arrayVC);
					xalCL.setEvaluator(evaluator);
					
					lSrc.set(i, new BooleanValue<>(xalCL.getValue()));
					continue;
				}

				continue;
			}
			
			VariableContext vc = new MapVariableContext(arrayVC);
			
			lSrc.set(i, object(parser, ov, evaluator, vc, libOV));
		}
		
		return res;
	}
	
	public static ArrayValue<XPOperand<?>> array(XALParser parser, ObjectValue<XPOperand<?>> rootOV, String path, XPEvaluator evaluator, VariableContext arrayVC) throws ManagedException {
		ObjectValue<XPOperand<?>> ov = rootOV;
		
		String parts[] = path.split("[.]");
		
		VariableContext lastVC = null;
		
		VariableContext parentVC = evaluator.getCurrentVariableContext();
		
		StringBuilder sbVCName = new StringBuilder();
		for(int i=0; i<parts.length-1; ++i) {
			String part = parts[i];
			
			if(i>0) sbVCName.append(".").append(part);
			else sbVCName.append(part);
			
			ov = ov.getRequiredAttributAsObjectValue(part);
			
			ObjectValue<XPOperand<?>> ovCallParams = ov.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			if(ovCallParams == null) continue;
			do {
				lastVC = new MapVariableContext(parentVC);
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				addParamsValueInContext(evaluator, parentVC, lastVC, mpParams);
				
				evaluator.pushVariableContext(lastVC);
				
				ObjectValue<XPOperand<?>> ovMother = rootOV.getPathAttributAsObjecValue(motherClass);
				
				Map<String, Value<?, XPOperand<?>>> mpRes = ov.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, ov, evaluator, lastVC);
				
				parentVC = lastVC;
				if(ov.getAttribut(PRTY_CALL_PARAMS) == null) break;
				
				ovCallParams = ov.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			} while(true);
		}
		
		ArrayValue<XPOperand<?>> res = ov.getRequiredAttributAsArrayValue(parts[parts.length-1]);
		arrayVC.setParent(parentVC);
		
		return array(parser, res, evaluator, arrayVC, Computing.getDefaultObjectLib(rootOV));
	}
	
	/*@SuppressWarnings("unchecked")
	private static ObjectValue<XPOperand<?>> computeAllCalculable(ObjectValue<XPOperand<?>> ovEntity, XPEvaluator evaluator, VariableContext vc, Map<String, ObjectValue<XPOperand<?>>> libOV) throws CloneNotSupportedException, ManagedException {
		ObjectValue<XPOperand<?>> res = ovEntity;
		
		ObjectValue<XPOperand<?>> ovCallParams = ovEntity.getAttributAsObjectValue(PRTY_CALL_PARAMS);
		if(ovCallParams != null) {
			VariableContext parentVC = vc;
			do {
				VariableContext lastVC = new MapVariableContext(vc);
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				addParamsValueInContext(evaluator, parentVC, lastVC, mpParams);
				
				String fqnParts[] = motherClass.split("[.]");
		
				
				ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
				
				ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
				
				Map<String, Value<?, XPOperand<?>>> mpRes = ovEntity.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, ovEntity, evaluator, lastVC);
				
				Map<String, Value<?, XPOperand<?>>> mp = ovEntity.getValue();
				
				for(String propertyName : mp.keySet()) {
					Value<?, XPOperand<?>> vl=mp.get(propertyName);
					
					
					ObjectValue<XPOperand<?>> vov = vl.asObjectValue();
					if(vov != null) {
						
						mp.put(propertyName, computeAllCalculable(vov, evaluator, lastVC, libOV));
						continue;
					}
					
					CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
					
					if(cl == null) continue;
					
					if(ET_RUNTIME.equals(cl.getEvalTime())) {
						XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
						if(xalCL.getVariableContext() == null) xalCL.setVariableContext(vc);
						xalCL.setEvaluator(evaluator);
						continue;
					}
					
					if("string".equals(cl.typeName())) {
						XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
						if(xalCL.getVariableContext() == null) xalCL.setVariableContext(vc);
						xalCL.setEvaluator(evaluator);

						mp.put(propertyName, new StringValue<>(xalCL.getValue()));
						continue;
					}
					
					if("integer".equals(cl.typeName())) {
						XALCalculabeValue<Integer> xalCL = (XALCalculabeValue<Integer>) cl;
						if(xalCL.getVariableContext() == null) xalCL.setVariableContext(vc);
						xalCL.setEvaluator(evaluator);
						
						mp.put(propertyName, new IntegerValue<>(xalCL.getValue()));
						continue;
					}
					
					if("float".equals(cl.typeName())) {
						XALCalculabeValue<Double> xalCL = (XALCalculabeValue<Double>) cl;
						if(xalCL.getVariableContext() == null) xalCL.setVariableContext(vc);
						xalCL.setEvaluator(evaluator);
						
						mp.put(propertyName, new DecimalValue<>(xalCL.getValue()));
						continue;
					}
								
					if("boolean".equals(cl.typeName())) {
						XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
						if(xalCL.getVariableContext() == null) xalCL.setVariableContext(vc);
						xalCL.setEvaluator(evaluator);
						
						mp.put(propertyName, new BooleanValue<>(xalCL.getValue()));
						continue;
					}
				}
				
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
			
				parentVC = lastVC;
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			} while(true);
		}
	
		return res;
		
	}*/
	
	@SuppressWarnings("unchecked")
	private static void addParamsValueInContext(XPEvaluator evaluator, VariableContext currentVC, VariableContext newVC, Map<String, Value<?, XPOperand<?>>> mpParams) throws ManagedException {
		for(String paramName : mpParams.keySet()) {
			Value<?, XPOperand<?>> vl = mpParams.get(paramName);
			
			CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
			if(cl != null) {
				
				if("string".equals(cl.typeName())) {
					XALCalculabeValue<String> xalCL = (XALCalculabeValue<String>) cl;
					xalCL.setEvaluator(evaluator);
					xalCL.setVariableContext(currentVC);
					newVC.addVariable(paramName, String.class, xalCL.getValue());
					continue;
				}
				
				if("integer".equals(cl.typeName())) {
					XALCalculabeValue<Integer> xalCL = (XALCalculabeValue<Integer>) cl;
					xalCL.setEvaluator(evaluator);
					xalCL.setVariableContext(currentVC);
					newVC.addVariable(paramName, Integer.class, xalCL.getValue());
					continue;
				}
				
				if("float".equals(cl.typeName())) {
					XALCalculabeValue<Double> xalCL = (XALCalculabeValue<Double>) cl;
					xalCL.setEvaluator(evaluator);
					xalCL.setVariableContext(currentVC);
					newVC.addVariable(paramName, Double.class, xalCL.getValue());
					continue;
				}
				
				if("date".equals(cl.typeName())) {
					XALCalculabeValue<Date> xalCL = (XALCalculabeValue<Date>) cl;
					xalCL.setEvaluator(evaluator);
					xalCL.setVariableContext(currentVC);
					newVC.addVariable(paramName, Date.class, xalCL.getValue());
					continue;
				}
				
				if("boolean".equals(cl.typeName())) {
					XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
					xalCL.setEvaluator(evaluator);
					xalCL.setVariableContext(currentVC);
					newVC.addVariable(paramName, Boolean.class, xalCL.getValue());
					continue;
				}
				
				XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
				xalCL.setEvaluator(evaluator);
				xalCL.setVariableContext(currentVC);
				
				Type<?> tp = evaluator.getClassesMan().getType(cl.typeName());
				
				newVC.addVariable(paramName, tp.valueClass(), xalCL.getValue());
				continue;
			}
			
			StringValue<XPOperand<?>> stVl = vl.asStringValue();
			if(stVl != null) {
				newVC.addVariable(paramName, String.class, stVl.getValue());
				continue;
			}
			
			IntegerValue<XPOperand<?>> itVl = vl.asIntegerValue();
			if(itVl != null) {
				newVC.addVariable(paramName, Integer.class, itVl.getValue());
				continue;
			}
			
			BooleanValue<XPOperand<?>> blVl = vl.asBooleanValue();
			if(blVl != null) {
				newVC.addVariable(paramName, Boolean.class, blVl.getValue());
				continue;
			}
			
			DecimalValue<XPOperand<?>> dcVl = vl.asDecimalValue();
			if(dcVl != null) {
				newVC.addVariable(paramName, Double.class, dcVl.getValue());
				continue;
			}
			
			ObjectValue<XPOperand<?>> obVl = vl.asObjectValue();
			if(obVl != null) {
				newVC.addVariable(paramName, Object.class, obVl.getValue());
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
		XALLexingRules lexingRules = parser.getLexingRules();
		//variableContextAnyWay(context);
		Value<?, XPOperand<?>> res = _readPropertyValueForObjectWithoutDec(context);
		if(res != null) return res;
		Character ch = lexingRules.nextForwardChar(charReader);
		
		
		if('(' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			
			return readObjectWithDeclarationParam(context);
		}
		
		throw new ParsingException(String.format("Unexpected error near %s", ch.toString()));
	}
	
	private ObjectValue<XPOperand<?>> readObjectWithDeclarationParam(String context) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
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
		XALLexingRules lexingRules = parser.getLexingRules();
		
		Character ch = lexingRules.nextForwardRequiredNonBlankChar(charReader);
		if('#' == ch) {
			ObjectValue<XPOperand<?>> ovRes = new ObjectValue<>();
			lexingRules.nextNonBlankChar(charReader);
			do {
				String propertyName = lexingRules.nextRequiredPropertyName(charReader);
				
				Value<?, XPOperand<?>> propetyValue = _readPropertyValueForObjectWithoutDec(context);
				
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

	public static XALCalculabeValue<?> calculableFor(XPOperand<?> xp, String evalTime) {
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
		XALLexingRules lexingRules = parser.getLexingRules();
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
		XALLexingRules lexingRules = parser.getLexingRules();
		
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
	
	private Value<?, XPOperand<?>> _readPropertyValueForObjectWithoutDec(String context) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		
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
		
		if('*' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			return readStatement(context);
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
	
	private ObjectValue<XPOperand<?>> readStatement(String context) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		
		String str = lexingRules.nextNonNullString(charReader);
		
		ComputingStatement cs = parser.getStatements().get(str);
		if(cs == null) throw new ParsingException(String.format("Unknow identifier '%s'. Statement expected after '*'", str));
		
		return cs.compileObject(this, context);
	}
	
	private ObjectValue<XPOperand<?>> readObjectByClass(String context) throws ManagedException {
		return readObjectByClass(new ObjectValue<XPOperand<?>>(), context);
	}
	
	public Value<? extends Number, XPOperand<?>> readNumeric() throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		
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
	
	public StringValue<XPOperand<?>> readString(String end) throws ManagedException {
		String str = readStringReturnString(end);
		
		return new StringValue<XPOperand<?>>(str);
	}
	
	private String readStringReturnString(String end) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		
		String str = lexingRules.nextNonNullString(charReader);
		
		if(!str.endsWith(end)) throw new ManagedException(String.format("%s is not a valid string", str));
		
		StringBuilder sb = new StringBuilder(str.substring(1, str.length()-1));
		EscapeCharMan.STANDARD.normalized(sb);
		return sb.toString();
	}
	
	public Value<?, XPOperand<?>> readArrayBody(String context) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		
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
		XALLexingRules lexingRules = parser.getLexingRules();
		
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
		XALLexingRules lexingRules = parser.getLexingRules();
		
		Character ch = lexingRules.nextForwardRequiredNonBlankChar(charReader);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) throw new ParsingException(String.format("Error near %s .", ch.toString()));
		
		String motherClass = lexingRules.nextNonNullString(charReader);
		if(!lexingRules.isIdentifier(motherClass)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", motherClass));
		
		ov.setAttribut(PRTY_OBJECT, motherClass);
		heirsObject.add(ov);
		
		ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) return ov;
		
		ObjectValue<XPOperand<?>> ovParams;
		Value<?, XPOperand<?>> params;
		if('(' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			params = readFunctionCallParams(context);
			
			ovParams = ov.getAttributAsObjectValue(PRTY_CALL_PARAMS);
			if(ovParams == null) ovParams = new ObjectValue<>();
		}
		else {
			ovParams = new ObjectValue<>();
			params = new ArrayValue<>();
		}
		
		ovParams.setAttribut("references."+motherClass, params);
		ov.setAttribut(PRTY_CALL_PARAMS, ovParams);
		
		ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) return ov;
		
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
	
	@SuppressWarnings("unchecked")
	private static void mergeInheritedObject(ObjectValue<XPOperand<?>> src, ObjectValue<XPOperand<?>> dst, XPEvaluator evaluator, VariableContext vc) throws ManagedException {
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
				
				
				mergeInheritedObject(srcOVAttribut, dstOVAttribut, evaluator, vc);
				continue;
			}
			
			vlv = src.getAttribut(v);
			if(vlv == null) continue;
			
			CalculableValue<?, XPOperand<?>> cl = vlv.asCalculableValue();
			if(cl != null) {
				XALCalculabeValue<XPOperand<?>> xalCl = (XALCalculabeValue<XPOperand<?>>)cl;
				xalCl.setVariableContext(vc);
			}
			
			mpDst.put(v, vlv);
			
		}
	}
	
	private void checkParameters(Map<String, Value<?, XPOperand<?>>> mpOvSrc, Map<String, Value<?, XPOperand<?>>> mpOvDst) throws ManagedException {
		Value<?, XPOperand<?>> prmSrc = mpOvSrc.get(PRTY_PARAMS);
		if(prmSrc == null) {
			prmSrc = new ObjectValue<>();
		}
		
		Value<?, XPOperand<?>> prmDstContexts = mpOvDst.get(PRTY_CALL_PARAMS);
		
		String motherClass = mpOvDst.get(PRTY_OBJECT).asRequiredString();
		
		if(prmSrc.asObjectValue().getValue().size() == 0 && prmDstContexts == null) 
			return;
		
		
		ObjectValue<XPOperand<?>> ovPrmDstContexts = prmDstContexts.asRequiredObjectValue();
	
		Iterator<Value<?, XPOperand<?>>> it = ovPrmDstContexts.getValue().values().iterator();
		Value<?, XPOperand<?>> prmDst = it.hasNext() ? it.next() : null;
		
		if(prmDst == null) throw new ManagedException(String.format("The number arguments does'nt match."));
		
		
		
		ObjectValue<XPOperand<?>> ovPrmSrc = prmSrc.asObjectValue();
		
		if(ovPrmSrc == null) throw new ManagedException(String.format("The called source arguments are invalid."));
		
		ArrayValue<XPOperand<?>> avDst = prmDst.asArrayValue();
		
		ObjectValue<XPOperand<?>> ovPrmDst = prmDst.asObjectValue();
		
		if(avDst == null && ovPrmDst == null) throw new ManagedException(String.format("The caller destination arguments are invalid."));
		
		Map<String, Value<?, XPOperand<?>>> mpPrmSrc = ovPrmSrc.getValue();
		
		int nbSrc = mpPrmSrc.keySet().size();
		if(avDst == null) {
			Map<String, Value<?, XPOperand<?>>> mpDst = ovPrmDst.getValue();
			int nbDst = mpDst.keySet().size();
			
			if(nbSrc != nbDst) throw new ManagedException(String.format("The number of source argument ( %s ) is different than the number of destination on ( %s )", nbSrc, nbDst));
			
			for(String v : mpPrmSrc.keySet()) {
				Value<?, XPOperand<?>> vlDstPrm = mpDst.get(v);
				if(vlDstPrm == null) throw new ManagedException(String.format("The parameter %s is not defined", v));
				
				Value<?, XPOperand<?>> vlSrcPrm = mpPrmSrc.get(v);
				
				String srcType = vlSrcPrm.asString();
				
				CalculableValue<?, XPOperand<?>> clDstPrm = vlDstPrm.asCalculableValue();
				
				String dstType = clDstPrm == null ? typeSolver.getTypeName(vlDstPrm.getClass()) : clDstPrm.typeName();
				
				if(dstType == null || srcType.equals(dstType)) continue;
				
				throw new ManagedException(String.format("Type mismatch %s %s", srcType, dstType));
				
			}
			
			return;
		}
		
		ovPrmDst = new ObjectValue<>();
		List<Value<?, XPOperand<?>>> lsDst = avDst.getValue();
		int nbDst = lsDst.size();
		if(nbSrc != nbDst) throw new ManagedException(String.format("The number of source argument ( %s ) is different than the number of destination on ( %s )", nbSrc, nbDst));
		
		int i= 0;
		for(String v : mpPrmSrc.keySet()) {
			Value<?, XPOperand<?>> vlDstPrm = lsDst.get(i);
			if(vlDstPrm == null) throw new ManagedException(String.format("The %s th parameter is not defined", i));
			
			Value<?, XPOperand<?>> vlSrcPrm = mpPrmSrc.get(v);
			
			String srcType = vlSrcPrm.asString();
			
			CalculableValue<?, XPOperand<?>> clDstPrm = vlDstPrm.asCalculableValue();
			
			String dstType = clDstPrm == null ? typeSolver.getTypeName(vlDstPrm.getClass()) : clDstPrm.typeName();
			
			//String dstType = valueTypeMapOnTypes.get(vlDstPrm.getClass());
			
			if(dstType == null || srcType.equals(dstType)) { ovPrmDst.setAttribut(v, vlDstPrm);  i++; continue;}
			
			throw new ManagedException(String.format("Type mismatch %s %s", srcType, dstType));
			
		}
		
		ovPrmDstContexts.setAttribut("references_" + motherClass, ovPrmDst);
		//mpOvDst.put(PRTY_CALL_PARAMS, ovDst);
	}
	
	public XPEvaluator getXPEvaluator() {
		return xpCompiler.evaluator();
	}

	public Parser getXpCompiler() {
		return xpCompiler;
	}

	public XALParser getParser() {
		return parser;
	}

	public CharReader getCharReader() {
		return charReader;
	}
	
	

}
