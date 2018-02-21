package com.exa.lang.parsing;

import java.io.IOException;

import com.exa.buffer.CharReader;
import com.exa.chars.EscapeCharMan;
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
	
	private XALLexingRules lexingRules = new XALLexingRules();
	
	public ObjectValue parse(CharReader cr) throws ManagedException {
		ObjectValue res = new ObjectValue();
		
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		if(ch.charValue() == ':') {
			lexingRules.nextNonBlankChar(cr);
			
			String type = lexingRules.nextNonNullString(cr);
			res.setAttribut("type", type);
			
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
			
			Value<?> propertyValue = readPropertyValueForObject(cr);
			
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
	
	public ObjectValue parseString(String script) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		return parse(cr);
	}
	
	public ObjectValue parseFile(String script) throws ManagedException {
		
		CharReader cr;
		try {
			cr = CharReader.forFile(script, false);
		} catch (IOException e) {
			throw new ManagedException(e);
		}
		
		return parse(cr);
	}
	
	
	
	private StringValue readString(CharReader cr, String end) throws ManagedException {
		String str = lexingRules.nextNonNullString(cr);
		
		if(!str.endsWith(end)) throw new ManagedException(String.format("%s is not a valid string", str));
		
		StringBuilder sb = new StringBuilder(str.substring(1, str.length()-1));
		EscapeCharMan.STANDARD.normalized(sb);
		return new StringValue(sb.toString());
	}
	
	private Value<? extends Number> readNumeric(CharReader cr) throws ManagedException {
		String str = lexingRules.nextNonNullString(cr);
		
		if(!lexingRules.isInteger(str, true)) throw new ParsingException(String.format("%s is not not numeric", str));
		
		if(XALLexingRules.EXTENDED_NUMERIC_TERMINATION.indexOf(str.charAt(str.length() - 1)) >= 0) return new DecimalValue(Double.parseDouble(str));
		
		Character ch = lexingRules.nextForwardChar(cr);
		
		if(ch == null) return new IntegerValue(Integer.parseInt(str));
		
		if(ch == '.') {
			cr.nextChar();
			
			ch = lexingRules.nextForwardChar(cr);
			if(ch == null || XALLexingRules.NUMERIC_DIGITS.indexOf(ch) < 0) return new DecimalValue(Double.parseDouble(str));
			
			String str2 = lexingRules.nextNonNullString(cr);
			
			if(!lexingRules.isInteger(str, true)) throw new ParsingException(String.format("%s is not not numeric", str));
			
			return new DecimalValue(Double.parseDouble(str+"."+str2));
		}
		
		return new IntegerValue(Integer.parseInt(str));
	}
	
	private ObjectValue readObjectByClass(CharReader cr, ObjectValue ov) throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) throw new ParsingException(String.format("Error near %s .", ch.toString()));
		
		String str = lexingRules.nextNonNullString(cr);
		if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
		
		ov.setAttribut("_class", str);
		
		return ov;
	}
	
	private ObjectValue readObjectByClass(CharReader cr) throws ManagedException {
		return readObjectByClass(cr, new ObjectValue());
	}
	
	public Value<?> readPropertyValueForObject(CharReader cr) throws ManagedException {
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		
		if(ch == null) return new BooleanValue(Boolean.TRUE);
		
		if(ch == '{') {
			lexingRules.nextNonBlankChar(cr);
			return readObjectBody(cr);
		}
		
		if(ch == '[') { 
			lexingRules.nextNonBlankChar(cr); 
			return readArrayBody(cr);
		}
		
		if(ch == ',') return new BooleanValue(Boolean.TRUE);
		
		if(ch == '\'' || ch == '\"') return readString(cr, ch.toString());
		
		if(XALLexingRules.NUMERIC_DIGITS.indexOf(ch) >= 0) return readNumeric(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) >= 0) {
			String str = lexingRules.nextNonNullString(cr);
			
			if("true".equals(str)) return new BooleanValue(Boolean.TRUE);
			
			if("false".equals(str)) return new BooleanValue(Boolean.FALSE);
			
			if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
			
			ObjectValue ov = new ObjectValue();
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
	
	private Value<?> readArrayItem(CharReader cr) throws ManagedException {
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
		
		if(ch == ',') return new BooleanValue(Boolean.TRUE);
		
		if(ch == '\'' || ch == '\"') return readString(cr, ch.toString());
		
		if(XALLexingRules.NUMERIC_DIGITS.indexOf(ch) >= 0) return readNumeric(cr);
		
		if(XALLexingRules.VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) >= 0) {
			String str = lexingRules.nextNonNullString(cr);
			
			if("true".equals(str)) return new BooleanValue(Boolean.TRUE);
			
			if("false".equals(str)) return new BooleanValue(Boolean.FALSE);
			
			if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Error near %s . May be identifier expected.", str));
			
			ObjectValue ov = new ObjectValue();
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
	
	public Value<?> readObjectBody(CharReader cr, ObjectValue ov) throws ManagedException {
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
			
			Value<?> propertyValue = readPropertyValueForObject(cr);
			
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
	
	public Value<?> readObjectBody(CharReader cr) throws ManagedException {
		return readObjectBody(cr, new ObjectValue());
	}
	
	public Value<?> readArrayBody(CharReader cr) throws ManagedException {
		ArrayValue av = new ArrayValue();
		Character ch = lexingRules.nextForwardNonBlankChar(cr);
		if(']' == ch.charValue()) {
			cr.nextChar();
			return av;
		}
		
		ch = null;
		
		do {
			Value<?> item = readArrayItem(cr);
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
