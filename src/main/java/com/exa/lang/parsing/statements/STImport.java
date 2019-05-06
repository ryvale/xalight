package com.exa.lang.parsing.statements;

import java.util.List;
import java.util.Map;

import com.exa.buffer.CharReader;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.expression.XALCalculabeValue;
import com.exa.lang.parsing.Computing;
import com.exa.lang.parsing.ComputingStatement;
import com.exa.lang.parsing.XALLexingRules;
import com.exa.lang.parsing.XALParser;
import com.exa.lexing.ParsingException;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.StringValue;
import com.exa.utils.values.Value;

public class STImport implements ComputingStatement {
	private XALParser parser;
	
	public STImport(XALParser parser) {
		super();
		this.parser = parser;
	}

	@Override
	public ObjectValue<XPOperand<?>> compileObject(Computing computing, String context) throws ManagedException {
		final CharReader charReader = computing.getCharReader();
		final XALLexingRules lexingRules = computing.getParser().getLexingRules();
		//final Parser xpCompiler = computing.getXpCompiler();
		
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) throw new ManagedException(String.format("Unexpected end of file in %s. '[' or '{' expected after import statement", context));
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		res.setAttribut(Computing.PRTY_STATEMENT, "import");
		ArrayValue<XPOperand<?>> avEntities = new ArrayValue<>();
		res.setAttribut(Computing.PRTY_ENTITIES, avEntities);
		
		if(ch == '[') {
			lexingRules.nextNonBlankChar(charReader);
			
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file. Expected identifier or ']' in import statement in context %s", context));
			
			
			
			if(ch == ']') {
				
			}
			else {
				
				do {
					String destEntity = null;
					String srcEntity = fullQualifiedName(charReader, lexingRules, context);
					
					ch = lexingRules.nextForwardNonBlankChar(charReader);
					
					if(ch == null) throw new ParsingException(String.format("Unexpected end of file. Expected ',', '->' or ']' in import statement in context %s", context));
					
					ObjectValue<XPOperand<?>> ovImportItem = new ObjectValue<>();
					if(ch == '-') {
						lexingRules.nextNonBlankChar(charReader);
						
						ch = charReader.nextChar();
						if(ch == null) throw new ParsingException(String.format("Unexpected end of file. Expected  '->' in import statement in context %s", context));
						if(ch != '>') throw new ParsingException(String.format("Syntax error. Expected  '->' in import statement in context %s", context));
						
						
						destEntity = lexingRules.nextString(charReader);
						if(destEntity == null) throw new ParsingException(String.format("Unexpected end of file. Expected identifier in import statement in context %s", context));
						if(destEntity.endsWith("[*]") && ! srcEntity.endsWith("[*]") || !destEntity.endsWith("[*]") && srcEntity.endsWith("[*]"))
							throw new ManagedException(String.format("Syntax error. Source entity '%s' and destination entity '%s' should end with wildcard '*' or not in import statement in context %s", srcEntity, destEntity, context));
						
					}
					else {
						destEntity = srcEntity;
					}
					
					ovImportItem.setAttribut(Computing.PRTY_SOURCE, srcEntity);
					ovImportItem.setAttribut(Computing.PRTY_DESTINATION, destEntity);
					avEntities.add(ovImportItem);
					
					ch = lexingRules.nextNonBlankChar(charReader);
					if(ch == null) throw new ParsingException(String.format("Unexpected end of file. Expected ',' or ']' in import statement in context %s", context));
					
					if(ch == ']' ) break;
					
					if(ch != ',') throw new ParsingException(String.format("Expected ',' or ']' in import statement in context %s", context));
					
				} while(true);
				
				ch = lexingRules.nextForwardNonBlankChar(charReader);
				if(ch == null) throw new ParsingException(String.format("Unexpected end of file. File name expected in import statement in context %s", context));
				
				if(ch != '"'  && ch !='\'') throw new ParsingException(String.format("Syntax error near %s.File name expected in import statement in context %s", ch.toString(), context));
				
				String file = computing.readString(ch.toString()).asString();
				res.setAttribut(Computing.PRTY_FILE, file);
				
			}
			
			return res;
		}
		
