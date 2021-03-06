package an.dpr.cyclingresultsapi.services.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import an.dpr.cyclingresultsapi.bo.CompetitionsBO;
import an.dpr.cyclingresultsapi.domain.Competition;
import an.dpr.cyclingresultsapi.util.Contracts;

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
 * 	-find by category (world, europe, asia ...) 
 */

@Path("/competitions/")
public class CompetitionRS {

    
    private static final Logger log = LoggerFactory.getLogger(CompetitionRS.class);
    
    @Inject
    private CompetitionsBO ejb;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/query/{initDate},{finDate},{sportID},{genderID},{classID},{competitionClass}")
    public List<Competition> getCompetitions(
	    @PathParam("initDate") String initDate,
	    @PathParam("finDate") String finDate,
	    @PathParam("sportID") String sportID,
	    @PathParam("genderID") String genderID,
	    @PathParam("classID") String classID,
	    @PathParam("competitionClass") String competitionClass){
	return ejb.getCompetitions(initDate, finDate, sportID, genderID, classID, competitionClass);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/next/")
    public List<Competition> getNextCompetitions() {
	return ejb.getNextCompetitions();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/year/{year}")
    public List<Competition> getYearCompetitions(@PathParam("year") String year) {
	return ejb.getYearCompetitions(year);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/month/{year},{month}")
    public List<Competition> getMonthCompetitions(
	    @PathParam("year") String year,
	    @PathParam("month") String month
	    ) {
	return ejb.getMonthCompetitions(year, month);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/allEditions/{competitionID}")
    public List<Competition> getAllEditions(@PathParam("competitionID") String competitionID){
	return ejb.getAllEditions(competitionID);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/stageRaceCompetitions/{competitionID},{eventID},{editionID},{genderID},{classID}")
    public List<Competition> getStageRaceCompetitionsService(
	    @PathParam("competitionID") String competitionID,
	    @PathParam("eventID") String eventID,
	    @PathParam("editionID") String editionID,
	    @PathParam("genderID") String genderID,
	    @PathParam("classID") String classID
	    ) {
	return ejb.getStageRaceCompetitionsService(competitionID, eventID, editionID, genderID, classID);
    }
	

    /**
     * Last competitions finished or in course.
     * @param initDate and finishDate in format yyyymmdd
     * @return List<Competition>
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/loadCompetitions/{genderID},{classID},{initDate},{finishDate}")
    public Boolean loadCompetitionsService(
	    @PathParam("genderID") String  genderID,
	    @PathParam("classID") String classID,
	    @PathParam("initDate") String initDate,
	    @PathParam("finishDate") String finishDate
	    ) {
	return ejb.loadCompetitionsService(Contracts.SPORT_ID_ROAD, genderID, classID, initDate, finishDate);
    }
    
    /**
     * Last competitions finished or in course.
     * @param initDate and finishDate in format yyyymmdd
     * @return List<Competition>
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/loadCompetitions/{sportID},{genderID},{classID},{initDate},{finishDate}")
    public Boolean loadCompetitionsService(
	    @PathParam("sportID") String  sportID,
	    @PathParam("genderID") String  genderID,
	    @PathParam("classID") String classID,
	    @PathParam("initDate") String initDate,
	    @PathParam("finishDate") String finishDate
	    ) {
	return ejb.loadCompetitionsService(sportID, genderID, classID, initDate, finishDate);
    }
    
    /**
     * Last competitions finished or in course.
     * @param initDate and finishDate in format yyyymmdd
     * @return List<Competition>
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/loadCompetitionDetails/{competitionID},{eventID},{editionID},{genderID},{classID}")
    public Boolean loadCompetitionService(
	    @PathParam("competitionID") String  competitionID,
	    @PathParam("eventID") String eventID,
	    @PathParam("editionID") String editionID,
	    @PathParam("genderID") String  genderID,
	    @PathParam("classID") String classID
	    ) {
	return ejb.loadCompetitionService(competitionID, eventID, editionID, genderID, classID);
    }
}
