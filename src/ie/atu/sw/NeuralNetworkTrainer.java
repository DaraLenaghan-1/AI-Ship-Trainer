package ie.atu.sw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jhealy.aicme4j.NetworkBuilderFactory;
import jhealy.aicme4j.net.Activation;
import jhealy.aicme4j.net.Loss;

public class NeuralNetworkTrainer {

    public static void main(String[] args) throws Exception {
        new NeuralNetworkTrainer().startTraining();
    }

    public void startTraining() throws Exception {
        // Read input data and expected output from CSV files
        double[][] inputData = readFromCSV("path/to/your/inputData.csv"); // ../resources/game data.csv --21
        double[][] expectedOutput = readFromCSV("path/to/your/expectedOutput.csv"); // expected csv

        // Initialise and configure the neural network
        var neuralNet = NetworkBuilderFactory.getInstance()
                .newNetworkBuilder()
                .inputLayer("Input", inputData[0].length) // Set the input layer size to match the feature count
                .hiddenLayer("Hidden1", Activation.RELU, 10) // Example: one hidden layer with 10 neurons
                .outputLayer("Output", Activation.SOFTMAX, expectedOutput[0].length) // Set the output layer size
                .lossFunction(Loss.CROSS_ENTROPY) // Use cross-entropy loss for classification tasks
                .learningRate(0.01) // Example learning rate
                .build();

        // Train the neural network
        neuralNet.train(inputData, expectedOutput, 1000); // Train for 1000 epochs, for example

        // Save the trained model to disk
        neuralNet.save("path/to/your/trainedModel.data");

        System.out.println("Training completed and model saved.");
    }

    // Utility method to read data from CSV file
    private double[][] readFromCSV(String filePath) throws IOException {
        List<double[]> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                double[] record = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    record[i] = Double.parseDouble(values[i]);
                }
                records.add(record);
            }
        }
        return records.toArray(new double[0][]);
    }
}
