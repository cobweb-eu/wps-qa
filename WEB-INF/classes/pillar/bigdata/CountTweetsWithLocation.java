package pillar.bigdata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;

import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.Query.ResultType;
import twitter4j.Query.Unit;
import twitter4j.conf.ConfigurationBuilder;

public class CountTweetsWithLocation extends AbstractAlgorithm {

	private final String inputLocation = "inputLocation";
	private final String dateSince = "dateSince";
	private final String searchTerm = "searchTerm";
	private final String inputDistance = "inputDistance";
	private final String inputObservations = "inputObservations";
	Logger LOG = Logger.getLogger(CountTweetsWithLocation.class);
	@Override
	public Class<?> getInputDataType(String identifier) {
	
		
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("inputLocation")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("dateSince")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("searchTerm")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("inputDistance")){
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
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {
		if (inputData == null || !inputData.containsKey(dateSince)) {
			throw new RuntimeException(
					"Error while allocating input parameters");
		}
		
		List<IData> inputDisList = inputData.get(inputDistance);
		List<IData> inputLoc = inputData.get(inputLocation);
		List<IData> dateList = inputData.get(dateSince);
		List<IData> searchList = inputData.get(searchTerm);
		List<IData> obsList = inputData.get(inputObservations);
		
		IData inputDistance = inputDisList.get(0);
		IData inputLocation = inputLoc.get(0);
		IData inputDate = dateList.get(0);
		IData searchTerm = searchList.get(0);
		
		FeatureCollection obsFc = ((GTVectorDataBinding)obsList.get(0)).getPayload();
		
		LiteralDoubleBinding inputDist = (LiteralDoubleBinding) inputDistance;
		LiteralStringBinding inputLoca = (LiteralStringBinding) inputLocation;
		LiteralStringBinding inputDateT = (LiteralStringBinding) inputDate;
		LiteralStringBinding search = (LiteralStringBinding) searchTerm;
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("I6TEkVf5h9Dt9Ro8sSOkNIuhy")
		  .setOAuthConsumerSecret("sGi6Xr0ZVFlYmfCkfuQ330rdiVRjehvBOl1b0PXNa4lkdKLQuW")
		  .setOAuthAccessToken("25795712-7MKeNgDhUUsXxtith4YmHTZgE8PwCwFlzM8ArSzHo")
		  .setOAuthAccessTokenSecret("UGMqrroxbgKIa6nizLIsChKUqZpVkq6ozE6fxUkq2uL71");
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();

		
		
		
		Unit unit = Unit.km;
	    ResultType resultType = ResultType.recent;
	    String[] temp = new String[2];
	    temp = inputLoca.getPayload().split(",");
	    double lat = Double.parseDouble(temp[0]);
	    double lon = Double.parseDouble(temp[1]);
		
		String qString = search.getPayload();
		
	   
	    
		GeoLocation location = new GeoLocation(lat, lon);
		double radius = inputDist.getPayload();
		String since = inputDateT.getPayload();
		Query query = new Query(qString);
	  	query.count(100);
	    query.resultType(resultType);
	    query.setGeoCode(location, radius, unit);
	    
	    int totalTweets = 0;
	    
	    try {
	    	query.setSince(since);
			QueryResult result = twitter.search(query);
		
			totalTweets = result.getTweets().size();
				
			
	
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    catch(Exception e){
	    	System.out.println(e);
	    }
	    
	   
		System.out.println("Total Tweets " + totalTweets);
		Map<String, IData> result = new HashMap<String, IData>();
		
		result.put("metadata", new GenericFileDataBinding(null));
		result.put("qual_result", new GTVectorDataBinding(obsFc));
		
		result.put("result", new GTVectorDataBinding(obsFc));
		return result;
	}
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}
}
