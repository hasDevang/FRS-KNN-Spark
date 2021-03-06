package uwt.frs.quality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import scala.Tuple2;
import uwt.frs.ApproxRow;
import uwt.frs.ClassVectorsGenerator;
import uwt.frs.FuzzySimilarityFunction;
import uwt.frs.InMemoryRows;
import uwt.frs.ListMerger;
import uwt.generic.Mergeable;
import uwt.generic.MinMax;
import uwt.generic.ParallelIteration;
import uwt.generic.RowsDescriptor;
import uwt.generic.Utility;

public class QualityIteration extends ParallelIteration {
	private String[] lines;
	private RowsDescriptor rowFormat;
	private MinMax minmax;
	private QualityRow[] inMemoryRows;
	private InMemoryQualityRows inMemRows;
	private FuzzySimilarityFunction simFunction;
	boolean numericOnly = false;
	//Double[] weights;
	double[] qualities;
	int numOfRows = 0;
	int partitionSize;
	//double[][] numericAttrs;
	

	@Override
	public Mergeable runIteration() {
		Mergeable result = null;
		/*if(bothApproxIncluded)
		{
			if(numericOnly)
				result = computeBothApproxNumeric();
			else
				result = computeBothApprox();
		}
		else*/
		/*if(numericOnly)
			result = computeQualityNumeric();
		else*/
			result = computeQuality();
		
		return result;//new FuzzyMergeable();
	}

	public ListMerger computeQuality()
	{
		QualityRow row;
		double implicator= 0;
		double simVal = -1;
		//int index = 0;
		//Double[] qualities = new Double[inMemoryRows.length];
		List<Double> qualities = new ArrayList<Double>();
		ListMerger result = new ListMerger();
		double d1,d2,rd,quality = 1;
		Double[] w;
		for(int j=start; j<end; j++)
		{
			
			if(lines[j] == null)
				break;
			row = new QualityRow(lines[j], rowFormat, minmax);
			d1 = row.getNormalizedOutcome();
			//quality = 1;
			for(QualityRow inMemRow: inMemoryRows)
			{
				if(inMemRow.getId()!= row.getId())
				{
					simVal = simFunction.getSimilarity(row, inMemRow);
					d2 = inMemRow.getNormalizedOutcome();
					rd = 1 - Math.abs(d1-d2);

					if(simVal>rd)
					{
						implicator = 1-simVal + rd;
						qualities.add(implicator);
						//quality = Math.min(implicator, quality);
						//qualities[index] = implicator;
					}
				}
				/*else
					qualities[index] = 1.0;*/
				//index++;
				
			}
			
			w = Utility.generateWeights(qualities.size());
			quality = Utility.owa(qualities,w);
			qualities.clear();
			
			result.add(new Tuple2<Integer, Double>(row.getId(), quality));
		}
		return result;

	}
	
	/*public ListMerger computeQualityNumeric()
	{
		QualityRow row;
		double simVal = -1;

		Double[] qualities = new Double[inMemoryRows.length];
		ListMerger result = new ListMerger();
		double d1,quality = 1;
		Double[] w = weights;
		double[] responseVarValues = inMemRows.getResponseVarValues();
		double[] numericAttrs = inMemRows.getNumericAttrs();
		double[] rowAttrs;
		int ai=0;
		int numOfAtts = inMemoryRows[0].getNumOfAttributes();
		int numOfRows = inMemoryRows.length;
		double implicator = 0;
		for(int j=start; j<end; j++)
		{
			
			if(lines[j] == null)
				break;
			row = new QualityRow(lines[j], rowFormat, maxVals);
			rowAttrs = row.getNumericAttributes();
			d1 = row.getNormalizedOutcome();

			for(int ri=0;ri<numOfRows;ri++)
			{
				simVal=0;
				for(int aj=0;aj<numOfAtts;ai++,aj++)
				{
					simVal += Math.abs(numericAttrs[ai] - rowAttrs[aj]);
				}
				simVal =  (numOfAtts - simVal)/numOfAtts;
				/*rd = 1 - Math.abs(d1-responseVarValues[ri]);

				implicator = 1-simVal + rd;
				
				qualities[ri] = implicator;
				//double temp = 1-simVal + rd;
				implicator = 2 - simVal - Math.abs(d1-responseVarValues[ri]);
				implicator = implicator>1?1:implicator;
				qualities[ri] = implicator;//Math.abs(d1-responseVarValues[ri]);
			}
			/*for(QualityRow inMemRow: inMemoryRows)
			{
				if(inMemRow.getId()!= row.getId())
				{
					simVal = simFunction.getSimilarity(row, inMemRow);
					d2 = inMemRow.getNormalizedResponse();
					rd = 1 - Math.abs(d1-d2);

					implicator = 1-simVal + rd;
					qualities[index] = implicator;
				}
				else
					qualities[index] = 1.0;
				index++;
				
			}
			
			quality = Utility.owa(qualities,w);
			
			result.add(new Tuple2<Integer, Double>(row.getId(), quality));
		}
		return result;
	
	}*/

