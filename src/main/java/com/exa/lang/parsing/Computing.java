package com.exa.lang.parsing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import com.exa.lang.expression.computer.XALComputer;
import com.exa.lexing.LexingException;
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
			this(new MapVariableContext(), unknownIdValidation);
		}

		public XPParser(VariableContext variableContext, UnknownIdentifierValidation unknownIdValidation) {
			super(variableContext, unknownIdValidation);
			
			classesMan.registerClass(XALParser.T_OBJECT_VALUE);
			
			classesMan.registerClass(XALParser.T_ARRAY_VALUE);
			
			classesMan.registerClass(XALParser.T_COMPUTER);
		}

		public XPParser(VariableContext variableContext) {
			this(variableContext, (id, context) -> null);
		}
		
	}
	
	public static final String VCC_CALLS = "calls";
	
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
	public static final String PRTY_INSERTION = "_insertion";
	public static final String PRTY_PREF_SUBSTITUTION = "_substitution";
	public static final String PRTY_VALUE = "_value";
	public static final String PRTY_FILE = "_file";
	public static final String PRTY_SOURCE = "_source";
	public static final String PRTY_DESTINATION = "_destination";
	public static final String PRTY_ENTITIES = "_entities";
	public static final String PRTY_GENERATED = "_generated";
	public static final String PRTY_VAR_TYPE = "_varType";
	
	public static final String VL_INCORPORATE = "incorporate";
	
	public static final String VL_ARRAY = "array";
	public static final String VL_VALUE = "value";
	public static final String VL_STATEMENT = "statement";
	public static final String VL_ALL = "all";
	
	public static final String CS_VALUE = "get-value";
	public static final String CS_IMPORT = "import";
	
	public static final String LIBN_DEFAULT = "references";
	
	public static final String ET_NOW = "now";
	
	public static final String ET_RUNTIME = "runtime";
	
	
	public static final String VN_ROOT_OV = "rootOv";
	
	private ObjectValue<XPOperand<?>> result;
	private Parser xpCompiler;
	
	private XALParser parser;
	
	private CharReader charReader;
	
	private List<ObjectValue<XPOperand<?>>> heirsObject = new ArrayList<>();
	
	private TypeSolver typeSolver;
	
	private XALComputer computer;
	
	private Map<String, ObjectValue<XPOperand<?>>> toBeCalculated = new LinkedHashMap<>();
	
		
	public Computing(XALParser parser, CharReader charReader, XPEvaluatorSetup evSteup, UnknownIdentifierValidation uiv) throws ManagedException {
		this.parser = parser;
		
		
		VariableContext rootVC = new MapVariableContext();
		this.xpCompiler = new XPParser(rootVC, uiv);
		typeSolver = new TypeSolver();
		
		XPEvaluator compEvaluator = xpCompiler.evaluator();
		evSteup.setup(compEvaluator);
		
		rootVC.addVariable(VN_ROOT_OV, ObjectValue.class, null);

		compEvaluator.getClassesMan().forAllTypeDo((typeName, valueClass) -> {
			if(typeSolver.containsType(typeName)) return;
			
			typeSolver.registerType(typeName, valueClass);
			
		});
		
		computer = new XALComputer(this);
		this.result = new ObjectValue<>();
		this.charReader = charReader;
	}
	
	public Computing(XALParser pareser, CharReader charReader) throws ManagedException {
		this(pareser, charReader, (evaluator) -> {}, (id, context) -> null); 
	}
	
	/*public Computing(CharReader charReader, VariableContext vc, XPEvalautorFactory cclEvaluatorFacory) {
		this.xpCompiler = new XPParser(vc);
		this.result = new ObjectValue<>();
		this.charReader = charReader;
		
		typeSolver = new TypeSolver();
		
		XPEvaluator compEvaluator = xpCompiler.evaluator();
		compEvaluator.getClassesMan().forAllTypeDo((typeName, valueClass) -> {
			if(typeSolver.containsType(typeName)) return;
			
			typeSolver.registerType(typeName, valueClass);
			
		});
		
		computer = new XALComputer(this);
		
		
	}*/
	
	public void closeCharReader() throws IOException {
		this.charReader.close();
	}
	
	public void addCustomType(String name, Class<?> valueClass) {
		
	}
	
	

	public ObjectValue<XPOperand<?>> execute() throws ManagedException {
		xpCompiler.evaluator().addVariable("computer", XALComputer.class, computer);
		
		XALLexingRules lexingRules = parser.getLexingRules();
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch.charValue() == ':') {
			lexingRules.nextNonBlankChar(charReader);
			
			String type = lexingRules.nextNonNullString(charReader);
			result.setAttribut(PRTY_TYPE, type);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if(ch == null) return result;
			
			/*if(ch != ',') throw new ParsingException(String.format("',' expected after type value."));
			lexingRules.nextNonBlankChar(charReader);*/
			
			if(ch == ',') lexingRules.nextNonBlankChar(charReader);
		}
		
		ch = null;
		
		XPEvaluator evaluator = xpCompiler.evaluator();
		VariableContext rootVC = evaluator.getRootVC();
		do {
			String propertyName;
			
			try {
				propertyName = lexingRules.nextRequiredPropertyName(charReader);
			}
			catch(LexingException e) {
				if(e.isRealParsingException()) throw e;
				
				if(e.getParsingString() == null) {
					if(ch == null) return result;
				}
				throw e;
			}
			
			Value<?, XPOperand<?>> propertyValue; //= readPropertyValueForObject(propertyName);
			
			if("init".equals(propertyName)) {
				propertyValue = readPropertyValueForObject(propertyName, true);
			}
			else {
				propertyValue = readPropertyValueForObject(propertyName, false);
			}
			
			result.setAttribut(propertyName, propertyValue);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if(ch == null) break;
			
			//if(ch != ',') throw new ParsingException(String.format("',' expected after property value."));
			//lexingRules.nextNonBlankChar(charReader);
			if(ch == ',') lexingRules.nextNonBlankChar(charReader);
		}
		while(true);
		
		Value<?, XPOperand<?>> vlReferences = result.getAttribut("references");
		
		if(vlReferences == null) return result;
		
		ObjectValue<XPOperand<?>> references = vlReferences.asObjectValue();
		
		if(references == null) return result;
		
		for(ObjectValue<XPOperand<?>> ov : heirsObject) {
		
			Set<String> cyclicRefs = new HashSet<>();
			
			String entity = ov.getAttributAsString(PRTY_ENTITY);
			
			getObjectInheritance(ov, references, cyclicRefs, entity);
		}
		
		rootVC.assignContextVariable(VN_ROOT_OV, result);
		return result;
	}
	
	public void resolveHeirsObject(ObjectValue<XPOperand<?>> ovLib) throws ManagedException {
		for(ObjectValue<XPOperand<?>> ov : heirsObject) {
			
			Set<String> cyclicRefs = new HashSet<>();
			
			String entity = ov.getAttributAsString(PRTY_ENTITY);
			
			getObjectInheritance(ov, ovLib, cyclicRefs, entity);
		}
	}
	
	public ObjectValue<XPOperand<?>> object(String path, VariableContext entityVC) throws ManagedException {
		
		ObjectValue<XPOperand<?>> rootOV = getResult();
		
		Map<String, ObjectValue<XPOperand<?>>> mpLib = XALParser.getDefaultObjectLib(rootOV);
		
		XALParser.loadImport(this, mpLib);
		
		for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
			resolveHeirsObject(ovLib);
		}
		
		ObjectValue<XPOperand<?>> ovEntity =  rootOV.getAttributByPathAsObjectValue(path);
		
		if(ovEntity != null)
			return object(ovEntity.clone(), entityVC, mpLib);
			
		
		String ovPath = path;
		int p;
		ObjectValue<XPOperand<?>> baseOvEntity;
		do {
			p = ovPath.lastIndexOf('.');
			if(p < 0) throw new ManagedException(String.format("Unable to reach the path '%s'", path));
			ovPath = ovPath.substring(0, p);
			
			baseOvEntity =  rootOV.getAttributByPathAsObjectValue(ovPath);
		} while(baseOvEntity == null);
		
		//try {
		baseOvEntity = object(baseOvEntity.clone(), entityVC, mpLib);
		/*} catch (CloneNotSupportedException e) {
			throw new ManagedException(e);
		}*/
		
		return baseOvEntity.getAttributAsObjectValue(path.substring(p+1));
	}
	
	public ObjectValue<XPOperand<?>> object(VariableContext entityVC) throws ManagedException {
		
		ObjectValue<XPOperand<?>> rootOV = getResult();
		
		Map<String, ObjectValue<XPOperand<?>>> mpLib = XALParser.getDefaultObjectLib(rootOV);
		
		XALParser.loadImport(this, mpLib);
		
		for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
			resolveHeirsObject(ovLib);
		}
		
		ObjectValue<XPOperand<?>> ovEntity =  rootOV;
		
		if(ovEntity != null)
			return object(ovEntity.clone(), entityVC, mpLib);
			
		
		return object(ovEntity, entityVC, mpLib);
	}
	
	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> relativeOV, String path, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		return object(relativeOV.getRequiredAttributAsObjectValue(path), entityVC, libOV);
	}
	
	/*public Value<?, XPOperand<?>> value(ObjectValue<XPOperand<?>> ovParent, String path, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> mpLib) throws ManagedException {
		
		
	}*/
	
	public Value<?, XPOperand<?>> value(Value<?, XPOperand<?>> vlEntity, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		ObjectValue<XPOperand<?>> ovEntity = vlEntity.asObjectValue();	
		
		if(ovEntity == null) return vlEntity;
		
		Map<String, Value<?, XPOperand<?>>> mpProperties = ovEntity.getValue();
		
		if(mpProperties.containsKey(PRTY_STATEMENT)) {
			ComputingStatement cs = parser.getStatements().get(ovEntity.getRequiredAttributAsString(PRTY_STATEMENT));
			
			Value<?, XPOperand<?>> vl = cs.translate(ovEntity, this, entityVC, libOV, CS_VALUE);
			if(vl == null) return null;
			ovEntity = vl.asObjectValue();
			
			if(ovEntity == null) return vl;
			
			String insertion = ovEntity.getAttributAsString(PRTY_INSERTION);
			if(insertion != null) {
				if(VL_VALUE.equals(insertion)) {
					ArrayValue<XPOperand<?>> av = ovEntity.getAttributAsArrayValue(PRTY_VALUE);
					
					return av.get(0);
				}
				
				if(VL_INCORPORATE.equals(insertion)) {
					ArrayValue<XPOperand<?>> av = ovEntity.getAttributAsArrayValue(PRTY_VALUE);
					
					ObjectValue<XPOperand<?>> newOvEntity = new ObjectValue<>();
					
					List<Value<?, XPOperand<?>>> lst = av.getValue();
					for(Value<?, XPOperand<?>> vlIncorporateItem : lst) {
						ObjectValue<XPOperand<?>> ovValue = vlIncorporateItem.asRequiredObjectValue();
						
						Map<String, Value<?, XPOperand<?>>> mpValue = ovValue.getValue();
						
						for(String newPropName : mpValue.keySet()) {
							newOvEntity.setAttribut(newPropName, mpValue.get(newPropName));
						}
					}
					
					return newOvEntity;
				}
				
				if(VL_ARRAY.equals(insertion)) {
					ArrayValue<XPOperand<?>> av = ovEntity.getAttributAsArrayValue(PRTY_VALUE);
					
					return av;
				}
			};
		}
		
		ObjectValue<XPOperand<?>> res = ovEntity;
		
		ObjectValue<XPOperand<?>> ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
		
		XPEvaluator evaluator = getXPEvaluator();
		
		if(ovCallParams == null) {
			computeCalculabe(ovEntity, entityVC, libOV);
		} else {
			VariableContext parentVC = entityVC;
			
			do {
				computeCalculabe(ovEntity, parentVC, libOV);
				
				VariableContext lastVC = new MapVariableContext(parentVC);
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				VariableContext pVC =  new MapVariableContext(parentVC);
				
				addParamsValueInContext(evaluator, pVC, lastVC, mpParams);
				
				evaluator.registerVariableContext(VCC_CALLS, lastVC);
				evaluator.registerVariableContext(VCC_CALLS, pVC);
				
				String fqnParts[] = motherClass.split("[.]");
		
				
				ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
				
				ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
				
				if(ovMother == null) throw new ManagedException(String.format("Inexistebt mother for the path '%s'", fqnParts[fqnParts.length - 1]));
				
				//try {
					ovMother = ovMother.clone();
				/*} catch (CloneNotSupportedException e) {
					throw new ManagedException(e);
				}*/
				
				Map<String, Value<?, XPOperand<?>>> mpRes = ovEntity.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, ovEntity, evaluator, lastVC);
				
				computeCalculabe(ovEntity, lastVC, libOV);
				
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
			
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
				
				parentVC = lastVC;
			} while(true);
			//evaluator.popVariableContext();
		}
		
		
		return res;
	}
	
	public Value<?, XPOperand<?>> value(String path, VariableContext entityVC) throws ManagedException {
		ObjectValue<XPOperand<?>> rootOV = getResult();
		
		Map<String, ObjectValue<XPOperand<?>>> mpLib = XALParser.getDefaultObjectLib(rootOV);
		
		XALParser.loadImport(this, mpLib);
		
		for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
			resolveHeirsObject(ovLib);
		}
		
		return value(rootOV, path, entityVC, mpLib);
	}
	
	public Value<?, XPOperand<?>> value(String path) throws ManagedException {
		VariableContext vc = new MapVariableContext(xpCompiler.evaluator().getCurrentVariableContext());
		
		return value(path, vc);
	}
	
	public Value<?, XPOperand<?>> value(ObjectValue<XPOperand<?>> baseOvEntity, String path, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		String ovPath = path;
		int p = -2;
		ObjectValue<XPOperand<?>> boe = baseOvEntity;
		while(!boe.containsAttribut(Computing.PRTY_CALL_PARAMS)) {
			p = ovPath.indexOf(".");
			if(p < 0) break;
			
			Value<?, XPOperand<?>> vl = boe.getAttributEx(ovPath.substring(0, p));
			
			if(vl == null) return null;
			
			ObjectValue<XPOperand<?>> ov = vl.asObjectValue();
			
			if(ov == null) 	if(ovPath.contains(".")) throw new ManagedException(String.format("Unable to reach the path '%s' (remain '%s' is not object)", path, ovPath));
			
			boe = ov;
			
			ovPath = ovPath.substring(p+1);
		}
		
		boe = object(boe.clone(), entityVC);
		
		return boe.getPathAttribut(ovPath);
	}
		
	/*public void calculateInit() throws ManagedException {

		ObjectValue<XPOperand<?>> rootOV = getResult();
		
		Map<String, ObjectValue<XPOperand<?>>> mpLib = XALParser.getDefaultObjectLib(rootOV);
		
		XALParser.loadImport(this, mpLib);
		
		for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
			resolveHeirsObject(ovLib);
		}
		
		VariableContext rootVC =  xpCompiler.evaluator().getRootVC();
		
		for(String var : toBeCalculated.keySet()) {
			Value<?, XPOperand<?>> vl0 = toBeCalculated.get(var).getAttribut(PRTY_VALUE);
			
			Value<?, XPOperand<?>> vl = value(vl0, new MapVariableContext(rootVC), mpLib);
			
			if("object".equals(vl.typeName()) || "array".equals(vl.typeName())) {
				rootVC.assignContextVariable(var, vl.getValue());
				continue;
			}
			
			rootVC.assignContextVariable(var, vl.getValue());
		}
		toBeCalculated.clear();
	}*/
	
	public void calculateInit() throws ManagedException {

		ObjectValue<XPOperand<?>> rootOV = getResult();
		
		Map<String, ObjectValue<XPOperand<?>>> mpLib = XALParser.getDefaultObjectLib(rootOV);
		
		XALParser.loadImport(this, mpLib);
		
		for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
			resolveHeirsObject(ovLib);
		}
		
		VariableContext rootVC =  xpCompiler.evaluator().getRootVC();
		
		for(String var : toBeCalculated.keySet()) {
			Value<?, XPOperand<?>> vl0 = toBeCalculated.get(var).getAttribut(PRTY_VALUE);
			
			Value<?, XPOperand<?>> vl = value(vl0, new MapVariableContext(rootVC), mpLib);
			
			if("object".equals(vl.typeName()) || "array".equals(vl.typeName())) {
				rootVC.assignContextVariable(var, vl);
				continue;
			}
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			XALCalculabeValue<XPOperand<?>>cl = (XALCalculabeValue)vl.asCalculableValue();
			
			if(cl != null)	{
				cl.setEvaluator(xpCompiler.evaluator());
				cl.setVariableContext(rootVC);
			}
			
			
			
			rootVC.assignContextVariable(var, vl.getValue());
			
		}
		toBeCalculated.clear();
	}
	
	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> ovEntity, VariableContext entityVC) throws ManagedException {
		return object(ovEntity, entityVC, XALParser.getDefaultObjectLib(result));
	}
	
	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> ovEntity, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		
		Map<String, Value<?, XPOperand<?>>> mpProperties = ovEntity.getValue();
		
		XALParser parser = getParser();
		if(mpProperties.containsKey(PRTY_STATEMENT)) {
			ComputingStatement cs = parser.getStatements().get(ovEntity.getRequiredAttributAsString(PRTY_STATEMENT));
			
			Value<?, XPOperand<?>> vl = cs.translate(ovEntity, this, entityVC, libOV, CS_VALUE);
			if(vl == null) return null;
			ovEntity = vl.asRequiredObjectValue();
		}
		
		String insertion = ovEntity.getAttributAsString(PRTY_INSERTION);
		if(insertion != null) {
			if(insertion.equals(VL_INCORPORATE)) {
				ArrayValue<XPOperand<?>> av = ovEntity.getAttributAsArrayValue(PRTY_VALUE);
				
				ObjectValue<XPOperand<?>> newOvEntity = new ObjectValue<>();
				
				List<Value<?, XPOperand<?>>> lst = av.getValue();
				for(Value<?, XPOperand<?>> vlIncorporateItem : lst) {
					ObjectValue<XPOperand<?>> ovValue = vlIncorporateItem.asRequiredObjectValue();
					
					Map<String, Value<?, XPOperand<?>>> mpValue = ovValue.getValue();
					
					for(String newPropName : mpValue.keySet()) {
						newOvEntity.setAttribut(newPropName, mpValue.get(newPropName));
					}
				}
				
				ovEntity = newOvEntity;
			}
		}
		
		ObjectValue<XPOperand<?>> res = ovEntity;
		
		ObjectValue<XPOperand<?>> ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
		
		XPEvaluator evaluator = getXPEvaluator();
		
		if(ovCallParams == null) {
			computeCalculabe(ovEntity, entityVC, libOV);
		} else {
			VariableContext parentVC = entityVC;
			
			do {
				VariableContext lastVC = new MapVariableContext(parentVC);
				
				Map<String, Value<?, XPOperand<?>>> mpCallParams = ovCallParams.getValue();
				
				Iterator<String> strIt = mpCallParams.keySet().iterator();
				String motherClass = strIt.next();
				
				ObjectValue<XPOperand<?>> ovParams = ovCallParams.getRequiredAttributAsObjectValue(strIt.next());
				Map<String, Value<?, XPOperand<?>>> mpParams = ovParams.getValue();
				
				VariableContext pVC =  new MapVariableContext(parentVC);
				
				addParamsValueInContext(evaluator, pVC, lastVC, mpParams);
				
				evaluator.registerVariableContext(VCC_CALLS, lastVC);
				evaluator.registerVariableContext(VCC_CALLS, pVC);
				
				String fqnParts[] = motherClass.split("[.]");
		
				
				ObjectValue<XPOperand<?>> libPartOV = libOV.get(fqnParts.length == 1 ? LIBN_DEFAULT : fqnParts[0]);
				
				ObjectValue<XPOperand<?>> ovMother = libPartOV.getPathAttributAsObjecValue(fqnParts[fqnParts.length - 1]);
				if(ovMother == null) throw new ManagedException(String.format("Inexistebt mother for the path '%s'", fqnParts[fqnParts.length - 1]));
				
				//try {
				ovMother = ovMother.clone();
				/*} catch (CloneNotSupportedException e) {
					throw new ManagedException(e);
				}*/
				
				Map<String, Value<?, XPOperand<?>>> mpRes = ovEntity.getValue();
				mpRes.remove(PRTY_CALL_PARAMS);
				
				mergeInheritedObject(ovMother, ovEntity, evaluator, lastVC);
				
				computeCalculabe(ovEntity, lastVC, libOV);
				
				//computeCalculabe(parser, ovEntity, evaluator, parentVC, libOV);
				
				if(res.getAttribut(PRTY_CALL_PARAMS) == null) break;
			
				ovCallParams = res.getAttributAsObjectValue(PRTY_CALL_PARAMS);
				
				parentVC = lastVC;
			} while(true);

		}
		
		
		return res;
	}
	
	public void computeCalculabe(ObjectValue<XPOperand<?>> ovEntity, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		Map<String, Value<?, XPOperand<?>>> currentMpEntity = ovEntity.getValue();
		
		List<String> propertiesTodelete = new ArrayList<>();
		
		Map<String, Value<?, XPOperand<?>>> propertiesToAdd = new LinkedHashMap<>();
		for(String propertyName : currentMpEntity.keySet()) {
			Value<?, XPOperand<?>> vl=currentMpEntity.get(propertyName);
			
			if(PRTY_PARAMS.equals(propertyName)) continue;
			
			if(vl == null) continue;
			ObjectValue<XPOperand<?>> vov = vl.asObjectValue();
			if(vov != null) {

				if(propertyName.startsWith(PRTY_PREF_SUBSTITUTION)) {
					propertiesTodelete.add(propertyName);
					
					Value<?, XPOperand<?>> vlNewPropValues = currentMpEntity.get(propertyName);
					ObjectValue<XPOperand<?>> ovNewPropValues = vlNewPropValues.asObjectValue();
					if(ovNewPropValues == null) {
						ArrayValue<XPOperand<?>> avNewPropValues = vlNewPropValues.asArrayValue();
						if(avNewPropValues == null) throw new ManagedException(String.format("Invalid syntax for substitution id %s. ObjectValue expected.", propertyName));
						// TODO
						continue;
					}
					
					if(ovNewPropValues.containsAttribut(Computing.PRTY_STATEMENT)) {
						vlNewPropValues = value(ovNewPropValues, entityVC, libOV);
						if(vlNewPropValues == null) continue;
						
						ovNewPropValues = vlNewPropValues.asObjectValue();
						
						if(ovNewPropValues == null) {
							ArrayValue<XPOperand<?>> avNewPropValues = vlNewPropValues.asArrayValue();
							if(avNewPropValues == null) throw new ManagedException(String.format("Invalid syntax for substitution id %s. ObjectValue expected.", propertyName));
							// TODO
							continue;
						}
						
					}
					
					String insertion = ovNewPropValues.getAttributAsString(PRTY_INSERTION);
					if(insertion != null) {
						ArrayValue<XPOperand<?>> arInsertion = ovNewPropValues.getRequiredAttributAsArrayValue(PRTY_VALUE);
						
						String context = ovNewPropValues.getAttributAsString(PRTY_CONTEXT);
						
						if(insertion.equals(VL_INCORPORATE)) {
							List<Value<?, XPOperand<?>>> lstIncorporate = arInsertion.getValue();
							for(Value<?, XPOperand<?>> vlIncorporateItem : lstIncorporate) {
								ObjectValue<XPOperand<?>> ovValue = vlIncorporateItem.asRequiredObjectValue();
								
								Map<String, Value<?, XPOperand<?>>> mpValue = ovValue.getValue();
								
								for(String newPropName : mpValue.keySet()) {
									propertiesToAdd.put(newPropName, mpValue.get(newPropName));
								}
							}
							continue;
						}
						
						if(insertion.equals(VL_ARRAY)) {
							currentMpEntity.put(propertyName, arInsertion);
							continue;
						}
						
						throw new ManagedException(String.format("Unknown type of insertion %s in context %s", insertion, context));
					}
		
					Map<String, Value<?, XPOperand<?>>> mpNewPropValue = ovNewPropValues.getValue();
					
					for(String newPropName : mpNewPropValue.keySet()) {
						Value<?, XPOperand<?>> vlNewPropValue = mpNewPropValue.get(newPropName);
						if(vlNewPropValue == null) {
							propertiesToAdd.put(newPropName, vlNewPropValue);
							continue;
						}
						ObjectValue<XPOperand<?>> ovNewPropValue = vlNewPropValue.asObjectValue();
						
						if(ovNewPropValue != null) {
							vlNewPropValue = value(ovNewPropValue, entityVC, libOV);
							if(vlNewPropValue == null) continue;
						}
						
						CalculableValue<?, XPOperand<?>> clNewPropValue = vlNewPropValue.asCalculableValue();
						if(clNewPropValue != null) {
							vlNewPropValue = computeCalculableValue(clNewPropValue, entityVC);
						}
						
						propertiesToAdd.put(newPropName, vlNewPropValue);
					}
					
					continue;
				}
				
				vl = value(vov, entityVC, libOV);
				if(vl == null) {
					propertiesTodelete.add(propertyName);
					continue;
				}
				
				CalculableValue<?, XPOperand<?>> cli = vl.asCalculableValue();
				if(cli == null) {
					currentMpEntity.put(propertyName, vl);
					continue;
				}
			}
			
			ArrayValue<XPOperand<?>> vav = vl.asArrayValue();
			if(vav != null) {
				List<Value<?, XPOperand<?>>> lstVav = vav.getValue();
				
				for(int i = 0; i<lstVav.size(); i++) {
					Value<?, XPOperand<?>> vlVav = lstVav.get(i);
					
					CalculableValue<?, XPOperand<?>> clVav = vlVav.asCalculableValue();
					if(clVav != null) {
						computeCalculableValue(clVav, entityVC);
						continue;
					}
					
					ObjectValue<XPOperand<?>> ovVav = vlVav.asObjectValue();
					if(ovVav != null) {
						Value<?, XPOperand<?>> newVlVav = value(ovVav, entityVC, libOV);
						String insertion = ovVav.getAttributAsString(PRTY_INSERTION);
						if(VL_ARRAY.equals(insertion)) {
							lstVav.remove(i);
							ArrayValue<XPOperand<?>> av = newVlVav.asArrayValue();
							List<Value<?, XPOperand< ?>>> lst = av.getValue();
							
							lstVav.addAll(i, lst);
							
							i += lst.size();
						}
						else if(VL_INCORPORATE.equals(insertion)) {
							ObjectValue<XPOperand<?>> newOvItem = newVlVav.asObjectValue();
							if(newOvItem == null) { lstVav.set(i, newVlVav); continue; }
							
							Set<String> pnames = newOvItem.getValue().keySet();
							lstVav.remove(i);
							List<Value<?, XPOperand< ?>>> lst = new ArrayList<>();
							
							boolean allIsObject = true;
							
							for(String name :  pnames) {
								ObjectValue<XPOperand<?>> ovI = new ObjectValue<>();
								
								Value<?, XPOperand<?>> vlArrayValue = newOvItem.getAttribut(name);
								
								if(vlArrayValue != null) {
									allIsObject &= (vlArrayValue.asObjectValue() != null);
								}
								
								ovI.setAttribut(PRTY_NAME, name);
								ovI.setAttribut(PRTY_VALUE, vlArrayValue);
								
								
								lst.add(ovI);
							}
							
							if(allIsObject) {
								for(Value<?, XPOperand< ?>> aio : lst) {
									ObjectValue<XPOperand<?>> ovAioR = aio.asObjectValue();
									
									ObjectValue<XPOperand<?>> ovAioV = ovAioR.getAttributAsObjectValue(PRTY_VALUE);
									if(ovAioV == null) 
										ovAioV = new ObjectValue<>();
										
									
									String pname = ovAioR.getAttributAsString(PRTY_NAME);
									
									if(!ovAioV.containsAttribut(pname)) 
										ovAioV.asObject().put(PRTY_NAME, new StringValue<>(pname));
									
									
									lstVav.add(ovAioV);
								}
							}
							else lstVav.addAll(i, lst);
						}
						else {
							lstVav.set(i, newVlVav);
						}
						
						continue;
					}
				}
				
				
				continue;
			}
			
			CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
			
			if(cl == null) continue;
			
			Value<?, XPOperand<?>> calculatedValue = computeCalculableValue(cl, entityVC);
			
			currentMpEntity.put(propertyName, calculatedValue);
		}
		
		for(String p : propertiesTodelete) {
			currentMpEntity.remove(p);
		}
		
		for(String p : propertiesToAdd.keySet()) {
			currentMpEntity.put(p, propertiesToAdd.get(p));
		}
	}
	
	public Value<?, XPOperand<?>> computeCalculableValue(CalculableValue<?, XPOperand<?>> cl, VariableContext entityVC) {
		XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
		if(xalCL.getVariableContext() == null) xalCL.setVariableContext(entityVC);
		xalCL.setEvaluator(getXPEvaluator());
		return cl;
	}
	
	public ArrayValue<XPOperand<?>> array(ArrayValue<XPOperand<?>> avSrc, VariableContext arrayVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		ArrayValue<XPOperand<?>> res = avSrc;
		
		List<Value<?, XPOperand<?>>> lSrc = avSrc.getValue();
		
		XPEvaluator evaluator = getXPEvaluator();
		
		for(int i=0; i< lSrc.size(); ++i) {
			Value<?,  XPOperand<?>> vl = lSrc.get(i);
			ObjectValue<XPOperand<?>> ov = vl.asObjectValue();
			if(ov == null) {
				CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
				
				if(cl == null) {
					ArrayValue<XPOperand<?>> av = vl.asArrayValue();
					if(av == null) continue;
					
					lSrc.set(i, array(av, arrayVC, libOV));
					continue;
				}
				
				XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
				xalCL.setVariableContext(arrayVC);
				xalCL.setEvaluator(evaluator);
				
				/*if(ET_RUNTIME.equals(cl.getEvalTime())) {
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
					xalCL.setEvaluator(getXPEvaluator());
					
					lSrc.set(i, new DecimalValue<>(xalCL.getValue()));
					continue;
				}
							
				if("boolean".equals(cl.typeName())) {
					XALCalculabeValue<Boolean> xalCL = (XALCalculabeValue<Boolean>) cl;
					xalCL.setVariableContext(arrayVC);
					xalCL.setEvaluator(getXPEvaluator());
					
					lSrc.set(i, new BooleanValue<>(xalCL.getValue()));
					continue;
				}*/

				continue;
			}
			
			VariableContext vc = new MapVariableContext(arrayVC);
			
			//try {
				lSrc.set(i, object(ov.clone(), vc, libOV));
			/*} catch (CloneNotSupportedException e) {
				throw new ManagedException(e);
			}*/
		}
		
		return res;
	}
	
	public ArrayValue<XPOperand<?>> array(String path, VariableContext arrayVC) throws ManagedException {
		ObjectValue<XPOperand<?>> rootOV = getResult();
		
		Map<String, ObjectValue<XPOperand<?>>> mpLib = XALParser.getDefaultObjectLib(rootOV);
		
		XALParser.loadImport(this, mpLib);
		
		for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
			resolveHeirsObject(ovLib);
		}
		
		ArrayValue<XPOperand<?>> av;
		String[] parts = path.split("[.]");
		if(parts.length == 1) return array(result.getRequiredAttributAsArrayValue(path), arrayVC, mpLib); //av = result.getRequiredAttributAsArrayValue(path);
		
		String parentPath = path.substring(0, path.length() - parts[parts.length - 1].length());
		ObjectValue<XPOperand<?>> ovBaseEntity =  rootOV.getAttributByPathAsObjectValue(parentPath);
		
		int p = -1;
		if(ovBaseEntity == null) {
			String ovPath = parentPath;

			do {
				p = ovPath.lastIndexOf('.');
				if(p < 0) throw new ManagedException(String.format("Unable to reach the path '%s'", path));
				ovPath = ovPath.substring(0, p);
				
				ovBaseEntity =  rootOV.getAttributByPathAsObjectValue(ovPath);
			} while(ovBaseEntity == null);
			
			//try {
				ovBaseEntity = object(ovBaseEntity.clone(), arrayVC, mpLib);
			/*} catch (CloneNotSupportedException e) {
				throw new ManagedException(e);
			}*/
		}
		
		av =  ovBaseEntity.getRequiredAttributAsArrayValue(path.substring(p+1));
		return array(av, arrayVC, mpLib);
	}
	
	public ArrayValue<XPOperand<?>> array(ObjectValue<XPOperand<?>> rootOV, String path, VariableContext arrayVC) throws ManagedException {
		ObjectValue<XPOperand<?>> ov = rootOV;
		
		String parts[] = path.split("[.]");
		
		VariableContext lastVC = null;
		
		XPEvaluator evaluator = getXPEvaluator();
		
		VariableContext parentVC = arrayVC; //evaluator.getCurrentVariableContext();
		
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
				
				VariableContext pVC =  new MapVariableContext(parentVC);
				
				addParamsValueInContext(evaluator, pVC, lastVC, mpParams);
				
				evaluator.registerVariableContext(VCC_CALLS, lastVC);
				evaluator.registerVariableContext(VCC_CALLS, pVC);
				
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
		
		return array(res, arrayVC, XALParser.getDefaultObjectLib(rootOV));
	}
	
	private static void addParamsValueInContext(XPEvaluator evaluator, VariableContext currentVC, VariableContext newVC, Map<String, Value<?, XPOperand<?>>> mpParams) throws ManagedException {
		for(String paramName : mpParams.keySet()) {
			Value<?, XPOperand<?>> vl = mpParams.get(paramName);
			
			CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
			if(cl != null) {
				
				XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
				xalCL.setEvaluator(evaluator);
				xalCL.setVariableContext(currentVC);
				
				Type<?> tp = evaluator.getClassesMan().getType(cl.typeName());
				
				newVC.addVariable(paramName, tp.valueClass(), xalCL.getXp(), evaluator, currentVC);
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
		return readPropertyValueForObject(context, false);
	}
	
	public Value<?, XPOperand<?>> readPropertyValueForObject(String context, boolean propertNameAsVariable) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		//variableContextAnyWay(context);
		Value<?, XPOperand<?>> res = _readPropertyValueForObjectWithoutDec(context, propertNameAsVariable);
		if(res != null) return res;
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		
		
		if('(' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			
			return readObjectWithDeclarationParam(context);
		}
		
		throw new ParsingException(String.format("Unexpected error near %s", ch.toString()));
	}
	
	private ObjectValue<XPOperand<?>> readObjectWithDeclarationParam(String context) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		XPEvaluator evaluator = xpCompiler.evaluator();
		
		VariableContext vc = new MapVariableContext(evaluator.getCurrentVariableContext());
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

	public static String getVariableContextName(String context) {
		String parts[] = context.split("[.]");
		return parts.length > 1 ? parts[0] + "_" + parts[1] : parts[0];
	}
	
	public Value<?, XPOperand<?>> readObjectBody(String context) throws ManagedException {
		return readObjectBody(context, false);
	}
	
	public Value<?, XPOperand<?>> readObjectBody(String context, boolean proprtyNameAsVariable) throws ManagedException {
		return readObjectBody(new ObjectValue<>(), context, proprtyNameAsVariable);
	}
	
	public ObjectValue<XPOperand<?>> readObjectBody(ObjectValue<XPOperand<?>> ov, String context) throws ManagedException {
		return readObjectBody(ov, context, false);
	}
	
	//right place
	public ObjectValue<XPOperand<?>> readObjectBody(ObjectValue<XPOperand<?>> ov, String context, boolean proprtyNameAsVariable) throws ManagedException {
		Integer unnamedAttributIndex = 1;
		
		XALLexingRules lexingRules = parser.getLexingRules();
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if('}' == ch.charValue()) {
			charReader.nextChar();
			return ov;
		}
		
		//
		do {
			String propertyName;
			Value<?, XPOperand<?>> propertyValue;
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			if(ch == '*') {
				lexingRules.nextNonBlankChar(charReader);
				propertyValue = readStatement(context);
				propertyName = PRTY_PREF_SUBSTITUTION + unnamedAttributIndex++;
			}
			else {

				try {
					propertyName = lexingRules.nextRequiredPropertyName(charReader);
				}
				catch(LexingException e) {
					if(e.isRealParsingException()) throw e;
					
					if(e.getParsingString() == null) {
						if(ch == null) throw new ParsingException(String.format("Unexpected end of file in context '%s'. Expecting '}' or property", context));
						throw e;
					}
					if("}".equals(e.getParsingString())) return ov;
					throw new ParsingException(String.format("Error near %s . '}' or property expected.", e.getParsingString()));
				}
				
				String newContext = context + "." + propertyName;
				
				propertyValue = readPropertyValueForObject(newContext);
				
				if(proprtyNameAsVariable && propertyValue != null) {
					
					assignPropertyVariavle(propertyValue, propertyName);
					/*if("ArrayValue".equals(propertyValue.typeName())) {
						xpCompiler.evaluator().assignOrDeclareVariable(propertyName, ArrayValue.class, null);
					}*/
				}
				
				
				
				String parts[] = context.split("[.]");
				if(parts.length == 1) {
					ObjectValue<XPOperand<?>> ovEntity = propertyValue.asObjectValue();
					if(ovEntity != null) {
						ovEntity.setAttribut(PRTY_ENTITY, newContext);
					}
				}
			}
			
			ov.setAttribut(propertyName, propertyValue);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file after property '%s' reading in context '%s'", propertyName, context));
			
			if('}' == ch.charValue())  { charReader.nextChar();	break;	}
			
			/*if(ch != ',') 
				throw new ParsingException(String.format("',' expected after property value."));
			
			lexingRules.nextNonBlankChar(charReader);*/
			
			if(ch == ',') lexingRules.nextNonBlankChar(charReader);
				
		}
		while(true);
		
		return ov;
	}
	
	private void assignPropertyVariavle(Value<?, XPOperand<?>> vl, String var) throws ManagedException {
		if(vl.asStringValue() != null) {
			xpCompiler.evaluator().assignOrDeclareVariable(var, String.class, vl.getValue());
			return;
		}
		
		if(vl.asIntegerValue() != null) {
			xpCompiler.evaluator().assignOrDeclareVariable(var, Integer.class, vl.getValue());
			return;
		}
		
		if(vl.asBooleanValue() != null) {
			xpCompiler.evaluator().assignOrDeclareVariable(var, Boolean.class, vl.getValue());
			return;
		}
		
		if(vl.asDecimalValue() != null) {
			xpCompiler.evaluator().assignOrDeclareVariable(var, Double.class, vl.getValue());
			return;
		}
		
		if("array".equals(vl.typeName())) {
			xpCompiler.evaluator().assignOrDeclareVariable(var, ArrayValue.class, vl);
			return;
		}
		
		if("object".equals(vl.typeName())) {
			ObjectValue<XPOperand<?>> ov = vl.asObjectValue();
			if(ov != null) {
				Value<?, XPOperand<?>> vlVarType = ov.getAttribut(PRTY_VAR_TYPE);
				if(vlVarType != null) {
					
					toBeCalculated.put(var, ov);
					Type<?> tp = xpCompiler.evaluator().getClassesMan().getType(vlVarType.asRequiredString());
					
					xpCompiler.evaluator().assignOrDeclareVariable(var, tp.valueClass(), null);
					return;
				}
				
			}
			xpCompiler.evaluator().assignOrDeclareVariable(var, ObjectValue.class, vl);
			return;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		XALCalculabeValue<XPOperand<?>>cl = (XALCalculabeValue)vl. asCalculableValue();
		if(cl != null) {
			String type = cl.typeName();
			
			if(type == null) return;
			
			if("string".equals(type)) {
				xpCompiler.evaluator().assignOrDeclareVariable(var, String.class, null);
				ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
				ov.setAttribut(PRTY_VAR_TYPE, type);
				ov.setAttribut(PRTY_VALUE, vl);
				toBeCalculated.put(var, ov);
			}
			else if("int".equals(type)) {
				xpCompiler.evaluator().assignOrDeclareVariable(var, Integer.class, null);
				ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
				ov.setAttribut(PRTY_VAR_TYPE, type);
				ov.setAttribut(PRTY_VALUE, vl);
				toBeCalculated.put(var, ov);
			}
			else if("boolean".equals(type)) {
				xpCompiler.evaluator().assignOrDeclareVariable(var, Boolean.class, null);
				ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
				ov.setAttribut(PRTY_VAR_TYPE, type);
				ov.setAttribut(PRTY_VALUE, vl);
				toBeCalculated.put(var, ov);
			}
			else if("float".equals(type)) {
				xpCompiler.evaluator().assignOrDeclareVariable(var, Double.class, null);
				ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
				ov.setAttribut(PRTY_VAR_TYPE, type);
				ov.setAttribut(PRTY_VALUE, vl);
				toBeCalculated.put(var, ov);
			}
			else if("ArrayValue".equals(type)) {
				xpCompiler.evaluator().assignOrDeclareVariable(var, ArrayValue.class, null);
				ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
				ov.setAttribut(PRTY_VAR_TYPE, type);
				ov.setAttribut(PRTY_VALUE, vl);
				toBeCalculated.put(var, ov);
				
				
			}
			else if("ObjectValue".equals(type)) {
				xpCompiler.evaluator().assignOrDeclareVariable(var, ObjectValue.class, null);
				ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
				ov.setAttribut(PRTY_VAR_TYPE, type);
				ov.setAttribut(PRTY_VALUE, vl);
				toBeCalculated.put(var, ov);
			}
		}
			
	}
	
	public XALCalculabeValue<?> readExpression(String context) throws ManagedException {
		XPOperand<?> xp = xpCompiler.parse(charReader, (lr, charReader) -> true, context);
		
		return calculableFor(xp, "now");
	}
	
	private ObjectValue<XPOperand<?>> readFunctionParamsDeclaration(VariableContext vc) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		
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
		return _readPropertyValueForObjectWithoutDec(context, false);
	}
	
	private Value<?, XPOperand<?>> _readPropertyValueForObjectWithoutDec(String context, boolean proprtyNameAsVariable) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		
		if(ch == null) return new BooleanValue<XPOperand<?>>(Boolean.TRUE);
		
		if(ch == '{') {
			lexingRules.nextNonBlankChar(charReader);
			return readObjectBody(context, proprtyNameAsVariable);
		}
		
		if(ch == '[') {
			lexingRules.nextNonBlankChar(charReader); 
			return readArrayBody(context);
		}
		
		if(ch == ',' || ch == '}') return new BooleanValue<XPOperand<?>>(Boolean.TRUE);
		
		if(ch == '\'' || ch == '\"') return readString(ch.toString());
				
		if(XALLexingRules.NUMERIC_DIGITS.indexOf(ch) >= 0) return readNumeric();
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) >= 0) {
			String str = lexingRules.nextNonNullString(charReader);
			
			if("true".equals(str)) return new BooleanValue<>(Boolean.TRUE);
			
			if("false".equals(str)) return new BooleanValue<>(Boolean.FALSE);
			
			if("null".equals(str)) return XALParser.NULL_VALUE;
			
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
	
	public ObjectValue<XPOperand<?>> readStatement(String context) throws ManagedException {
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
	
	public String readStringReturnString(String end) throws ManagedException {
		XALLexingRules lexingRules = parser.getLexingRules();
		
		String str = lexingRules.nextNonNullString(charReader);
		
		if(!str.endsWith(end)) throw new ManagedException(String.format("%s is not a valid string", str));
		
		StringBuilder sb = new StringBuilder(str.substring(1, str.length()-1));
		EscapeCharMan.STANDARD.normalized(sb);
		return sb.toString();
	}
	
	public ArrayValue<XPOperand<?>> readArrayBody(String context) throws ManagedException {
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
			
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file reading array item in context '%s'", context));
			
			if(']' == ch.charValue())  { charReader.nextChar();	break;	}
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after property value in context '%s'.", context));
			
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
		
		if('*' == ch) {
			lexingRules.nextNonBlankChar(charReader);
			return readStatement(context + "[");
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
				XALCalculabeValue<XPOperand<?>> xalCl;
				//try {
					xalCl = (XALCalculabeValue<XPOperand<?>>)cl.clone();
					xalCl.setVariableContext(vc);
					mpDst.put(v, xalCl);
				/*} catch (CloneNotSupportedException e) {
					throw new ManagedException(e);
				}*/
			}
			else mpDst.put(v, vlv);
			
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

	public TypeSolver getTypeSolver() {
		return typeSolver;
	}
	
	public String getTypeName(Value<?, XPOperand<?>> vl) throws ManagedException {
		if(vl.asStringValue() != null) return typeSolver.getTypeName(StringValue.class);
		
		if(vl.asIntegerValue() != null) return typeSolver.getTypeName(IntegerValue.class);
		
		if(vl.asBooleanValue() != null) return typeSolver.getTypeName(BooleanValue.class);
		
		if(vl.asDecimalValue() != null) return typeSolver.getTypeName(DecimalValue.class);
		
		if(vl.asObjectValue() != null) return XALParser.T_OBJECT_VALUE.typeName();
		
		CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
		if(cl != null) return cl.typeName();
		
		throw new ManagedException("Unable to retreive type name");
		
	}

	public ObjectValue<XPOperand<?>> getResult() {
		return result;
	}

	
}
