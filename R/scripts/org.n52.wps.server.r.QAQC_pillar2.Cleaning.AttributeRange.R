#################################################################
# COBWEB QAQC October 2014
# pillar2 Cleaning
# some typical QCs
#
# Didier Leibovici Sam Meek and Mike Jackson University of Nottingham
#
# each function is to be encapsulated as process part of the WPS
# input format and output format of the data and metadata 
# are managed within the java wrapper 


# pillar2.Cleaning.xxxx 
#   where xxxx is the name of the particular QC test


#  pillar2.Cleaning.AttributeRange
#     to compare an attribute value with an obvious and non expert range with the aim of removing obvious mistakes 
#     with possible feedback fro correction
#####################ISO10157#############
DQ=c("DQ_Usability","DQ_Completeness_abs","DQ_CompletenessCommission","DQ_CompletenessOmission","DQ_ThematicAccuracy_abs","DQ_ThematicClassificationCorrectness","DQ_NonQuantitativeAttributeAccuracy","DQ_QuantitativeAttributeAccuracy","DQ_LogicalConsistency_abs","DQ_ConceptualConsistency","DQ_DomainConsistency","DQ_FormatConsistency","DQ_TopologicalConsistency","DQ_TemporalAccuracy_abs","DQ_AccuracyOfATimeMeasurement","DQ_TemporalConsistency","DQ_TemporalValidity","DQ_PositionalAccuracy_abs","DQ_AbsoluteExternalPositionalAccuracy","DQ_GriddedDataPositionalAccuracy","DQ_RelativeInternalPositionalAccuracy")
####################GeoViQUA basic########
GVQ=c("GVQ_PositiveFeedback","GVQ_NegativeFeedback") #can be used also for user
################# Stakeholder Quality Model 
CSQ=c("CSQ_Vagueness","CSQ_Ambiguity","CSQ_Judgement","CSQ_Reliability","CSQ_Validity","CSQ_Trust")
###########################################
######## function used   
pillar2.Cleaning.AttributeRange <-function(ObsAttrib,ObsMetaQ=NULL,UserMetaQ=NULL, UsabScore=80, Range=c(-999,999),FilterOut=FALSE){
	# all this is at feature level
	#
	# ObsAttrib is a quantitative value  ... citizen data captured
	# AuthAttrib is the expected value as measured by the authoritative data
	#
	# test if the Obs does (not) belongs to the given Range
	#        
	#       assign the 2*prob (being over) to ISO19157::DomainConsistency
	#       assign "no" to  ISO19157::Usability (maybe a temporary no)
	#       
	# if not below threshold	 
	#    i.e.  one cannot reject Obs= Auth  and Obs belongs to the distrib 
	#   assign the variance to the data captured ISO19157::QuantitativeAttributeAccuracy  and  Usability to "yes" 
	##  	 
	  
	#if(is.null(ObsMetaQ)){ObsMetaQ=vector();colnames(ObsMetaQ)=DQ} 
	#if(is.null(UserMetaQ)){UserMetaQ=vector();colnames(UserMetaQ)=CSQ}
	
	if(Range[1] <= ObsAttrib & ObsAttrib <= Range[2]) {	
		ObsMetaQ[DQ[1]]= UsabScore #DQ_Usability
		UserMetaQ[CSQ[3]]=min(UserMetaQ[CSQ[3]] +UsabScore/10, 100) #judgement
		UserMetaQ[CSQ[4]]=min(UserMetaQ[CSQ[4]] +1,100) # reliability
		trustHere= min((UserMetaQ[CSQ[5]]+CSQ[3]]*UserMetaQ[CSQ[4]]/2)/UsabScore, 100) # (valid+reli*judg)/(2*UsabScore)
		UserMetaQ[CSQ[6]]=(UserMetaQ[CSQ[6]] + newtrust)/2  # trust
	}
	else{
		ObsMetaQ[DQ[1]]=100-UsabScore #DQ_Usability
		UserMetaQ[CSQ[3]]=max(0,min(UserMetaQ[CSQ[3]] - UsabScore/10, 100) )#judgement
		UserMetaQ[CSQ[4]]=max(0,min(UserMetaQ[CSQ[4]] -1,100)) # reliability
		trustHere= min((UserMetaQ[CSQ[3]]*UserMetaQ[CSQ[4]])/UsabScore, 100)
		if(UserMetaQ[CSQ[5]]>UsabScore )UserMetaQ[CSQ[6]]=min(max(0,UserMetaQ[CSQ[6]] - UsabScore/10),(UserMetaQ[CSQ[6]] + newtrust)/2 ) # trust
		else UserMetaQ[CSQ[6]]=max(0,UserMetaQ[CSQ[6]] -(UsabScore-CSQ[5]]) )
	}
		
	if(FilterOut)ObsMetaQ[DQ[1]]=0
