package ie.atu.sw;
import java.util.Random;

public class NeuralNetwork {
	    double[][] weightsHidden; // Weights between input and hidden layer
	    double[][] weightsOutput; // Weights between hidden and output layer
	    
	    // Include structures for storing activations and z-values
	    double[] activationsHidden;
	    double[] zHidden;
	    double[] activationsOutput;
	    double[] zOutput;

	    public NeuralNetwork(int inputSize, int hiddenSize, int outputSize) {
	    	weightsHidden = new double[hiddenSize][inputSize + 1]; // +1 for bias
	        weightsOutput = new double[outputSize][hiddenSize + 1]; // +1 for bias
	        
	        initialiseWeights();
	    }

	    private void initialiseWeights() {
	        Random rand = new Random();
	        // Initialise hidden layer weights
	        for (int i = 0; i < weightsHidden.length; i++) {
	            for (int j = 0; j < weightsHidden[i].length; j++) {
	                weightsHidden[i][j] = rand.nextDouble() - 0.5; // Random values between -0.5 and 0.5
	            }
	        }
	        // Initialise output layer weights
	        for (int i = 0; i < weightsOutput.length; i++) {
	            for (int j = 0; j < weightsOutput[i].length; j++) {
	                weightsOutput[i][j] = rand.nextDouble() - 0.5; // Random values between -0.5 and 0.5
	            }
	        }
	    }

	    private double relu(double x) {
	        return Math.max(0, x);
	    }

	    private double[] softmax(double[] logits) {
	        double maxLogit = Double.NEGATIVE_INFINITY;
	        for (double logit : logits) {
	            maxLogit = Math.max(maxLogit, logit);
	        }
	        
	        double sum = 0.0;
	        for (int i = 0; i < logits.length; i++) {
	            logits[i] = Math.exp(logits[i] - maxLogit); // Improve numerical stability
	            sum += logits[i];
	        }

	        for (int i = 0; i < logits.length; i++) {
	            logits[i] /= sum;
	        }

	        return logits;
	    }

	    public double[] forward(double[] inputs) {
	        double[] hiddenOutputs = new double[weightsHidden.length];
	        // Compute activations for hidden layer
	        for (int i = 0; i < weightsHidden.length; i++) {
	            double sum = 0;
	            for (int j = 0; j < inputs.length; j++) {
	                sum += inputs[j] * weightsHidden[i][j];
	            }
	            // Bias for hidden layer
	            sum += 1 * weightsHidden[i][inputs.length]; // Assuming the bias input is always 1
	            hiddenOutputs[i] = relu(sum);
	        }
	        
	        double[] logits = new double[weightsOutput.length];
	        // Compute output layer activations
	        for (int i = 0; i < weightsOutput.length; i++) {
	            double sum = 0;
	            for (int j = 0; j < hiddenOutputs.length; j++) {
	                sum += hiddenOutputs[j] * weightsOutput[i][j];
	            }
	            // Bias for output layer
	            sum += 1 * weightsOutput[i][hiddenOutputs.length]; // Assuming the bias input is always 1
	            logits[i] = sum; 
	        }

	        // Apply softmax to convert logits to probabilities
	        double[] outputs = softmax(logits);
	        return outputs;
	    }

	    public static void main(String[] args) {
	        NeuralNetwork nn = new NeuralNetwork(4, 5, 3);
	        double[] inputs = {0.5, 0.1, 0.2, 1.0}; 
	        double[] outputs = nn.forward(inputs);
	        System.out.println("Network output:");
	        for (double output : outputs) {
	            System.out.println(output);
	        }
	    }
	    
	    public double calculateMSE(double[] outputs, double[] expectedOutputs) {
	        double sum = 0;
	        for (int i = 0; i < outputs.length; i++) {
	            sum += Math.pow(outputs[i] - expectedOutputs[i], 2);
	        }
	        return sum / outputs.length;
	    }
	    
	    public void updateWeights() {
	        Random rand = new Random();
	        // Simplified update rule for demonstration purposes
	        for (int i = 0; i < weightsHidden.length; i++) {
	            for (int j = 0; j < weightsHidden[i].length; j++) {
	                weightsHidden[i][j] += 0.01 * (rand.nextDouble() - 0.5); // Simulate weight adjustment
	            }
	        }
	        for (int i = 0; i < weightsOutput.length; i++) {
	            for (int j = 0; j < weightsOutput[i].length; j++) {
	                weightsOutput[i][j] += 0.01 * (rand.nextDouble() - 0.5); // Simulate weight adjustment
	            }
	        }
	    }

	    public double crossEntropyLoss(double[] predicted, int actual) {
	        double loss = -Math.log(predicted[actual] + 1e-15); // Adding epsilon to avoid log(0)
	        return loss;
	    }


	}