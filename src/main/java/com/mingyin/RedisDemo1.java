package com.mingyin;
import redis.clients.jedis.Jedis;
import java.util.*;
public class RedisDemo1 {

    private static final int ARTICLES_PER_PAGE = 5;

    public static void main(String[] args) {
        new RedisDemo1().run();
    }

    public void run() {
        Jedis conn = new Jedis("localhost");
        conn.select(11);

        String articleId = postArticle(
                conn, "username2", "A title2", "http://www.google.com");
        System.out.println("We posted a new article with id: " + articleId);

        String Id = "1";
        articleVote(conn, "other_user", "article:" + Id);

        List<Map<String,String>> articles2 = getArticles(conn, 1);
        printArticles(articles2);
    }


    public String postArticle(Jedis conn, String user, String title, String link) {
        String articleId = String.valueOf(conn.incr("article:"));

        String voted = "voted:" + articleId;
        conn.sadd(voted, user);

        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;
        HashMap<String,String> articleData = new HashMap<String,String>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");

        conn.hmset(article, articleData);
//        conn.zadd("time:", now, article);
        conn.zadd("voted:",1,article);

        return articleId;
    }

    public void articleVote(Jedis conn, String user, String article) {    //给文章投票
        String articleId = article.substring(article.indexOf(':') + 1);
        if (conn.sadd("voted:" + articleId, user) == 1) {

            conn.zincrby("voted:",1,article);
            conn.hincrBy(article, "votes", 1);
//            conn.zincrby("score",1,article);
//            conn.hincrBy(article, "votes", 1);
        }
    }

    public List<Map<String,String>> getArticles(Jedis conn, int page) {    //获取文章列表，分页查询
        int start = (page - 1) * ARTICLES_PER_PAGE;//定义分页开始结束序号
        int end = start + ARTICLES_PER_PAGE - 1;
        //从order（time：或score：）中取出全部文章ID

        Set<String> ids = conn.zrevrange("voted:", start, end);

        //定义一个链表，存储要取出的全部文章
        List<Map<String,String>> articles = new ArrayList<Map<String,String>>();
        for (String id : ids){
            //从对应文章表中取出文章数据（hgetAll为取出全部字段）
            Map<String,String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        }

        return articles;
    }
    private void printArticles(List<Map<String,String>> articles){
        for (Map<String,String> article : articles){
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String,String> entry : article.entrySet()){
                if (entry.getKey().equals("id")){
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
