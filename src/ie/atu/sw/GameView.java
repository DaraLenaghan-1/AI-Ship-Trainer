package ie.atu.sw;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.ThreadLocalRandom.current;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.Timer;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;

import jhealy.aicme4j.net.Aicme4jUtils;
import jhealy.aicme4j.net.NeuralNetwork;
import jhealy.aicme4j.net.Output;


public class GameView extends JPanel implements ActionListener{
	//Some constants
	private static final long serialVersionUID	= 1L;
	private static final int MODEL_WIDTH 		= 30;
	private static final int MODEL_HEIGHT 		= 20;
	private static final int SCALING_FACTOR 	= 30;
	
	private static final int MIN_TOP 			= 2;
	private static final int MIN_BOTTOM 		= 18;
	private static final int PLAYER_COLUMN 		= 15;
	private static final int TIMER_INTERVAL 	= 100;
	
	private static final byte ONE_SET 			=  1;
	private static final byte ZERO_SET 			=  0;

	/*
	 * The 30x20 game grid is implemented using a linked list of 
	 * 30 elements, where each element contains a byte[] of size 20. 
	 * 
	 * private int shipMovement = 0; -- move,sample,save
	
		public NeuralNetwork nn; - use to move ship -- automove 
	 */
	private LinkedList<byte[]> model = new LinkedList<>();

	//These two variables are used by the cavern generator. 
	private int prevTop = MIN_TOP;
	private int prevBot = MIN_BOTTOM;
	
	//Once the timer stops, the game is over
	private Timer timer;
	private long time;
	
	private int playerRow = 11;
	private int index = MODEL_WIDTH - 1; //Start generating at the end
	private Dimension dim;
	
	//Some fonts for the UI display
	private Font font = new Font ("Dialog", Font.BOLD, 50);
	private Font over = new Font ("Dialog", Font.BOLD, 100);

	//The player and a sprite for an exploding plane
	private Sprite sprite;
	private Sprite dyingSprite;
	
	private int lastAction = 0; // -1 for up, 1 for down, 0 for stay
	
	private boolean auto;
	
	  private int shipMovement = 0; // Track ship movement direction
	  private NeuralNetwork nn; // Neural network for decision making

