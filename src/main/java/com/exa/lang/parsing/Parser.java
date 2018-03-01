package com.exa.lang.parsing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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
	
	private XALLexingRules lexingRules = new XALLexingRules();
	
	private XPParser xpParser = new XPParser();
	
	private List<ObjectValue<XPOperand<?>>> heirsObject = new ArrayList<>();
	
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
	
	private void getObjectInheritance(ObjectValue<XPOperand<?>> object, ObjectValue<XPOperand<?>> references,  Set<String> cyclicRefs) throws ManagedException {
		
		String objectClass = object.getAttributAsString(PRTY_OBJECT);
		
		if(objectClass == null) return;
		
		if(cyclicRefs.contains(objectClass)) throw new ManagedException(String.format("Cyclic reference in inheritance for %s. ( %s )", object, cyclicRefs.toString()));
		
		ObjectValue<XPOperand<?>> refObj = references.getAttributAsObjectValue(objectClass);
		if(refObj == null) return;
		
		cyclicRefs.add(objectClass);
		if(refObj.getAttribut(PRTY_OBJECT) != null) getObjectInheritance(refObj, references, cyclicRefs);
		
		Map<String, Value<?, XPOperand<?>>> mpRefObj = refObj.getValue();
		if(mpRefObj == null) return;
		
		Map<String, Value<?, XPOperand<?>>> mpOV = object.getValue();
		
		for(String v : mpRefObj.keySet()) {
			Value<?, XPOperand<?>> vlv = object.getAttribut(v);
			if(vlv != null) continue;
			
			try {
				mpOV.put(v, refObj.getAttribut(v).clone());
			} catch (CloneNotSupportedException e) {
				throw new ManagedException(e);
			}
		}
		
		mpOV.remove(PRTY_OBJECT);
		
		
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
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) throw new ParsingException(String.format("Error near %s .", ch.toString()));
		
		String str = lexingRules.nextNonNullString(cr);
		if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
		
		ov.setAttribut(PRTY_OBJECT, str);
		
		heirsObject.add(ov);
		
		ch = lexingRules.nextForwardNonBlankChar(cr);
		if('{' == ch) {
			lexingRules.nextNonBlankChar(cr);
			return readObjectBody(cr, ov);
		}
		
		return ov;
	}
	
	private ObjectValue<XPOperand<?>> readObjectByClass(CharReader cr) throws ManagedException {
		return readObjectByClass(cr, new ObjectValue<XPOperand<?>>());
	}
	
	public Value<?, XPOperand<?>> readPropertyValueForObject(CharReader cr) throws ManagedException {
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
				cr.nextChar();
				
				readObjectByClass(cr, ov);
				
				
				
				return ov;
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
