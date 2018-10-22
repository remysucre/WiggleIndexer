import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;


import org.junit.After;
import org.junit.Before;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.kernel.logging.BufferingLogger;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.graphdb.Result;

import tests.utils.TestUtils;
import visitors.WiggleVisitor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;


public class Driver {

	private static String getcode(String fn, Long pos, Long size) throws IOException {
		byte[] bytesRead = new byte[Math.toIntExact(size)];

		RandomAccessFile file = new RandomAccessFile(fn.substring(5), "r");
		file.seek(pos);
		file.read(bytesRead);
		file.close();
		return new String (bytesRead);
	}

	private static String hungarize(String name, String type) {
		return name + type.replaceAll("[^a-zA-Z ]", "").toLowerCase();
	}

    private static GraphDatabaseService graphDb;

    public static void main(String args[]) throws Exception {
    	graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File("/Users/remywang/WiggleIndexer/neo4j/data/wiggle.db")).
				setConfig(GraphDatabaseSettings.node_keys_indexable, "nodeType").
				setConfig(GraphDatabaseSettings.relationship_keys_indexable, "typeKind").
				setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").
				setConfig(GraphDatabaseSettings.node_auto_indexing, "true").
				setConfig(GraphDatabaseSettings.relationship_auto_indexing, "true").newGraphDatabase();
        //graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().setConfig(GraphDatabaseSettings.node_keys_indexable, "nodeType").
		//		setConfig(GraphDatabaseSettings.relationship_keys_indexable, "typeKind").
        //        setConfig(GraphDatabaseSettings.node_auto_indexing, "true").
        //        setConfig(GraphDatabaseSettings.relationship_auto_indexing, "true").newGraphDatabase();


        /*
        for (Iterator<Invariant> i = invs.iterator(); i.hasNext(); ) {
            Invariant mine = i.next();
            if ((mine.getClass() == inv.getClass()) && mine.isSameFormula(inv))
            return (mine);
        } * */

        String goodloops = "START n=node:node_auto_index(nodeType ='JCForLoop')  MATCH p-[*]->n " +
                "WHERE p.nodeType='JCCompilationUnit' " +
                "RETURN n.size, n.position, p.fileName;";

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

		String file_name = "START n=node:node_auto_index(nodeType ='JCCompilationUnit') RETURN n.fileName ";

        String qr_fetchOverloaded = "START n=node:node_auto_index(nodeType ='JCMethodDecl') " +
				"MATCH c-[:DECLARES_METHOD]->n " +
				 "WITH n.name as method, count(*) as overloadedCount " +
				 "WHERE NOT(n.name = '<init>') AND overloadedCount > 1 " +
				 "RETURN method, overloadedCount;" ;

		String all = "START n=node(*) MATCH (n)-[r]->(m) RETURN n,r,m;";


		// n - condition -> METHODINVOCATION_METHOD_SELECT -> hasNext - MEMBER_SELECT_EXPR -> i
        // n.name=i - init -> HAS_VARIABLEDECL_INIT -> JCMethodInvocation -METHODINVOCATION_METHOD_SELECT> iterator
        String allloops = "START n=node:node_auto_index(nodeType ='JCForLoop') " +
                "MATCH " +
                "n-[:FORLOOP_INIT]->c ,\n" +
                "n-[:FORLOOP_STATEMENT]->b ,\n" +
                "c-[r]->x-[s]->cs-[f]->o \n" +
                "RETURN c.actualType, c.name , o.name, b;";

		String loops = "START n=node:node_auto_index(nodeType ='JCForLoop')  MATCH p-[*]->n " +
				"WHERE p.nodeType='JCCompilationUnit' " +
				"RETURN n.size, n.position, p.fileName;";

        String qr_hunvar = "START n=node:node_auto_index(nodeType='JCIdent') " +
				"WHERE exists(n.actualType) AND n.actualType <> '()void' AND n.name =~ '[a-z].*'" +
				// "SET n.name = n.name + n.actualType " +
				// "RETURN n.name, n.actualType, n.lineNumber;";
				"RETURN ID(n), n.name, n.actualType, n.lineNumber \n";

        String qr_hundecl = "START n=node:node_auto_index(nodeType='JCVariableDecl') " +
				"WHERE exists(n.actualType) AND n.actualType <> '()void' AND n.name =~ '[a-z].*'" +
				// "SET n.name = n.name + n.actualType " +
				// "RETURN n.name, n.actualType, n.lineNumber;";
				"RETURN ID(n), n.name, n.actualType, n.lineNumber ;";

		String arrs = "START n=node:node_auto_index(nodeType='JCIdent')" +
				"WHERE n.name='myNumber'" +
				"RETURN n;";

        String qr_hungarian = qr_hunvar + " UNION ALL \n " + qr_hundecl;

		String src;

		src = new String(Files.readAllBytes(Paths.get("samples/Test.java")));

		String src0 = "import java.util.Collection;\n" +
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

		// JavacTaskImpl task = TestUtils.getTask(src0);

		// List<? extends CompilationUnitTree> parse = (List<? extends CompilationUnitTree>) task.parse();
		// System.out.println("before analyze");
		// task.analyze(); // attribute with symbols?
		// System.out.println("after analyze");

		// CompilationUnitTree u = parse.get(0);

		// WiggleVisitor v = new WiggleVisitor(task, graphDb, Collections.singletonMap("projectName", "Test_Extends"));
		// v.scan(u, null);

//		ExecutionEngine engine = new ExecutionEngine(graphDb, new BufferingLogger());


        // Result result = graphDb.execute(qr_hungarian);
		// String words = instring.replaceAll("[^a-zA-Z ]", "").toLowerCase();
		Result result = graphDb.execute(allloops);
		System.out.println(result.resultAsString());
/*		while ( result.hasNext() )
		{
			Map<String, Object> row = result.next();
			String n = (String) row.get( "n.name" );
			String t = (String) row.get( "n.actualType" );
			System.out.printf( "%s%n", hungarize(n, t));
		}*/
		/*while ( result.hasNext() )
		{
			Map<String, Object> row = result.next();
			Long size = (Long) row.get( "n.size" );
			Long pos = (Long) row.get( "n.position" );
			String fn = (String) row.get( "p.fileName" );
			System.out.printf( "%s%n", getcode(fn, pos, size));
		}*/
        graphDb.shutdown();

    }

}