return(list("ObsMetaQ"=ObsMetaQ, "USerMetaQ"=UserMetaQ))	
}# 

# 
GetMetaQ<-function(Attrib,QQ=DQ[c(1)]){
	# QQ list the fields of
	# get if they exists corresponding fields DQ GVQ or CSQ
	# create a vector withe values or 0 or 
	MetaQ=matrix(rep(0,*dim(Attrib)[1]*length(QQ)),c(dim(Attrib)[1],length(QQ)))
	colnames(MetaQ)=QQ
	for (j in QQ){
		if (j in colnames(Attrib)) MetaQ[,j]=Attrib@data[,j]
	}
return(MetaQ)
}
##################################################################
#describtion set for WPS4R
# input  set for 52North WPS4R
#output set for 52North WPS4R

# wps.des: pillar2.Cleaning.AttributeRange , title = pillar2.Cleaning.AttributeRange ,
# abstract = QC test comparing quantitative attribute input Obs to given arange of values ; 

# wps.in: inputObservations, shp, title = Observation(s) input, abstract= gml or shp of the citizen observations ; 
# wps.in: ObsAttribFieldName, string, title = AttributeName,  abstract = attribute name existing in the vector format ; 

# wps.in: AuthAttribFieldName, string, title= Fieldname in Obserbations;
# wps.in: RangeOfAttribute, string, title = two values  min and max, abstract= values given as in R "c(minvalue,maxvaule)" ; 
# wps.in: UsabScore, real, title= 0-100 score, abstract= Subjective value given to direct usability of being in the range i.e. if the range is large because of lack of expertise knowledge its direct usability is low; 

# ######not used#####  ObsMeta, xml,  title = Observation metadata, abstract= if given will update the metadata record(s) ; 
# ######not used#####  UserMeta, xml, title = User metadata, abstract= if given will update the metadata record(s) ; 

# wps.in: FilterOut, Boolean, title = FilterOut yes or no, abstract = Immediate flag to discard the observation putting DQ_Usability at 0 ;



#################################################################
#libraries to read gml  or shapefile or geoJSON or ....
# see possible file formats  ogrDrivers()   ...

library(rgdal)
libary(rgeos)

 Obs <-readOGR(ObsVector,layer= inputObservations)
 ObsAttrib=Obs@data[ObsAttribFieldName] # attribute with UUID  includes DQ_ and CSQ_ if previous (where is the user Id?)
 
# metaQ as matrices 

ObsMetaQ=GetMetaQ(ObsAttrib, QQ=DQ[1])
UserMetaQ=GetMetaQ(ObsAttrib, QQ=CSQ[c(3,4,5,6)])

#
	for (i in 1:dim(ObsAttrib)[1]){
	# creating the matching
		Res=pillar2.Cleaning.AttributeRange(ObsAttrib[i],ObsMetaQ[i],UserMetaQ[i],UsabScore=UsabScore, Range= RangeOfAttribute,FilterOut=FilterOut)
 		#ObsMetaQ[i,]=Res$ObsMetaQ ; UserMetaQ[i,]=Res$UserMetaQ
		Obs@data[i,DQ[1]]=Res$ObsMetaQ
		Obs@data[i,CSQ[c(3,4,5,6)]]=Res$UserMetaQ
	#
	}

#
localDir=tempdir()
writeOGR(Obs,localDir, as.character(inputObservations), driver="GML" )

cat(paste("Destination: ", localDir, "/",as.character(inputObservations) )

# wps.out: inputObservations, shp, title = Observation metadata for quality updated, abstract= each feature in the collection; 
#
# outputs  by WPS4R