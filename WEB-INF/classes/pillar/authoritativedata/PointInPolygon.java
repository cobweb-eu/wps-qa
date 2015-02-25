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
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class PointInPolygon extends AbstractAlgorithm {
	Logger LOG = Logger.getLogger(PointInPolygon.class);
	
	private final String inputObservations = "inputObservations";
	private final String inputAuthoritativeData = "inputAuthoritativeData";
	
	@Override
	public Class<?> getInputDataType(String identifier) {
		if(identifier.equalsIgnoreCase( "inputObservations")){
			return GTVectorDataBinding.class;
		}
		if (identifier.equalsIgnoreCase("inputAuthoritativeData")){
			return GTVectorDataBinding.class;
		}
		return null;
	}

	@Override
	public Class<?> getOutputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("result")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("qual_result")){
			return GTVectorDataBinding.class;
		}
	
		
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {
		
		List <IData> inputObs = inputData.get("inputObservations");
		List <IData> inputAuth = inputData.get("inputAuthoritativeData");
		
		
		LOG.warn("+++++++++HERE+++++++++++");
		FeatureCollection obsFc = ((GTVectorDataBinding) inputObs.get(0)).getPayload();
		FeatureCollection authFc = ((GTVectorDataBinding) inputAuth.get(0)).getPayload();
		
		
		SimpleFeatureIterator sfi = (SimpleFeatureIterator) obsFc.features();
		SimpleFeature tempPropFeature = sfi.next();
		CoordinateReferenceSystem inputObsCrs = obsFc.getSchema().getCoordinateReferenceSystem();
		
		Collection<Property> obsProp = tempPropFeature.getProperties();
		
		
		//SimpleFeatureType typeF = tempPropFeature.getType();
		
		SimpleFeatureTypeBuilder resultTypeBuilder = new SimpleFeatureTypeBuilder();
		resultTypeBuilder.setName("typeBuilder");
		resultTypeBuilder.setCRS(inputObsCrs);
		
		Iterator<Property> pItObs = obsProp.iterator();
		
	
		
		sfi.close();
		while (pItObs.hasNext()==true){
			
			try{
				
			Property tempProp = pItObs.next();
			PropertyType type = tempProp.getDescriptor().getType();
			String name = type.getName().getLocalPart();
			Class<String> valueClass = (Class<String>)tempProp.getType().getBinding();
			
	
			
			resultTypeBuilder.add(name, valueClass);
			
			
			

			LOG.warn ("Obs property " + name + " " + valueClass + " " +type.toString());
			}
			catch (Exception e){
				LOG.error("property error " + e);
			}
		}
		
		//add DQ_Field
		
		resultTypeBuilder.add("DQ_SA_RelativeAccuracy", Double.class);
		SimpleFeatureType typeF = resultTypeBuilder.buildFeatureType();
		LOG.warn("Get Spatial Accuracy Feature Type " + typeF.toString());
		
		SimpleFeatureBuilder resultFeatureBuilder = new SimpleFeatureBuilder(typeF);
		ArrayList<SimpleFeature> resultArrayList = new ArrayList<SimpleFeature>(); 
		ArrayList<SimpleFeature> qual_resultArrayList = new ArrayList<SimpleFeature>();
		
		
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) obsFc.features();
		
		obsIt.close();
		
		SimpleFeatureIterator obsIt2 = (SimpleFeatureIterator) obsFc.features();
		
		int within = 0;
		
		while (obsIt2.hasNext()==true){
			within = 0;
			SimpleFeature tempSf = obsIt2.next();	
			
			Geometry obsGeom = (Geometry) tempSf.getDefaultGeometry();
			
			for (Property obsProperty : tempSf.getProperties()){

				
				String name = obsProperty.getName().toString();
				Object value = obsProperty.getValue();
				
				resultFeatureBuilder.set(name, value);
				
			}
			
			SimpleFeatureIterator authIt = (SimpleFeatureIterator) authFc.features();
			
			
			
			while (authIt.hasNext()==true){
				
				SimpleFeature tempAuth = authIt.next();
				Geometry authGeom = (Geometry) tempAuth.getDefaultGeometry();
				
			
				
				
				
				if (obsGeom.within(authGeom)){
					within = 1;
					
				}
				
				
				
				
				
				
			}
			
			resultFeatureBuilder.set("DQ_SA_RelativeAccuracy", within);
			SimpleFeature result = resultFeatureBuilder.buildFeature(tempSf.getID());
			result.setDefaultGeometry(obsGeom);
			resultArrayList.add(result);
			
			if(within == 1){
				qual_resultArrayList.add(result);
				
				
			}
			
			authIt.close();
		
			
			
			
		}
		obsIt2.close();
		FeatureCollection resultFeatureCollection = new ListFeatureCollection(typeF, resultArrayList);
		FeatureCollection qual_resultFeatureCollection = new ListFeatureCollection(typeF, qual_resultArrayList);
		
		LOG.warn("Feature Collection Size " + resultFeatureCollection.size());
		
		
		HashMap<String, IData> results = new HashMap<String, IData>();
		results.put("result", new GTVectorDataBinding((FeatureCollection)resultFeatureCollection));
		results.put("qual_result", new GTVectorDataBinding((FeatureCollection)qual_resultFeatureCollection));
		
		
		
		
		return results;
	}
	
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
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
	

}
