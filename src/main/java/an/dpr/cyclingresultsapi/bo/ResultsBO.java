package an.dpr.cyclingresultsapi.bo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.http.client.ClientProtocolException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import an.dpr.cyclingresultsapi.dao.CompetitionDAO;
import an.dpr.cyclingresultsapi.dao.ResultRowDAO;
import an.dpr.cyclingresultsapi.domain.Competition;
import an.dpr.cyclingresultsapi.domain.ResultRow;
import an.dpr.cyclingresultsapi.util.Contracts;
import an.dpr.cyclingresultsapi.util.NetworkUtils;

/**
 * Business Object for results rows
 * 
 * @author saez
 */
@Stateless
public class ResultsBO {
    private static final Logger log = LoggerFactory.getLogger(ResultsBO.class);

    @Inject
    private CompetitionDAO dao;
    @Inject
    private ResultRowDAO rDao;

    private static final String ONE_DAY_RESULTS_URL = "http://www.uci.infostradasports.com/asp/lib/TheASP.asp?PageID=19006&TaalCode=2&StyleID=0"
	    + "&SportID="
	    + Contracts.SPORT_ID
	    + "&CompetitionID="
	    + Contracts.COMPETITION_ID
	    + "&EditionID="
	    + Contracts.EDITION_ID
	    + "&EventPhaseID="
	    + Contracts.EVENT_PHASE_ID
	    + "&PhaseClassificationID=-1&Phase1ID=0&Phase2ID=0&Phase3ID=0"
	    + "&SeasonID=488&EventID=10635&GenderID=1&ClassID=1&PhaseStatusCode=262280"
	    + "&Detail=1&Ranking=0&DerivedEventPhaseID=-1&S00=1&S01=2&S02=3&PageNr0=-1&Cache=8";

    /*
     * competitionId EditionID EventPhaseID PhaseClassificationID
     */
    

    public List<ResultRow> getOneDayResult(String competitionID,String eventID,String editionID,String genderID,String classID) {
	Competition comp = dao.getCompetition(Long.parseLong(competitionID),Long.parseLong(eventID),
		Long.parseLong(editionID), Long.parseLong(genderID), Long.parseLong(classID),(long)-1);
	List<ResultRow> list = rDao.getResults(comp);
	if (list == null || list.size() == 0){
	    String url = getURLOneDayResults(comp);
	    list = readStageFromUCIWebResults(comp, url);
	}
	return list;
    }
    
    private List<ResultRow> readStageFromUCIWebResults(Competition comp, String url){
	String htmlString = null;
	try {
	    htmlString = NetworkUtils.getRequest(Contracts.BASE_URL_UCI, url);
	} catch (ClientProtocolException e) {
	    log.error("error leyendo info", e);
	} catch (IOException e) {
	    log.error("error leyendo info", e);
	}
	log.debug(htmlString);
	if (htmlString != null){
	    return tratarXmlStageResult(htmlString, comp);
	} else {
	    return new ArrayList<ResultRow>();
	}
    }

