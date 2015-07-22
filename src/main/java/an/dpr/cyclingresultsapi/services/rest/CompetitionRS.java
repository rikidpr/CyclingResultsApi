package an.dpr.cyclingresultsapi.services.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import an.dpr.cyclingresultsapi.bean.CompetitionClass;
import an.dpr.cyclingresultsapi.dao.CompetitionDAO;
import an.dpr.cyclingresultsapi.domain.Competition;
import an.dpr.cyclingresultsapi.exception.CyclingResultsException;
import an.dpr.cyclingresultsapi.util.Contracts;
import an.dpr.cyclingresultsapi.util.DateUtil;

/**
 * REST service for cycling competitions
 * 
 * @author saez url all competitions 2015 road man
 *         http://www.uci.infostradasports.com/
 *         asp/lib/TheASP.asp?PageID=19004&TaalCode
 *         =2&StyleID=0&SportID=102&CompetitionID
 *         =-1&EditionID=-1&CompetitionID=-1&GenderID
 *         =1&ClassID=1&CompetitionPhaseID=0&Phase1ID
 *         =0&Phase2ID=0&CompetitionCodeInv=1
 *         &PhaseStatusCode=262280&DerivedCompetitionPhaseID
 *         =-1&SeasonID=488&StartDateSort
 *         =20150108&EndDateSort=20151225&Detail=1&
 *         DerivedCompetitionID=-1&S00=-3&S01=2&S02=1&PageNr0=-1&Cache=8
 *         
 * Todo el calendario: http://www.uci.ch/road/calendar/
 * TODO LIST:
 * 	-find by gender (male, female)
 * 	-find by class (elite, sub23..)
 * 	-find by competitionClass (WT, hc1, 1.1...)
 * 	-find by category (world, europe, asia ...) 
 */

@Path("/competitions/")
public class CompetitionRS {

    
    private static final Logger log = LoggerFactory.getLogger(CompetitionRS.class);
    private static final String ALL_COMPS = "http://www.uci.infostradasports.com/asp/lib/TheASP.asp?PageID=19004&TaalCode=2&StyleID=0&SportID=102&CompetitionID=-1&EditionID=-1&EventID=-1&GenderID=1&ClassID=1&EventPhaseID=0&Phase1ID=0&Phase2ID=0&CompetitionCodeInv=1&PhaseStatusCode=262280&DerivedEventPhaseID=-1&SeasonID=488&StartDateSort=20150108&EndDateSort=20151225&Detail=1&DerivedCompetitionID=-1&S00=-3&S01=2&S02=1&PageNr0=-1&Cache=8";
    
    @Autowired
    private CompetitionDAO dao;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/query/{initDate},{finDate},{genderID},{classID},{competitionClass}")
    public List<Competition> getCompetitions(
	    @PathParam("initDate") String initDate,
	    @PathParam("finDate") String finDate,
	    @PathParam("genderID") String genderID,
	    @PathParam("classID") String classID,
	    @PathParam("competitionClass") String competitionClass) {
	List<Competition> list = null;
	Date id = getDate(initDate, false);
	Date fd = getDate(finDate, true);
	if (fd == null){
	    list = dao.getCompetitions(id, getGenderID(genderID), getClassID(classID), getCompetitionClass(competitionClass));
	} else {
	    list = dao.getCompetitions(id, fd, getGenderID(genderID), getClassID(classID), getCompetitionClass(competitionClass));
	}
	return list;
    }
    
    private CompetitionClass getCompetitionClass(String competitionClass) {
	CompetitionClass ret = CompetitionClass.get(competitionClass);
	if (ret == null){
	    ret = CompetitionClass.ALL;
	}
	return ret;
    }

    private Long getClassID(String classID) {
	try{
	    return Long.parseLong(classID);
	} catch(NumberFormatException e){
	    return Contracts.DEFAULT_CLASS_ID;
	}
    }

    private Long getGenderID(String genderID) {
	try{
	    return Long.parseLong(genderID);
	} catch(NumberFormatException e){
	    return Contracts.DEFAULT_GENDER_ID;
	}
    }

