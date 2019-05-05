package com.exa.lang;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exa.chars.EscapeCharMan;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.MapVariableContext;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.expression.VIEvaluatorSetup;
import com.exa.lang.parsing.Computing;
import com.exa.lang.parsing.XALParser;
import com.exa.utils.ManagedException;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.Value;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class XalightApplicationTests extends TestCase {
	
	public XalightApplicationTests( String testName ) {
        super( testName );
    }
	
	public static Test suite()  {
        return new TestSuite( XalightApplicationTests.class );
    }
	
	public void testXalFile() throws ManagedException {
		XALParser parser = new XALParser();
		ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test.xal");
		
		assertTrue("xlsx".equals(ov.getAttributAsString(Computing.PRTY_TYPE)));
		
		assertTrue("repo:default/equipement-a-renouveler.xls".equals(ov.getPathAttributAsString("model.file")));
		
		List<Value<?, XPOperand<?>>> l = ov.getPathAttributAsArray("model.sheets");
		
		assertTrue(l.size() > 0);
		
		assertTrue("Automates".equals(l.get(0).asObjectValue().getAttributAsString(Computing.PRTY_NAME)));
		
		assertTrue("Forages".equals(l.get(1).asObjectValue().getAttributAsString(Computing.PRTY_NAME)));
		
		assertTrue(new Integer(2).equals(l.get(1).asObjectValue().getAttributAsInteger("index")));
	}
	
	public void testEscape() {
		String str = "abc\\\\d";
		
		System.out.println(str);
		
		StringBuilder sb = new StringBuilder(str);
		EscapeCharMan.STANDARD.normalized(sb);
		
		System.out.println(sb);
		
		assertTrue("abc\\d".equals(sb.toString()));
	}
	
	public void testXalString() throws ManagedException {
		XALParser parser = new XALParser();
		ObjectValue<XPOperand<?>> ov = parser.parseString(":xlsx, model { file 'repo:default/equipement-a-renouveler.xls', sheets [ Automates, Forages { num 2 } ] }, data [ automates { defaultSheet Automates, record [ A3 { sheet Automates,  exp code }, B8 { exp libelle} ], lists [ { sheet Forages, row 5, record [A { exp debut }] }] } ]");
		
		assertTrue("xlsx".equals(ov.getAttributAsString(Computing.PRTY_TYPE)));
		
		System.out.println(ov.getPathAttributAsString("model.file"));
		
		assertTrue("repo:default/equipement-a-renouveler.xls".equals(ov.getPathAttributAsString("model.file")));
		
		List<Value<?, XPOperand<?>>> l = ov.getPathAttributAsArray("model.sheets");
		
		assertTrue(l.size() > 0);
		
		assertTrue("Automates".equals(l.get(0).asObjectValue().getAttributAsString(Computing.PRTY_NAME)));
		
		assertTrue("Forages".equals(l.get(1).asObjectValue().getAttributAsString(Computing.PRTY_NAME)));
		
		assertTrue(new Integer(2).equals(l.get(1).asObjectValue().getAttributAsInteger("num")));
	}
	
	public void testXalInheritance() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test2.xal");
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = parser.object("./src/test/java/com/exa/lang/test2.xal", "entities.entity2", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property2")));
		
		assertTrue("afb".equals(ovEntity.getPathAttributAsString("cplx.property1")));
		
		//assertTrue("a".equals(ov.getPathAttributAsString("entities.entity2.cplx.property1")));
		
		//assertTrue("af".equals(ov.getPathAttributAsString("entities.entity2._call_params.prm")));
		
		assertTrue("afc".equals(ovEntity.getPathAttributAsString("cplx.property3")));
		
		//assertTrue(new Integer(10).equals(ov.getPathAttribut("entities.entity2.cplx.property0")));
		
		//ObjectValue<XPOperand<?>> ov1 = ov.getPathAttributAsObjecValue("entities.entity2");
		
		//assertTrue(new Integer(2).equals(ov1.getAttributAsInteger("property1")));
		/*Map<String, ObjectValue<XPOperand<?>>> libOV = new HashMap<>();
		libOV.put(Computing.LIBN_DEFAULT, ov.getAttributAsObjectValue(Computing.LIBN_DEFAULT));
		
		entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		ObjectValue<XPOperand<?>> ovEntity2 = parser.object(ov, "entities.entity2", evaluator, entityVC, libOV);
		
		assertTrue("a".equals(ovEntity2.getPathAttributAsString("property2")));
		
		assertTrue("afb".equals(ovEntity2.getPathAttributAsString("cplx.property1")));
				
		assertTrue("afc".equals(ovEntity2.getPathAttributAsString("cplx.property3")));*/
	}
	
	public void testXalInheritance2() throws ManagedException {
		XALParser parser = new XALParser();
		ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test2.xal");
		
		Map<String, ObjectValue<XPOperand<?>>> libOV = new HashMap<>();
		libOV.put(Computing.LIBN_DEFAULT, ov.getAttributAsObjectValue(Computing.LIBN_DEFAULT));
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity2 = parser.object(ov.getAttributByPathAsObjectValue("entities.entity2"), evaluator, entityVC, libOV);
		
		assertTrue("a".equals(ovEntity2.getPathAttributAsString("property2")));
		
		assertTrue("afb".equals(ovEntity2.getPathAttributAsString("cplx.property1")));
		
		assertTrue("afc".equals(ovEntity2.getPathAttributAsString("cplx.property3")));
	}

	
	public void testXalInheritance5() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test2.xal");
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = parser.object("./src/test/java/com/exa/lang/test3.xal", "entities.entity3.cplx", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue("b".equals(ovEntity.getPathAttributAsString("property2")));
		
		assertTrue("okc".equals(ovEntity.getPathAttributAsString("property")));
	
		
		
	}
	
	public void testXalInheritance6() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test4.xal");
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = parser.object("./src/test/java/com/exa/lang/test4.xal", "entities.entity1", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue("2".equals(ovEntity.getPathAttributAsString("property")));
		
	}
	
	public void testXalInheritance7() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test4.xal");
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = parser.object("./src/test/java/com/exa/lang/test4.xal", "entities.entity2", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		evaluator.addVariable("prm", String.class, "3");
		assertTrue("2".equals(ovEntity.getPathAttributAsString("property1")));
		
	}
	
	public void testXalInheritance8() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test4.xal");
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		evaluator.getCurrentVariableContext().addVariable("start", String.class, "01/02/2016");
		evaluator.getCurrentVariableContext().addVariable("end", String.class, "17/08/2018");
		
		ObjectValue<XPOperand<?>> ovEntity = parser.object("./src/test/java/com/exa/lang/test2.ds.xal", "entities.entity2", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		
		//assertTrue("2".equals(ovEntity.getPathAttributAsString("property1")));
		
	}
	
	public void testXalInheritance9() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test4.xal");
		
		//XPEvaluator evaluator = new XPEvaluator();
		
		//VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		//evaluator.getCurrentVariableContext().addVariable("start", String.class, "01/02/2016");
		//evaluator.getCurrentVariableContext().addVariable("end", String.class, "17/08/2018");
		
		VIEvaluatorSetup evSetup = new VIEvaluatorSetup();
		evSetup.addVaraiable("start", String.class, "01/02/2016");
		evSetup.addVaraiable("end", String.class, "17/08/2018");
		
		
		
		Computing computing = parser.getComputeObjectFormFile("./src/test/java/com/exa/lang/private/stats-intervention.ds.xal", evSetup, (id, context) -> {
			if("rootOv".equals(id)) return "ObjectValue";
			//String p[] = context.split("[.]");
			
			return null;
		});
		
		//ObjectValue<XPOperand<?>> rootOv = computing.execute();
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		ObjectValue<XPOperand<?>> rootOV = computing.execute();
		
		try {
			computing.closeCharReader();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		evaluator.addVariable("rootOv", ObjectValue.class, rootOV);
		ObjectValue<XPOperand<?>> ovEntities = rootOV.getAttributAsObjectValue("entities");
		
		VariableContext vc = new MapVariableContext(evaluator.getCurrentVariableContext());
		evaluator.pushVariableContext(vc);
		
		ObjectValue<XPOperand<?>> ovEntity = parser.object(ovEntities, "sdtt", evaluator, vc, Computing.getDefaultObjectLib(rootOV));		
		
		assertTrue("row-to-field".equals(ovEntity.getPathAttributAsString("dt.type")));
		
		assertTrue("int".equals(ovEntity.getPathAttributAsString("dt.fields.type")));
		
		assertTrue("sql".equals(ovEntity.getPathAttributAsString("dt.source.type")));
		
		System.out.println(ovEntity.getPathAttributAsString("dt.source.criteria"));
		
	}

	public void testStatementIf() throws ManagedException {
		XALParser parser = new XALParser();
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		entityVC.assignContextVariable("gv", Boolean.TRUE);
		
		ObjectValue<XPOperand<?>> ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity1", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue(new Integer(2).equals(ovEntity.getAttributAsInteger("property")));
		
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity3", evaluator, entityVC);
		assertTrue(new Integer(10).equals(ovEntity.getAttributAsInteger("property")));
		
		entityVC.addVariable("v", Boolean.class, Boolean.TRUE);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity4", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx.property")));
		
		
		entityVC.assignContextVariable("v", Boolean.FALSE);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity4", evaluator, entityVC);
		assertTrue("b".equals(ovEntity.getPathAttributAsString("cplx.property")));
		
		
		entityVC.assignContextVariable("v", Boolean.FALSE);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity5", evaluator, entityVC);
		assertTrue(ovEntity.getAttribut("cplx") == null);
		
		
		entityVC.assignContextVariable("v", Boolean.TRUE);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity6", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getAttributAsString("property")));
		
		
		entityVC.assignContextVariable("v", Boolean.TRUE);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity7", evaluator, entityVC);
		assertTrue(new Integer(2).equals(ovEntity.getPathAttributAsInteger("cplx.property")));
	}
	
	public void testStatementIf2() throws ManagedException {
		XALParser parser = new XALParser();
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		entityVC.assignContextVariable("gv", Boolean.TRUE);
		entityVC.assignContextVariable("v", Boolean.TRUE);
		entityVC.assignContextVariable("gi", 1);
		
		ObjectValue<XPOperand<?>>  ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity8", evaluator, entityVC);
		assertTrue("8a".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity9", evaluator, entityVC);
		assertTrue("9-1a".equals(ovEntity.getPathAttributAsString("property1")));
		assertTrue("9-2a".equals(ovEntity.getPathAttributAsString("property2")));
		
		
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity10", evaluator, entityVC);
		assertTrue(new Integer(1).equals(ovEntity.getPathAttributAsInteger("property")));
		
		entityVC.assignContextVariable("gi", 0);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity10", evaluator, entityVC);
		assertTrue(new Integer(2).equals(ovEntity.getPathAttributAsInteger("property")));
		
		
		entityVC.assignContextVariable("gi", 1);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity11", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property")));
		
		
		entityVC.assignContextVariable("gi", 0);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity11", evaluator, entityVC);
		assertTrue(ovEntity.getAttribut("property") == null);
		
		entityVC.assignContextVariable("gi", 1);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity12", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property")));
		
		entityVC.assignContextVariable("gi", 0);
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity12", evaluator, entityVC);
		assertTrue("b".equals(ovEntity.getPathAttributAsString("cplx.property")));
		
		
		ovEntity = parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity13", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx.property")));
	}
	
	
	public void testStatementFor() throws ManagedException {
		XALParser parser = new XALParser();
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity1", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property1")));
		
		ovEntity = parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity2", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property1")));
		
		ovEntity = parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity6", evaluator, entityVC);
		assertTrue("1".equals(ovEntity.getPathAttributAsString("property1")));
	}
}
