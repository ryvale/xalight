package com.exa.lang;

import java.util.List;

import com.exa.chars.EscapeCharMan;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.lang.parsing.Computing;
import com.exa.lang.parsing.Parser;
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
		Parser parser = new Parser();
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
		Parser parser = new Parser();
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
		Parser parser = new Parser();
		ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test2.xal");
		
		ObjectValue<XPOperand<?>> ovEntity = parser.parseFile("./src/test/java/com/exa/lang/test2.xal", "entities.entity2", new XPEvaluator()); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue("a".equals(ovEntity.getPathAttributAsString("property2")));
		
		assertTrue("afb".equals(ovEntity.getPathAttributAsString("cplx.property1")));
		
		//assertTrue("a".equals(ov.getPathAttributAsString("entities.entity2.cplx.property1")));
		
		//assertTrue("af".equals(ov.getPathAttributAsString("entities.entity2._call_params.prm")));
		
		assertTrue("afc".equals(ovEntity.getPathAttributAsString("cplx.property3")));
		
		//assertTrue(new Integer(10).equals(ov.getPathAttribut("entities.entity2.cplx.property0")));
		
		//ObjectValue<XPOperand<?>> ov1 = ov.getPathAttributAsObjecValue("entities.entity2");
		
		//assertTrue(new Integer(2).equals(ov1.getAttributAsInteger("property1")));
		
		
	}
	
	public void testXalInheritance2() throws ManagedException {
		Parser parser = new Parser();
		ObjectValue<XPOperand<?>> ov = parser.parseFile("./src/test/java/com/exa/lang/test2.xal");
		
		ObjectValue<XPOperand<?>> ovEntity = parser.parseFile("./src/test/java/com/exa/lang/test3.xal", "entities.entity3.cplx", new XPEvaluator()); //ov.getPathAttributAsObjecValue("entities.entity2");
		
		assertTrue("b".equals(ovEntity.getPathAttributAsString("property2")));
		
		assertTrue("okc".equals(ovEntity.getPathAttributAsString("property")));
	
		
		
	}

}