    private Date getDate(String cadena, boolean nullable) {
	Date ret = null;
	if (cadena == null || cadena.isEmpty()) {
	    //fecha por defecto 1/1 del año actual
	    if (!nullable){
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		Date date = new Date(0);
		cal.setTime(date);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.YEAR, year);
		ret=cal.getTime();
	    }
	    
	} else {
	    try {
		ret = DateUtil.parse(cadena, DateUtil.DDMMYYYY);
	    } catch (CyclingResultsException e) {
		log.error("",e);
	    }
	}
	return ret;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/next/")
    public List<Competition> getNextCompetitions() {
	return dao.getYearCompetitions(2015);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/year/{year}")
    public List<Competition> getYearCompetitions(@PathParam("year") String year) {
	return dao.getYearCompetitions(Integer.parseInt(year));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/month/{year},{month}")
    public List<Competition> getMonthCompetitions(
	    @PathParam("year") String year,
	    @PathParam("month") String month
	    ) {
	Calendar cal = Calendar.getInstance();
	Date date = new Date(0);
	cal.setTime(date);
	cal.set(Calendar.HOUR, 0);
	cal.set(Calendar.YEAR, Integer.parseInt(year));
	cal.set(Calendar.MONTH, Integer.parseInt(month)-1);
	Date init = cal.getTime();
	
	cal.add(Calendar.MONTH, 1);
	Date fin= cal.getTime();
	
	return dao.getCompetitionsBetweenDates(init, fin);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/stageRaceCompetitions/{competitionId}")
    public List<Competition> getStageRaceCompetitions(
	    @PathParam("competitionId") String competitionId
	    ) {
	StringBuilder ret = new StringBuilder();
	Competition competition = dao.getCompetition(Long.parseLong(competitionId), (long)-1);
	try {
	    HttpClient client = new DefaultHttpClient();
	    HttpGet get = new HttpGet(getURLStageEvents(competition));
	    HttpResponse response = client.execute(get);
	    InputStreamReader isr = new InputStreamReader(response.getEntity()
		    .getContent(), "cp1252");
//	    String file = "C:/Users/saez/workspace/andpr/CyclingResultsApi/html/stageRaceCompetitions.htm";
//	    FileReader isr = new FileReader(new File(file));
	    BufferedReader br = new BufferedReader(isr);
	    String line;
	    while ((line = br.readLine()) != null) {
		ret.append(line);
	    }
	} catch (ClientProtocolException e) {
	    log.error("error leyendo info ", e);
	} catch (URISyntaxException e) {
	    log.error("error leyendo info ", e);
	} catch (IOException e) {
	    log.error("error leyendo info ", e);
	}
	log.info(ret.toString());
	return tratarXmlStageRaceCompetitions(ret.toString(), competition);
    }

    private URI getURLStageEvents(Competition competition) throws URISyntaxException {
	StringBuilder sb = new StringBuilder();
	sb.append(Contracts.URL_STAGE_EVENTS_1);
	sb.append(Contracts.URL_STAGE_EVENT_DATA
		.replace(Contracts.SPORT_ID,competition.getSportID().toString())
		.replace(Contracts.COMPETITION_ID,competition.getCompetitionID().toString())
		.replace(Contracts.EDITION_ID,competition.getEditionID().toString())
		.replace(Contracts.SEASON_ID,competition.getSeasonID().toString())
		.replace(Contracts.CLASS_ID,competition.getClassID().toString())
		.replace(Contracts.GENDER_ID,competition.getGenderID().toString())
		.replace(Contracts.EVENT_ID,competition.getEventID().toString())
		.replace(Contracts.EVENT_PHASE_ID,competition.getEventPhaseID().toString())
		);
	
	return new URI(sb.toString());
    }

    private List<Competition> tratarXmlStageRaceCompetitions(String html, Competition competition) {
	Document doc = Jsoup.parse(html);
	Elements tableElements = doc.select("table.datatable");
	List<Competition> list = new ArrayList<Competition>();

	Elements tableRowElements = tableElements.select(":not(thead) tr");

	// Date Competition Nat. Class Winner
	for (int i = 0; i < tableRowElements.size(); i++) {
	    Element row = tableRowElements.get(i);
	    if (row.attr("valign").equals("top")) {// los que no tienen
						   // valing=top son headers
		Elements rowItems = row.select("td");
		Competition stage = new Competition.Builder()
			.setDates(rowItems.get(0).text().replace("\u00a0", "").trim())
			.setName(rowItems.get(1).text())
			.setWinner(rowItems.get(2).text())
			.setLeader(rowItems.get(3).text())
			.setPhase1ID(getKeyId(rowItems.get(1).toString(), Contracts.PHASE1_ID_KEY))
			.setSportID(competition.getSportID())
			.setCompetitionID(competition.getCompetitionID())
			.setEventID(competition.getEventID())
			.setEditionID(competition.getEditionID())
			.setSeasonID(competition.getSeasonID())
			.setCompetitionID(competition.getCompetitionID())
			.setGenderID(competition.getGenderID())
			.setClassID(competition.getClassID())
			.build();
		log.debug(getURLStageResults(stage));
		persistCompetition(stage);
		list.add(stage);
	    }
	}
	return list;
    }
    
    private String getURLStageResults(Competition comp){
	StringBuilder sb = new StringBuilder();
	sb.append(Contracts.URL_STAGE_1)
	.append(Contracts.URL_STAGE_EVENT_DATA
		.replace(Contracts.SPORT_ID, String.valueOf(comp.getSportID()))
		.replace(Contracts.COMPETITION_ID, String.valueOf(comp.getCompetitionID()))
		.replace(Contracts.EDITION_ID, String.valueOf(comp.getEditionID()))
		.replace(Contracts.SEASON_ID, String.valueOf(comp.getSeasonID()))
		.replace(Contracts.EVENT_ID, String.valueOf(comp.getEventID()))
		.replace(Contracts.GENDER_ID, String.valueOf(comp.getGenderID()))
		.replace(Contracts.CLASS_ID, String.valueOf(comp.getClassID()))
		)
	.append(Contracts.URL_STAGE_DATA.replace(Contracts.PHASE1_ID, String.valueOf(comp.getPhase1ID())));
	return sb.toString();
    }

    /**
     * Last competitions finished or in course.
     * 
     * @return List<Competition>
     * @throws URISyntaxException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/last/")
    public List<Competition> getLastCompetitions() throws URISyntaxException {
	StringBuilder ret = new StringBuilder();
	try {
//	    HttpClient client = new DefaultHttpClient();
//	    HttpGet get = new HttpGet(ALL_COMPS);
//	    HttpResponse response = client.execute(get);
//	    InputStreamReader isr = new InputStreamReader(response.getEntity()
//		    .getContent(), "cp1252");
	    String file = "C:/Users/saez/workspace/andpr/CyclingResultsApi/html/RoadResults.htm";
	    FileReader isr = new FileReader(new File(file));
	    BufferedReader br = new BufferedReader(isr);
	    String line;
	    while ((line = br.readLine()) != null) {
		ret.append(line);
	    }
	} catch (ClientProtocolException e) {
	    log.error("error leyendo info ", e);
	} catch (IOException e) {
	    log.error("error leyendo info ", e);
	}
	log.info(ret.toString());
	return tratarXmlCompetitions(ret.toString());
    }

    private List<Competition> tratarXmlCompetitions(String html) {
	Document doc = Jsoup.parse(html);
	Elements tableElements = doc.select("table.datatable");
	List<Competition> list = new ArrayList<Competition>();

	Elements tableRowElements = tableElements.select(":not(thead) tr");

	// Date Competition Nat. Class Winner
	for (int i = 0; i < tableRowElements.size(); i++) {
	    Element row = tableRowElements.get(i);
	    if (row.attr("valign").equals("top")) {// los que no tienen
						   // valing=top son headers
		Elements rowItems = row.select("td");
		Competition competition = new Competition.Builder()
			.setEventID(getKeyId(rowItems.get(1).toString(),Contracts.EVENT_ID_KEY))
			.setSeasonID(getKeyId(rowItems.get(1).toString(),Contracts.SEASON_ID_KEY))
			.setCompetitionID(getKeyId(rowItems.get(1).toString(),Contracts.COMPETITION_ID_KEY))
			.setEventPhaseID(getKeyId(rowItems.get(1).toString(), Contracts.EVENT_PHASE_ID_KEY))
			.setPhaseClassificationID(getKeyId(rowItems.get(1).toString(), Contracts.PHASE_CLASSIFICATION_ID_KEY))
			.setEditionID(getKeyId(rowItems.get(1).toString(), Contracts.EDITION_ID_KEY))
			.setGenderID(getKeyId(rowItems.get(1).toString(), Contracts.GENDER_ID_KEY))
			.setClassID(getKeyId(rowItems.get(1).toString(), Contracts.CLASS_ID_KEY))
			.setPageID(getKeyId(rowItems.get(1).toString(), Contracts.PAGE_ID_KEY))
			.setSportID(getKeyId(rowItems.get(1).toString(), Contracts.SPORT_ID_KEY))
			.setPhase1ID(getKeyId(rowItems.get(1).toString(), Contracts.PHASE1_ID_KEY))
			.setPhase2ID(getKeyId(rowItems.get(1).toString(), Contracts.PHASE2_ID_KEY))
			.setPhase3ID(getKeyId(rowItems.get(1).toString(), Contracts.PHASE3_ID_KEY))
			
			.setDates(
				rowItems.get(0).text().replace("\u00a0", "")
					.trim())
			.setName(rowItems.get(1).text())
			.setNationality(rowItems.get(2).text())	
			.setCompetitionClass(CompetitionClass.get(rowItems.get(3).text())).build();
		competition.calculateCompetitionType();
		log.debug(competition.toString());
		persistCompetition(competition);
		list.add(competition);
	    }
	}
	return list;
    }

    private void persistCompetition(Competition comp) {
	if (comp!= null && comp.getCompetitionID() != null 
		&& dao.getCompetition(comp.getCompetitionID(), comp.getPhase1ID())==null){
	    dao.save(comp);
	}
    }

    /**
     * 
     * @param string
     * @return
     */
    private Long getKeyId(String string, String key) {
	int fromIdx = string.indexOf(key);
	if (fromIdx < 0 ){
	    return null;
	} else {
	    int toIdx = string.indexOf("&", fromIdx);
	    return Long.parseLong(string.substring(fromIdx + key.length()+1, toIdx));
	}
    }
}
