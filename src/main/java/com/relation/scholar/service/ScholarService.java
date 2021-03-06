package com.relation.scholar.service;
import com.relation.pager.PageBean;
import com.relation.pager.sqlExpression;
import com.relation.scholar.dao.ScholarDao;
import com.relation.scholar.domain.Scholar;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by T.Cage on 2017/1/21.
 */
public class ScholarService {
    private ScholarDao scholarDao=new ScholarDao();
    public PageBean<Scholar>findByAdvisee(String advisee, int pc){
        try {
            return scholarDao.findByAdvisee(advisee,pc);
        } catch (SQLException e) {
            throw new RuntimeException();
        }
    }
    public Scholar getScholarInfo(int advisee_id){
        try {
            return scholarDao.getScholarInfoByName(advisee_id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 得到学者的多层关系树
     * @param level
     * @param advisor
     * @return
     */
    public JSONObject getMentorTree(int level, String advisor){
        JSONObject tree=new JSONObject();
        if(1>=level)
        {
            tree.put("name", advisor);
            List<JSONObject> lsjo=new LinkedList<JSONObject>();
            for(String as: scholarDao.getMentorship(advisor))
                lsjo.add(JSONObject.fromObject("{name:\""+as+"\"}"));
            tree.put("children", lsjo);
            return tree;
        }
        else
        {
            tree.put("name", advisor);
            List<JSONObject>lsjo=new LinkedList<JSONObject>();
            for(String as: scholarDao.getMentorship(advisor))
                lsjo.add(getMentorTree(--level,as));
            tree.put("children", lsjo);
            return tree;
        }
    }
    /**
     * 得到学者合作网
     * @param level
     * @param advisor
     * @return
     * @throws SQLException
     */
    public JSONObject getCollaboratorNet(int level,String advisor) {
        List<sqlExpression>exprList=new ArrayList<sqlExpression>();
        exprList.add(new sqlExpression("all_author","like","%#"+advisor+"#% or all_author like"+advisor+"#%"));
        List<Map<String, Object>> mapList= null;
        try {
            mapList = scholarDao.getCollaborator(advisor);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Creat Link&nodes list
        List<Map<String,Object>> link=new ArrayList<Map<String, Object>>();
        List<Map<String,Object>> nodes=new ArrayList<Map<String, Object>>();

        for(Map<String, Object> map:mapList){
            String[] tmp=map.get("all_author").toString().split("#");
            for(String name:tmp){
                if(!advisor.equals(name))
                {
                    Map<String, Object>map1=new HashMap<String,Object>();
                    map1.put("id",name);
                    map1.put("group",5);
                    if(!nodes.contains(map1)){
                        nodes.add(map1);
                    }
                }
            }

            List<Map<String,Object>>tmp_link=scholarDao.getCopLinkList(tmp);
            //System.out.println(tmp_link);
            //去重（未考虑键值互换）value求值

            link.removeAll(tmp_link);
            link.addAll(tmp_link);
        }
        Map<String, Object>map2=new HashMap<String,Object>();
        map2.put("id",advisor);
        map2.put("group",1);
        nodes.add(map2);

        JSONObject jsonObject=new JSONObject();
        jsonObject.put("nodes", JSONArray.fromObject(nodes));
        jsonObject.put("links",JSONArray.fromObject(link));
        System.out.println(jsonObject);
        return jsonObject;

    }
    public JSONArray getDetail(int advisee_id){
        Map map=scholarDao.getDetail(advisee_id);

        JSONArray paperArray=new JSONArray();
        JSONArray advisorArray=new JSONArray();
        JSONArray colArray=new JSONArray();
        JSONArray detailArray=new JSONArray();

        String paper=(String) map.get("paper_detail");
        String advisor=(String) map.get("advisor_cop_detail");
        String col=(String) map.get("col_cop_detail");
        String[] paper_year=paper.split(",");
        String[] advisee_year=advisor.split(",");
        String[] col_year=col.split(",");

        for(String y:paper_year){
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("year",y.substring(0,4));
            jsonObject.put("number",Integer.parseInt(y.substring(5)));
            paperArray.add(jsonObject);
        }

        for(String y:advisee_year){
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("year",y.substring(0,4));
            jsonObject.put("number",Integer.parseInt(y.substring(5)));
            advisorArray.add(jsonObject);
        }

        int count=0;
        for(String y:col_year){
            if(count<=20){
                count++;
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("coworker",y.substring(0,y.indexOf(":")));
                jsonObject.put("times",Integer.parseInt(y.substring(y.indexOf(":")+1)));
                colArray.add(jsonObject);
            }
            else break;
        }


        detailArray.add(paperArray);
        detailArray.add(advisorArray);
        detailArray.add(colArray);

        return detailArray;//第一个jsonObeject是开始年份和总论文数
    }

    public JSONObject getTree(String advisor,int advisee_id){
        JSONObject jsonObject=new JSONObject();
        Object o=scholarDao.getTree(advisee_id).get("advisee_tree");
        if(o==null){
            jsonObject=getMentorTree(3,advisor);
            scholarDao.setTree(jsonObject,advisee_id);
            return jsonObject;
        }
        else {
            return JSONObject.fromObject(o);
        }
    }
    public JSONObject getNet(String advisor,int advisee_id){
        JSONObject jsonObject=new JSONObject();
        Object o=scholarDao.getNet(advisee_id).get("col_net");
        if(o==null){
            jsonObject=getCollaboratorNet(1,advisor);
            scholarDao.setNet(jsonObject,advisee_id);
            return jsonObject;
        }
        else {
            return JSONObject.fromObject(o);
        }
    }
    @Test
    public void getNetTest(){
        System.out.println(getTree("feng xia",762695));
    }
    @Test
    public void MentorTreeTest(){
        System.out.println(getMentorTree(3,"feng xia"));
    }
    @Test
    public void copNetTest() throws SQLException {
        getCollaboratorNet(2,"Sanjeev Saxena");
    }
    @Test
    public void detailTest(){
        System.out.println(getDetail(762695));
    }
}
