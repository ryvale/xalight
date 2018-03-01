package com.exa.lang.parsing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exa.buffer.CharReader;
import com.exa.chars.EscapeCharMan;
import com.exa.expression.TypeMan;
import com.exa.expression.XPOperand;
import com.exa.lexing.ParsingException;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.BooleanValue;
import com.exa.utils.values.DecimalValue;
import com.exa.utils.values.IntegerValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.StringValue;
import com.exa.utils.values.Value;

public class Parser {
	
	public static final String PRTY_NAME = "_name";
	public static final String PRTY_CLASS = "_class";
	public static final String PRTY_TYPE = "_type";
	public static final String PRTY_EXTEND = "_extend";
	public static final String PRTY_OBJECT = "_object";
	public static final String PRTY_DEC_PARAMS = "_dec_params";
	public static final String PRTY_CALL_PARAMS = "_call_params";
	
	private XALLexingRules lexingRules = new XALLexingRules();
	
	private XPParser xpParser = new XPParser();
	
	private List<ObjectValue<XPOperand<?>>> heirsObject = new ArrayList<>();
	
	private final Set<String> xalTypes =  new HashSet<>();
	
	private Map<Class<?>, String> valeuMapOnTypes = new HashMap<>();
	
	public Parser() {
		xalTypes.add("string"); xalTypes.add("int"); xalTypes.add("boolean"); xalTypes.add("float"); xalTypes.add("date");
		xalTypes.add("object"); xalTypes.add("array");
		
		valeuMapOnTypes.put(StringValue.class, "string");
		valeuMapOnTypes.put(IntegerValue.class, "int");
		valeuMapOnTypes.put(DecimalValue.class, "float");
		valeuMapOnTypes.put(BooleanValue.class, "boolean");
		valeuMapOnTypes.put(ObjectValue.class, "object");
		valeuMapOnTypes.put(ArrayValue.class, "array");
	}
	
	public ObjectValue<XPOperand<?>> parse(CharReader cr) throws ManagedException {
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		if(ch.charValue() == ':') {
			lexingRules.nextNonBlankChar(cr);
			
			String type = lexingRules.nextNonNullString(cr);
			res.setAttribut(PRTY_TYPE, type);
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			
			if(ch == null) return res;
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after type value."));
			lexingRules.nextNonBlankChar(cr);
		}
		
		ch = null;
		do {
			String propertyName;
			
			try {
				propertyName = lexingRules.nextRequiredPropertyName(cr);
			}
			catch(LexingException e) {
				if(e.isRealParsingException()) throw e;
				
				if(e.getParsingString() == null) {
					if(ch == null) return res;
				}
				throw e;
			}
			
			Value<?, XPOperand<?>> propertyValue = readPropertyValueForObject(cr);
			
			
			res.setAttribut(propertyName, propertyValue);
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			
			if(ch == null) break;
			
			//if('}' == ch.charValue())  { cr.nextChar();	break;	}
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after property value."));
			
			cr.nextChar();
		}
		while(true);
		
		Value<?, XPOperand<?>> vlReferences = res.getAttribut("references");
		
		if(vlReferences == null) return res;
		
		ObjectValue<XPOperand<?>> references = vlReferences.asObjectValue();
		
		if(references == null) return res;
		
		for(ObjectValue<XPOperand<?>> ov : heirsObject) {
		
			Set<String> cyclicRefs = new HashSet<>();
			
			getObjectInheritance(ov, references, cyclicRefs);
		}
	
		
		return res;
	}
	
	private void getObjectInheritance(ObjectValue<XPOperand<?>> dst, ObjectValue<XPOperand<?>> references,  Set<String> cyclicRefs) throws ManagedException {
		
		String objectClass = dst.getAttributAsString(PRTY_OBJECT);
		
		if(objectClass == null) return;
		
		if(cyclicRefs.contains(objectClass)) throw new ManagedException(String.format("Cyclic reference in inheritance for %s. ( %s )", dst, cyclicRefs.toString()));
		
		ObjectValue<XPOperand<?>> src = references.getAttributAsObjectValue(objectClass);
		if(src == null) return;
		
		cyclicRefs.add(objectClass);
		if(src.getAttribut(PRTY_OBJECT) != null) getObjectInheritance(src, references, cyclicRefs);
		
		Map<String, Value<?, XPOperand<?>>> mpDst = dst.getValue();
		
		mergeObject(src, dst);
		
		mpDst.remove(PRTY_OBJECT);
	}
	
