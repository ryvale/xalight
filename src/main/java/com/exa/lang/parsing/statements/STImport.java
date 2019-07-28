package com.exa.lang.parsing.statements;

import java.util.List;
import java.util.Map;

import com.exa.buffer.CharReader;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.expression.parsing.Parser.UnknownIdentifierValidation;
import com.exa.lang.expression.XALCalculabeValue;
import com.exa.lang.expression.XPEvaluatorSetup;
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
	
	private XPEvaluatorSetup evaluatorSetup;
	private UnknownIdentifierValidation uiv;
	
	
	public STImport(XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) {
		super();
		this.evaluatorSetup = evaluatorSetup;
		this.uiv = uiv;
	}
	
	public STImport() { this(evSetup -> {}, (id, context) -> null ); }

	//private 

	@Override
	public ObjectValue<XPOperand<?>> compileObject(Computing computing, String context) throws ManagedException {
		final CharReader charReader = computing.getCharReader();
		final XALLexingRules lexingRules = computing.getParser().getLexingRules();
		
		Character ch = lexingRules.nextForwardNonBlankChar(charReader);
		if(ch == null) throw new ManagedException(String.format("Unexpected end of file in %s. '[' or '{' expected after import statement", context));
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<>();
		res.setAttribut(Computing.PRTY_STATEMENT, "import");
		ArrayValue<XPOperand<?>> avEntities = new ArrayValue<>();
		res.setAttribut(Computing.PRTY_ENTITIES, avEntities);
		res.setAttribut(Computing.PRTY_CONTEXT, context);
		
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
				
				String file = computing.readStringReturnString(ch.toString());
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

	@SuppressWarnings("unchecked")
	@Override
	public Value<?, XPOperand<?>> translate(ObjectValue<XPOperand<?>> ov, Computing executedComputing, VariableContext ovc, Map<String, ObjectValue<XPOperand<?>>> libOV, String cmd) throws ManagedException {
		String context = ov.getAttributAsString(Computing.PRTY_CONTEXT);
		
		XALParser parser = executedComputing.getParser();
		
		Value<?, XPOperand<?>> vl = ov.getRequiredAttribut(Computing.PRTY_FILE);
		CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
		
		XPEvaluator evaluator = executedComputing.getXPEvaluator();
		String fileName; 
		if(cl == null) {
			fileName = vl.asRequiredString();
		}
		else {
			if(Computing.CS_IMPORT.equals(cmd)) return null;
			
			XALCalculabeValue<String> xalCLStr = (XALCalculabeValue<String>) vl;
			if(xalCLStr.getVariableContext() == null) xalCLStr.setVariableContext(ovc);
			xalCLStr.setEvaluator(evaluator);
			fileName = xalCLStr.asRequiredString();
		}
		fileName = parser.getFilesRepos().getName(fileName);
		
		ArrayValue<XPOperand<?>> avEntities = ov.getAttributAsArrayValue(Computing.PRTY_ENTITIES);
		if(avEntities == null) throw new ManagedException(String.format("The property '%s' is misconfigured in context %s in import statement", Computing.PRTY_ENTITIES, context));
		
		List<Value<?, XPOperand<?>>> lstEntities = avEntities.getValue();
		
		ArrayValue<XPOperand<?>> arRes = new ArrayValue<>();
		
		for(Value<?, XPOperand<?>> vlImport : lstEntities) {
			ObjectValue<XPOperand<?>> ovImport = vlImport.asObjectValue();
			if(ovImport == null) throw new ManagedException(String.format("The property '%s' is misconfigured in context %s in import statement", Computing.PRTY_ENTITIES, context));
			
			String source;
			vl = ovImport.getAttribut(Computing.PRTY_SOURCE);
			cl = vl.asCalculableValue();
			if(cl == null) {
				source = vl.asRequiredString();
			}
			else {
				if(Computing.CS_IMPORT.equals(cmd)) return null;
				XALCalculabeValue<String> xalCLStr = (XALCalculabeValue<String>) vl;
				if(xalCLStr.getVariableContext() == null) xalCLStr.setVariableContext(ovc);
				xalCLStr.setEvaluator(evaluator);
				source = xalCLStr.asRequiredString();
			}
			
			String dest;
			vl = ovImport.getAttribut(Computing.PRTY_DESTINATION);
			cl = vl.asCalculableValue();
			if(cl == null) dest = vl.asRequiredString();
			else {
				if(Computing.CS_IMPORT.equals(cmd)) return null;
				XALCalculabeValue<String> xalCLStr = (XALCalculabeValue<String>) vl;
				if(xalCLStr.getVariableContext() == null) xalCLStr.setVariableContext(ovc);
				xalCLStr.setEvaluator(evaluator);
				dest = xalCLStr.asRequiredString();
			}
			
			Computing importComputing = parser.getExecutedComputeObjectFormFile(fileName, evaluatorSetup, uiv);
			
			ObjectValue<XPOperand<?>> ovDest;
			if(source.endsWith("*")) {
				String parts[] = source.split("[.]");
				ObjectValue<XPOperand<?>> ovSrc;
				
				if(parts.length > 2) {
					String entitPath = source.substring(0, source.length() - parts[parts.length-1].length() - 1);
					
					ovSrc = executedComputing.object(entitPath, ovc);
				}
				else {
					ovSrc = importComputing.object(ovc);
				}
				
				ovDest = new ObjectValue<>();
				
				Map<String, Value<?, XPOperand<?>>> mpSrc = ovSrc.getValue();
				for(String property : mpSrc.keySet()) {
					Value<?, XPOperand<?>> vlSrc = mpSrc.get(property);
					
					if(vlSrc == null) continue;
					
					ovDest.setAttribut(dest.replaceAll("[*]", property), vlSrc);
				}
			}
			else {
				
				ObjectValue<XPOperand<?>> ovDestInt;
				
				if(Computing.CS_IMPORT.equals(cmd)) {
					ObjectValue<XPOperand<?>> rootOV = importComputing.getResult();
					ovDestInt = rootOV.getPathAttributAsObjecValue(source);
				}
				else ovDestInt = importComputing.object(source, ovc);
				
				String parts[] = dest.split("[.]");
				
				ovDest = new ObjectValue<>();
				ovDest.setAttribut(parts[parts.length - 1], ovDestInt);
			}
			
			arRes.add(ovDest);
		}
		
		ObjectValue<XPOperand<?>> res = new ObjectValue<XPOperand<?>>();
		
		res.setAttribut(Computing.PRTY_CONTEXT, ov.getAttribut(Computing.PRTY_CONTEXT));
		res.setAttribut(Computing.PRTY_INSERTION, Computing.VL_INCORPORATE);
		res.setAttribut(Computing.PRTY_VALUE, arRes);
		
		
		return res;
	}

	public XPEvaluatorSetup getEvaluatorSetup() {
		return evaluatorSetup;
	}

	public void setEvaluatorSetup(XPEvaluatorSetup evaluatorSetup) {
		this.evaluatorSetup = evaluatorSetup;
	}

	public UnknownIdentifierValidation getUiv() {
		return uiv;
	}

	public void setUiv(UnknownIdentifierValidation uiv) {
		this.uiv = uiv;
	}

}
