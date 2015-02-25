package pillar.lbs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.DefaultFeatureCollections;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
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
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class LineOfSightCoordinates extends AbstractAlgorithm {
	
	private final String inputObservations = "inputObservations";
	private final String inputSurfaceModelPath = "inputSurfaceModelPath";
	private final String inputBaringFieldName = "inputBaringFieldName";
	private final String inputTiltFieldName = "inputTiltFieldName";
	private final String inputUserHeight = "inputUserHeight";
	private final String inputSurfaceModelCRS = "inputSurfaceModelCRS";
	
	Logger LOGGER = Logger.getLogger(LineOfSightCoordinates.class);
	
	@Override
	public Class<?> getInputDataType(String identifier) {
		if (identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		if (identifier.equalsIgnoreCase("inputSurfaceModelPath")){
			return LiteralStringBinding.class;
		}
		if (identifier.equalsIgnoreCase("inputBaringFieldName")){
			return LiteralStringBinding.class;
		}
		if (identifier.equalsIgnoreCase("inputTiltFieldName")){
			return LiteralStringBinding.class;
		}
		if (identifier.equalsIgnoreCase("inputUserHeight")){
			return LiteralDoubleBinding.class;
		}	
		if (identifier.equalsIgnoreCase("inputSurfaceModelCRS")){
			return LiteralStringBinding.class;
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
		if(identifier.equalsIgnoreCase("metadata")){
			return GenericFileDataBinding.class;
		}
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData){
	List<IData> inputObsList = inputData.get("inputObservations");
	List<IData> inputSMPList = inputData.get("inputSurfaceModelPath");
	List<IData> inputBarList = inputData.get("inputBaringFieldName");
	List<IData> inputTiltList = inputData.get("inputTiltFieldName");
	List<IData> inputUserList = inputData.get("inputUserHeight");
	List<IData> inputCRSList = inputData.get("inputSurfaceModelCRS");
	
	FeatureCollection pointInputs = ((GTVectorDataBinding) inputObsList.get(0)).getPayload();
	String surfaceModelPath = ((LiteralStringBinding)inputSMPList.get(0)).getPayload();
	String baringFieldName = ((LiteralStringBinding)inputBarList.get(0)).getPayload();
	String tiltFieldName = ((LiteralStringBinding)inputTiltList.get(0)).getPayload();
	double userHeight = ((LiteralDoubleBinding)inputUserList.get(0)).getPayload();
	String surfaceModelCRS = ((LiteralStringBinding)inputCRSList.get(0)).getPayload();
	
	
	
	RasterReader rr = new RasterReader(surfaceModelPath);
	
	
	LOGGER.warn("Feature Collection Size " + pointInputs.size());
	LOGGER.warn("inputCRS " + surfaceModelCRS);
	
	SimpleFeatureIterator iterator = (SimpleFeatureIterator) pointInputs.features();
	
	SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
	
	CRSAuthorityFactory  factory = CRS.getAuthorityFactory(true);
	
	CoordinateReferenceSystem sourceCRS = null;
	
	CoordinateReferenceSystem projectCRS = null;
	try {
		sourceCRS = factory.createCoordinateReferenceSystem("EPSG:4326");
		projectCRS = factory.createCoordinateReferenceSystem("EPSG:"+surfaceModelCRS);
	} catch (NoSuchAuthorityCodeException e1) {
		LOGGER.warn("No Such CRS Authority code " + e1);
		e1.printStackTrace();
	} catch (FactoryException e1) {
		LOGGER.warn("Factory Exception " + e1);
		e1.printStackTrace();
	}
	
	FeatureCollection rPointInputs = new ReprojectingFeatureCollection(pointInputs, projectCRS);

	//build result feature type
	b.setName("Temp");
	
	SimpleFeatureIterator sfi = (SimpleFeatureIterator) rPointInputs.features();
	
	SimpleFeature temp = sfi.next();
	
	Collection<Property> property = temp.getProperties();
	
	
	Iterator<Property> inObsPropIt = property.iterator();
	
	while (inObsPropIt.hasNext()==true){
		try{
			
			Property tempProp = inObsPropIt.next();
			PropertyType type = tempProp.getDescriptor().getType();
			String name = type.getName().getLocalPart();
			Class<String> valueClass = (Class<String>)tempProp.getType().getBinding();
			
			b.add(name, valueClass);
			

			//LOGGER.warn ("Obs property " + name + " " + valueClass);
			}
			catch (Exception e){
				LOGGER.error("property error " + e);
			}
		
	}
	
	b.add("easting", Double.class);
	b.add("northing", Double.class);
	
	
	double[] headerData = new double[6];
	
	headerData=rr.getASCIIHeader();
	
	LOGGER.warn("Get raster header " +headerData[0] + " " + headerData[1] + " " + headerData[2] + 
			" " + headerData[3] + " " +headerData[4] + " " + headerData[5]); 
	ArrayList<SimpleFeature> list = new ArrayList<SimpleFeature>();
	FeatureCollection fc = DefaultFeatureCollections.newCollection();
	
	SimpleFeatureType outFeat = b.buildFeatureType();
	
	SimpleFeatureBuilder builder = new SimpleFeatureBuilder(outFeat);
			
	
	try{
		while (iterator.hasNext()){
			
			SimpleFeature tempFeature = iterator.next();
			
			Object[] attributes = new Object[10];
			int i = 0;

			
			/**for (org.opengis.feature.Property property : tempFeature.getProperties()) {
				  Name name = property.getName();
				  Object value = tempFeature.getAttribute( property.getName() );
				  attributes[i] = value;
				  i++;
				}**/
			
			Geometry geom = (Geometry) tempFeature.getDefaultGeometry();
			
			
		//	LOGGER.warn("WGS " + geom.getCoordinate().x + " " + geom.getCoordinate().y);
			
			GeometryFactory gf2 = new GeometryFactory();
			
			Coordinate tempCoord = new Coordinate(geom.getCoordinate().y, geom.getCoordinate().x);
			
			Point point2 = gf2.createPoint(tempCoord);
		
			MathTransform transform = CRS.findMathTransform(sourceCRS, projectCRS, true);
			
			Geometry transformedGeom = JTS.transform(point2, transform);
			
			Coordinate coord = transformedGeom.getCoordinate();
			
			//LOGGER.warn("Coordinates " + coord.x + " " + coord.y);
			
			attributes[0] = tempFeature.getAttribute("DevID");
			String URN = attributes[0].toString();
			
			attributes[1] = coord.x;				
			attributes[2] = coord.y;
			attributes[3] = tempFeature.getAttribute(tiltFieldName);
			attributes[4] = tempFeature.getAttribute(baringFieldName);
			
			Double myX = Double.parseDouble(attributes[1].toString());
			Double myY = Double.parseDouble(attributes[2].toString());
			Double tilt = Double.parseDouble(attributes[3].toString());
			Double compass = Double.parseDouble(attributes[4].toString());
	
			//LOGGER.error(myX + " " + myY + " " +  compass + " " + tilt );
			
			double [] myResult = new double[4];
			
			
			GetHeightICanSee gh = new GetHeightICanSee(myX, myY, rr, compass, tilt, 2);
			myResult = gh.getMyResult();
			
			
			double easting = myResult[2];
			double northing = myResult[3];
			
			GeometryFactory gf = new GeometryFactory();
			
			Coordinate outCoord = new Coordinate(easting, northing);
			Point point = gf.createPoint(outCoord);
			
			
			builder.set("easting",  easting);
			builder.set("northing", northing);
			//builder.set("location", point);
			String s = ""+URN;
			
			//Geometry geom = point;
			
			SimpleFeature feature = builder.buildFeature(s);
			feature.setDefaultGeometry(point);
			
			//LOGGER.warn("output " + feature.toString());
			
			//SimpleFeature feature = tempFeature;
			
			list.add(feature);
			
			
		}
	
	}
	catch (Exception e){
		LOGGER.error("main process error " + e);
	}

	
	iterator.close();
	
	FeatureCollection output = new ListFeatureCollection(outFeat, list);
	FeatureCollection returnOutput = new ReprojectingFeatureCollection(output, sourceCRS);
	
	GenericFileData fd = null;
	
	HashMap<String, Object> metadataElements = new HashMap<String, Object>();
	
	metadataElements.put("element", "LineOfSightElement");
	
	File tempFile = createXMLMetadata(metadataElements);
	
	try {
		fd = new GenericFileData(tempFile,"text/xml");
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	HashMap<String, IData> result = new HashMap<String, IData>();
	
	//output the same as input for chaining
	
	result.put("result", new GTVectorDataBinding(pointInputs));
	result.put("qual_result", new GTVectorDataBinding(returnOutput));
	result.put("metadata", new GenericFileDataBinding(fd));
	
return result;
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
    issue.addNewWorkAround().setCharacterString("LineOfSight");

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

public double[] parseInputData(FeatureCollection fc){
	double [] inputDataDouble = new double[5];
	
	
	return inputDataDouble;
}




private static class GetHeightICanSee {
	private static final String TAG = null;
	double Easting;
	double Northing;
	static double [] headerData = new double[6];
	double [][] ASCIIData;
	private dhTuple myResult; 
	private double[] myData;
	private double myHeight;
	private double myHeightOffset;
	private static double distanceToTarget;
	public GetHeightICanSee(){
		//constructor to use the inner methods
		
	}
	//rr = the class which parses the raster
	//theta = compass
	//elevation = tilt
	//heightOffset = height of the device above the surface model (usually 1.75m, my height at eye level)
	public GetHeightICanSee(double easting, double northing, RasterReader rr, double theta, 
			double elevation, double heightOffset){
	
		//this.Easting = easting;
		//this.Northing = northing;
		GetHeightICanSee.headerData = rr.getASCIIHeader();
		
		ASCIIData = new double[(int)headerData[1]][(int)headerData[0]];
		
		this.ASCIIData = rr.getASCIIData();
		
		this.Easting = getMyx(easting);
		this.Northing = getMyy(northing);
		myHeight = - 1;
		
		myHeightOffset = heightOffset;
		
		try{
			myHeight = ASCIIData[(int)Northing][(int)Easting] + myHeightOffset;
			
			}
		catch (ArrayIndexOutOfBoundsException e){
			
			}
		
		myResult = heightICanSee(makeAngle(theta), Math.toRadians(elevation), ASCIIData, headerData[4],
				(int)Easting, (int)Northing, myHeight);
		myData = new double[3];
		myData = conMyResult(myResult);
	}
	
	public double[] getMyResult(){
		return myData;
	}
	
	//get Array Coordinates for drawing
	
	public double[] getArrayCoords(){
		double[] arrayCoords = new double[2];
		arrayCoords[0] = (getMyx(getMyResult()[2]))/headerData[0];
		arrayCoords[1] = (getMyy(getMyResult()[3]))/headerData[1];
		return arrayCoords;
		
	}
	
	public double[] getMyPositionDraw(){
		double[] myPos = new double[2];
		myPos [0] = (Easting)/headerData[0];
		myPos [1] = (Northing)/headerData[1];
		return myPos;
	}
	
	//convert tuple to double[]
	private double [] conMyResult(dhTuple dh){
		double [] myResult = new double[5];
		try{
		
		myResult[0] = dh.d;
		myResult[1] = dh.h;
		myResult[2] =  (int) ((int) ((dh.x) * headerData[4]) + headerData[2]);;
		myResult[3] = (int) ((int) ((int) ((headerData[0] - dh.y - 1)  * headerData[4])) + headerData[3]);
		myResult[4] = (int) ASCIIData[(int)Northing][(int)Easting];
		}
		catch (NullPointerException e){
			
		
			for(int i = 0; i < 4; i++){
				myResult[i] = -1;
			}
			
		}
		
		return myResult;
	}
	
	//convert easting to array coordinates
	private static double getMyx(double Easting){
		double i = ((Easting - headerData[2])) / headerData[4];
		return i;
	}
	
	//convert Northing to Array Coordinates
	private static double getMyy(double Northing){
		double i = (headerData[0] - ((Northing - headerData[3])) / headerData[4]);
		return i;
	}
	
	
	/* -- DEEP TRIG MAGIC STARTS HERE --RM -- */
	// All angles, call makeAngle before you call these functions
	
	// this is a helper class to get around the fact that we can't return
	// tuples in java
	private static class xyTuple {
		double x, y;
		int xcell, ycell;
		
		public xyTuple(double x, double y, int xcell, int ycell) {
			this.x = x;
			this.y = y;
			this.xcell = xcell;
			this.ycell = ycell;
		}
		
		public String toString() {
			return "(" + x + "," + y + "," + xcell + "," + ycell + ")";
		}
	}
	
	//convert bearing into direction
	private static double makeAngle(double degrees) {
		double realDegrees = 360 - 
			(degrees - 90); // to turn our bearing into a degree from y=0
		return Math.toRadians(realDegrees);
	}
	
	private static xyTuple getCellAt(double theta, double d, double cellsize) {
			double x = Math.cos(theta) * d;
			double y = Math.sin(theta) * d;
			int xcell = (int) Math.floor(x / cellsize);
			int ycell = (int) Math.floor(y / cellsize);
			return new xyTuple(x, y, xcell, ycell);
	}
	
	
	
	//extract floorheight from data array
	private static double getHeightAt(double elevation, double d, double myHeight) {
		return (d * Math.tan(elevation)) + myHeight;
	}
	
	private static class dhTuple {
		double d, h, x, y;
		
		public dhTuple(double d, double h, int y, int x) {
			this.d = d;
			this.h = h;
			this.y = y;
			this.x = x;
		}
		
		
	}
	
	//calculate height I can see from data in correct format
	
	private static dhTuple heightICanSee(double theta, double elevation, double[][] universe, 
								  double cellsize, int myx, int myy, double myHeight) {
		double d = cellsize * 4 + 0.1; // enter number of cells to ignore here (if any).
		                     // This is so that we ignore the cell we are 'in'.
		double dincr = cellsize;
		xyTuple xy = null;
		
		
			
		try { 
			while (d < (universe[0].length * cellsize)/2) {
				xy = getCellAt(theta, d, cellsize);
				
				double h = getHeightAt(elevation, d, myHeight);

				// The strange -s are because we have to flip from 'geometry' to 'array index'
				
				
				if (h < universe[myy-xy.ycell - 1][myx + xy.xcell]){
					//int E = (int) ((int) ((myx + xy.xcell) * headerData[4]) + headerData[2]);
					//int N = (int) ((int) ((int) ((headerData[0] - myy+xy.ycell - 1)  * headerData[4])) + headerData[3]);
					//System.out.println("E = " + E + " N = " + N);
					distanceToTarget = d;
					return new dhTuple(d, (universe[myy-xy.ycell - 1][myx + xy.xcell]),myy-xy.ycell - 1, myx + xy.xcell);
				}
				
				
				d += dincr;
				
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			//System.out.println("** The world is flat and we fell off it.");
			return null;
		}
		//System.out.println("** Can't see anything at this distance.");
		return null;
	}
	
	public double getMyHeight(){
		return myHeight;
	}
	
	public double[] getMyArrayCoords(){
		double[] myArrayCoords = new double[2];
		
		myArrayCoords[0] = getMyx(getMyResult()[2]);
		myArrayCoords[1] = getMyy(getMyResult()[3]);
		
		return myArrayCoords;
		
	}
	public double getDistanceToTarget(){
		return distanceToTarget;
		
	}
	

	}
private static class RasterReader {
	//RASTER MUST BE A SQUARE
	
	String fileName;
	static double [] headerData = new double[6];
	double [][]ASCIIData;
	
	public RasterReader(String fileName){
		
		this.fileName = fileName;
		
		headerData = getRasterHeader(fileName);
		ASCIIData = new double[(int)headerData[0]][(int)headerData[1]];
		ASCIIData = inputASCIIData(fileName);
		
		
	}
	
	
	
		
	//eMod and nMod are the numbers the raster start and finish should be modified by
	//i.e. 5m resolution raster but taken starting with an E of 1 would be eMod 1;
	
	//read in header data of study area to calculate the viewshed tile required
	private static double[] getRasterHeader(String file){
		Logger rrLOGGER = Logger.getLogger(LineOfSight.class);
	BufferedReader br = null;
	
	double[] headerData = new double[6];
	//read in header
	try {
		br = new BufferedReader(new FileReader(file));
		
		String line = null;
		double var;
							
		for(int i = 0; i < 6; i++){
						
			line = br.readLine();
			char[] buffer = new char[line.length()];	
		 	line.getChars(14,line.length(), buffer, 0);
			String s = String.valueOf(buffer);
			var = Double.parseDouble(s);
			headerData[i] = var;
						
		}
		br.close();
		
	
	}
	catch(Exception e){
		rrLOGGER.error("rrLOGGER " + e);
	}
	
	return headerData;
	}
	
	// read in the raster
	private static double[][] inputASCIIData(String rasterPath){
		Logger rrLOGGER = Logger.getLogger(LineOfSight.class);
		BufferedReader br = null;
		
		headerData = new double[6];
		
		double[][] ASCIIData = null;
	
		
		//read in headers
		int counter = 0;
		try {
			//read in DSM
			
			br = new BufferedReader(new FileReader(rasterPath));
			
			String DSMline = null;
			
			double var;
								
			//get header data from DSM
			for(int i = 0; i < 6; i++){
							
				DSMline = br.readLine();
				
				char[] buffer = new char[DSMline.length()];	
				DSMline.getChars(14,DSMline.length(), buffer, 0);
				String s = String.valueOf(buffer);
				var = Double.parseDouble(s);
				headerData[i] = var;
							
				//System.out.println("H = " + var);
				
			} 
			
			//read in DSM values
		
			ASCIIData = new double[(int) headerData[0]][(int) headerData[1]];
			
			for(int i = 0;i < headerData[0];i++){
				DSMline = br.readLine();
				String [] temp = new String[(int) headerData[1]];
				temp = DSMline.split("[ ]+");
			
				
				for(int j = 0;j < headerData[1];j++){
					//read in LiDAR
					counter++;
					ASCIIData[i][j] = Double.parseDouble(temp[j]);
					
				 	}
									
				}
			
			br.close();
		
			
			rrLOGGER.debug("Raster read in!");
			
							
		}
			
			catch( IOException e ) {
				
			}
			catch(NullPointerException e){
				rrLOGGER.error("NullPointerException" + e);
			}
			catch(NumberFormatException e){
				rrLOGGER.error("Number format exception " + e + " " + counter);
			}
			
			
			return ASCIIData;             
		}
	
	public double[][] getASCIIData(){
		return ASCIIData;
	}
	
	public double[] getASCIIHeader(){
		return headerData;
	}
	
	
}
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}


}