		if(ch == '{') {
			Value<?, XPOperand<?>> vl = computing.readPropertyValueForObject(context);
			ObjectValue<XPOperand<?>> ov = vl.asObjectValue();
			
			Value<?, XPOperand<?>> vlFile = ov.getAttribut(Computing.PRTY_FILE);
			if(vlFile == null) throw new ManagedException(String.format("Syntax error near %s. The property 'file' is missing in import statement in context %s", ch.toString(), context));
			if(vlFile.asStringValue() == null) {
				CalculableValue<?, XPOperand<?>> cl = vlFile.asCalculableValue();
				if(cl == null) throw new ManagedException(String.format("Syntax error. The property 'file' should have a string type in import statement in context %s", context));
				
				XALCalculabeValue<?> xalCl = (XALCalculabeValue<?>) cl;
				if("string".equals(xalCl.typeName())) throw new ManagedException(String.format("The file expression is not a string in import statement in context %s", context));
			}
			res.setAttribut(Computing.PRTY_FILE, vlFile);
			
			Value<?, XPOperand<?>> vlCfgEntities = ov.getAttribut(Computing.PRTY_ENTITIES);
			if(vlCfgEntities == null) throw new ManagedException(String.format("Syntax error. The property '%' is missing in import statement in context %s", Computing.PRTY_ENTITIES, context));
			
			ArrayValue<XPOperand<?>> avCfgEntities = vlCfgEntities.asArrayValue();
			if(avCfgEntities == null) throw new ManagedException(String.format("Syntax error. The property '%' should be an array in import statement in context %s", Computing.PRTY_ENTITIES, context));
			
			List<Value<?, XPOperand<?>>> lstCfgEntities = avCfgEntities.getValue();
			
			for(Value<?, XPOperand<?>> vlEntity : lstCfgEntities) {
				StringValue<?> strEntity = vlEntity.asStringValue();
				ObjectValue<XPOperand<?>> ovEntity;
				if(strEntity != null) {
					String entity = strEntity.getValue();
					ovEntity = new ObjectValue<>();
					if(entity.endsWith("*")) {
						ovEntity.setAttribut(Computing.PRTY_SOURCE, entity);
						ovEntity.setAttribut(Computing.PRTY_DESTINATION, "*");
					}
					else {
						ovEntity.setAttribut(Computing.PRTY_SOURCE, entity);
						ovEntity.setAttribut(Computing.PRTY_DESTINATION, entity);
					}
					
					avEntities.add(ovEntity);
					continue;
				}
				
				ovEntity = vlEntity.asObjectValue();
				if(ovEntity == null) throw new ManagedException(String.format("Syntax error. The property '%' should have string or object items in import statement in context %s", Computing.PRTY_ENTITIES, context));
				
				Value<?, XPOperand<?>> vlSrcEtity = ovEntity.getAttribut(Computing.PRTY_SOURCE);
				
				if(vlSrcEtity == null) throw new ManagedException(String.format("Syntax error. The property '%' should have  object items with %s property in import statement in context %s", Computing.PRTY_ENTITIES, Computing.PRTY_SOURCE, context));
				if(vlSrcEtity.asStringValue() == null) {
					CalculableValue<?, XPOperand<?>> cl = vlSrcEtity.asCalculableValue();
					
					if(cl == null) throw new ManagedException(String.format("Syntax error. The property '%' should have  object items with %s property with string type in import statement in context %s", Computing.PRTY_ENTITIES, Computing.PRTY_SOURCE, context));
					
					XALCalculabeValue<?> xalCl = (XALCalculabeValue<?>) cl;
					if("string".equals(xalCl.typeName())) throw new ManagedException(String.format("Syntax error. The property '%' should have object items with %s property with string type in import statement in context %s", Computing.PRTY_ENTITIES, Computing.PRTY_SOURCE, context));
				}
				ovEntity.setAttribut(Computing.PRTY_SOURCE, vlSrcEtity);
				
				Value<?, XPOperand<?>> vlDstEtity = ovEntity.getAttribut(Computing.PRTY_DESTINATION);
				if(vlDstEtity == null) {
					ovEntity.setAttribut(Computing.PRTY_DESTINATION, vlSrcEtity);
				} else {
					if(vlDstEtity.asStringValue() == null) {
						CalculableValue<?, XPOperand<?>> cl = vlDstEtity.asCalculableValue();
						
						if(cl == null) throw new ManagedException(String.format("Syntax error. The property '%' should have  object items with %s property with string type in import statement in context %s", Computing.PRTY_ENTITIES, Computing.PRTY_DESTINATION, context));
						
						XALCalculabeValue<?> xalCl = (XALCalculabeValue<?>) cl;
						if("string".equals(xalCl.typeName())) throw new ManagedException(String.format("Syntax error. The property '%' should have object items with %s property with string type in import statement in context %s", Computing.PRTY_ENTITIES, Computing.PRTY_DESTINATION, context));
					}
					ovEntity.setAttribut(Computing.PRTY_DESTINATION, vlDstEtity);
					
				}
				
				avEntities.add(ovEntity);
			}
		}
		return res;
	}
	
	private String fullQualifiedName(CharReader charReader, XALLexingRules lexingRules, String context) throws ManagedException {
		String str = lexingRules.nextNonNullString(charReader);
		if(str == null) throw new ParsingException(String.format("Unexpected end of file. Expected identifier or '*' in import statement instead of '%s' in context %s", str, context));
		if(str.equals("*")) return str;
		
		if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Syntax error. expected identifier in import statement instead of '%s' in context %s", str, context));
		StringBuilder res = new StringBuilder(str);
		
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) throw new ParsingException(String.format("Unexpected end of file. Expected ',', '->' or ']' in import statement instead of '%s' in context %s", str, context));
		while(ch == '.') {
			lexingRules.nextNonBlankChar(charReader);
			str = lexingRules.nextNonNullString(charReader);
			if(str == null) throw new ParsingException(String.format("Unexpected end of file. Expected identifier or '*' in import statement instead of '%s' in context %s", str, context));
			
			if(str.equals("*")) {
				res.append(".").append(str);
				break;
			}
			
			if(!lexingRules.isIdentifier(str)) throw new ParsingException(String.format("Syntax error. expected identifier in import statement instead of '%s' in context %s", str, context));
			res.append(".").append(str);
			ch = lexingRules.nextForwardNonBlankChar(charReader);
			if(ch == null) throw new ParsingException(String.format("Unexpected end of file. Expected ',', '-' or ']' in import statement instead of '%s' in context %s", str, context));
		}
		
		return res.toString();
	}

	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, XPEvaluator evaluator, VariableContext ovc,
			Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		
		return null;
	}

}
