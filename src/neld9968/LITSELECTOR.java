package neld9968;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LITSELECTOR {
	
	public static final int NUM_SELECTIONS = 1000;
	
	public static void selection() {
		ArrayList<LITCHROMOSOME> parents = readParents();
		ArrayList<LITCHROMOSOME> children = new ArrayList<>(NUM_SELECTIONS);
		
		for(int i = 0; i < NUM_SELECTIONS; i++){
			int index1 = LITCHROMOSOME.getRandom(0, parents.size() - 2);
			System.out.println(index1);
			int index2 = LITCHROMOSOME.getRandom(index1 + 2, parents.size() - 1);
			System.out.println(index2);
			//find max and second max score
			int maxIndex = index1;
			int secondMaxIndex = index1;
			for(int index = index1; index < index2; index++){
				if(parents.get(index).score > parents.get(maxIndex).score){
					maxIndex = index;
				}
				else if(parents.get(index).score > parents.get(secondMaxIndex).score){
					secondMaxIndex = index;
				}
			}
			LITCHROMOSOME child = LITGENERATOR.crossover(parents.get(maxIndex), parents.get(secondMaxIndex));
			children.add(child);
		}
		
		writeChildren(children);
		
	}
	
	//import from csv
	public static ArrayList<LITCHROMOSOME> readParents(){
		ArrayList<LITCHROMOSOME> parents = new ArrayList<>(NUM_SELECTIONS);
		String csvFile = "LITCSV.csv";
		BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] chromosome = line.split(cvsSplitBy);
                
                LITCHROMOSOME chrom = new LITCHROMOSOME(Integer.parseInt(chromosome[1]),
                		Integer.parseInt(chromosome[2]),
                		Integer.parseInt(chromosome[3]),
                		Integer.parseInt(chromosome[4]),
                		Integer.parseInt(chromosome[5]),
                		Integer.parseInt(chromosome[6]),
                		Integer.parseInt(chromosome[7]));
                chrom.score = Integer.parseInt(chromosome[0]);
                
                parents.add(chrom);
                System.out.println(chrom);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return parents;
	}
	public static void writeChildren(ArrayList<LITCHROMOSOME> children){
		
		FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter("children.csv", true);
            for(LITCHROMOSOME child : children){
	            fileWriter.append("\n" + Integer.toString(child.rateOfFireFast) + ",");
	            fileWriter.append(Integer.toString(child.rateOfFireSlow) + ",");
	            fileWriter.append(Integer.toString(child.enemyDistanceThresholdClose) + ",");
	            fileWriter.append(Integer.toString(child.enemyDistanceThresholdMedium) + ",");
	            fileWriter.append(Integer.toString(child.enemyDistanceThresholdFar) + ",");
	            fileWriter.append(Integer.toString(child.aStarDistanceThreshold) + ",");
	            fileWriter.append(Integer.toString(child.aStarCounter));
            }
        } catch (Exception e) {
            System.out.println("Error when writing to file");
            e.printStackTrace();
        } finally {

            try {
                fileWriter.flush();
                fileWriter.close();

            } catch (IOException e) {

                System.out.println("Error when closing file writer");
                e.printStackTrace();
            }

        }
	}
	public static void main(String[] args){
		selection();
	}
}
