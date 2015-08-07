/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.graph.test.library;

import java.util.Arrays;
import java.util.List;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.example.utils.PageRankData;
import org.apache.flink.graph.library.GSAPageRank;
import org.apache.flink.graph.library.PageRank;
import org.apache.flink.test.util.MultipleProgramsTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PageRankITCase extends MultipleProgramsTestBase {

	public PageRankITCase(TestExecutionMode mode){
		super(mode);
	}

	private String expectedResult;

	@Test
	public void testPageRankWithThreeIterations() throws Exception {
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		Graph<Long, Double, Double> inputGraph = Graph.fromDataSet(
				PageRankData.getDefaultEdgeDataSet(env), new InitMapper(), env);
		
		DataSet<Tuple2<Long, Long>> vertexOutDegrees = inputGraph.outDegrees();

		Graph<Long, Double, Double> networkWithWeights = inputGraph
				.joinWithEdgesOnSource(vertexOutDegrees, new InitWeightsMapper());

        List<Vertex<Long, Double>> result = networkWithWeights.run(new PageRank<Long>(0.85, 3))
        		.getVertices().collect();
        
        compareWithDelta(result, expectedResult, 0.01);
	}

	@Test
	public void testGSAPageRankWithThreeIterations() throws Exception {
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		Graph<Long, Double, Double> inputGraph = Graph.fromDataSet(
				PageRankData.getDefaultEdgeDataSet(env), new InitMapper(), env);
		
		DataSet<Tuple2<Long, Long>> vertexOutDegrees = inputGraph.outDegrees();

		Graph<Long, Double, Double> networkWithWeights = inputGraph
				.joinWithEdgesOnSource(vertexOutDegrees, new InitWeightsMapper());

        List<Vertex<Long, Double>> result = networkWithWeights.run(new GSAPageRank<Long>(0.85, 3))
        		.getVertices().collect();
        
        compareWithDelta(result, expectedResult, 0.01);
	}

	private void compareWithDelta(List<Vertex<Long, Double>> result,
			String expectedResult, double delta) {

		String resultString = "";
        for (Vertex<Long, Double> v : result) {
        	resultString += v.f0.toString() + "," + v.f1.toString() +"\n";
        }
        
		expectedResult = PageRankData.RANKS_AFTER_3_ITERATIONS;
		String[] expected = expectedResult.isEmpty() ? new String[0] : expectedResult.split("\n");

		String[] resultArray = resultString.isEmpty() ? new String[0] : resultString.split("\n");

		Arrays.sort(expected);
        Arrays.sort(resultArray);

		for (int i = 0; i < expected.length; i++) {
			String[] expectedFields = expected[i].split(",");
			String[] resultFields = resultArray[i].split(",");

			double expectedPayLoad = Double.parseDouble(expectedFields[1]);
			double resultPayLoad = Double.parseDouble(resultFields[1]);

			Assert.assertTrue("Values differ by more than the permissible delta",
					Math.abs(expectedPayLoad - resultPayLoad) < delta);
		}
	}

	@SuppressWarnings("serial")
	private static final class InitMapper implements MapFunction<Long, Double> {
		public Double map(Long value) {
			return 1.0;
		}
	}

	@SuppressWarnings("serial")
	private static final class InitWeightsMapper implements MapFunction<Tuple2<Double, Long>, Double> {
		public Double map(Tuple2<Double, Long> value) {
			return value.f0 / value.f1;
		}
	}
}
