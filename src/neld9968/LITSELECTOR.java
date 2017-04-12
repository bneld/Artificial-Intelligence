package neld9968;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class implements the tournament selection function for a population.
 * It also reads in the parent population and writes the children to a csv.
 * @author Luis n Brian
 *
 */
public class LITSELECTOR {
	
	/**
	 * Driver for selection
	 * @param args command line
	 */
	public static void main(String[] args){
		selection();
	}
	
	/**
	 * Implements tournament selection
	 * From a parent ArrayList, picks two parents, crosses them over, and mutates the result.
	 * Repeat for however many parents there are
	 */
	public static void selection() {
		ArrayList<LITCHROMOSOME> parents = readParents();
		ArrayList<LITCHROMOSOME> children = new ArrayList<>();
		
		if(parents.size() > 3){
			for(int i = 0; i < parents.size(); i++){
				int index1 = LITCHROMOSOME.getRandom(0, parents.size() - 2);
				int index2 = LITCHROMOSOME.getRandom(index1 + 2, parents.size() - 1);
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
				child = LITGENERATOR.mutate(child);
				children.add(child);
			}
			
			writeChildren(children);
		}
		
		
	}
	
	/**
	 * Import parents from csv file
	 * @return ArrayList of chromosomes
	 */
	public static ArrayList<LITCHROMOSOME> readParents(){
		ArrayList<LITCHROMOSOME> parents = new ArrayList<>();
		String csvFile = "/Users/Luis/Documents/workspace/LITBOIZ/LITCSV.csv";
		BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            line = br.readLine();
            while ((line != null && !line.isEmpty())) {

                // use comma as separator
                String[] chromosome = line.split(cvsSplitBy);
                
                LITCHROMOSOME chrom = new LITCHROMOSOME((int)Double.parseDouble(chromosome[1]),
                		(int)Double.parseDouble(chromosome[2]),
                		(int)Double.parseDouble(chromosome[3]),
                		(int)Double.parseDouble(chromosome[4]),
                		(int)Double.parseDouble(chromosome[5]),
                		(int)Double.parseDouble(chromosome[6]),
                		(int)Double.parseDouble(chromosome[7]));
                chrom.score = (int)Double.parseDouble(chromosome[0]);
                
                parents.add(chrom);
                System.out.println(chrom);
                line = br.readLine();
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
	
	/**
	 * Writes selected children to a csv
	 * @param children list of children chromosomes
	 */
	public static void writeChildren(ArrayList<LITCHROMOSOME> children){
		
		FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter("/Users/Luis/Documents/workspace/LITBOIZ/children.csv", true);
            for(LITCHROMOSOME child : children){
	            fileWriter.append(Integer.toString(child.rateOfFireFast) + ",");
	            fileWriter.append(Integer.toString(child.rateOfFireSlow) + ",");
	            fileWriter.append(Integer.toString(child.enemyDistanceThresholdClose) + ",");
	            fileWriter.append(Integer.toString(child.enemyDistanceThresholdMedium) + ",");
	            fileWriter.append(Integer.toString(child.enemyDistanceThresholdFar) + ",");
	            fileWriter.append(Integer.toString(child.aStarDistanceThreshold) + ",");
	            fileWriter.append(Integer.toString(child.aStarCounter) + "\n");
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
	
}
