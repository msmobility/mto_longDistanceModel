package de.tum.bgu.msm.dataAnalysis;

import de.tum.bgu.msm.dataAnalysis.surveyModel.*;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Joe on 16/08/2016.
 */
public class DatabaseInteractions {

    private final static Logger logger = Logger.getLogger(DatabaseInteractions.class);

    public static Connection getPostgresConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost/canada";
        String user = "postgres";
        String password = "postgres";

        return DriverManager.getConnection(url, user, password);
    }

    public static void loadPersonsToDb(mtoSurveyData data) {
        try (Connection conn = getPostgresConnection()){

            String stm = "INSERT INTO tsrc_person(REFYEARP, REFMTHP, PUMFID, WTPM, WTPM2, " +
                    "RESPROV,RESCD2, RESCMA2, AGE_GR2, SEX, EDLEVGR, LFSSTATG, INCOMGR2, " +
                    "G_ADULTS, G_KIDS, TRIP_CNT, ON_CNT,SD_CNT, TRIPCTOT)" +
                    "VALUES (?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?);";
            PreparedStatement pst = conn.prepareStatement(stm);

            for (surveyPerson p : data.getPersons()) {
                int i=1;
                pst.setInt(i++, p.getRefYear());
                pst.setInt(i++, p.getRefMonth());
                pst.setLong(i++, p.getPumfId());
                pst.setFloat(i++, p.getWeight());
                pst.setFloat(i++, p.getWeight2());
                pst.setInt(i++, p.getProv());

                int full_cd = p.getCd() == 999 ? 9999 : p.getProv()*100 + p.getCd();
                pst.setInt(i++, full_cd);

                pst.setInt(i++, p.getCma());
                pst.setInt(i++, p.getAgeGroup());
                pst.setInt(i++, p.getGender());
                pst.setInt(i++, p.getEducation());
                pst.setInt(i++, p.getLaborStat());
                pst.setInt(i++, p.getHhIncome());
                pst.setInt(i++, p.getAdultsInHh());
                pst.setInt(i++, p.getKidsInHh());
                pst.setInt(i++, p.getTripfile_count());
                pst.setInt(i++, p.getOvernight_count());
                pst.setInt(i++, p.getSameday_count());
                pst.setInt(i++, p.getTotal_count());
                pst.addBatch();
            }
            pst.executeBatch(); //batching is so much faster!!

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex.getNextException());

        }
    }

    public static void loadTripsToDb(mtoSurveyData data) throws ClassNotFoundException, UnsupportedEncodingException {

        String variables  = "refyear, refmth, pumfid, tripid, quarter, orcprovt, orccdt2, orccmat2, "
                + "mddplfl, mdccd, mdccma2, wtep, wttp, age_gr2, sex, edlevgr, lfsstatg,"
                + " incomgr2, tp_d01, t_g0802, tr_g08, tp_g02, mrdtrip2, mrdtrip3, dist2, "
                + "tmdtype2, cannite, trip_cnt, tripctot, tr_d11, triptype, geom";

        try (Connection conn = getPostgresConnection()){

            Statement stmt = conn.createStatement();
            //when this doesn't work, need to repair Visual C++ Redistributable Packages for Visual Studio 2013

            String stm = "INSERT INTO tsrc_trip(" + variables + ")" +
                    "VALUES (?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ST_GeomFromText(?, 4269));";
            PreparedStatement pst = conn.prepareStatement(stm);
            Stream<surveyTour> allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());

            conn.setAutoCommit(false);

            for (surveyTour t : allTours.collect(Collectors.toList())) {
                int i = 1;
                pst.setInt(i++, t.getPerson().getRefYear());
                pst.setInt(i++, t.getPerson().getRefMonth());
                pst.setLong(i++, t.getPerson().getPumfId());
                pst.setInt(i++, t.getTripId());
                pst.setInt(i++, t.getQuarter());
                pst.setInt(i++, t.getOrigProvince());

                int full_orig_cd = t.getOrigCD() == 999 ? 9999 : t.getOrigProvince() *100 + t.getOrigCD();
                pst.setInt(i++, full_orig_cd);

                pst.setInt(i++, t.getOrigCma());
                pst.setInt(i++, t.getDestProvince());

                int full_dest_cd = t.getDestCD() == 999 ? 9999 : t.getDestProvince() *100 + t.getDestCD();
                pst.setInt(i++, full_dest_cd);

                pst.setInt(i++, t.getDestCma());
                pst.setDouble(i++, t.getHHWeight());
                pst.setDouble(i++, t.getWeight());
                pst.setInt(i++, t.getPerson().getAgeGroup());
                pst.setInt(i++, t.getPerson().getGender());
                pst.setInt(i++, t.getPerson().getEducation());
                pst.setInt(i++, t.getPerson().getLaborStat());
                pst.setInt(i++, t.getPerson().getHhIncome());
                pst.setInt(i++, t.getPartySize());
                pst.setInt(i++, t.getNumHhMembersOnTrip());
                pst.setInt(i++, t.getNumHhAdultsOnTrip());
                pst.setInt(i++, t.getNumHhKidsOnTrip());
                pst.setInt(i++, t.getTripPurp());
                pst.setInt(i++, 0);
                pst.setInt(i++, t.getDistance());
                pst.setInt(i++, t.getMainMode());
                pst.setInt(i++, t.getNumberNights());
                pst.setInt(i++, 0);
                pst.setInt(i++, 0);
                pst.setInt(i++, t.getNumIdenticalTrips());
                pst.setInt(i++, t.getTripType());
                String geom = "LineStringFromText('" + t.getLineString().toText() + "', 4269 )";
                //logger.info(geom);
                pst.setString(i++, t.getLineString().toText());

                pst.addBatch();
            }
            logger.info("executing batch insert");
            pst.executeBatch(); //batching is so much faster!!
            logger.info("committing...");
            conn.commit(); //needed to turn off auto commit to speed up inserts

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex.getNextException());

        }
    }

    public static void loadVisitsToDb(mtoSurveyData data) {
        try (Connection conn = getPostgresConnection()){

            String stm = "INSERT INTO tsrc_visit(REFYEAR, refmonth, PUMFID, TRIPID, VISITID, VPROV, VCD2," +
                    "VCMA2, AC_Q04, vsirecfl, airflag, aircode2)" +
                    "VALUES (?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?);";
            PreparedStatement pst = conn.prepareStatement(stm);

            for (surveyTour t : data.getPersons()
                    .stream()
                    .flatMap(p -> p.getTours().stream()).collect(Collectors.toList())) {
                for (SurveyVisit v : t.getStops()) {
                    int i=1;
                    pst.setInt(i++, t.getPerson().getRefYear());
                    pst.setInt(i++, t.getPerson().getRefMonth());
                    pst.setLong(i++, t.getPerson().getPumfId());
                    pst.setInt(i++, t.getTripId());
                    pst.setInt(i++, v.visitId);
                    pst.setInt(i++, v.province);
                    pst.setInt(i++, v.cd);
                    pst.setInt(i++, v.cma);
                    pst.setInt(i++, v.nights);
                    pst.setInt(i++, v.visitIdentification);
                    pst.setInt(i++, v.visitAirport ? 1 : 0);
                    pst.setString(i++, v.airport);
                    pst.addBatch();
                }

            }
            pst.executeBatch(); //batching is so much faster!!

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex.getNextException());

        }
    }

    public void build_zone_lvl2_mapping() {
        try (Connection conn = getPostgresConnection()){
            conn.prepareStatement("DROP TABLE IF EXISTS zone_lvl2; ").execute();
            conn.prepareStatement("CREATE TABLE zone_lvl2(pr integer, cd integer, cma integer, zone_lvl2 numeric);").execute();
            conn.setAutoCommit(false);
            PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO zone_lvl2 VALUES (?,?,?, ?)");
            conn.prepareStatement("INSERT INTO zone_lvl2 SELECT distinct ;").execute();


            conn.commit();
            conn.setAutoCommit(true);
            conn.prepareStatement("CREATE INDEX zone_lvl2_idx on zone_lvl2(pr, cd, cma);").execute();


        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex.getNextException());

        }
    }


}

