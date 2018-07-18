package com.autonomy.find.api.controllers.admin;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/api/admin/datatable.json")
public class AdminDatatable {


    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserController.class);

    @Autowired
    @Qualifier("findSessionFactory")
    private SessionFactory findSessionFactory;

    @RequestMapping(method= RequestMethod.GET)
    public @ResponseBody
    HttpServletResponse getDatatableData(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final HttpSession session) {
        final Session db = findSessionFactory.openSession();

//        result = db.createQuery("from NamedUser where username in :usernames")
//                .setParameterList("usernames", userNames)
//                .list();



        String[] cols = {"username"};
        String table = "NamedUser";

        JSONObject result = new JSONObject();
        JSONArray array = new JSONArray();
        int amount = 10;
        int start = 0;
        int echo = 0;
        int col = 0;

        String id = "";
        String name = "";
        String dateAdded = "";
        String accountType = "";
        String roles = "";

        String dir = "asc";
        String sStart = request.getParameter("iDisplayStart");
        String sAmount = request.getParameter("iDisplayLength");
        String sEcho = request.getParameter("sEcho");
        String sCol = request.getParameter("iSortCol_0");
        String sdir = request.getParameter("sSortDir_0");

        id = request.getParameter("sSearch_0");

        List<String> sArray = new ArrayList<String>();
        if (!id.equals("")) {
            String sEngine = " id like '%" + id + "%'";
            sArray.add(sEngine);
            //or combine the above two steps as:
            //sArray.add(" engine like '%" + engine + "%'");
            //the same as followings
        }
        if (!name.equals("")) {
            String sBrowser = " name like '%" + name + "%'";
            sArray.add(sBrowser);
        }
        if (!dateAdded.equals("")) {
            String sPlatform = " dateAdded like '%" + dateAdded + "%'";
            sArray.add(sPlatform);
        }
        if (!accountType.equals("")) {
            String sVersion = " accountType like '%" + accountType + "%'";
            sArray.add(sVersion);
        }
        if (!roles.equals("")) {
            String sGrade = " roles like '%" + roles + "%'";
            sArray.add(sGrade);
        }

        String individualSearch = "";
        if(sArray.size()==1){
            individualSearch = sArray.get(0);
        }else if(sArray.size()>1){
            for(int i=0;i<sArray.size()-1;i++){
                individualSearch += sArray.get(i)+ " and ";
            }
            individualSearch += sArray.get(sArray.size()-1);
        }

        if (sStart != null) {
            start = Integer.parseInt(sStart);
            if (start < 0)
                start = 0;
        }
        if (sAmount != null) {
            amount = Integer.parseInt(sAmount);
            if (amount < 10 || amount > 100)
                amount = 10;
        }
        if (sEcho != null) {
            echo = Integer.parseInt(sEcho);
        }
//        if (sCol != null) {
//            col = Integer.parseInt(sCol);
//            if (col < 0 || col > 5)
//                col = 0;
//        }
//        if (sdir != null) {
//            if (!sdir.equals("asc"))
//                dir = "desc";
//        }
        String colName = cols[0];
        int total = 0;
        db.beginTransaction();
        List rs = null;
        try {
            String sql = "SELECT count(*) FROM "+table;
            rs = db.createQuery(sql).list();
            total = Integer.parseInt((String)rs.get(0));

        }catch(Exception e){

        }

        int totalAfterFilter = total;

        try {
            result.put("sEcho",echo);
            String searchSQL = "";
            String sql = "FROM "+table;
            String searchTerm = request.getParameter("sSearch");
            String globeSearch =  " where (username like '%"+searchTerm+"%')";
            if(searchTerm!=""&&individualSearch!=""){
                searchSQL = globeSearch + " and " + individualSearch;
            }
            else if(individualSearch!=""){
                searchSQL = " where " + individualSearch;
            }else if(searchTerm!=""){
                searchSQL=globeSearch;
            }
            sql += searchSQL;
            sql += " order by " + colName + " " + dir;
            sql += " limit " + start + ", " + amount;
            List fullResult = db.createQuery(sql).list();
            LOGGER.warn(fullResult.toString());
           /* while (rs.next()) {
                JSONArray ja = new JSONArray();
//                fullResult.contains()
//                ja.put(rs.getString("engine"));
//                ja.put(rs.getString("browser"));
//                ja.put(rs.getString("platform"));
//                ja.put(rs.getString("version"));
//                ja.put(rs.getString("grade"));
                array.put(ja);
            }*/
            String sql2 = "SELECT count(*) FROM "+table;
            if (searchTerm != "") {
                sql2 += searchSQL;
//                PreparedStatement ps2 = conn.prepareStatement(sql2);
//                ResultSet rs2 = ps2.executeQuery();
//                if (rs2.next()) {
//                    totalAfterFilter = rs2.getInt("count(*)");
//                }
            }
            result.put("iTotalRecords", total);
            result.put("iTotalDisplayRecords", totalAfterFilter);
            result.put("aaData", array);
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "no-store");
            System.out.print(result);
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

}
