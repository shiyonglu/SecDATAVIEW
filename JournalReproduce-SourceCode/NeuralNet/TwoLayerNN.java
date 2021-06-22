import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dataview.models.*;

public class TwoLayerNN extends Task{
	/*
	 * The constructor will decide how many inputports and how many outputports and the detailed information of each port.
	 */
	public TwoLayerNN()
	{
		super("TwoLayerNN", "This is a task that evaluates 2 layer NN. It has one inputport and one outputport.");
		ins = new InputPort[1];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_int, "This is the first number");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");	
		
	}
	
	public void run() 
	{
		
		// step 1: read from the input ports
		int numberOfNodes = (int)ins[0].read1();
		//int numberOfNodes =200;
		double[][] X = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
        double[][] Y = {{0}, {1}, {1}, {0}};

        int m = 4;
        int nodes = numberOfNodes;

        X = np.T(X);
        Y = np.T(Y);

        long start = System.currentTimeMillis();
        double[][] W1 = np.random(nodes, 2);
        double[][] b1 = new double[nodes][m];

        double[][] W2 = np.random(1, nodes);
        double[][] b2 = new double[1][m];

        double cost = 0;
        for (int i = 0; i < 350000; i++) {
            // Foward Prop
            // LAYER 1
            double[][] Z1 = np.add(np.dot(W1, X), b1);
            double[][] A1 = np.sigmoid(Z1);

            //LAYER 2
            double[][] Z2 = np.add(np.dot(W2, A1), b2);
            double[][] A2 = np.sigmoid(Z2);

            cost = np.cross_entropy(m, Y, A2);
            //costs.getData().add(new XYChart.Data(i, cost));
         
            // Back Prop
            //LAYER 2
            double[][] dZ2 = np.subtract(A2, Y);
            double[][] dW2 = np.divide(np.dot(dZ2, np.T(A1)), m);
            double[][] db2 = np.divide(dZ2, m);

            //LAYER 1
            double[][] dZ1 = np.multiply(np.dot(np.T(W2), dZ2), np.subtract(1.0, np.power(A1, 2)));
            double[][] dW1 = np.divide(np.dot(dZ1, np.T(X)), m);
            double[][] db1 = np.divide(dZ1, m);

            // G.D
            W1 = np.subtract(W1, np.multiply(0.01, dW1));
            b1 = np.subtract(b1, np.multiply(0.01, db1));

            W2 = np.subtract(W2, np.multiply(0.01, dW2));
            b2 = np.subtract(b2, np.multiply(0.01, db2));

         /*   if (i % 10000 == 0) {
                System.out.println("==============");
                System.out.print("Cost = " + cost);
                System.out.print("Predictions = " + Arrays.deepToString(A2));
           } */
        }
        long end = System.currentTimeMillis();
        System.out.println("\nTotal execution : " + (end - start) + " ms");
		
		
		// step 3: write to the output port
		outs[0].write1("Cost = " + cost);
//		172.30.17.203
//		FileTransfer.send("/home/ubuntu/output0.txt", "/home/ubuntu/", "diagnosisResult.txt", "172.30.18.183");
//		FileTransfer.send("/home/ubuntu/output0.txt", "/home/ishtiaq/", "diagnosisResult.txt", "172.30.17.203", "ishtiaq", "123", 22);
		
	}
	
}