    /**
     * equipo (con su puesto) todos los ciclistas que tomaron parte (sin puesto)
     * Habria que pasar de los que no tienen rank
     * 
     * @param html
     * @return
     */
    private List<ResultRow> tratarXmlStageResult(String html, Competition comp) {
	Document doc = Jsoup.parse(html);
	Elements tableElements = doc.select("table.datatable");
	List<ResultRow> list = new ArrayList<ResultRow>();

	Elements tableRowElements = tableElements.select(":not(thead) tr");

	boolean isTeamTimeTrial = true;
	// headers
	for (int i = 0; i < tableRowElements.size(); i++) {
	    Element row = tableRowElements.get(i);
	    if (!row.attr("valign").equals("top")) {// los que no tienen
						    // valing=top son headers
		Elements rowItems = row.select("td.caption");
		if (rowItems.get(3).text().equals("Team")) {
		    isTeamTimeTrial = false;
		    break;
		}
	    }
	}

	// Date Event Nat. Class Winner
	for (int i = 0; i < tableRowElements.size(); i++) {
	    Element row = tableRowElements.get(i);
	    if (row.attr("valign").equals("top")) {// los que no tienen
						   // valing=top son headers
		Elements rowItems = row.select("td");
		int idx = 0;
		ResultRow.Builder builder;
		if (isTeamTimeTrial) {
		    builder = new ResultRow.Builder()
			    .setRank(rowItems.get(idx++).text())
			    .setName(rowItems.get(idx++).text())
			    .setNat(rowItems.get(idx++).text())
			    .setAge(rowItems.get(idx++).text())
			    .setResult(rowItems.get(idx++).text());
		} else {
		    builder = new ResultRow.Builder()
			    .setRank(rowItems.get(idx++).text())
			    .setName(rowItems.get(idx++).text())
			    .setNat(rowItems.get(idx++).text())
			    .setTeam(rowItems.get(idx++).text())
			    .setAge(rowItems.get(idx++).text())
			    .setResult(rowItems.get(idx++).text());
		}

		if (rowItems.size() > 6) {
		    builder.setPaR(rowItems.get(idx++).text())
		    	.setPcR(rowItems.get(idx++).text());
		}

		ResultRow odr = builder.build();
		odr.setCompetition(comp);
		persistResultRow(odr);
		log.debug(odr.toString());
		if (odr.getRank() != null && !odr.getRank().isEmpty()) {
		    list.add(odr);
		}
	    }
	}
	return list;
    }

    private void persistResultRow(ResultRow rr) {
	if (rr!= null && !rDao.resultRowExists(rr)){
	    rDao.save(rr);
	    log.debug("salvado con exito");
	}
    }
    
    public List<ResultRow> getStageResult(String competitionID,String eventID,String editionID
	    ,String genderID, String classID,String phase1ID) {
	Competition comp = dao.getCompetition(Long.parseLong(competitionID),
		Long.parseLong(eventID),Long.parseLong(editionID),
		Long.parseLong(genderID), Long.parseLong(classID),
		Long.parseLong(phase1ID));
	List<ResultRow> list = rDao.getResults(comp);
	if (list == null || list.size() == 0){
	    String url = getURLStageResults(comp);
	    list = readStageFromUCIWebResults(comp, url);
	}
	return list;
    }
    
    public List<ResultRow> getClassificationResult(String competitionID,String eventID,String editionID,
	    String genderID,String classID,String phase1ID,String phaseClassificationID) {
	Competition comp = dao.getCompetition(Long.parseLong(competitionID),
		Long.parseLong(eventID), Long.parseLong(editionID),
		Long.parseLong(genderID), Long.parseLong(classID),
		Long.parseLong(phase1ID), Long.parseLong(phaseClassificationID));
	List<ResultRow> list = null;
	if (comp!= null){
	    list = rDao.getResults(comp);
	    if (list == null || list.size() == 0){
		String url = getURLClassificationResults(comp);
		list = readStageFromUCIWebResults(comp, url);
	    }
	}
	return list;
    }
    
    public List<ResultRow> reloadClassification(Competition comp){
	rDao.deleteCompetitionRows(comp);	
	String url = getURLClassificationResults(comp);
	return readStageFromUCIWebResults(comp, url);
    }
    
    public boolean tieneResultados(Competition comp){
	return rDao.getResults(comp).size()>0;
    }
    
    private String getURLOneDayResults(Competition comp) {
	String url = ONE_DAY_RESULTS_URL
		.replace(Contracts.SPORT_ID, comp.getSportID().toString())
		.replace(Contracts.COMPETITION_ID, comp.getCompetitionID().toString())
		.replace(Contracts.EDITION_ID, comp.getEditionID().toString())
		.replace(Contracts.GENDER_ID, String.valueOf(comp.getGenderID()))
		.replace(Contracts.CLASS_ID, String.valueOf(comp.getClassID()))
		.replace(Contracts.EVENT_PHASE_ID,
			comp.getEventPhaseID().toString());
	return url;
    }

