package Utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;


public class ArrayUtils {

	public final static Logger LOGGER = Logger.getLogger(ArrayUtils.class);

	public static int findIndexOfMax(int[] array) {
		
		int maxIndex = 0;
		int max = array[maxIndex];

		for (int i = 0; i < array.length; i++)
			if(array[i] > max) {
				maxIndex = i;
				max = array[i];
			}

		return maxIndex;
	}
	
	public static List<Integer> findIndicesOfMax(int[] array) {

		ArrayList<Integer> maxIndices = new ArrayList<Integer>();		
		int max = array[0];

		for (int i = 0; i < array.length; i++)
			if(array[i] > max) {
				max = array[i];
				maxIndices.clear();
				maxIndices.add(i);
			}
			else if(array[i] == max) {
				maxIndices.add(i);
			}

		return maxIndices;
	}
	
	public static int[] subsetByIndices(int[] array, List<Integer> indices) {
		
		int[] subset = new int[indices.size()];
		
		for (int i = 0; i < indices.size(); i++) {
			subset[i] = array[indices.get(i)];
		}
		
		return subset;
	}
}
