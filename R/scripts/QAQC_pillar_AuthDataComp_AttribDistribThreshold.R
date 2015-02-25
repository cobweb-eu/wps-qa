#################################################################
# COBWEB QAQC   June 2014
# Pillar Authoritative Data comparison
# some typical QCs
# Didier Leibovici Sam Meek and Mike Jackson University of Nottingham
#
# each function is to be encapsulated as process part of the WPS
# input format and output format of the data and metadata 
# are managed within the java wrapper 


# Pillar_AuthoritativeDataComparison_xxxx 
#   where xxxx is the name of the particular QC test


# example test in R:  Pillar_AuthoritativeDataComparison_AttribDistribThreshold(ObsAttrib=3.5,ObsMetaQ=NULL,AuthAttrib=3)
# example test in R:  Pillar_AuthoritativeDataComparison_AttribDistribThreshold(ObsAttrib=3.5,ObsMetaQ=NULL,AuthAttrib=3,AuthMetaQ=c("variance"=2))

#################################################################
#functions used

Pillar_AuthoritativeDataComparison_AttribDistribThreshold <-function(ObsAttrib,ObsMetaQ=NULL,AuthAttrib,AuthMetaQ=NULL,ProbThreshold=0.10){
	# ObsAttrib is a quantitative value  ... citizen data captured
	# AuthAttrib is the expected value as measured by the authoritative data
	#
	# AuthMetaQ is vector  of quality metadata with  the variance (normal) of the expected distribution
	#   would potentially need parsing UncertML within the ISO19157
	#
	# test if the obs belongs to this distrib i.e. if p(Auth>Obs)<ProbThreshold Obs is considered "not equal" to AuthAttrib (test is not)
	# assign the 2+prob (being over) to DomainConsistency
	# if not below threshold	 assign the variance to the data captured ISO19157::QuantitativeAttributeAccuracy  Usability to "yes" 
	# if below 	 assign "no" to  ISO19157::Usability
	
	#i
     
	if(is.null(ObsMetaQ)) ObsMetaQ=vector()
	if(is.null(AuthMetaQ)){
		AuthMetaQ=vector()
		AuthMetaQ["distrib"]="poisson"
	}
	else {
		if(is.na(AuthMetaQ["distrib"]))AuthMetaQ["distrib"]="normal"
	}
	ObsMetaQ["distrib"]=AuthMetaQ["distrib"]
	if(AuthMetaQ["distrib"]=="normal") p=pnorm(abs(ObsAttrib-AuthAttrib),mean=0,sd=sqrt(as.numeric(AuthMetaQ["variance"])),lower.tail=FALSE)
	if(AuthMetaQ["distrib"]=="poisson") {
		if(is.na(AuthMetaQ["lambda"]))AuthMetaQ["lambda"]=AuthAttrib
		if(ObsAttrib <AuthMetaQ["lambda"])p=ppois(ObsAttrib,as.numeric(AuthMetaQ["lambda"]),lower.tail=TRUE)
		else p=ppois(ObsAttrib,as.numeric(AuthMetaQ["lambda"]),lower.tail=FALSE)
	}
	
	ObsMetaQ["DomainConsistency"]=2*p
	ObsMetaQ["Usability"]="no" #
	if(is.na(AuthMetaQ["Feedback"]))AuthMetaQ["Feedback"]=0
		if (p>ProbThreshold){
			if(AuthMetaQ["distrib"]=="normal")ObsMetaQ["AttributeAccuracy"]= AuthMetaQ["variance"] # measure is variance
			else ObsMetaQ["AttributeAccuracy"]= AuthMetaQ["lambda"] 
			ObsMetaQ["Usability"]="yes"
			AuthMetaQ["Feedback"]=as.numeric(AuthMetaQ["Feedback"])+1
		}
		else {
			AuthMetaQ["Feedback"]=as.numeric(AuthMetaQ["Feedback"])-1
		}
		
	
return(list("ObsMetaQ"=ObsMetaQ, "AuthMetaQ"=AuthMetaQ))	
}#NormPoisDistribTreshold 

##################################################################
#describtion set for WPS4R
# input  set for 52North WPS4R
#output set for 52North WPS4R

# wps.des: Pillar_AuthoritativeDataComparison_AttribDistribThreshold , title = Pillar_AuthoritativeDataComparison_AttribDistribThreshold ,
# abstract = QC test comparing quantitative input Obs to given authoritative data with metadata about quality ....  ; 

# wps.in: ObsVector, string, title = ObsVector, abstract= gml or shp of the citizen obserevation ; 
# wps.in: ObsAttribFieldName, string, title = AttributeName,  abstract = attribute name existing in the vector format ; 

# wps.in: AuthVector, string; 
# wps.in: AuthAttribFieldName, string; 

# wps.in: AuthMeta, xml; 
# wps.in: ObsMeta, xml;

# wps.in: ProbThreshold, double;

# wps.out: AuthMeta.output, xml;
# wps.out: ObsMeta.output, xml;


#################################################################
#libraries to read gml  or shapefile or geoJSON or ....
# see possible file formats  ogrDrivers()   ...

library(rgdal)
libary(rgeos)

Obs <-readOGR(ObsVector,l ayer="ObsVector")
Auth <-radOGR(AuthVector,layer="AuthVector") # supposed to be only one geometry corresponding to the location of the user
											     # the query has been done before ? or do we need to do the query in the WPS
											     #this may not be easy to do in BPMN as then the location is parametrised

ObsAttrib=Obs@data[ObsAttribFieldName]
AuthAttrib=Auth@data[AuthAttribFieldName]

#parsing metadata xml  read
library(XML
AuthMetaQ=NULL
if(AuthMeta!=""){
	AuthMetaQ=vector() # putting distrib variance ... from values for DQ_QuantitatieAttributeAccuracy
	
	AuthMeta.all <- xmlTreeParse(AuthMeta, isURL= isUrl(AuthMeta))
	node=xmlGetNode() # to get DQ_ ....
	AuthMetaQ["variance"]=xmlGetAttr(node,"variance")
	node=xmlGetNode() # to get DQ_ ... with UncertML
	AuthMetaQ["distrib"]=xmlGetAttr(node,"distribution")
}

#
Res=Pillar_AuthoritativeDataComparison_AttribDistribThreshold(ObsAttrib,ObsMetaQ,AuthAttrib,AuthMetaQ,ProbThreshold=0.15)
#
#metadata xml write

# outputs  by WPS4R