package pillar.authoritativedata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geoviqua.gmd19157.DQDataQualityType;
import org.geoviqua.qualityInformationModel.x40.GVQDataQualityType;
import org.geoviqua.qualityInformationModel.x40.GVQDiscoveredIssueType;
import org.geoviqua.qualityInformationModel.x40.GVQMetadataDocument;
import org.geoviqua.qualityInformationModel.x40.GVQMetadataType;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class AuthoritativeDataComparison extends AbstractSelfDescribingAlgorithm {
	Logger LOGGER = Logger.getLogger(AuthoritativeDataComparison.class);
	
	@Override
	public Class<?> getInputDataType(String identifier) {
		// TODO Auto-generated method stub
		
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		
		if (identifier.equalsIgnoreCase("inputAuthoritativeData")){
			return GTVectorDataBinding.class;
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
		
		HashMap<String, Object> metadataElements = new HashMap<String, Object>();
		
		System.setProperty("org.geotools.referencing.forceXY", "true");
		
		List <IData> inputObs = inputData.get("inputObservations");
		List <IData> inputAuth = inputData.get("inputAuthoritativeData");
		
		IData observations = inputObs.get(0);
		IData authoritative = inputAuth.get(0);

		FeatureCollection obsFC = ((GTVectorDataBinding) observations).getPayload();
		FeatureCollection authFC = ((GTVectorDataBinding) authoritative).getPayload();
		
		CoordinateReferenceSystem obsCRS = obsFC.getSchema().getCoordinateReferenceSystem();
		
		CoordinateReferenceSystem authCRS = authFC.getSchema().getCoordinateReferenceSystem();
		
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
		
		ArrayList<SimpleFeature> list = new ArrayList<SimpleFeature>();
		
		resultTypeBuilder.setCRS(DefaultGeographicCRS.WGS84);
		
		Iterator<Property> pItObs = property.iterator();
		Iterator<Property> pItAuth = authProperty.iterator();
		
		while (pItObs.hasNext()==true){
		
			try{
				
			Property tempProp = pItObs.next();
			PropertyType type = tempProp.getDescriptor().getType();
			String name = type.getName().getLocalPart();
			Class<String> valueClass = (Class<String>)tempProp.getType().getBinding();
			
			resultTypeBuilder.add(name, valueClass);
			

			LOGGER.warn ("Obs property " + name + " " + valueClass);
			}
			catch (Exception e){
				LOGGER.error("property error " + e);
			}
			
		}
		
		int i = 0;
		while (pItAuth.hasNext()==true){
			
			try{
			Property tempProp = pItAuth.next();
			
		
		if (i > 3){
			PropertyType type = tempProp.getDescriptor().getType();
			String name = type.getName().getLocalPart();
			Class<String> valueClass = (Class<String>)tempProp.getType().getBinding();
			
			LOGGER.warn ("Auth property " + name + " " + valueClass);
		
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
		resultTypeBuilder.add("within", Integer.class);
		
		// set up result feature builder
		
		resultTypeBuilder.setCRS(obsCRS);
		
		SimpleFeatureType type = resultTypeBuilder.buildFeatureType();
		
		SimpleFeatureBuilder resultFeatureBuilder = new SimpleFeatureBuilder(type);
	
		// process data here:
		
		SimpleFeatureIterator obsIt2 = (SimpleFeatureIterator) obsFC.features();
		
		int within = 0;
		
		LOGGER.warn("FeatureCollection Size " + obsFC.size());
		while (obsIt2.hasNext() == true){
			within = 0;
			SimpleFeature tempObs = obsIt2.next();
			Geometry obsTest = (Geometry) tempObs.getDefaultGeometry();
			Geometry obsGeom = (Geometry) obsTest.clone();

			for (Property obsProperty : tempObs.getProperties()){
				
				String name = obsProperty.getName().toString();
				Object value = obsProperty.getValue();
				resultFeatureBuilder.set(name, value);
				
			}
			SimpleFeatureIterator authIt2 = (SimpleFeatureIterator) authFC.features();
			while (authIt2.hasNext() == true){
				
				SimpleFeature tempAuth = authIt2.next();
				Geometry authTest = (Geometry) tempAuth.getDefaultGeometry();
				Geometry authGeom = (Geometry) authTest.clone();
			
				try{
				if(obsGeom.within(authGeom) == true){
				//	LOGGER.warn("Within! " + obsGeom.toString());
					within = 1;
					int j = 0;
					for (Property authProperty1 : tempAuth.getProperties()){
						
						if(j > 3){
						String name = authProperty1.getName().toString();
						
						
						Object value = authProperty1.getValue();
						Class valueClass = (Class<String>)authProperty1.getType().getBinding();
							resultFeatureBuilder.set(name, value);
						}
						j++;
		
						
					}
			
				}
				}
				catch(NullPointerException e){
					LOGGER.error("NullPointerException " + e);
				}
				
			}
			resultFeatureBuilder.set("within", within);
			
			metadataElements.put("element1", "elementReturn");
			
			SimpleFeature resultFeature = resultFeatureBuilder.buildFeature(tempObs.getName().toString());
		
			resultFeature.setDefaultGeometry(obsGeom);
			
			//LOGGER.warn("RESULT FEATURE " + resultFeature.getDefaultGeometry().toString());
			
			list.add(resultFeature);			
			//LOGGER.warn("LIST SIZE " + list.size());
			authIt2.close();
			
			
			
		}
		obsIt2.close();
		
	
		FeatureCollection collection = new ListFeatureCollection(type,list);
		//resultFeatureCollection = obsFC;
		
	
		
	
		LOGGER.warn("resultFeatureCollection Size " + list.size());
		//output data here
		
		//metadata
		
		GenericFileData fd = null;
	
		File file = createXMLMetadata(metadataElements);
		
		
			
		
		
		try {
			
			
			fd = new GenericFileData(file, "text/xml");
			LOGGER.warn("mimeType " + fd.getMimeType());
			
		
			} catch (IOException e) {
			// TODO Auto-generated catch block
			LOGGER.warn("IOException " + e);
		
			} 
		
		HashMap<String, IData> results = new HashMap<String, IData>();
		
		
		results.put("result", new GTVectorDataBinding ((FeatureCollection) obsFC));
		results.put("qual_result", new GTVectorDataBinding((FeatureCollection) collection));
		results.put("metadata",  new GenericFileDataBinding (fd));
		
		return results;
	}

	@Override
	public List<String> getInputIdentifiers() {
		List<String> inputIdentifiers = new ArrayList<String>();
		
		inputIdentifiers.add("inputObservations");
		inputIdentifiers.add("inputAuthoritativeData");
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
			
			LOGGER.error("createXMLMetadataError " + e);
			
		}
		return null;
	}
	
	
}