	/*public Mergeable computeQualityNumeric()
	{
		ApproxRow row;
		double implicator= 0;
		double tnorm = 0;
		double simVal = -1;
		double[] rowAttrs;
		//double[] la = lowerApproxValues;
		//double[] ua = upperApproxValues;
		double[] cv = classVectors;
		int r1=0, attrStartInd=0;
		double[] la;
		double[] ua;
		double[] inMemRowAttrs = inMemRows.getNumericAttrs();
		//double[][] attributes = inMemRows.getAttributes();
		ListMerger result = new ListMerger();
		int aj = 0;
		int c = 0;

		for(int j=start; j<end; j++)
		{
			if(lines[j] == null)
				break;
			row = new ApproxRow(lines[j], rowFormat, cvGen, rangesVal,bothApproxIncluded);
			rowAttrs = row.getNumericAttributes();
			la = new double[numOfClasses];
			ua = new double[numOfClasses];
			Arrays.fill(la, 1);
			aj = 0;
			c = 0;
			
			for(int ri = 0; ri< partitionSize; ri++)
			{
				simVal = 0;
				//attrs = (double[][]) attributes[ri];
				for(int ai=0;ai<rowAttrs.length; ai++, aj++)
				{
					simVal += Math.abs(inMemRowAttrs[aj] - rowAttrs[ai]);
					//simVal += Math.abs(attributes[ri][ai] - rowAttrs[ai]);
				}
				simVal =  (rowAttrs.length - simVal)/rowAttrs.length;

				for(int i=0; i<numOfClasses;i++,c++)
				{
					implicator = Math.max((1-simVal), cv[c]);
					la[i] = Math.min(implicator, la[i]);
					
					tnorm = Math.min(simVal, cv[c]);
					ua[i] = Math.max(tnorm, ua[i]);
				}
				
			}
			result.add(new Tuple2<Integer, ApproxRow>(row.getId(), new ApproxRow(la, ua)));
		}

		return result;
	}*/

	@Override
	public void setParameters(Object parameters) {
		Map param = (Map)parameters;
		lines = (String[]) param.get("lines");
		rowFormat = (RowsDescriptor) param.get("rowFormat");
		minmax = (MinMax) param.get("minmax");
		
		simFunction = (FuzzySimilarityFunction) param.get("simFunction");
		inMemRows = (InMemoryQualityRows) param.get("inMemoryRows");
		inMemoryRows = inMemRows.getInMemRows();
		numericOnly = (boolean) param.get("numericOnly");
		partitionSize = inMemoryRows.length;
		//weights = (Double[]) param.get("weights");
		
	}
	
	public Object generateParameters(String[] lines,
			RowsDescriptor rowFormat, MinMax minmax, InMemoryQualityRows inMemoryRows,
			FuzzySimilarityFunction simFunction, boolean numericOnly) {
		
		Map param = new HashMap();
		param.put("lines", lines);
		param.put("rowFormat", rowFormat);
		param.put("minmax", minmax);
		param.put("inMemoryRows", inMemoryRows);
		param.put("simFunction", simFunction);
		param.put("numericOnly", numericOnly);
		//param.put("weights", weights);
		setParameters(param);
		return param;

	}

}
