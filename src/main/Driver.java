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


		String src = "import java.util.Collection;\n" +
                "class A{\n"
				   + "static class B extends A{}\n"
				   + "static class C extends B{" +
                "void printCollection(Collection<?> c) {\n" +
                "    for (Object e : c) {\n" +
                "        System.out.println(e);\n" +
                "    }\n" +
                "}}\n"
				   + "}";

		JavacTaskImpl task = TestUtils.getTask(src);

		List<? extends CompilationUnitTree> parse = (List<? extends CompilationUnitTree>) task.parse();
		task.analyze(); // attribute with symbols?

		CompilationUnitTree u = parse.get(0);

		WiggleVisitor v = new WiggleVisitor(task, graphDb, Collections.singletonMap("projectName", "Test_Extends"));
		v.scan(u, null);

//		ExecutionEngine engine = new ExecutionEngine(graphDb, new BufferingLogger());


        Result result = graphDb.execute("START n=node:node_auto_index(nodeType ='JCWildcard') RETURN n.typeBoundKind , count (*);");
//		ExecutionResult result = engine.execute("start n=node(*) MATCH m-[r]->n RETURN m,r,n");
		System.out.println(result.resultAsString());


        graphDb.shutdown();

    }

}
