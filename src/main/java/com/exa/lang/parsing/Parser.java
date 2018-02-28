package com.exa.lang.parsing;

import java.io.IOException;

import com.exa.buffer.CharReader;
import com.exa.chars.EscapeCharMan;
import com.exa.expression.XPression;
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

public class Parser {
	
	public static final String PRTY_NAME = "_name";
	public static final String PRTY_CLASS = "_class";
	public static final String PRTY_TYPE = "_type";
	public static final String PRTY_EXTEND = "_extend";
	public static final String PRTY_OBJECT = "_object";
	
	private XALLexingRules lexingRules = new XALLexingRules();
	
	private XPParser xpParser = new XPParser();
	
	public ObjectValue<XPression<?>> parse(CharReader cr) throws ManagedException {
		ObjectValue<XPression<?>> res = new ObjectValue<>();
		
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
			
			Value<?, XPression<?>> propertyValue = readPropertyValueForObject(cr);
			
			res.setAttribut(propertyName, propertyValue);
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			
			if(ch == null) break;
			
			//if('}' == ch.charValue())  { cr.nextChar();	break;	}
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after property value."));
			
			cr.nextChar();
		}
		while(true);
		
		return res;
	}
	
	public ObjectValue<XPression<?>> parseString(String script) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		return parse(cr);
	}
	
	public ObjectValue<XPression<?>> parseFile(String script) throws ManagedException {
		
		CharReader cr;
		try {
			cr = CharReader.forFile(script, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		
		return parse(cr);
	}
	
	
	
	private StringValue<XPression<?>> readString(CharReader cr, String end) throws ManagedException {
		String str = readStringReturnString(cr, end);
		
		return new StringValue<XPression<?>>(str);
	}
	
	private String readStringReturnString(CharReader cr, String end) throws ManagedException {
		String str = lexingRules.nextNonNullString(cr);
		
		if(!str.endsWith(end)) throw new ManagedException(String.format("%s is not a valid string", str));
		
		StringBuilder sb = new StringBuilder(str.substring(1, str.length()-1));
		EscapeCharMan.STANDARD.normalized(sb);
		return sb.toString();
	}
	
	private Value<? extends Number, XPression<?>> readNumeric(CharReader cr) throws ManagedException {
		String str = lexingRules.nextNonNullString(cr);
		
		if(!lexingRules.isInteger(str, true)) throw new ParsingException(String.format("%s is not not numeric", str));
		
		if(XALLexingRules.EXTENDED_NUMERIC_TERMINATION.indexOf(str.charAt(str.length() - 1)) >= 0) return new DecimalValue<XPression<?>>(Double.parseDouble(str));
		
		Character ch = lexingRules.nextForwardChar(cr);
		
		if(ch == null) return new IntegerValue<XPression<?>>(Integer.parseInt(str));
		
		if(ch == '.') {
			cr.nextChar();
			
			ch = lexingRules.nextForwardChar(cr);
			if(ch == null || XALLexingRules.NUMERIC_DIGITS.indexOf(ch) < 0) return new DecimalValue<XPression<?>>(Double.parseDouble(str));
			
			String str2 = lexingRules.nextNonNullString(cr);
			
			if(!lexingRules.isInteger(str, true)) throw new ParsingException(String.format("%s is not not numeric", str));
			
			return new DecimalValue<XPression<?>>(Double.parseDouble(str+"."+str2));
		}
		
		return new IntegerValue<XPression<?>>(Integer.parseInt(str));
	}
	
	private ObjectValue<XPression<?>> readObjectByClass(CharReader cr, ObjectValue<XPression<?>> ov) throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) throw new ParsingException(String.format("Error near %s .", ch.toString()));
		
		String str = lexingRules.nextNonNullString(cr);
		if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
		
		ov.setAttribut(PRTY_OBJECT, str);
		
		ch = lexingRules.nextForwardNonBlankChar(cr);
		if('{' == ch) {
			lexingRules.nextNonBlankChar(cr);
			return readObjectBody(cr, ov);
		}
		
		return ov;
	}
	
	private ObjectValue<XPression<?>> readObjectByClass(CharReader cr) throws ManagedException {
		return readObjectByClass(cr, new ObjectValue<XPression<?>>());
	}
	
	public Value<?, XPression<?>> readPropertyValueForObject(CharReader cr) throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		
		if(ch == null) return new BooleanValue<XPression<?>>(Boolean.TRUE);
		
		if(ch == '{') {
			lexingRules.nextNonBlankChar(cr);
			return readObjectBody(cr);
		}
		
		if(ch == '[') { 
			lexingRules.nextNonBlankChar(cr); 
			return readArrayBody(cr);
		}
		
		if(ch == ',') return new BooleanValue<XPression<?>>(Boolean.TRUE);
		
		if(ch == '\'' || ch == '\"') return readString(cr, ch.toString());
		
		if('#' == ch) {
			lexingRules.nextNonBlankChar(cr);
			ch = cr.nextChar();
			
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file. 0 or 1 exepected after #"));
			
			if(ch != '0' && ch != '1') throw new ParsingException(String.format("Error near # %s . 0 or 1 exepected after #", ch.toString()));
			String expType ="#"+ch.toString();
			
			ch = lexingRules.nextForwardChar(cr);
			if(ch != '\'' && ch != '\"') throw new ParsingException(String.format("Error near # %s . ' or \" expected.", ch.toString()));
			
			String str = readStringReturnString(cr, ch.toString());
			XPression<?> xp = xpParser.parseString(str);
			
			return new CalculableValue<XPression<?>>(xp, expType);
		}
		
		if(XALLexingRules.NUMERIC_DIGITS.indexOf(ch) >= 0) return readNumeric(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) >= 0) {
			String str = lexingRules.nextNonNullString(cr);
			
			if("true".equals(str)) return new BooleanValue<>(Boolean.TRUE);
			
			if("false".equals(str)) return new BooleanValue<>(Boolean.FALSE);
			
			if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
			
			ObjectValue<XPression<?>> ov = new ObjectValue<>();
			ov.setAttribut(PRTY_NAME, str);
			
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
		
		throw new ParsingException(String.format("Unexpected error near %s", ch.toString()));
	}
	
	private Value<?, XPression<?>> readArrayItem(CharReader cr) throws ManagedException {
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
			
			ObjectValue<XPression<?>> ov = new ObjectValue<>();
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
		
		throw new ParsingException(String.format("Unexpected error near %s", ch.toString()));
	}
	
	public ObjectValue<XPression<?>> readObjectBody(CharReader cr, ObjectValue<XPression<?>> ov) throws ManagedException {
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
			
			Value<?, XPression<?>> propertyValue = readPropertyValueForObject(cr);
			
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
	
	public Value<?, XPression<?>> readObjectBody(CharReader cr) throws ManagedException {
		return readObjectBody(cr, new ObjectValue<>());
	}
	
	public Value<?, XPression<?>> readArrayBody(CharReader cr) throws ManagedException {
		ArrayValue<XPression<?>> av = new ArrayValue<>();
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		if(']' == ch.charValue()) {
			cr.nextChar();
			return av;
		}
		
		ch = null;
		
		do {
			Value<?, XPression<?>> item = readArrayItem(cr);
			av.add(item);
			
			ch = lexingRules.nextForwardNonBlankChar(cr);
			
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file reading array item"));
			
			if(']' == ch.charValue())  { cr.nextChar();	break;	}
			
			if(ch != ',') throw new ParsingException(String.format("',' expected after property value."));
			
			lexingRules.nextNonBlankChar(cr);
			
		} while(true);
		
		return av;
	}

}
