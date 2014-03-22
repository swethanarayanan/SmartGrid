package jchart;
import de.progra.charting.*;
import de.progra.charting.model.*;
import de.progra.charting.render.*;
import java.awt.*;
import java.io.*;

public class Main {

	public static void main(String[] args)
	{
		System.out.println("Hello World!");
		createChart(null);
	}

	public static void createChart(double[] tPCS)
	{
		double[][] model = {{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}};     // Create data array

		for(int i=0;i<24;i++)
			model[0][i] = tPCS[i];
		
		double[] columns = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};  // Create x-axis values

		String[] rows = {"Power"};          // Create data set title

		String title = "Total Power Consumption";          // Create diagram title

		int width = 640;                        // Image size
		int height = 480;

		// Create data model
		DefaultChartDataModel data = new DefaultChartDataModel(model, columns, rows);

		// Create chart with default coordinate system
		DefaultChart c = new DefaultChart(data, title, DefaultChart.LINEAR_X_LINEAR_Y);

		// Add a line chart renderer
		c.addChartRenderer(new LineChartRenderer(c.getCoordSystem(), data), 1);

		// Set the chart size
		c.setBounds(new Rectangle(0, 0, width, height));

		// Export the chart as a PNG image
		try {
			ChartEncoder.createEncodedImage(new FileOutputStream(System.getProperty("user.home")+"/first.png"), c, "png");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