	public GameView(boolean auto) throws Exception{
		this.auto = true; //Use the autopilot - was set to auto
		setBackground(Color.LIGHT_GRAY);
		setDoubleBuffered(true);
		
		//Creates a viewing area of 900 x 600 pixels
		dim = new Dimension(MODEL_WIDTH * SCALING_FACTOR, MODEL_HEIGHT * SCALING_FACTOR);
    	super.setPreferredSize(dim);
    	super.setMinimumSize(dim);
    	super.setMaximumSize(dim);
		
    	initModel();
    	
    	initializeCSVWithHeaders(); // Initialise CSV with headers
    	
		timer = new Timer(TIMER_INTERVAL, this); //Timer calls actionPerformed() every second
		timer.start();
		
		try {
            // Load the neural network model
            String modelPath = "../resources/trainedShipModel.data"; // -- NEEDS TO CHANGE
            nn = Aicme4jUtils.load(modelPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	//Build our game grid
	private void initModel() {
		for (int i = 0; i < MODEL_WIDTH; i++) {
			model.add(new byte[MODEL_HEIGHT]);
		}
	}
	
	public void setSprite(Sprite s) {
		this.sprite = s;
	}
	
	public void setDyingSprite(Sprite s) {
		this.dyingSprite = s;
	}
	
	//Called every second by actionPerformed(). Paint methods are usually ugly.
	public void paintComponent(Graphics g) {
        super.paintComponent(g);
        var g2 = (Graphics2D)g;
        
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, dim.width, dim.height);
        
        int x1 = 0, y1 = 0;
        for (int x = 0; x < MODEL_WIDTH; x++) {
        	for (int y = 0; y < MODEL_HEIGHT; y++){  
    			x1 = x * SCALING_FACTOR;
        		y1 = y * SCALING_FACTOR;

        		if (model.get(x)[y] != 0) {
            		if (y == playerRow && x == PLAYER_COLUMN) {
            			timer.stop(); //Crash...
            		}
            		g2.setColor(Color.BLACK);
            		g2.fillRect(x1, y1, SCALING_FACTOR, SCALING_FACTOR);
        		}
        		
        		if (x == PLAYER_COLUMN && y == playerRow) {
        			if (timer.isRunning()) {
            			g2.drawImage(sprite.getNext(), x1, y1, null);
        			}else {
            			g2.drawImage(dyingSprite.getNext(), x1, y1, null);
        			}
        			
        		}
        	}
        }
        
        /*
         * Not pretty, but good enough for this project... The compiler will
         * tidy up and optimise all of the arithmetics with constants below.
         */
        g2.setFont(font);
        g2.setColor(Color.RED);
        g2.fillRect(1 * SCALING_FACTOR, 15 * SCALING_FACTOR, 400, 3 * SCALING_FACTOR);
        g2.setColor(Color.WHITE);
        g2.drawString("Time: " + (int)(time * (TIMER_INTERVAL/1000.0d)) + "s", 1 * SCALING_FACTOR + 10, (15 * SCALING_FACTOR) + (2 * SCALING_FACTOR));
        
        if (!timer.isRunning()) {
			g2.setFont(over);
			g2.setColor(Color.RED);
			g2.drawString("Game Over!", MODEL_WIDTH / 5 * SCALING_FACTOR, MODEL_HEIGHT / 2* SCALING_FACTOR);
        }
	}

	//Move the plane up or down
	public void move(int step) {
		playerRow += step; // Update the plane's vertical position.
	    lastAction = step; // Record the last action taken.
	}
	
	
	/*
	 * ----------
	 * AUTOPILOT!
	 * ----------
	 * The following implementation randomly picks a -1, 0, 1 to control the plane. You 
	 * should plug the trained neural network in here. This method is called by the timer
	 * every TIMER_INTERVAL units of time from actionPerformed(). There are other ways of
	 * wiring your neural network into the application, but this way might be the easiest. 
	 *  
	 */
	private void autoMove() {
	    try {
	        double[] gameState = sample();
	        double step = nn.process(gameState, Output.NUMERIC_ROUNDED);
	        move((int) Math.round(step)); // Cast to int as move accepts an int
	        shipMovement = (int) Math.round(step); // Update shipMovement with the action taken
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}


	
	//Called every second by the timer 
	public void actionPerformed(ActionEvent e) {
		time++; //Update our timer
		this.repaint(); //Repaint the cavern
		
		//Update the next index to generate
		index++;
		index = (index == MODEL_WIDTH) ? 0 : index;
		
		generateNext(); //Generate the next part of the cave
		if (auto) autoMove();
		
		/*
		 * Use something like the following to extract training data.
		 * It might be a good idea to submit the double[] returned by
		 * the sample() method to an executor and then write it out 
		 * to file. You'll need to label the data too and perhaps add
		 * some more features... Finally, you do not have to sample 
		 * the data every TIMER_INTERVAL units of time. Use some modular
		 * arithmetic as shown below. Alternatively, add a key stroke 
		 * to fire an event that starts the sampling.
		 */
		// Here, we capture the game state and the last action taken
	    if (time % 10 == 0) { // Log at specified intervals, adjust as necessary
	        double[] gameState = sample(); // Capture the current game state
	        int actionTaken = lastAction; // Capture the last action taken

	        logGameStateAndAction(gameState, actionTaken); // Log the game state and action
		}
	}
	
	
	/*
	 * Generate the next layer of the cavern. Use the linked list to
	 * move the current head element to the tail and then randomly
	 * decide whether to increase or decrease the cavern. 
	 */
	private void generateNext() {
		var next = model.pollFirst(); 
		model.addLast(next); //Move the head to the tail
		Arrays.fill(next, ONE_SET); //Fill everything in
		
		
		//Flip a coin to determine if we could grow or shrink the cave
		var minspace = 4; //Smaller values will create a cave with smaller spaces
		prevTop += current().nextBoolean() ? 1 : -1; 
		prevBot += current().nextBoolean() ? 1 : -1;
		prevTop = max(MIN_TOP, min(prevTop, prevBot - minspace)); 		
		prevBot = min(MIN_BOTTOM, max(prevBot, prevTop + minspace));

		//Fill in the array with the carved area
		Arrays.fill(next, prevTop, prevBot, ZERO_SET);
	}
	
	
	/*
	 * Use this method to get a snapshot of the 30x20 matrix of values
	 * that make up the game grid. The grid is flatmapped into a single
	 * dimension double array... (somewhat) ready to be used by a neural 
	 * net. You can experiment around with how much of this you actually
	 * will need. The plane is always somehere in column PLAYER_COLUMN
	 * and you probably do not need any of the columns behind this. You
	 * can consider all of the columns ahead of PLAYER_COLUMN as your
	 * horizon and this value can be reduced to save space and time if
	 * needed, e.g. just look 1, 2 or 3 columns ahead. 
	 * 
	 * You may also want to track the last player movement, i.e.
	 * up, down or no change. Depending on how you design your neural
	 * network, you may also want to label the data as either okay or 
	 * dead. Alternatively, the label might be the movement (up, down
	 * or straight). 
	 *  
	 */
	public double[] sample() {
		// Initial capacity: MODEL_WIDTH * MODEL_HEIGHT for the grid, +2 for playerRow and obstacle distances
	    var vector = new double[MODEL_WIDTH * MODEL_HEIGHT + 3]; //was + 1
	    var index = 0;
		
		for (byte[] bm : model) {
			for (byte b : bm) {
	            vector[index++] = b;
			}
		}
		 // Append playerRow
	    vector[index++] = playerRow;
	    
	    // Calculate and append distances to nearest obstacles above and below the plane
	    vector[index++] = calculateDistanceToObstacleAbove();
	    vector[index] = calculateDistanceToObstacleBelow();
	    
	    return vector;
	}
	
	private int calculateDistanceToObstacleAbove() {
	    int distance = 0;
	    // Start scanning from the row above the plane's current position.
	    for (int row = playerRow - 1; row >= 0; row--) {
	        if (model.get(PLAYER_COLUMN)[row] == ONE_SET) {
	            // Found the nearest obstacle above the plane.
	            return distance;
	        }
	        distance++;
	    }
	    // If no obstacle is found above, the distance is the plane's row position.
	    return distance;
	}

	private int calculateDistanceToObstacleBelow() {
	    int distance = 0;
	    // Start scanning from the row below the plane's current position.
	    for (int row = playerRow + 1; row < MODEL_HEIGHT; row++) {
	        if (model.get(PLAYER_COLUMN)[row] == ONE_SET) {
	            // Found the nearest obstacle below the plane.
	            return distance;
	        }
	        distance++;
	    }
	    // If no obstacle is found below, the distance is the difference between the bottom and the plane's row.
	    return distance;
	}

	
	/*
	 * Resets and restarts the game when the "S" key is pressed
	 */
	public void reset() {
		model.stream() 		//Zero out the grid
		     .forEach(n -> Arrays.fill(n, 0, n.length, ZERO_SET));
		playerRow = 11;		//Centre the plane
		time = 0; 			//Reset the clock
		timer.restart();	//Start the animation
	}
	
	private void initializeCSVWithHeaders() {
        String filePath = "C:\\Users\\daral\\OneDrive\\Documents\\game_data.csv";
        File file = new File(filePath);

        // Check if the file does not exist or if it's empty.
        if (!file.exists() || file.length() == 0) {
            // Try-with-resources will ensure the PrintWriter is closed after use, even if an exception occurs.
            try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) { // Set to false to overwrite if needed.
                // Write column headers for the CSV file.
                StringBuilder sb = new StringBuilder();
                // Construct header names for each grid cell.
                for (int i = 0; i < MODEL_WIDTH * MODEL_HEIGHT; i++) {
                    sb.append("Cell").append(i + 1).append(",");
                }
                // Add headers for playerRow, obstacle distances, and the last action taken.
                sb.append("PlayerRow,ObstacleAbove,ObstacleBelow,ActionTaken\n");
                // Write the constructed header string to the file.
                pw.write(sb.toString());
            } catch (IOException e) {
                // If an IOException occurs, print the stack trace to help with debugging.
                e.printStackTrace(System.err);
            }
        }
    }

	private void logGameStateAndAction(double[] gameState, int actionTaken) {
	    // Define the path to your CSV file
	    String filePath = "C:\\Users\\daral\\OneDrive\\Documents\\game_data.csv";
	    System.out.println("Attempting to write to file: " + filePath); // Existing log statement

	    try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
	        System.out.println("FileWriter opened successfully."); // Additional log statement

	        // Log the game state features
	        for (double feature : gameState) {
	            pw.print(feature + ",");
	        }
	        
	        // Log the action taken
	        pw.println(actionTaken);
	        System.out.println("Data written successfully."); // Additional log statement
	    } catch (IOException e) {
	        System.err.println("An error occurred while writing to the file."); // Log to standard error
	        e.printStackTrace(System.err); // Print stack trace to standard error
	    }
	}

	public void sampleAndSave(String dataFilename, String expectedFilename) {
	    double[] gameState = sample(); // Use your existing sample method to get the current game state.
	    int actionTaken = lastAction; // Last action taken by the autopilot.

	    try (FileWriter dataWriter = new FileWriter(dataFilename, true);
	         FileWriter expectedWriter = new FileWriter(expectedFilename, true)) {
	        
	        // Writing the game state to the data file.
	        for (double feature : gameState) {
	            dataWriter.write(feature + ",");
	        }
	        dataWriter.write("\n");

	        // Writing the action taken to the expected output file.
	        expectedWriter.write(actionTaken + "\n");

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}


}