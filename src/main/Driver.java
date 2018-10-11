import java.util.Collections;
import java.util.List;


import org.junit.After;
import org.junit.Before;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.kernel.logging.BufferingLogger;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.graphdb.Result;

import tests.utils.TestUtils;
import visitors.WiggleVisitor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;


public class Driver {


    private static GraphDatabaseService graphDb;

    public static void main(String args[]) throws Exception {
        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().setConfig(GraphDatabaseSettings.node_keys_indexable, "nodeType").
                setConfig(GraphDatabaseSettings.relationship_keys_indexable, "typeKind").
                setConfig(GraphDatabaseSettings.node_auto_indexing, "true").
                setConfig(GraphDatabaseSettings.relationship_auto_indexing, "true").newGraphDatabase();


        String qr_wildcard = "START n=node:node_auto_index(nodeType ='JCWildcard') RETURN n.typeBoundKind , count (*);";
        String qr_coundMethod = "START m=node:node_auto_index(nodeType='JCMethodDecl') " +
				"MATCH c-[:DECLARES_METHOD]->m-[:CALLS]->m" +
				" RETURN c,m;";
        String qr_subtype = "START m=node:node_auto_index(nodeType='ClassType')\n" +
				"MATCH path=n-[r:IS_SUBTYPE_EXTENDS*]->m\n" +
				"WHERE m.fullyQualifiedName='java.lang.Exception'\n" +
				"RETURN path;";
        String qr_covariant = "START n=node:node_auto_index(nodeType='JCAssign')\n" +
				 "MATCH lhs<-[:ASSIGNMENT_LHS]-n-[:ASSIGNMENT_RHS]->rhs\n" +
				 "WHERE has(lhs.typeKind) AND lhs.typeKind='ARRAY' AND\n" +
				 "      has(rhs.typeKind) AND rhs.typeKind='ARRAY' AND\n" +
				 "      lhs.actualType <> rhs.actualType\n" +
				"RETURN n;";
        String qr_fetchOverloaded = "START n=node:node_auto_index(nodeType ='JCMethodDecl') " +
				"MATCH c-[:DECLARES_METHOD]->n " +
				 "WITH n.name as method, count(*) as overloadedCount " +
				 "WHERE NOT(n.name = '<init>') AND overloadedCount > 1 " +
				 "RETURN method, overloadedCount;" ;

		String all = "START n=node(*) MATCH (n)-[r]->(m) RETURN n,r,m;";

        String qr_hunvar = "START n=node:node_auto_index(nodeType='JCIdent') " +
				"WHERE n.typeKind='DECLARED' AND has(n.actualType) AND n.name =~ '[a-z].*'" +
				"SET n.name = n.name + head(split(last(split(n.actualType, '.')), '<')) " +
				"RETURN n.name, n.actualType, n.lineNumber\n";

        String qr_hundecl = "START n=node:node_auto_index(nodeType='JCVariableDecl') " +
				"WHERE n.typeKind='DECLARED' AND has(n.actualType) AND n.name =~ '[a-z].*'" +
				"SET n.name = n.name + head(split(last(split(n.actualType, '.')), '<')) " +
				"RETURN n.name, n.actualType, n.lineNumber;";

		String arrs = "START n=node:node_auto_index(nodeType='JCIdent')" +
				"WHERE n.name='myNumber'" +
				"RETURN n;";

        String qr_hungarian = qr_hunvar + " UNION ALL \n " + qr_hundecl;

		String src = "import java.util.Collection;\n" +
				"import java.util.ArrayList;\n" +
				"class D extends java.lang.Exception {}\n" +
				"class E extends D {\n" +
				"void thing(){\n" +
				"Integer[] myInts = {1,2,3,4};\n" +
				"Number[] myNumber = null;\n" +
				"myNumber = myInts;\n" +
				"myNumber = myInts;\n" +
				"Integer x = 1;\n" +
				"x = 2;\n" +
				"}\n" +
				"}\n" +
                "class A{\n"
				   + "static class B extends A{}\n"
				   + "static class C extends B{\n" +
				"void pa() {}\n" +
				"void pa(Collection<?> c) {\n" +
				"	pa(c);\n" +
				"    for (Object e : c) {\n" +
				"        System.out.println(e);\n" +
				"    }}\n" +
				"void print(Collection<?> c) {\n" +
				"	print(c);\n" +
				"    for (Object e : c) {\n" +
				"        System.out.println(e);\n" +
				"    }}\n" +
                "void printCollection(Collection<?> c) {\n" +
                "    for (Object e : c) {\n" +
                "        System.out.println(e);\n" +
                "    }\n" +
                "}}\n"
				   + "}\n";

		JavacTaskImpl task = TestUtils.getTask(src);

		List<? extends CompilationUnitTree> parse = (List<? extends CompilationUnitTree>) task.parse();
		task.analyze(); // attribute with symbols?

		CompilationUnitTree u = parse.get(0);

		WiggleVisitor v = new WiggleVisitor(task, graphDb, Collections.singletonMap("projectName", "Test_Extends"));
		v.scan(u, null);

//		ExecutionEngine engine = new ExecutionEngine(graphDb, new BufferingLogger());


        Result result = graphDb.execute(arrs);
//		ExecutionResult result = engine.execute("start n=node(*) MATCH m-[r]->n RETURN m,r,n");
		System.out.println(result.resultAsString());


        graphDb.shutdown();

    }

}
