package de.tum.bgu.msm.dataAnalysis;

import de.tum.bgu.msm.dataAnalysis.surveyModel.*;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;
import org.sqlite.SQLiteConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Joe on 16/08/2016.
 */
public class DatabaseInteractions {

    private final static Logger logger = Logger.getLogger(DatabaseInteractions.class);

    public static void loadPersonsToDb(mtoSurveyData data) {
        String url = "jdbc:postgresql://localhost/spatial";
        String user = "postgres";
        String password = "postgres";

        try (Connection con = DriverManager.getConnection(url, user, password)){
            String stm = "INSERT INTO mto.TSRC_person(REFYEARP, REFMTHP, PUMFID, WTPM, WTPM2, " +
                    "RESPROV,RESCD2, RESCMA2, AGE_GR2, SEX, EDLEVGR, LFSSTATG, INCOMGR2, " +
                    "G_ADULTS, G_KIDS, PROXY, TRIP_CNT, ON_CNT,SD_CNT, TRIPCTOT)" +
                    "VALUES (?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?);";
            PreparedStatement pst = con.prepareStatement(stm);

            for (surveyPerson p : data.getPersons()) {
                pst.setInt(1, p.getRefYear());
                pst.setInt(2, p.getRefMonth());
                pst.setLong(3, p.getPumfId());
                pst.setFloat(4, p.getWeight());
                pst.setFloat(5, p.getWeight2());
                pst.setInt(6, p.getProv());
                pst.setInt(7, p.getProv()*100 + p.getCd());
                pst.setInt(8, p.getCma());
                pst.setInt(9, p.getAgeGroup());
                pst.setInt(10, p.getGender());
                pst.setInt(11, p.getEducation());
                pst.setInt(12, p.getLaborStat());
                pst.setInt(13, p.getHhIncome());
                pst.setInt(14, p.getAdultsInHh());
                pst.setInt(15, p.getKidsInHh());
                pst.setBoolean(16, false);
                pst.setInt(17, p.getTripfile_count());
                pst.setInt(18, p.getOvernight_count());
                pst.setInt(19, p.getSameday_count());
                pst.setInt(20, p.getTotal_count());
                pst.addBatch();
            }
            pst.executeBatch(); //batching is so much faster!!

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

        }
    }

    public static void loadTripsToDb(mtoSurveyData data) throws ClassNotFoundException, UnsupportedEncodingException {
        Class.forName("org.sqlite.JDBC");
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);


        String variables  = "refyear, refmth, pumfid, tripid, quarter, orcprovt, orccdt2, orccmat2, "
                + "mddplfl, mdccd, mdccma2, wtep, wttp, age_gr2, sex, edlevgr, lfsstatg,"
                + " incomgr2, tp_d01, t_g0802, tr_g08, tp_g02, mrdtrip2, mrdtrip3, dist2, "
                + "tmdtype2, cannite, trip_cnt, tripctot, tr_d11, triptype, geometry";


        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:input/canada.sqlite",
                config.toProperties())
        ){
            Statement stmt = conn.createStatement();
            String dll_location = System.getProperty("java.library.path") + File.separator + "mod_spatialite.dll";
            stmt.execute(String.format("SELECT load_extension('%s')", "mod_spatialite.dll"));
            //when this doesn't work, need to repair Visual C++ Redistributable Packages for Visual Studio 2013

            String stm = "INSERT INTO tsrc_trip(" + variables + ")" +
                    "VALUES (?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, GeomFromText(?, 4269));";
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
                pst.setInt(i++, t.getOrigProvince() *100 + t.getOrigCD());
                pst.setInt(i++, t.getOrigCma());
                pst.setInt(i++, t.getDestProvince());
                pst.setInt(i++, t.getDestProvince() *100 + t.getDestCD());
                pst.setInt(i++, t.getDestCma());
                pst.setDouble(i++, 0);
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
            logger.error(ex.getMessage(), ex);

        }
    }

}

