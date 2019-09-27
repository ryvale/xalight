package com.exa.lang.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exa.buffer.CharReader;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.MapVariableContext;
import com.exa.expression.parsing.Parser.UnknownIdentifierValidation;
import com.exa.lang.expression.TArrayValue;
import com.exa.lang.expression.TObjectValue;
import com.exa.lang.expression.XPEvaluatorSetup;
import com.exa.lang.expression.computer.TComputer;
import com.exa.lang.parsing.statements.STFor;
import com.exa.lang.parsing.statements.STIf;
import com.exa.lang.parsing.statements.STImport;
import com.exa.lang.parsing.statements.STName;
import com.exa.utils.ManagedException;
import com.exa.utils.io.FilesRepositories;
import com.exa.utils.values.ArrayValue;
import com.exa.utils.values.NullValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

public class XALParser {
	public static final TObjectValue T_OBJECT_VALUE = new TObjectValue();
	
	public static final TArrayValue T_ARRAY_VALUE = new TArrayValue();
	
	public static final TComputer T_COMPUTER = new TComputer();
	
	public static final NullValue<XPOperand<?>> NULL_VALUE = new NullValue<>();
	
	private FilesRepositories filesRepos;
	
	private XALLexingRules lexingRules = new XALLexingRules();
	
	private Map<String, ComputingStatement> statements = new HashMap<>();
	
	private STImport stImport = new STImport();
	
	public XALParser(FilesRepositories filesRepos) {
		this.filesRepos = filesRepos;
		
		statements.put("if", new STIf());
		
		statements.put("for", new STFor());
		
		statements.put("import", stImport);
		
		statements.put("name", new STName());
	}
	
	public static Map<String, ObjectValue<XPOperand<?>>> getDefaultObjectLib(ObjectValue<XPOperand<?>> rootOV) throws ManagedException {
		Map<String, ObjectValue<XPOperand<?>>> res = new HashMap<>();
		
		ObjectValue<XPOperand<?>> ovLib = rootOV.getAttributAsObjectValue(Computing.LIBN_DEFAULT);
		
		if(ovLib == null) ovLib = new ObjectValue<>();
		
		res.put(Computing.LIBN_DEFAULT, ovLib);
		
		return res;
	}
	
	public XALParser() {
		this(new FilesRepositories());
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(this, cr);
		
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		
		CharReader cr = new CharReader(script);
		
		Computing computing = new Computing(this, cr, evaluatorSetup, uiv);
		
		return computing.execute();
	}
	
	public ObjectValue<XPOperand<?>> parseString(String script, XPEvaluatorSetup evaluatorSetup) throws ManagedException {
		return parseString(script, evaluatorSetup, (id, context) -> null);
	}
	
	
	/*public ObjectValue<XPOperand<?>> parseFile(String script, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		CharReader cr = null;

		try {
			cr = CharReader.forFile(script, false);
			Computing computing = new Computing(this, cr, evaluatorSetup, uiv);
			return computing.execute();
		} catch (Exception e) {
			throw new ManagedException(e);
		}
		finally {
			if(cr != null) try { cr.close(); } catch (Exception e2) { e2.printStackTrace(); }
		}
		
	}
	
	public ObjectValue<XPOperand<?>> parseFile(String script, XPEvaluatorSetup evaluatorSetup) throws ManagedException {
		return parseFile(script, evaluatorSetup, (id, context) -> null);
	}
	
	public ObjectValue<XPOperand<?>> parseFile(String script) throws ManagedException {
		
		CharReader cr = null;
		try {
			cr = CharReader.forFile(script, false);
			Computing computing = new Computing(this, cr);
			return computing.execute();
		} catch (Exception e) {
			if(cr != null) try { cr.close(); } catch (Exception e2) { e2.printStackTrace(); }
			throw new ManagedException(e);
		}
		finally {
			if(cr != null) try { cr.close(); } catch (Exception e2) { e2.printStackTrace(); }
		}
		
	}*/
	
	/*public ObjectValue<XPOperand<?>> object(String scriptFile, String path, VariableContext entityVC) throws ManagedException {
		Computing computing = getExecutedComputeObjectFormFile(scriptFile);
		
		return Computing.object(computing, path, entityVC);
	}
	

	public static ObjectValue<XPOperand<?>> object(Computing executedComputing, String path, VariableContext entityVC) throws ManagedException {
		return Computing.object(executedComputing, path, entityVC);
	}

	public ObjectValue<XPOperand<?>> object(ObjectValue<XPOperand<?>> relativeOV, String path, Computing executedComputing, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		return Computing.object(this, relativeOV, path, executedComputing, entityVC, libOV);
	}
	
	public ObjectValue<XPOperand<?>> object(Computing executedComputing, ObjectValue<XPOperand<?>> ov, VariableContext entityVC, Map<String, ObjectValue<XPOperand<?>>> libOV) throws ManagedException {
		return Computing.object(executedComputing, ov, entityVC, libOV);
	}*/
	
