package pillar.authoritativedata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollections;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geoviqua.gmd19157.DQDataQualityType;
import org.geoviqua.qualityInformationModel.x40.GVQDataQualityType;
import org.geoviqua.qualityInformationModel.x40.GVQDiscoveredIssueType;
import org.geoviqua.qualityInformationModel.x40.GVQMetadataDocument;
import org.geoviqua.qualityInformationModel.x40.GVQMetadataType;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;

import com.vividsolutions.jts.geom.Geometry;

public class BufferDataComparison extends AbstractSelfDescribingAlgorithm{
Logger LOGGER = Logger.getLogger(BufferDataComparison.class);
	@Override
	public Class<?> getInputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		
		if (identifier.equalsIgnoreCase("inputAuthoritativeData")){
			return GTVectorDataBinding.class;
		}
		if (identifier.equalsIgnoreCase("bufferSize")){
			return LiteralDoubleBinding.class;
		}
		
		return null;
	
	}

	@Override
	public Class<?> getOutputDataType(String identifier) {
		if (identifier.equalsIgnoreCase("result")){
			return GTVectorDataBinding.class;
		}
		
		if (identifier.equalsIgnoreCase("qual_result")){
			return GTVectorDataBinding.class;
		}
		
		if (identifier.equalsIgnoreCase("metadata")){
			return GenericFileDataBinding.class;
		}
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {
		
		
		HashMap<String, Object> metadataMap = new HashMap<String,Object>();
		ArrayList<SimpleFeature> list = new ArrayList<SimpleFeature>();
		List <IData> inputObs = inputData.get("inputObservations");
		List <IData> inputAuth = inputData.get("inputAuthoritativeData");
		List <IData> inputLit = inputData.get("bufferSize");
		IData observations = inputObs.get(0);
		IData authoritative = inputAuth.get(0);
		
		IData buffersize = inputLit.get(0);
		
		double doubleB = (Double) buffersize.getPayload();
		
		
		
		FeatureCollection obsFC = ((GTVectorDataBinding) observations).getPayload();
		FeatureCollection authFC = ((GTVectorDataBinding) authoritative).getPayload();
		
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) obsFC.features();
		
		SimpleFeatureIterator authIt = (SimpleFeatureIterator) authFC.features();
		
		
		//setup result feature
		
		SimpleFeature obsItFeat = obsIt.next();
		
		SimpleFeature obsItAuth = authIt.next();
		
		Collection<Property> property = obsItFeat.getProperties();
		Collection<Property> authProperty = obsItAuth.getProperties();
		
		//setup result type builder
		SimpleFeatureTypeBuilder resultTypeBuilder = new SimpleFeatureTypeBuilder();
		resultTypeBuilder.setName("typeBuilder");
		
		
		
		Iterator<Property> pItObs = property.iterator();
		Iterator<Property> pItAuth = authProperty.iterator();
		
		
		metadataMap.put("element", "elementBufferedMetadata");
		File metadataFile = createXMLMetadata(metadataMap);
		
		while (pItObs.hasNext()==true){
			
			try{
			Property tempProp = pItObs.next();
			
			PropertyType type = tempProp.getDescriptor().getType();
			String name = type.getName().getLocalPart();
			Class<String> valueClass = (Class<String>)tempProp.getType().getBinding();
		
			resultTypeBuilder.add(name, valueClass);
			
		
			}
			catch (Exception e){
				LOGGER.error("property error " + e);
			}
			
		}
		int i = 0;
		while (pItAuth.hasNext()==true){
			try{
			Property tempProp = pItAuth.next();
			
			PropertyType type = tempProp.getDescriptor().getType();
			String name = type.getName().getLocalPart();
			Class<String> valueClass = (Class<String>)tempProp.getType().getBinding();
			
			if(i > 3){
			
				resultTypeBuilder.add(name, valueClass);
			
			}
			
			i++;
		
			}
			catch (Exception e){
				LOGGER.error("property error " + e);
			}
			
		}
		
		obsIt.close();
		authIt.close();
		resultTypeBuilder.add("withinBuffer", Integer.class);
		
		// set up result feature builder
		
		SimpleFeatureType type = resultTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder resultFeatureBuilder = new SimpleFeatureBuilder(type);
		
		// process data here:
		
		SimpleFeatureIterator obsIt2 = (SimpleFeatureIterator) obsFC.features();
		
		
		int within = 0;
				
		FeatureCollection resultFeatureCollection = DefaultFeatureCollections.newCollection();	
		
		while (obsIt2.hasNext() == true){
			within = 0;
			SimpleFeature tempObs = obsIt2.next();
			Geometry obsGeom = (Geometry) tempObs.getDefaultGeometry();
			
			
			
			for (Property obsProperty : tempObs.getProperties()){
				
				String name = obsProperty.getName().getLocalPart();
				Object value = obsProperty.getValue();
				
				
				resultFeatureBuilder.set(name, value);
				//LOGGER.warn("obs Property set " + name);
			}
			
			double bufferSizeDouble = doubleB;
			
			
			Geometry bufferGeom = obsGeom.buffer(bufferSizeDouble);
			
		
			int j = 0;
			SimpleFeatureIterator authIt2 = (SimpleFeatureIterator) authFC.features();
			while (authIt2.hasNext() == true){
				
				SimpleFeature tempAuth = authIt2.next();
				Geometry authGeom = (Geometry) tempAuth.getDefaultGeometry();
				
				
				if(bufferGeom.intersects(authGeom) == true){
					within = 1;
					j=0;
					
					LOGGER.warn("Intersection = true");
					for (Property authProperty1 : tempAuth.getProperties()){
						
						String name = authProperty1.getName().getLocalPart();
						Object value = authProperty1.getValue();
						//Class valueClass = (Class<String>)authProperty1.getType().getBinding();
						
				//		LOGGER.warn("Auth property " + name);
						if (j > 3){
							resultFeatureBuilder.set(name, value);
						//	LOGGER.warn("Auth property set " + name);
							
						}
						
						j++;
						
		
						
					}
					
				}
				
			}
			resultFeatureBuilder.set("withinBuffer", within);
				
			
			
			SimpleFeature resultFeature = resultFeatureBuilder.buildFeature(tempObs.getName().toString());
			Geometry geom = (Geometry) tempObs.getDefaultGeometry();
			resultFeature.setDefaultGeometry(geom);
			
			
			
			list.add(resultFeature);
			
			//resultFeatureCollection.add(resultFeature);
			//LOGGER.warn("RESULT FEATURE " + resultFeatureCollection.getSchema().toString());
			//resultFeatureCollection = obsFC;
		}
		
		
		ListFeatureCollection listFeatureCollection = new ListFeatureCollection(type, list);
		LOGGER.warn("Result Feature Size " + listFeatureCollection.size());
	 
		
		//sort HashMap
		GenericFileData gf = null;
		try{
			gf = new GenericFileData(metadataFile, "text/xml");
		}
		catch(IOException e){
			LOGGER.error("GenericFileData " + e);
		}
		
		HashMap<String, IData> results = new HashMap<String, IData>();
		results.put("result", new GTVectorDataBinding ((FeatureCollection) obsFC));
		results.put("qual_result", new GTVectorDataBinding ((FeatureCollection) listFeatureCollection));
		results.put("metadata", new GenericFileDataBinding (gf) );
		
		return results;
				
		
		
	}

	@Override
	public List<String> getInputIdentifiers() {
		List<String> inputIdentifiers = new ArrayList<String>();
		
		inputIdentifiers.add("inputObservations");
		inputIdentifiers.add("inputAuthoritativeData");
		inputIdentifiers.add("bufferSize");
		return inputIdentifiers;	
	}

	@Override
	public List<String> getOutputIdentifiers() {
		List<String> outputIdentifiers = new ArrayList<String>();
		outputIdentifiers.add("result");
		outputIdentifiers.add("qual_result");
		outputIdentifiers.add("metadata");
		return outputIdentifiers;
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
	    issue.addNewKnownProblem().setCharacterString(inputs.get("element").toString());
	    issue.addNewWorkAround().setCharacterString("solution");

	        // validate schema conformity
	        boolean isValid = doc.validate();
	        if ( !isValid)
	            System.out.println(Arrays.toString(validationErrors.toArray()));

	        // print out as XML
	        System.out.println(doc.xmlText(options));
	        

		
		try {
			 File tempFile = File.createTempFile("wpsMetdataTempFile", "xml");
			 
			 doc.save(tempFile);
		
		
		return tempFile;
		
		}
		catch(Exception e){
			
			LOGGER.error("createXMLMetadataError " + e);
			
		}
		return null;
	}
	


}
