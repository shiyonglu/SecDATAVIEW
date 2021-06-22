import dataview.models.Task;
import dataview.models.Workflow;

public class NeuralNetWorkflow extends Workflow {

	public NeuralNetWorkflow() {
		super("NeuralNetWorkflow", " A 2 layer neural net");
	}
	public void design()
	{

        // create and add all the tasks
		
		Task T1 = addTask("TwoLayerNN");
		
		
		// add edges
		addEdge("originalInput.enc", T1, 0);
	    addEdge(T1, 0, "output0.enc");	    
	}
		
}