	private void mergeObject(ObjectValue<XPOperand<?>> src, ObjectValue<XPOperand<?>> dst) throws ManagedException {
		Map<String, Value<?, XPOperand<?>>> mpSrc = src.getValue();
		Map<String, Value<?, XPOperand<?>>> mpDst = dst.getValue();
		
		for(String v : mpSrc.keySet()) {
			
			checkParameters(mpSrc, mpDst);
			Value<?, XPOperand<?>> vlv = dst.getAttribut(v);
			if(vlv != null) {
				ObjectValue<XPOperand<?>> dstOVAttribut = vlv.asObjectValue();
				if(dstOVAttribut == null) {
					continue;
				}
				
				vlv = src.getAttribut(v);
				if(vlv == null) continue;
				
				ObjectValue<XPOperand<?>> srcOVAttribut = vlv.asObjectValue();
				if(srcOVAttribut == null) continue;
				mergeObject(srcOVAttribut, dstOVAttribut);
				continue;
			}
			
			vlv = src.getAttribut(v);
			if(vlv == null) continue;
			
			try {
				mpDst.put(v, src.getAttribut(v).clone());
			} catch (CloneNotSupportedException e) {
				throw new ManagedException(e);
			}
		}
	}
	
	private void checkParameters(Map<String, Value<?, XPOperand<?>>> mpOvSrc, Map<String, Value<?, XPOperand<?>>> mpOvDst) throws ManagedException {
		Value<?, XPOperand<?>> src = mpOvSrc.get(PRTY_DEC_PARAMS);
		
		Value<?, XPOperand<?>> dst = mpOvDst.get(PRTY_CALL_PARAMS);
		
		if(src == null && dst == null) return;
		
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
				
				String dstType = valeuMapOnTypes.get(vlDstPrm.getClass());
				
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
			
			String dstType = valeuMapOnTypes.get(vlDstPrm.getClass());
			
			if(dstType == null || srcType.equals(dstType)) { ovDst.setAttribut(v, vlDstPrm);  i++; continue;}
			
			throw new ManagedException(String.format("Type mismatch %s %s", srcType, dstType));
			
		}
		
