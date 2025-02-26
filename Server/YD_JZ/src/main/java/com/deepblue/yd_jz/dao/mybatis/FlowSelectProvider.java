package com.deepblue.yd_jz.dao.mybatis;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;

import java.util.Date;

@Slf4j
public class FlowSelectProvider {

    public String getFlowByMain(@Param("handle") int handle, int order
            , @Param("date") String date, @Param("username") String username) {
        String sqlStr = new SQL() {
            {
                SELECT("flow.id,flow.f_date,flow.money,flow.collect,flow.exempt,flow.note,a.handle,a.h_name,ac.a_name,acc.a_name t_a_name,t.t_name,t2.t_name p_t_name,ac.username t_a_username");
                FROM("flow");
                LEFT_OUTER_JOIN("action a on flow.action_id = a.id");
                LEFT_OUTER_JOIN("type t on flow.type_id = t.id");
                LEFT_OUTER_JOIN("type t2 on t.parent = t2.id");
                LEFT_OUTER_JOIN("account ac on flow.account_id = ac.id");
                LEFT_OUTER_JOIN("account acc on flow.account_to_id = acc.id");
                if(username!=null&&!username.isEmpty() && !"all".equals(username)) {
                    WHERE("ac.username=#{username}");
                }
                if (handle >= 3) {
                    WHERE("a.handle < #{handle}", "flow.f_date like #{date}");
                } else {
                    WHERE("a.handle = #{handle}", "flow.f_date  like #{date}");
                }
                if (order == 0) {
                    ORDER_BY("flow.f_date desc ");
                } else {
                    ORDER_BY("flow.money+0 desc ");
                }

            }
        }.toString();
        log.info("method: getFlowByMain\n"+sqlStr);
        return sqlStr;
    }

    public String getFlowByAccount(@Param("handle") int handle, @Param("accid") int accId
            , @Param("date") String date) {
        String sqlStr = new SQL() {
            {
                SELECT("flow.id,flow.f_date,flow.money,flow.collect,flow.exempt,flow.note,a.handle,a.h_name,ac.a_name,ac.id,acc.a_name t_a_name,t.t_name,t2.t_name p_t_name");
                FROM("flow");
                LEFT_OUTER_JOIN("action a on flow.action_id = a.id");
                LEFT_OUTER_JOIN("type t on flow.type_id = t.id");
                LEFT_OUTER_JOIN("type t2 on t.parent = t2.id");
                LEFT_OUTER_JOIN("account ac on flow.account_id = ac.id");
                LEFT_OUTER_JOIN("account acc on flow.account_to_id = acc.id");

                if (handle >= 3) {
                    WHERE("a.handle < #{handle}", "flow.f_date like #{date}","flow.account_id=#{accid}");
                } else {
                    WHERE("a.handle = #{handle}", "flow.f_date  like #{date}","flow.account_id=#{accid}");
                }
                ORDER_BY("flow.f_date desc ");
            }
        }.toString();
        log.info( "method:  getFlowByAccount\n"+sqlStr);
        return sqlStr;
    }

    public String getFlowByScreen(int handle, int account, String startDate, String endDate, boolean isSingleMonth, boolean isCollect, String note) {
        StringBuilder sql = new StringBuilder("SELECT " +
                "flow.id, flow.f_date AS flowDate, flow.money, flow.collect, flow.exempt, flow.note," +
                "a.handle, a.h_name AS handleName, a.id AS actionId," +
                "ac.a_name AS accountName, ac.id AS accountId, acc.a_name AS toAccountName," +
                "t.t_name AS typeName, t.id AS typeId, t2.t_name AS parentTypeName, t2.id AS parentTypeId\n");
        sql.append("FROM flow\n");
        sql.append("LEFT OUTER JOIN action a ON flow.action_id = a.id\n");
        sql.append("LEFT OUTER JOIN type t ON flow.type_id = t.id\n");
        sql.append("LEFT OUTER JOIN type t2 ON t.parent = t2.id\n");
        sql.append("LEFT OUTER JOIN account ac ON flow.account_id = ac.id\n");
        sql.append("LEFT OUTER JOIN account acc ON flow.account_to_id = acc.id\n");
        sql.append("WHERE a.handle ").append(handle == 3 ? "<" : "=").append(handle).append("\n");

        if (account > 0) {
            sql.append("AND (flow.account_id = ").append(account).append(" OR flow.account_to_id = ").append(account).append(")\n");
        }

        if (isSingleMonth) {
            String likeDate = startDate.substring(0, 7);
            sql.append("AND flow.f_date LIKE '").append(likeDate).append("%'\n");
        } else {
            if (!"null".equals(startDate) && !"".equals(startDate)) {
                sql.append("AND flow.f_date >= '").append(startDate).append("'\n");
            }

            if (!"null".equals(endDate) && !"".equals(endDate)) {
                sql.append("AND flow.f_date <= '").append(endDate).append("'\n");
            }
        }

        if (isCollect) {
            sql.append("AND flow.collect = 1\n");
        }

        if (note != null) {
            // Trim the note and check if it's not empty
            String trimmedNote = note.trim();
            if (!trimmedNote.isEmpty()) {
                sql.append("AND flow.note LIKE '%").append(trimmedNote).append("%'\n");
            }
        }

        sql.append("ORDER BY flow.f_date DESC");
        log.info("method: getFlowByScreen\n" + sql);
        return sql.toString();
    }


    public String getYearlySummary(@Param("year")int year) {
        String sql =  new SQL() {{
            SELECT("SUM(CASE WHEN a.handle = 0 THEN f.money ELSE 0 END) AS totalEarns",
                    "SUM(CASE WHEN a.handle = 1 THEN f.money ELSE 0 END) AS totalCosts",
                    "SUM(CASE WHEN a.handle = 0 THEN f.money ELSE 0 END) - SUM(CASE WHEN a.handle = 1 THEN f.money ELSE 0 END) AS totalBalance");
            FROM("flow f");
            JOIN("action a ON f.action_id = a.id");
            WHERE("YEAR(f.f_date) = #{year}");
        }}.toString();
        log.info("method: getYearlySummary\n"+sql);
        return sql;
    }

    public String getFlowsTypeByStartMonthAndEndMonth(String startMonth, String endMonth) {
        String sql = "SELECT t.id, t.t_name, t.parent, SUM(CAST(f.money AS DECIMAL(10, 2))) AS money, tp.t_name as parentName \n" +
                "FROM flow f \n" +
                "JOIN type t ON f.type_id = t.id \n" +
                "LEFT JOIN type tp ON t.parent = tp.id \n";

        if (endMonth == null || endMonth.isEmpty()) {
            sql += "WHERE f.f_date LIKE '" + startMonth + "%' AND f.account_to_id = 0 \n";
        } else {
            sql += "WHERE f.f_date BETWEEN '" + startMonth + "' AND '" + endMonth + "' AND f.account_to_id = 0 \n";
        }

        sql += "GROUP BY t.id, t.t_name, t.parent, tp.t_name\n";
        log.info("method: getFlowsTypeByStartMonthAndEndMonth\n" + sql);
        return sql;
    }


}
