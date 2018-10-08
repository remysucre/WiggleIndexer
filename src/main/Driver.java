import java.util.Collections;
import java.util.List;


import org.junit.After;
import org.junit.Before;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.kernel.logging.BufferingLogger;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.graphdb.Result;

import tests.utils.TestUtils;
import visitors.WiggleVisitor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;


public class Driver {
    private static GraphDatabaseService graphDb;

    public static void main(String args[]) throws Exception {
        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();

		String src = "class A{\n"
				   + "static class B extends A{}\n"
				   + "static class C extends B{}\n"
				   + "}";

		JavacTaskImpl task = TestUtils.getTask(src);

		List<? extends CompilationUnitTree> parse = (List<? extends CompilationUnitTree>) task.parse();
		task.analyze(); // attribute with symbols?

		CompilationUnitTree u = parse.get(0);

		WiggleVisitor v = new WiggleVisitor(task, graphDb, Collections.singletonMap("projectName", "Test_Extends"));
		v.scan(u, null);

//		ExecutionEngine engine = new ExecutionEngine(graphDb, new BufferingLogger());


        Result result = graphDb.execute("start n=node(*) MATCH m-[r]->n RETURN m,r,n");
//		ExecutionResult result = engine.execute("start n=node(*) MATCH m-[r]->n RETURN m,r,n");
		System.out.println(result.resultAsString());


        graphDb.shutdown();

    }

}