		mpOvDst.put(PRTY_CALL_PARAMS, ovDst);
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		return parse(cr);
	}
	
	public ObjectValue<XPOperand<?>> parseFile(String script) throws ManagedException {
		
		CharReader cr;
		try {
			cr = CharReader.forFile(script, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		
		return parse(cr);
	}
	
	
	
	private StringValue<XPOperand<?>> readString(CharReader cr, String end) throws ManagedException {
		String str = readStringReturnString(cr, end);
		
		return new StringValue<XPOperand<?>>(str);
	}
	
	private String readStringReturnString(CharReader cr, String end) throws ManagedException {
		String str = lexingRules.nextNonNullString(cr);
		
		if(!str.endsWith(end)) throw new ManagedException(String.format("%s is not a valid string", str));
		
		StringBuilder sb = new StringBuilder(str.substring(1, str.length()-1));
		EscapeCharMan.STANDARD.normalized(sb);
		return sb.toString();
	}
	
	private Value<? extends Number, XPOperand<?>> readNumeric(CharReader cr) throws ManagedException {
		String str = lexingRules.nextNonNullString(cr);
		
		if(!lexingRules.isInteger(str, true)) throw new ParsingException(String.format("%s is not not numeric", str));
		
		if(XALLexingRules.EXTENDED_NUMERIC_TERMINATION.indexOf(str.charAt(str.length() - 1)) >= 0) return new DecimalValue<XPOperand<?>>(Double.parseDouble(str));
		
		Character ch = lexingRules.nextForwardChar(cr);
		
		if(ch == null) return new IntegerValue<XPOperand<?>>(Integer.parseInt(str));
		
		if(ch == '.') {
			cr.nextChar();
			
			ch = lexingRules.nextForwardChar(cr);
			if(ch == null || XALLexingRules.NUMERIC_DIGITS.indexOf(ch) < 0) return new DecimalValue<XPOperand<?>>(Double.parseDouble(str));
			
			String str2 = lexingRules.nextNonNullString(cr);
			
			if(!lexingRules.isInteger(str, true)) throw new ParsingException(String.format("%s is not not numeric", str));
			
			return new DecimalValue<XPOperand<?>>(Double.parseDouble(str+"."+str2));
		}
		
		return new IntegerValue<XPOperand<?>>(Integer.parseInt(str));
	}
	
	private ObjectValue<XPOperand<?>> readObjectByClass(CharReader cr, ObjectValue<XPOperand<?>> ov) throws ManagedException {
		Character ch = lexingRules.nextForwardRequiredNonBlankChar(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) throw new ParsingException(String.format("Error near %s .", ch.toString()));
		
		String str = lexingRules.nextNonNullString(cr);
		if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
		
		ov.setAttribut(PRTY_OBJECT, str);
		heirsObject.add(ov);
		
		ch = lexingRules.nextForwardNonBlankChar(cr);
		if(ch == null) return ov;
		
		if('(' == ch) {
			lexingRules.nextNonBlankChar(cr);
			Value<?, XPOperand<?>> params = readFunctionCallParams(cr);
			ov.setAttribut(PRTY_CALL_PARAMS, params);
			ch = lexingRules.nextForwardNonBlankChar(cr);
			if(ch == null) return ov;
		}
	
		if('{' == ch) {
			lexingRules.nextNonBlankChar(cr);
			return readObjectBody(cr, ov);
		}
		
		return ov;
	}
	
	 
	
	private Value<?, XPOperand<?>> readFunctionCallParams(CharReader cr) throws ManagedException {
		
		Character ch = lexingRules.nextForwardRequiredNonBlankChar(cr);
		if('#' == ch) {
			ObjectValue<XPOperand<?>> ovRes = new ObjectValue<>();
			lexingRules.nextNonBlankChar(cr);
			do {
				String propertyName = lexingRules.nextRequiredPropertyName(cr);
				
				Value<?, XPOperand<?>> propetyValue = _readPropertyValueForObjectWithOutDec(cr);
				
				ch = lexingRules.nextForwardRequiredNonBlankChar(cr);
				
				ovRes.setAttribut(propertyName, propetyValue);
				
				if(')' == ch) {
					lexingRules.nextNonBlankChar(cr);
					break;
				}
				
				if(',' != ch) throw new ManagedException(String.format("Unexpected char %s while reading function call params", ch.toString()));
				
			} while(true);
			
			return ovRes;
		}
		
		ArrayValue<XPOperand<?>> avRes = new ArrayValue<>();
		//lexingRules.nextNonBlankChar(cr);
		do {
			Value<?, XPOperand<?>> propetyValue = _readPropertyValueForObjectWithOutDec(cr);
			
			ch = lexingRules.nextForwardRequiredNonBlankChar(cr);
			
			avRes.add(propetyValue);
			
			if(')' == ch) {
				lexingRules.nextNonBlankChar(cr);
				break;
			}
			
			if(',' != ch) throw new ManagedException(String.format("Unexpected char %s while reading function call params", ch.toString()));
			
			lexingRules.nextNonBlankChar(cr);
		} while(true);
		
		return avRes;
	}

	private ObjectValue<XPOperand<?>> readObjectByClass(CharReader cr) throws ManagedException {
		return readObjectByClass(cr, new ObjectValue<XPOperand<?>>());
	}
	
	private Value<?, XPOperand<?>> _readPropertyValueForObjectWithOutDec(CharReader cr) throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		
		if(ch == null) return new BooleanValue<XPOperand<?>>(Boolean.TRUE);
		
		if(ch == '{') {
			lexingRules.nextNonBlankChar(cr);
			return readObjectBody(cr);
		}
		
		if(ch == '[') { 
			lexingRules.nextNonBlankChar(cr); 
			return readArrayBody(cr);
		}
		
		if(ch == ',') return new BooleanValue<XPOperand<?>>(Boolean.TRUE);
		
		if(ch == '\'' || ch == '\"') return readString(cr, ch.toString());
				
		if(XALLexingRules.NUMERIC_DIGITS.indexOf(ch) >= 0) return readNumeric(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) >= 0) {
			String str = lexingRules.nextNonNullString(cr);
			
			if("true".equals(str)) return new BooleanValue<>(Boolean.TRUE);
			
			if("false".equals(str)) return new BooleanValue<>(Boolean.FALSE);
			
			if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
			
			ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
			ov.setAttribut(PRTY_NAME, str);
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			
			if(ch == '{') {
				lexingRules.nextNonBlankChar(cr);
				return readObjectBody(cr, ov);
			}
			
			if('@' == ch) {
				lexingRules.nextNonBlankChar(cr);
				
				readObjectByClass(cr, ov);
				
				return ov;
			}
			
			return ov;
		}
		
		if('@' == ch) {
			lexingRules.nextNonBlankChar(cr);
			return readObjectByClass(cr);
		}
		
		if('=' == ch) {
			lexingRules.nextNonBlankChar(cr);
			XPOperand<?> xp = xpParser.parse(cr, (lr, charReader) -> true );
			
			return calculableFor(xp);
		}
		
		return null;
	}
	
	public Value<?, XPOperand<?>> readPropertyValueForObject(CharReader cr) throws ManagedException {
		Value<?, XPOperand<?>> res = _readPropertyValueForObjectWithOutDec(cr);
		if(res != null) return res;
		Character ch = lexingRules.nextForwardChar(cr);
		
		if('(' == ch) {
			lexingRules.nextNonBlankChar(cr);
			
			return readObjectWithDeclarationParam(cr);
		}
		
		throw new ParsingException(String.format("Unexpected error near %s", ch.toString()));
	}
	
	private ObjectValue<XPOperand<?>> readObjectWithDeclarationParam(CharReader cr) throws ManagedException {
		ObjectValue<XPOperand<?>> params = readFunctionParamsDeclaration(cr);
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		res.setAttribut(PRTY_DEC_PARAMS, params);
		
		Character ch = lexingRules.nextForwardRequiredNonBlankChar(cr);
		if('{' == ch) {
			lexingRules.nextNonBlankChar(cr);
			return readObjectBody(cr, res);
		}
		
		if('@' == ch) {
			lexingRules.nextNonBlankChar(cr);
			return readObjectByClass(cr);
		}
		
		throw new ParsingException(String.format("'{' or '@' expected after declarion params. Unexpected %S", ch.toString()));
	}
	
	private ObjectValue<XPOperand<?>> readFunctionParamsDeclaration(CharReader cr) throws ManagedException {
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		
		Character ch = null;
		do {
			String propertyName = lexingRules.nextRequiredPropertyName(cr);
			String type  = null;
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			if(ch == null) throw new ManagedException(String.format("Unexpected end of file while reading declaration params"));
			
			if('@' == ch) {
				lexingRules.nextNonBlankChar(cr);
				
				type = lexingRules.nextNonNullString(cr);
				if(!xalTypes.contains(type)) throw new ManagedException(String.format("Unknown %s while expecting type after @", type));
				
				ch = lexingRules.nextForwardNonBlankChar(cr);
				if(ch == null) throw new ManagedException(String.format("Unexpected end of file while reading declaration params"));
			}
			
			res.setAttribut(propertyName, type == null ? "string" : type);
			
			if(')' == ch) {
				lexingRules.nextNonBlankChar(cr);
				break;
			}
			
			if(',' != ch) throw new ManagedException(String.format("Unexpected char %s while reading declaration params", ch.toString()));
			
			lexingRules.nextNonBlankChar(cr);
		}
		while(true);
		
		return res;
	}

	private Value<?, XPOperand<?>> readArrayItem(CharReader cr) throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		
		if(ch == null) throw new ParsingException(String.format("Unexpected end of file while reading array item"));
		
		if(ch == '{') {
			lexingRules.nextNonBlankChar(cr);
			return readObjectBody(cr);
		}
		
		if(ch == '[') { 
			lexingRules.nextNonBlankChar(cr);
			readArrayBody(cr);
		}
		
		if(ch == ',') return new BooleanValue<>(Boolean.TRUE);
		
		if(ch == '\'' || ch == '\"') return readString(cr, ch.toString());
		
		if(XALLexingRules.NUMERIC_DIGITS.indexOf(ch) >= 0) return readNumeric(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) >= 0) {
			String str = lexingRules.nextNonNullString(cr);
			
			if("true".equals(str)) return new BooleanValue<>(Boolean.TRUE);
			
			if("false".equals(str)) return new BooleanValue<>(Boolean.FALSE);
			
			if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
			
			ObjectValue<XPOperand<?>> ov = new ObjectValue<>();
			ov.setAttribut("_name", str);
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			
			if(ch == '{') {
				lexingRules.nextNonBlankChar(cr);
				return readObjectBody(cr, ov);
			}
			
			if('@' == ch) {
				cr.nextChar();
				return readObjectByClass(cr);
			}
			
			
			return ov;
		}
		
		if('@' == ch) {
			cr.nextChar();
			return readObjectByClass(cr);
		}
		
		if('=' == ch) {
			cr.nextChar();
			XPOperand<?> xp = xpParser.parse(cr, (lr, charReader) -> true );
			
			return calculableFor(xp);
		}
		
		throw new ParsingException(String.format("Unexpected error near %s", ch.toString()));
	}
	
	public ObjectValue<XPOperand<?>> readObjectBody(CharReader cr, ObjectValue<XPOperand<?>> ov) throws ManagedException {
		//ObjectValue ov = new ObjectValue();
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		if('}' == ch.charValue()) {
			cr.nextChar();
			return ov;
		}
		
		ch = null;
		do {
			String propertyName;
			
			try {
				propertyName = lexingRules.nextRequiredPropertyName(cr);
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
			
			Value<?, XPOperand<?>> propertyValue = readPropertyValueForObject(cr);
			
			ov.setAttribut(propertyName, propertyValue);
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file after property %s reading", propertyName));
			
			if('}' == ch.charValue())  { cr.nextChar();	break;	}
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after property value."));
			
			lexingRules.nextNonBlankChar(cr);
		}
		while(true);
		
		return ov;
	}
	
	public Value<?, XPOperand<?>> readObjectBody(CharReader cr) throws ManagedException {
		return readObjectBody(cr, new ObjectValue<>());
	}
	
	public Value<?, XPOperand<?>> readArrayBody(CharReader cr) throws ManagedException {
		ArrayValue<XPOperand<?>> av = new ArrayValue<>();
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		if(']' == ch.charValue()) {
			cr.nextChar();
			return av;
		}
		
		ch = null;
		
		do {
			Value<?, XPOperand<?>> item = readArrayItem(cr);
			av.add(item);
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file reading array item"));
			
			if(']' == ch.charValue())  { cr.nextChar();	break;	}
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after property value."));
			
			lexingRules.nextNonBlankChar(cr);
			
		} while(true);
		
		return av;
	}
	
	private XALCalculabeValue<?> calculableFor(XPOperand<?> xp) {
		TypeMan<?> type = xp.type();
		
		if(type == TypeMan.STRING) return new XALCalculabeValue<>(TypeMan.STRING.valueOrNull(xp));
		
		if(type == TypeMan.INTEGER) return new XALCalculabeValue<>(TypeMan.INTEGER.valueOrNull(xp));
		
		if(type == TypeMan.BOOLEAN) return new XALCalculabeValue<>(TypeMan.BOOLEAN.valueOrNull(xp));
		
		if(type == TypeMan.DOUBLE) return new XALCalculabeValue<>(TypeMan.DOUBLE.valueOrNull(xp));
		
		if(type == TypeMan.DATE) return new XALCalculabeValue<>(TypeMan.DATE.valueOrNull(xp));
		
		return new XALCalculabeValue<>(xp);
	}

}
