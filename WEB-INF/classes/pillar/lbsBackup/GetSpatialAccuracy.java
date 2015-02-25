package pillar.lbs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geoviqua.gmd19157.DQDataQualityType;
import org.geoviqua.qualityInformationModel.x40.GVQDataQualityType;
import org.geoviqua.qualityInformationModel.x40.GVQDiscoveredIssueType;
import org.geoviqua.qualityInformationModel.x40.GVQMetadataDocument;
import org.geoviqua.qualityInformationModel.x40.GVQMetadataType;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class GetSpatialAccuracy extends AbstractAlgorithm {
	Logger LOG = Logger.getLogger(GetSpatialAccuracy.class);
	private final String inputObservations = "inputObservations";
	private final String UUIDField = "UUIDField";
	private final String inputSatelliteNumberField = "inputSatelliteNumberField";
	private final String inputAccuracyField = "inputAccuracyField";
	private final String minSatNum = "minSatNum";
	private final String minAcc = "minAcc";
	
	@Override
	public Class<?> getInputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("UUIDField")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("inputSatelliteNumberField")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("inputAccuracyField")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("minSatNum")){
			return LiteralIntBinding.class;
		}
		if(identifier.equalsIgnoreCase("minAcc")){
			return LiteralDoubleBinding.class;
		}
		
		return null;
		
		
	}

	@Override
	public Class<?> getOutputDataType(String identifier) {
		if (identifier.equalsIgnoreCase("result")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("qual_result")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("metadata")){
			return GenericFileDataBinding.class;
		}
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {

		List inputObsList = inputData.get("inputObservations");
		List satFieldList = inputData.get("inputSatelliteNumberField");
		List accFieldList = inputData.get("inputAccuracyField");
		List UUIDList = inputData.get("UUIDField");
		List minSatList= inputData.get("minSatNum");
		List minAccList = inputData.get("minAcc");
		
		
		HashMap <String, String[]> metadata = new HashMap<String, String[]>();
		
		FeatureCollection inputObs = ((GTVectorDataBinding) inputObsList.get(0)).getPayload();
		String inputSatField = ((LiteralStringBinding) satFieldList.get(0)).getPayload();
		String accField = ((LiteralStringBinding) accFieldList.get(0)).getPayload();
		String UUIDField = ((LiteralStringBinding) UUIDList.get(0)).getPayload();
		int minSatNum = ((LiteralIntBinding) minSatList.get(0)).getPayload();
		double minAcc = ((LiteralDoubleBinding) minAccList.get(0)).getPayload();
		
		SimpleFeatureIterator sfi = (SimpleFeatureIterator) inputObs.features();
		
		SimpleFeatureType typeF = sfi.next().getType();
		
		LOG.warn("Get Spatial Accuracy Feature Type " + typeF.toString());
		
		sfi.close();
		
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) inputObs.features();
		ArrayList<SimpleFeature> resultList = new ArrayList<SimpleFeature>();

		
		
		
		
		while (obsIt.hasNext() == true){
			
			SimpleFeature tempFeature = obsIt.next();
			SimpleFeature outFeat = tempFeature;
					
			Property accProperty = (Property) tempFeature.getProperty(inputSatField);
			Property satNumProperty = (Property) tempFeature.getProperty(accField);
			Property UUIDProperty = (Property) tempFeature.getProperty(UUIDField);
			
			double numberSat = Double.parseDouble(accProperty.getValue().toString());
			double acc = Double.parseDouble(satNumProperty.getValue().toString());
			
			
			String UUID = UUIDProperty.getValue().toString();
			
			String[] element = new String[2];
			element[0] = "DQ_PositionalAccuracy";
			element[1] = "element Output";
			
			
			metadata.put(UUID, element);
			
			if(numberSat >= minSatNum){
				
				
				if(acc >= minAcc){
					resultList.add(outFeat);
				}
				
			}
			
			
		}
		
		obsIt.close();
		ListFeatureCollection qualResult = new ListFeatureCollection(typeF, resultList);
		
		Map <String, IData> results = new HashMap<String, IData>();
		
		results.put("result", new GTVectorDataBinding(inputObs));
		results.put("qual_result", new GTVectorDataBinding (qualResult));
		results.put("metadata", new GenericFileDataBinding(null));
		
		return results;
	}
	
private File createXMLMetadata(HashMap<String,Object> inputs){
	
		
		ArrayList< ? > validationErrors = new ArrayList<Object>();
		XmlOptions options; 
		options = new XmlOptions();
		options.setSavePrettyPrint();
		options.setSaveAggressiveNamespaces();

		HashMap<String, String> suggestedPrefixes = new HashMap<String, String>();
		suggestedPrefixes.put("http://www.geoviqua.org/QualityInformationModel/4.0", "gvq");
		options.setSaveSuggestedPrefixes(suggestedPrefixes);

		options.setErrorListener(validationErrors);
		
	
		
		GVQMetadataDocument doc = GVQMetadataDocument.Factory.newInstance();
		GVQMetadataType gvqMetadata = doc.addNewGVQMetadata();
		gvqMetadata.addNewLanguage().setCharacterString("en");
	    gvqMetadata.addNewMetadataStandardName().setCharacterString("GVQ");
	    gvqMetadata.addNewMetadataStandardVersion().setCharacterString("1.0.0");
	    gvqMetadata.addNewDateStamp().setDate(Calendar.getInstance());
	    DQDataQualityType quality = gvqMetadata.addNewDataQualityInfo2().addNewDQDataQuality();
	    GVQDataQualityType gvqQuality = (GVQDataQualityType) quality.substitute(new QName("http://www.geoviqua.org/QualityInformationModel/4.0",
	                                                                                          "GVQ_DataQuality"),
	                                                                                GVQDataQualityType.type);
	    GVQDiscoveredIssueType issue = gvqQuality.addNewDiscoveredIssue().addNewGVQDiscoveredIssue();
	    issue.addNewKnownProblem().setCharacterString(inputs.get("element1").toString());
	    issue.addNewWorkAround().setCharacterString("solution");

	        // validate schema conformity
	        boolean isValid = doc.validate();
	        if ( !isValid)
	            System.out.println(Arrays.toString(validationErrors.toArray()));

	        // print out as XML
	        System.out.println(doc.xmlText(options));
	        

		
		try {
			 File tempFile = File.createTempFile("wpsMetdataTempFile", "txt");
			 
			 doc.save(tempFile);
		
		
		return tempFile;
		
		}
		catch(Exception e){
			
			LOG.error("createXMLMetadataError " + e);
			
		}
		return null;
	}
	
	
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}

}
