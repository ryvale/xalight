package com.exa.lang.parsing;

import com.exa.buffer.CharReader;
import com.exa.lexing.LexingRules;
import com.exa.lexing.ParsingException;
import com.exa.lexing.WordWithOpenCloseDelimiter;

public class XALLexingRules extends LexingRules {
	
	static final String VALID_PROPERTY_NAME_CHARS_LC = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
	static final String EXTENDED_PROPERTY_FIRST_CHARS = "$";
	
	static final String NUMERIC_DIGITS = "0123456789";
	
	static final String EXTENDED_NUMERIC_TERMINATION = "d";

	public XALLexingRules() {
		super(" \t\n\r");
		
		addWordSeparator(new WordWithOpenCloseDelimiter(this, '"','"'));
		addWordSeparator(new WordWithOpenCloseDelimiter(this, '\'','\''));
		addWordSeparator("[", "]", "{", "}", ",", "(", ")", ":", "=>", "?", ".", "=");
	}
	
	public String nextPropertyName(CharReader cr) throws ParsingException {
		String str = nextString(cr);
		
		if(isPropertyName(str)) return str;
		
		return null;
		
	}
	
	public String nextRequiredPropertyName(CharReader cr) throws LexingException {
		String str;
		try {
			str = nextString(cr);
		} catch (ParsingException e) {
			throw new LexingException(e.getMessage(), null);
		}
		
		if(isPropertyName(str)) return str;
		
		throw new LexingException(String.format("%s is not a valid property name.", str), str);
	}
	
	public boolean isPropertyName(String name) {
		if(name == null || name.equals("")) return false;
		
		if(name.equals("=>")) return true;
		
		//name = name.toLowerCase();

		char ch = name.charAt(0);
		
		if(NUMERIC_DIGITS.indexOf(ch) >= 0) return false;
		
		if((VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) && (EXTENDED_PROPERTY_FIRST_CHARS.indexOf(ch) < 0)) {
			if(ch != '?') return false;
		}
		
		for(int i=1; i<name.length(); i++) {
			ch = name.charAt(i);
			if((VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) && (NUMERIC_DIGITS.indexOf(ch) < 0)) return false;
		}
		
		return true;
	}
	
	public boolean isIdentifier(String name) {
		if(name == null || name.equals("")) return false;
		
		name = name.toLowerCase();

		char ch = name.charAt(0);
		
		if(VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) return false;
		
		for(int i=1; i<name.length(); i++) {
			ch = name.charAt(i);
			if((VALID_PROPERTY_NAME_CHARS_LC.indexOf(ch) < 0) && (NUMERIC_DIGITS.indexOf(ch) < 0)) return false;
		}
		
		return true;
	}
	
	public boolean isInteger(String str, boolean exetendNum) {
		if(exetendNum) {
			for(int i=0; i<str.length()-1; i++) {
				char ch = str.charAt(i);
				if(NUMERIC_DIGITS.indexOf(ch) < 0) return false;
			}
			
			char ch = str.charAt(str.length()-1);
			if(NUMERIC_DIGITS.indexOf(ch) < 0) {
				if(EXTENDED_NUMERIC_TERMINATION.indexOf(ch) < 0) return false;
			}
			
			return true;
		}
		
		for(int i=1; i<str.length(); i++) {
			char ch = str.charAt(i);
			if(NUMERIC_DIGITS.indexOf(ch) < 0) return false;
		}
		
		return true;
	}

	public boolean isInteger(String str) {
		return isInteger(str, false);
	}
}