	public static void loadImport(Computing excutedComputing, Map<String, ObjectValue<XPOperand<?>>> mpRefs) throws ManagedException {
		XALParser parser = excutedComputing.getParser();
		for(ObjectValue<XPOperand<?>> ovLib : mpRefs.values()) {
			Map<String, Value<?, XPOperand<?>>> mpRef = ovLib.getValue();
			
			List<String> propertiesTodelete = new ArrayList<>();
			Map<String, Value<?, XPOperand<?>>> propertiesToAdd = new LinkedHashMap<>();
			
			for(String oname : mpRef.keySet()) {
				if(oname.startsWith(Computing.PRTY_PREF_SUBSTITUTION)) {
					Value<?, XPOperand<?>> vlImport = mpRef.get(oname);
					ObjectValue<XPOperand<?>> ovImport = vlImport.asObjectValue();
					if(ovImport == null) continue;
					
					String statement = ovImport.getAttributAsString(Computing.PRTY_STATEMENT);
					
					if(!"import".equals(statement)) continue;
					
					ComputingStatement cs = parser.getStatements().get(statement);
					if(cs == null) continue;
					
					propertiesTodelete.add(oname);
					
					Value<?, XPOperand<?>> vlImportRes = cs.translate(ovImport, excutedComputing, new MapVariableContext(), new LinkedHashMap<>(), Computing.CS_IMPORT);
					
					if(vlImportRes == null) continue;
					
					ObjectValue<XPOperand<?>> ovImportRes = vlImportRes.asObjectValue();
					if(ovImportRes == null) continue;
					
					String insertion = ovImportRes.getAttributAsString(Computing.PRTY_INSERTION);
					if(Computing.VL_INCORPORATE.equals(insertion)) {
						ArrayValue<XPOperand<?>> av = ovImportRes.getAttributAsArrayValue(Computing.PRTY_VALUE);
						
						List<Value<?, XPOperand<?>>> lst = av.getValue();
						
						for(Value<?, XPOperand<?>> vlIncorporateItem : lst) {
							ObjectValue<XPOperand<?>> ovValue = vlIncorporateItem.asRequiredObjectValue();
							
							Map<String, Value<?, XPOperand<?>>> mpValue = ovValue.getValue();
							
							for(String newPropName : mpValue.keySet()) {
								propertiesToAdd.put(newPropName, mpValue.get(newPropName));
							}
						}
						
					}
				}
			}
			
			for(String p : propertiesTodelete) {
				mpRef.remove(p);
			}
			
			for(String p : propertiesToAdd.keySet()) {
				mpRef.put(p, propertiesToAdd.get(p).asObjectValue());
			}
		}
	}
	
	/*public ObjectValue<XPOperand<?>> objectFromFile(String scriptFile, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		Computing executedComputing = getExecutedComputeObjectFormFile(scriptFile, evaluatorSetup, uiv);
		
		ObjectValue<XPOperand<?>> ovRoot = executedComputing.getResult();
		
		Map<String, ObjectValue<XPOperand<?>>> mpLib = XALParser.getDefaultObjectLib(ovRoot);
		
		loadImport(executedComputing, mpLib);
		
		for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
			executedComputing.resolveHeirsObject(ovLib);
		}
		
		return ovRoot;
	}*/
	
	/*public ObjectValue<XPOperand<?>> object(String scriptFile, VariableContext entityVC) throws ManagedException {
		Computing executedComputing = getExecutedComputeObjectFormFile(scriptFile);
		
		Map<String, Value<?, XPOperand<?>>> mpRoot = executedComputing.getResult().getValue();
		
		for(String entityName : mpRoot.keySet()) {
			mpRoot.put(entityName, Computing.object(executedComputing, entityName, entityVC));
		}
		return executedComputing.getResult(); 
	}*/

	
	public Computing getComputeObjectFormFile(String script, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		CharReader cr = null;
		try {
			cr = CharReader.forFile(script, false);
		} catch (Exception e) {
			if(cr != null) try { cr.close();} catch (Exception e2) { e2.printStackTrace(); }
			throw new ManagedException(e);
		}
		
		return new Computing(this, cr, evaluatorSetup, uiv);
	}
	
	public Computing getExecutedComputeObjectFormFile(String script, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv, Map<String, ObjectValue<XPOperand<?>>> mpLib) throws ManagedException {
		CharReader cr = null;
		try {
			cr = CharReader.forFile(script, false);
			Computing res = new Computing(this, cr, evaluatorSetup, uiv);
			
			res.execute();
			
			loadImport(res, mpLib);
			
			for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
				res.resolveHeirsObject(ovLib);
			}
			
			
			return res;
		} catch (Exception e) {
			throw new ManagedException(e);
		}
		finally {
			if(cr != null) try { cr.close(); } catch (Exception e2) { e2.printStackTrace(); }
		}
	}
	
	public Computing getExecutedComputeObjectFormFile(String script, XPEvaluatorSetup evaluatorSetup, UnknownIdentifierValidation uiv) throws ManagedException {
		CharReader cr = null;
		try {
			cr = CharReader.forFile(script, false);
			Computing res = new Computing(this, cr, evaluatorSetup, uiv);
			
			ObjectValue<XPOperand<?>> ovRoot = res.execute();
			
			Map<String, ObjectValue<XPOperand<?>>> mpLib = getDefaultObjectLib(ovRoot);
			
			loadImport(res, mpLib);

			for(ObjectValue<XPOperand<?>> ovLib : mpLib.values()) {
				res.resolveHeirsObject(ovLib);
			}
			
			return res;
		} catch (Exception e) {
			throw new ManagedException(e);
		}
		finally {
			if(cr != null) try { cr.close(); } catch (Exception e2) { e2.printStackTrace(); }
		}
	}
	
	
	public Computing getExecutedComputeObjectFormFile(String script) throws ManagedException {
		return getExecutedComputeObjectFormFile(script, evSetup -> {}, (id, context) -> null);
		
	}

	public XALLexingRules getLexingRules() {
		return lexingRules;
	}

	public Map<String, ComputingStatement> getStatements() {
		return statements;
	}

	public FilesRepositories getFilesRepos() {
		return filesRepos;
	}

	public void setFilesRepos(FilesRepositories filesRepos) {
		this.filesRepos = filesRepos;
	}
	
	public void setImportParam(XPEvaluatorSetup evSetup) {
		stImport.setEvaluatorSetup(evSetup);
	}
	
	public void setImportParam(UnknownIdentifierValidation uiv) {
		stImport.setUiv(uiv);
	}
	
}