    private String getURLStageResults(Competition comp) {
	StringBuilder sb = new StringBuilder();
	sb.append(Contracts.STAGE_URL
        		.replace(Contracts.SPORT_ID,
        			String.valueOf(comp.getSportID()))
			.replace(Contracts.COMPETITION_ID,
				String.valueOf(comp.getCompetitionID()))
			.replace(Contracts.EDITION_ID,
				String.valueOf(comp.getEditionID()))
			.replace(Contracts.EVENT_ID,
				String.valueOf(comp.getEventID()))
			.replace(Contracts.GENDER_ID,
				String.valueOf(comp.getGenderID()))
			.replace(Contracts.CLASS_ID,
				String.valueOf(comp.getClassID()))
			.replace(Contracts.PHASE1_ID,String.valueOf(comp.getPhase1ID()))
			);
	return sb.toString();
    }

    private String getURLClassificationResults(Competition comp) {
	StringBuilder sb = new StringBuilder();
   	sb.append(Contracts.CLASSIFICATIONS_URL
   		.replace(Contracts.SPORT_ID,
			String.valueOf(comp.getSportID()))
   		.replace(Contracts.COMPETITION_ID,
   			String.valueOf(comp.getCompetitionID()))
   		.replace(Contracts.EDITION_ID,
   			String.valueOf(comp.getEditionID()))
   		.replace(Contracts.EVENT_ID,
   			String.valueOf(comp.getEventID()))
   		.replace(Contracts.GENDER_ID,
   			String.valueOf(comp.getGenderID()))
   		.replace(Contracts.CLASS_ID,
   			String.valueOf(comp.getClassID()))
   		.replace(Contracts.PHASE_CLASSIFICATION_ID,
   			String.valueOf(comp.getPhaseClassificationID()))
   		.replace(Contracts.EVENT_PHASE_ID,
   			String.valueOf(comp.getEventPhaseID()))
   		);
   	return sb.toString();
    }
    
    private List<ResultRow> tratarXmlClassificationResult(String html, Competition comp) {
	Document doc = Jsoup.parse(html);
	Elements tableElements = doc.select("table.datatable");
	List<ResultRow> list = new ArrayList<ResultRow>();
//
//	Elements tableRowElements = tableElements.select(":not(thead) tr");
//
//	boolean isTeamTimeTrial = true;
//	// headers
//	for (int i = 0; i < tableRowElements.size(); i++) {
//	    Element row = tableRowElements.get(i);
//	    if (!row.attr("valign").equals("top")) {// los que no tienen
//						    // valing=top son headers
//		Elements rowItems = row.select("td.caption");
//		if (rowItems.get(3).text().equals("Team")) {
//		    isTeamTimeTrial = false;
//		    break;
//		}
//	    }
//	}
//
//	// Date Event Nat. Class Winner
//	for (int i = 0; i < tableRowElements.size(); i++) {
//	    Element row = tableRowElements.get(i);
//	    if (row.attr("valign").equals("top")) {// los que no tienen
//						   // valing=top son headers
//		Elements rowItems = row.select("td");
//		int idx = 0;
//		ResultRow.Builder builder;
//		if (isTeamTimeTrial) {
//		    builder = new ResultRow.Builder()
//			    .setRank(rowItems.get(idx++).text())
//			    .setName(rowItems.get(idx++).text())
//			    .setNat(rowItems.get(idx++).text())
//			    .setAge(rowItems.get(idx++).text())
//			    .setResult(rowItems.get(idx++).text());
//		} else {
//		    builder = new ResultRow.Builder()
//			    .setRank(rowItems.get(idx++).text())
//			    .setName(rowItems.get(idx++).text())
//			    .setNat(rowItems.get(idx++).text())
//			    .setTeam(rowItems.get(idx++).text())
//			    .setAge(rowItems.get(idx++).text())
//			    .setResult(rowItems.get(idx++).text());
//		}
//
//		if (rowItems.size() > 6) {
//		    builder.setPaR(rowItems.get(idx++).text())
//		    	.setPcR(rowItems.get(idx++).text());
//		}
//
//		ResultRow odr = builder.build();
//		odr.setCompetition(comp);
//		persistResultRow(odr);
//		log.debug(odr.toString());
//		if (odr.getRank() != null && !odr.getRank().isEmpty()) {
//		    list.add(odr);
//		}
//	    }
//	}
	return list;
    }
}
