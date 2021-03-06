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
import com.exa.utils.io.FilesRepositories;
import com.exa.utils.io.OSFileRepoPart;
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
		ObjectValue<XPOperand<?>> ov = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test.xal").getResult();
		
		assertTrue("xlsx".equals(ov.getAttributAsString(Computing.PRTY_TYPE)));
		
		assertTrue("repo:default/equipement-a-renouveler.xls".equals(ov.getPathAttributAsString("model.file")));
		
		List<Value<?, XPOperand<?>>> l = ov.getPathAttributAsArray("model.sheets");
		
		assertTrue(l.size() > 0);
		
		assertTrue("Automates".equals(l.get(0).asObjectValue().getAttributAsString(Computing.PRTY_NAME)));
		
		assertTrue("Forages".equals(l.get(1).asObjectValue().getAttributAsString(Computing.PRTY_NAME)));
		
		assertTrue(new Integer(2).equals(l.get(1).asObjectValue().getAttributAsInteger("index")));
	}
	
	public void testXalFileWithComments() throws ManagedException {
		XALParser parser = new XALParser();
		ObjectValue<XPOperand<?>> ov = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test-with-comments.xal").getResult();
		
		assertTrue("xlsx".equals(ov.getAttributAsString(Computing.PRTY_TYPE)));
		
		assertTrue("repo:default/equipement-a-renouveler.xls".equals(ov.getPathAttributAsString("model.file")));
		
		List<Value<?, XPOperand<?>>> l = ov.getPathAttributAsArray("model.sheets");
		
		assertTrue(l.size() > 0);
		
		assertFalse("Automates".equals(l.get(0).asObjectValue().getAttributAsString(Computing.PRTY_NAME)));
		
		assertTrue("Forages".equals(l.get(0).asObjectValue().getAttributAsString(Computing.PRTY_NAME)));
		
		assertTrue(new Integer(2).equals(l.get(0).asObjectValue().getAttributAsInteger("index")));
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
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test2.xal");
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity2", entityVC); //parser.object("./src/test/java/com/exa/lang/test2.xal", "entities.entity2", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue( null == ovEntity.getPathAttributAsString("nullValue"));
		
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property2")));
		
		assertTrue("afb".equals(ovEntity.getPathAttributAsString("cplx.property1")));
				
		assertTrue("afc".equals(ovEntity.getPathAttributAsString("cplx.property3")));
		
	}
	
	public void testXalInheritance2() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test2.xal");
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test2.xal");
		ObjectValue<XPOperand<?>> ov = computing.getResult();
		
		Map<String, ObjectValue<XPOperand<?>>> libOV = new HashMap<>();
		libOV.put(Computing.LIBN_DEFAULT, ov.getAttributAsObjectValue(Computing.LIBN_DEFAULT));
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity2 = computing.object(ov.getAttributByPathAsObjectValue("entities.entity2"), entityVC, libOV);
		
		assertTrue("a".equals(ovEntity2.getPathAttributAsString("property2")));
		
		assertTrue("afb".equals(ovEntity2.getPathAttributAsString("cplx.property1")));
		
		assertTrue("afc".equals(ovEntity2.getPathAttributAsString("cplx.property3")));
	}

	
	public void testXalInheritance5() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test2.xal");
		
		//XPEvaluator evaluator = new XPEvaluator();
		
		
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test3.xal");
		
		VariableContext entityVC = new MapVariableContext(computing.getXPEvaluator().getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity3.cplx", entityVC);  //parser.object("./src/test/java/com/exa/lang/test3.xal", "entities.entity3.cplx", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue("b".equals(ovEntity.getPathAttributAsString("property2")));
		
		assertTrue("okc".equals(ovEntity.getPathAttributAsString("property")));
	}
	
	public void testXalInheritance6() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test4.xal");
		
		//XPEvaluator evaluator = new XPEvaluator();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test4.xal");
		
		VariableContext entityVC = new MapVariableContext(computing.getXPEvaluator().getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity1", entityVC); //parser.object("./src/test/java/com/exa/lang/test4.xal", "entities.entity1", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue("2".equals(ovEntity.getPathAttributAsString("property")));
		
	}
	
	public void testXalInheritance7() throws ManagedException {
		XALParser parser = new XALParser();
		//ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test4.xal");
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test4.xal");
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity2", entityVC); //parser.object("./src/test/java/com/exa/lang/test4.xal", "entities.entity2", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		evaluator.addVariable("prm", String.class, "3");
		assertTrue("2".equals(ovEntity.getPathAttributAsString("property1")));
		
	}
	
	public void testXalInheritance8() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test2.ds.xal");
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		evaluator.getCurrentVariableContext().addVariable("start", String.class, "01/02/2016");
		evaluator.getCurrentVariableContext().addVariable("end", String.class, "17/08/2018");
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity2", entityVC); //parser.object("./src/test/java/com/exa/lang/test2.ds.xal", "entities.entity2", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		
		assertTrue("smart".equals(ovEntity.getPathAttributAsString("type")));
		
	}
	
	public void testXalInheritance9() throws ManagedException {
		XALParser parser = new XALParser();
		
		VIEvaluatorSetup evSetup = new VIEvaluatorSetup();
		evSetup.addVaraiable("start", String.class, "01/02/2016");
		evSetup.addVaraiable("end", String.class, "17/08/2018");
		
		Computing computing = parser.getComputeObjectFormFile("./src/test/java/com/exa/lang/private/stats-intervention.ds.xal", evSetup, (id, context) -> {
			if("rootOv".equals(id)) return "ObjectValue";
			//String p[] = context.split("[.]");
			
			return null;
		});

		
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
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object(ovEntities, "sdtt", vc, XALParser.getDefaultObjectLib(rootOV)); //parser.object(ovEntities, "sdtt", evaluator, vc, XALParser.getDefaultObjectLib(rootOV));		
		
		assertTrue("row-to-field".equals(ovEntity.getPathAttributAsString("dt.type")));
		
		assertTrue("int".equals(ovEntity.getPathAttributAsString("dt.fields.type")));
		
		assertTrue("sql".equals(ovEntity.getPathAttributAsString("dt.source.type")));
		
		System.out.println(ovEntity.getPathAttributAsString("dt.source.criteria"));
		
	}
	
	public void testXalInheritance10() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test11.xal");
		
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		VariableContext vc = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		Value<?, XPOperand<?>> vl = computing.value("entities.e2.sb1", vc);
		assertTrue("a".equals(vl.asObjectValue().getPathAttributAsString("p1")));
		assertTrue("b".equals(vl.asObjectValue().getPathAttributAsString("p2")));
		
		vl = computing.value("entities.e2.sb2", vc);
		assertTrue("b".equals(vl.asObjectValue().getPathAttributAsString("p")));
		
		vl = computing.value("entities.e2.sb1.p1", vc);
		assertTrue("a".equals(vl.getValue()));
		
		vl = computing.value("entities.e3", vc);
		assertTrue("a1".equals(vl.asObjectValue().getPathAttributAsString("cplx1")));
		assertTrue("a2".equals(vl.asObjectValue().getPathAttributAsString("cplx2")));
	}
	
	public void testComputer() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test-computer.xal");
		
		computing.calculateInit();
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		VariableContext vc = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.e3", vc); 
		
		assertTrue("aa".equals(ovEntity.getPathAttributAsString("cplxa")));
		assertTrue("ab".equals(ovEntity.getPathAttributAsString("cplxb")));
		
	}

	public void testXalRealCase() throws ManagedException {
		XALParser parser = new XALParser();
		
		VIEvaluatorSetup evSetup = new VIEvaluatorSetup();
		evSetup.addVaraiable("start", String.class, "01/02/2016");
		evSetup.addVaraiable("end", String.class, "17/08/2018");
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/private/stats-sollicitation.ds.xal", evSetup, (id, context) -> {
			if("rootOv".equals(id)) return "ObjectValue";
			
			return null;
		});

		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		ObjectValue<XPOperand<?>> rootOV = computing.getResult();

		
		evaluator.addVariable("rootOv", ObjectValue.class, rootOV);
		
		VariableContext vc = new MapVariableContext(evaluator.getCurrentVariableContext());
		evaluator.pushVariableContext(vc);
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.dras", vc); //parser.object(ovEntities, "sdtt", evaluator, vc, XALParser.getDefaultObjectLib(rootOV));		
		
		assertTrue("row-to-field".equals(ovEntity.getPathAttributAsString("ot.type")));
		
		assertTrue("int".equals(ovEntity.getPathAttributAsString("ot.fields.type")));
		
		assertTrue("sql".equals(ovEntity.getPathAttributAsString("ot.source.type")));
		
		System.out.println(ovEntity.getPathAttributAsString("ot.source.criteria"));
		
		assertTrue("021".equals(ovEntity.getPathAttributAsString("ot.fields.items.dtEmise021.departement")));
		
		assertTrue("022".equals(ovEntity.getPathAttributAsString("ot.fields.items.dtEmise022.departement")));
		
		assertTrue("024".equals(ovEntity.getPathAttributAsString("ot.fields.items.dtEmise024.departement")));
		
		ovEntity = computing.object("entities.dryop", vc);
		
		assertTrue("031".equals(ovEntity.getPathAttributAsString("ot.fields.items.dtEmise031.departement")));
		
	}

	
	public void testStatementIf() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test5.xal");
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		entityVC.assignContextVariable("gv", Boolean.TRUE);
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity1", entityVC); //parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity1", evaluator, entityVC); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue(new Integer(2).equals(ovEntity.getAttributAsInteger("property")));
		
		ovEntity = computing.object("entities.entity3", entityVC); //parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity3", evaluator, entityVC);
		assertTrue(new Integer(10).equals(ovEntity.getAttributAsInteger("property")));
		
		entityVC.addVariable("v", Boolean.class, Boolean.TRUE);
		ovEntity = computing.object("entities.entity4", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity4", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx.property")));
		
		
		entityVC.assignContextVariable("v", Boolean.FALSE);
		ovEntity = computing.object("entities.entity4", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity4", evaluator, entityVC);
		assertTrue("b".equals(ovEntity.getPathAttributAsString("cplx.property")));
		
		
		entityVC.assignContextVariable("v", Boolean.FALSE);
		ovEntity = computing.object("entities.entity5", entityVC); //parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity5", evaluator, entityVC);
		assertTrue(ovEntity.getAttribut("cplx") == null);
		
		
		entityVC.assignContextVariable("v", Boolean.TRUE);
		ovEntity = computing.object("entities.entity6", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity6", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getAttributAsString("property")));
		
		
		entityVC.assignContextVariable("v", Boolean.TRUE);
		ovEntity = computing.object("entities.entity7", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity7", evaluator, entityVC);
		assertTrue(new Integer(2).equals(ovEntity.getPathAttributAsInteger("cplx.property")));
	}
	
	public void testStatementIf2() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test5.xal");
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		entityVC.assignContextVariable("gv", Boolean.TRUE);
		entityVC.assignContextVariable("v", Boolean.TRUE);
		entityVC.assignContextVariable("gi", 1);
		
		ObjectValue<XPOperand<?>>  ovEntity = computing.object("entities.entity8", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity8", evaluator, entityVC);
		assertTrue("8a".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity9", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity9", evaluator, entityVC);
		assertTrue("9-1a".equals(ovEntity.getPathAttributAsString("property1")));
		assertTrue("9-2a".equals(ovEntity.getPathAttributAsString("property2")));
		
		
		ovEntity = computing.object("entities.entity10", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity10", evaluator, entityVC);
		assertTrue(new Integer(1).equals(ovEntity.getPathAttributAsInteger("property")));
		
		entityVC.assignContextVariable("gi", 0);
		ovEntity = computing.object("entities.entity10", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity10", evaluator, entityVC);
		assertTrue(new Integer(2).equals(ovEntity.getPathAttributAsInteger("property")));
		
		
		entityVC.assignContextVariable("gi", 1);
		ovEntity = computing.object("entities.entity11", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity11", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property")));
		
		
		entityVC.assignContextVariable("gi", 0);
		ovEntity = computing.object("entities.entity11", entityVC); //parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity11", evaluator, entityVC);
		assertTrue(ovEntity.getAttribut("property") == null);
		
		entityVC.assignContextVariable("gi", 1);
		ovEntity = computing.object("entities.entity12", entityVC); //parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity12", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property")));
		
		entityVC.assignContextVariable("gi", 0);
		ovEntity = computing.object("entities.entity12", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity12", evaluator, entityVC);
		assertTrue("b".equals(ovEntity.getPathAttributAsString("cplx.property")));
		
		
		ovEntity = computing.object("entities.entity13", entityVC);//parser.object("./src/test/java/com/exa/lang/test5.xal", "entities.entity13", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx.property")));
	}
	
	public void testDoubleFormuleProperties() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test9.xal");
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		entityVC.assignContextVariable("prm", "123456");
		ObjectValue<XPOperand<?>>  ovEntity = computing.object("entities.entity1", entityVC); //parser.object("./src/test/java/com/exa/lang/test9.xal", "entities.entity1", evaluator, entityVC);
		
		System.out.println(ovEntity.getAttributAsString("p1"));
		System.out.println(ovEntity.getAttributAsString("p2"));
	}
	
	public void testStatementFor() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test6.xal", new VIEvaluatorSetup(), (id, context) -> {
			if("rootOv".equals(id)) return "ObjectValue";
			//String p[] = context.split("[.]");
			
			return null;
		});
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		ObjectValue<XPOperand<?>> rootOV = computing.getResult();

		evaluator.addVariable("rootOv", ObjectValue.class, rootOV);
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity1", entityVC); //parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity1", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property1")));
		
		ovEntity = computing.object("entities.entity2", entityVC); //parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity2", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property1")));
		
		ovEntity = computing.object("entities.entity3", entityVC); //parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity3", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property1[0]")));
		
		ovEntity = computing.object("entities.entity4", entityVC);// parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity4", evaluator, entityVC);
		assertTrue("1".equals(ovEntity.getPathAttributAsString("cplx[0].property")));
		assertTrue("2".equals(ovEntity.getPathAttributAsString("cplx[1].property")));
		
		ovEntity = computing.object("entities.entity6", entityVC); //parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity6", evaluator, entityVC);
		assertTrue("1".equals(ovEntity.getPathAttributAsString("property1")));
		assertTrue("2".equals(ovEntity.getPathAttributAsString("property2")));
		
		ovEntity = computing.object("entities.entity7", entityVC); //parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity7", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx1.property")));
		
		ovEntity = computing.object("entities.entity8", entityVC);//parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity8", evaluator, entityVC);
		assertTrue("1".equals(ovEntity.getPathAttributAsString("cplx1.property")));
		assertTrue("2".equals(ovEntity.getPathAttributAsString("cplx2.property")));
		
		ovEntity = computing.object("entities.entity9", entityVC); //parser.object("./src/test/java/com/exa/lang/test6.xal", "entities.entity9", evaluator, entityVC);
		assertTrue("2".equals(ovEntity.getPathAttributAsString("cplx2.property")));
		assertTrue("1a".equals(ovEntity.getPathAttributAsString("cplx1.newProperty")));
		assertTrue("2a".equals(ovEntity.getPathAttributAsString("cplx2.newProperty")));
		
		ovEntity = computing.object("entities.entity10", entityVC);
		
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity12", entityVC);
		
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx1.property")));
		
		ovEntity = computing.object("entities.entity13", entityVC);
		
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx2.property")));
		
		ovEntity = computing.object("entities.entity14", entityVC);
		
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx141.property")));
		
		assertTrue("a".equals(ovEntity.getPathAttributAsString("cplx142.property")));
		
		ovEntity = computing.object("entities.entity16", entityVC);
		assertTrue(Boolean.TRUE.equals(ovEntity.getAttributAsBoolean("property1")));
		assertTrue(Boolean.TRUE.equals(ovEntity.getAttributAsBoolean("property2")));
		
		ovEntity = computing.object("entities.entity17", entityVC);
		
		assertTrue("property1".equals(ovEntity.getPathAttributAsString("array[0]._name")));
		
		
		ovEntity = computing.object("entities.entity18", entityVC);
		
		assertTrue("emis1".equals(ovEntity.getPathAttributAsString("array[0].group[0]._name")));
		
		ovEntity = computing.object("entities.entity19", entityVC);
		
		assertTrue("1".equals(ovEntity.getPathAttributAsString("array[0].property")));
		
		
		ovEntity = computing.object("entities.entity20", entityVC);
		
		assertTrue("1".equals(ovEntity.getPathAttributAsString("array[0]")));
		
		ovEntity = computing.object("entities.entity21", entityVC);
		assertTrue("property1".equals(ovEntity.getPathAttributAsString("array[0]._name")));
		
	}
	
	public void testStatementImport() throws ManagedException {
		
		FilesRepositories fr = new FilesRepositories();
		
		fr.addRepoPart("default", new OSFileRepoPart("./src/test/java/com/exa/lang"));
		
		XALParser parser = new XALParser(fr);
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test7.xal");
		
		XPEvaluator evaluator = new XPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity2", entityVC);//parser.object("./src/test/java/com/exa/lang/test7.xal", "entities.entity2", evaluator, entityVC);
		assertTrue("ac".equals(ovEntity.getPathAttributAsString("cplx.property3").toString()));
		
		
		ovEntity = computing.object("entities.entity3", entityVC);//parser.object("./src/test/java/com/exa/lang/test7.xal", "entities.entity3", evaluator, entityVC);
		assertTrue("bc".equals(ovEntity.getPathAttributAsString("cplx.property3").toString()));
		
		ovEntity = computing.object("entities.entity4", entityVC);//parser.object("./src/test/java/com/exa/lang/test7.xal", "entities.entity3", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property2").toString()));
	}
	
	public void testStatementName() throws ManagedException {
		XALParser parser = new XALParser();
		
		Computing computing = parser.getExecutedComputeObjectFormFile("./src/test/java/com/exa/lang/test8.xal");
		
		XPEvaluator evaluator = computing.getXPEvaluator();
		
		VariableContext entityVC = new MapVariableContext(evaluator.getCurrentVariableContext());
		entityVC.assignContextVariable("p", "param");
		ObjectValue<XPOperand<?>> ovEntity = computing.object("entities.entity1", entityVC);//parser.object("./src/test/java/com/exa/lang/test8.xal", "entities.entity1", evaluator, entityVC);
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity_param", entityVC);//parser.object("./src/test/java/com/exa/lang/test8.xal", "entities.entity_param", evaluator, entityVC);
		assertTrue("b".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity2", entityVC); //parser.object("./src/test/java/com/exa/lang/test8.xal", "entities.entity2", evaluator, entityVC);
		assertTrue("c".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity4", entityVC);//parser.object("./src/test/java/com/exa/lang/test8.xal", "entities.entity4", evaluator, entityVC);
		assertTrue("d".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity5", entityVC);//parser.object("./src/test/java/com/exa/lang/test8.xal", "entities.entity5", evaluator, entityVC);
		assertTrue("e".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity6", entityVC);//parser.object("./src/test/java/com/exa/lang/test8.xal", "entities.entity6", evaluator, entityVC);
		assertTrue("e".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity7", entityVC);//parser.object("./src/test/java/com/exa/lang/test8.xal", "entities.entity7", evaluator, entityVC);
		assertTrue("ea".equals(ovEntity.getPathAttributAsString("property")));
		
		ovEntity = computing.object("entities.entity8", entityVC);//parser.object("./src/test/java/com/exa/lang/test8.xal", "entities.entity8", evaluator, entityVC);
		assertTrue("0".equals(ovEntity.getPathAttributAsString("property0")));
		assertTrue("ea".equals(ovEntity.getPathAttributAsString("propertya")));
	}
	
}
