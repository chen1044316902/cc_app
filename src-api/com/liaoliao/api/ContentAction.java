package com.liaoliao.api;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.liaoliao.common.service.CommonService;
import com.liaoliao.content.entity.Article;
import com.liaoliao.content.entity.ArticleComment;
import com.liaoliao.content.entity.KeyWords;
import com.liaoliao.content.entity.Likes;
import com.liaoliao.content.entity.OriginalArticleInfo;
import com.liaoliao.content.entity.OriginalVideoInfo;
import com.liaoliao.content.entity.Video;
import com.liaoliao.content.entity.VideoComment;
import com.liaoliao.content.service.ArticleCommentService;
import com.liaoliao.content.service.ArticleService;
import com.liaoliao.content.service.KeyWordsService;
import com.liaoliao.content.service.LikesService;
import com.liaoliao.content.service.OriginalArticleInfoService;
import com.liaoliao.content.service.OriginalVideoInfoService;
import com.liaoliao.content.service.VideoCommentService;
import com.liaoliao.content.service.VideoService;
import com.liaoliao.profit.service.FenrunLogService;
import com.liaoliao.redisclient.RedisService;
import com.liaoliao.sys.entity.OriginalProfitLog;
import com.liaoliao.sys.entity.TaskLog;
import com.liaoliao.sys.entity.UserTask;
import com.liaoliao.sys.service.AdvertService;
import com.liaoliao.sys.service.HandleCountService;
import com.liaoliao.sys.service.OriginalProfitLogService;
import com.liaoliao.sys.service.TaskLogService;
import com.liaoliao.sys.service.UserTaskService;
import com.liaoliao.user.entity.FocusLog;
import com.liaoliao.user.entity.ReadHistory;
import com.liaoliao.user.entity.Users;
import com.liaoliao.user.service.FocusLogService;
import com.liaoliao.user.service.ReadHistoryService;
import com.liaoliao.user.service.UserService;
import com.liaoliao.util.CommonUtil;
import com.liaoliao.util.HanlpKeyWords;
import com.liaoliao.util.RC4Kit;
import com.liaoliao.util.RandomKit;
import com.liaoliao.util.StaticKey;
import com.liaoliao.util.TimeKit;


@Controller
@RequestMapping(value="/api")
public class ContentAction {
	
	
	@Autowired
	private KeyWordsService keyWordsService;
	@Autowired
	private VideoService videoService;
	
	@Autowired
	private ArticleService articleService;

	@Autowired
	private RedisService redisService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ArticleCommentService articleCommentService;
	
	@Autowired
	private VideoCommentService videoCommentService;
	
	@Autowired
	private HandleCountService handleCountService;
	
	@Autowired
	private OriginalArticleInfoService originalArticleInfoService;
	
	@Autowired
	private OriginalVideoInfoService originalVideoInfoService;
	
	@Autowired
	private FenrunLogService  fenrunLogService;
	
	@Autowired
	private TaskLogService taskLogService;
	
	@Autowired
	private UserTaskService userTaskService;
	
	@Autowired
	private OriginalProfitLogService originalProfitLogService;
	
	@Autowired
	private FocusLogService focusLogService;
	
	@Autowired
	private CommonService commonService;
	
	@Autowired
	private LikesService likesService;
	
	@Autowired
	AdvertService advertService;
	
	@Autowired
	private ReadHistoryService readHistoryService;
	private Integer page = 1;
	//Users user=userService.queryOne(10029);
	/**
	 * 获取视频数据：
	 * @return
	 */
	@RequestMapping(value="/getVideo")
	@ResponseBody 
	public Map<String,Object> getVideo(HttpServletRequest request,Integer pageNo,Integer flushType,Integer userId){
		Map<String,Object> map=new HashMap<String,Object>();
		if(flushType==null||(flushType!=0&&flushType!=1)){
			map.put("msg", "参数为空或异常");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		if(pageNo==null){
			pageNo=page;
		}else{
			pageNo=pageNo>1?pageNo:page;
		}
		List<Video> list = null;
		Map<String,Object> hashMap = new HashMap<String,Object>();
		if(flushType==1){
			
			hashMap.put("flushType", flushType);
			list = videoService.findAll(pageNo,hashMap);
			
		}
		if(flushType==0){
			list = videoService.findByRand(30);//由8-->30 暂时采用此方法解决刷新和下拉造成内容重复(因为请求内容少,看了几条之后就是重复内容)
		}
		// Map-->List-->Map 三层转换
		List<Map<String, Object>> datas = new ArrayList<>();
		Map<String, Object> item = null;
		Users luser = userService.findById(StaticKey.liaoliaoVideoId);
		Long luserFl = focusLogService.countNum(StaticKey.liaoliaoVideoId);
		luserFl = focusLogService.countNum(luser.getId());
		
		//随机出虚拟用户替换官方用户(User,关注人数)
		luser=userService.findInventUserByRand();
		if(luser!=null){
			luserFl = focusLogService.countNum(luser.getId());
		}
		
				
		int luserCount=0;
		if(luserFl != null){
			luserCount= luserFl.intValue();
		}
		
		
		List<Likes> likes = null;
		if(userId!=null){
			likes = likesService.findLikesById(userId,1);
		}
		Map<Integer,Object> likesMap = new HashMap<Integer,Object>();
		Integer likeType = 0;
		if(likes!=null){
			for (Likes li : likes) {
				likesMap.put(li.getContentId(), 1);//1:视频
			}
		}
		
		
		
		for(Video video : list){
			//随机出虚拟用户替换官方用户(User,关注人数)
			luser=userService.findInventUserByRand();
			if(luser!=null){
				luserFl = focusLogService.countNum(luser.getId());
			}
			
			if(luserFl != null){
				luserCount= luserFl.intValue();
			}
			
			item = new LinkedHashMap<>();
			item.put("id", video.getId());
			item.put("title", video.getTitle());
			item.put("description", video.getDescription());
			if(video.getImgUrl()==null){
				video.setImgUrl("http://appliaoliao.oss-cn-hangzhou.aliyuncs.com/sys_imgs/video_default2.jpg?x-oss-process=style/blank_style");
			}
			item.put("imgUrl", video.getImgUrl());
			item.put("videoUrl", video.getVideoUrl());
//			item.put("duration", video.getDuration());
			item.put("duration", null);
			item.put("playMoneySum", video.getPlayMoneySum());
//			System.out.println(video.getDuration());
//			item.put("commentCount", video.getCommentCount());
//			item.put("likingCount", video.getLikingCount());
//			item.put("playCount", video.getPlayCount());
			item.put("type", video.getType());
			item.put("commentCount", ThreadLocalRandom.current().nextInt(100, 1000));
			item.put("likingCount", ThreadLocalRandom.current().nextInt(1000, 3000));
			item.put("sendingCount", ThreadLocalRandom.current().nextInt(1000, 3000));
			item.put("playCount", ThreadLocalRandom.current().nextInt(9000, 25000));
			//随机出现的打赏金额
			item.put("playMoneySum", ThreadLocalRandom.current().nextInt(3000, 25000));
//			RC4加密
			String idStr = RC4Kit.encry_RC4_string(String.valueOf(video.getId()), "liao");
			item.put("shareUrl", "/share/video/"+idStr);
			if(video.getType()==1){
				Users user = userService.findById(video.getSourceId());
				if(user==null){
					item.put("name", luser.getNickName());//料料头条
					item.put("userId",luser.getId());
					item.put("avatar",luser.getAvatar());
					item.put("focusCount", luserCount);
			
					//虚拟用户的关注数(非数据库中数据)开始
					if(luser.getId()>=2000&&luser.getId()<=2009){
						item.put("focusCount", 20138);
					}
					if(luser.getId()>=2010 &&luser.getId()<=2019 ){
						item.put("focusCount",10026 );
					}
					if(luser.getId()>=2020 &&luser.getId()<=2039 ){
						item.put("focusCount",5386 );
					}
					if(luser.getId()>=2040 &&luser.getId()<=2059 ){
						item.put("focusCount",2563 );
					}
					if(luser.getId()>=2060 &&luser.getId()<=2079 ){
						item.put("focusCount",1257 );
					}
					if(luser.getId()>=2080 &&luser.getId()<=2099 ){
						item.put("focusCount",625 );
					}
					if(luser.getId()>=2100 &&luser.getId()<=2119 ){
						item.put("focusCount",319 );
					}
					if(luser.getId()>=2120 &&luser.getId()<=2139 ){
						item.put("focusCount",151 );
					}
					if(luser.getId()>=2140 &&luser.getId()<=2159 ){
						item.put("focusCount",86 );
					}
					if(luser.getId()>=2160 &&luser.getId()<=2179 ){
						item.put("focusCount",43 );
					}
					if(luser.getId()>=2180 &&luser.getId()<=2199 ){
						item.put("focusCount",28 );
					}
					//虚拟用户的关注数结束
					
					
					
					
				}else{
					Long number = focusLogService.countNum(user.getId());
					item.put("focusCount", number!=null?number:0);
					
					//虚拟用户的关注数(非数据库中数据)开始
					if(luser.getId()>=2000&&luser.getId()<=2009){
						item.put("focusCount", 20138);
					}
					if(luser.getId()>=2010 &&luser.getId()<=2019 ){
						item.put("focusCount",10026 );
					}
					if(luser.getId()>=2020 &&luser.getId()<=2039 ){
						item.put("focusCount",5386 );
					}
					if(luser.getId()>=2040 &&luser.getId()<=2059 ){
						item.put("focusCount",2563 );
					}
					if(luser.getId()>=2060 &&luser.getId()<=2079 ){
						item.put("focusCount",1257 );
					}
					if(luser.getId()>=2080 &&luser.getId()<=2099 ){
						item.put("focusCount",625 );
					}
					if(luser.getId()>=2100 &&luser.getId()<=2119 ){
						item.put("focusCount",319 );
					}
					if(luser.getId()>=2120 &&luser.getId()<=2139 ){
						item.put("focusCount",151 );
					}
					if(luser.getId()>=2140 &&luser.getId()<=2159 ){
						item.put("focusCount",86 );
					}
					if(luser.getId()>=2160 &&luser.getId()<=2179 ){
						item.put("focusCount",43 );
					}
					if(luser.getId()>=2180 &&luser.getId()<=2199 ){
						item.put("focusCount",28 );
					}
					//虚拟用户的关注数结束
					
					item.put("name", user.getNickName());
					item.put("userId", user.getId());
					item.put("avatar", user.getAvatar());
				}
			}else{
				item.put("name", luser.getNickName());//料料头条
				item.put("userId",luser.getId());
				item.put("avatar",luser.getAvatar());
				item.put("focusCount", luserCount);
				
				//虚拟用户的关注数(非数据库中数据)开始
				if(luser.getId()>=2000&&luser.getId()<=2009){
					item.put("focusCount", 20138);
				}
				if(luser.getId()>=2010 &&luser.getId()<=2019 ){
					item.put("focusCount",10026 );
				}
				if(luser.getId()>=2020 &&luser.getId()<=2039 ){
					item.put("focusCount",5386 );
				}
				if(luser.getId()>=2040 &&luser.getId()<=2059 ){
					item.put("focusCount",2563 );
				}
				if(luser.getId()>=2060 &&luser.getId()<=2079 ){
					item.put("focusCount",1257 );
				}
				if(luser.getId()>=2080 &&luser.getId()<=2099 ){
					item.put("focusCount",625 );
				}
				if(luser.getId()>=2100 &&luser.getId()<=2119 ){
					item.put("focusCount",319 );
				}
				if(luser.getId()>=2120 &&luser.getId()<=2139 ){
					item.put("focusCount",151 );
				}
				if(luser.getId()>=2140 &&luser.getId()<=2159 ){
					item.put("focusCount",86 );
				}
				if(luser.getId()>=2160 &&luser.getId()<=2179 ){
					item.put("focusCount",43 );
				}
				if(luser.getId()>=2180 &&luser.getId()<=2199 ){
					item.put("focusCount",28 );
				}
				//虚拟用户的关注数结束
				
			}
			
			if(userId!=null&&redisService.getValidate(request,userId)){
				if(video.getType()==1){
					FocusLog fl = focusLogService.findByFocusId(userId, video.getSourceId());
					if(fl!=null&&fl.getStatus()==1){
						item.put("focusStatus", StaticKey.FocusTrue);
					}else{
						item.put("focusStatus", StaticKey.FocusFlase);
					}
				}else{
					FocusLog fl = focusLogService.findByFocusId(userId, StaticKey.liaoliaoVideoId);
					fl = focusLogService.findByFocusId(userId, luser.getId());
					if(fl!=null&&fl.getStatus()==1){
						item.put("focusStatus", StaticKey.FocusTrue);
					}else{
						item.put("focusStatus", StaticKey.FocusFlase);
					}
				}
			}else{
						item.put("focusStatus", StaticKey.FocusFlase);
			}
			
			
			if(likesMap!=null&&likesMap.containsKey(video.getId())){
				likeType = 1;
			}
			
			
			item.put("likeType",likeType);
			
			datas.add(item);
		}
//		统计每日videoList点击量
		handleCountService.handleCountPlusOne("videoList");
		map.put("list", datas);
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}
	
	
	
	/**
	 * 获取文章数据
	 * @return
	 * @throws ParseException 
	 */
	@RequestMapping(value="/getArticle")
	@ResponseBody
	public Map<String,Object> getArticle(HttpServletRequest request,Integer kindId,Integer pageNo,Integer flushType,Integer userId) {
		Map<String,Object> map = new HashMap<String,Object>();
		if(flushType==null||(flushType!=0&&flushType!=1)){
			map.put("msg", "参数为空或异常");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		if(pageNo==null){
			pageNo=page;
		}else{
			pageNo=pageNo>1?pageNo:page;
		}
		
		List<Article> list =null;//articleService.findFive()
		if(flushType==1){
			list = articleService.findByKind(kindId,pageNo);
			System.out.println(list.size());
		}
		
		if(flushType==0){
			list = articleService.findByRand(kindId, 30);  //由8-->30 暂时采用此方法解决刷新和下拉造成内容重复(因为请求内容少,看了几条之后就是重复内容)
		}
		
		//获取用户点赞文章记录
		List<Likes> likes = null;
		if(userId!=null){
			likes = likesService.findLikesById(userId,0);//0:文章 
		}
		
		Map<Integer,Object> likesMap = new HashMap<Integer,Object>();
		Integer likeType = 0;
		if(likes!=null){
			for (Likes li : likes) {
				likesMap.put(li.getContentId(), 1);
			}
		}
		
		List imgListObj = new ArrayList();
		// Map-->List-->Map 三层转换(保留)
		List<Map<String, Object>> datas = new ArrayList<>();
		Map<String, Object> item = null;
		for(Article article : list){
			Users users=userService.queryOne(article.getSourceId());
			item = new LinkedHashMap<>();
			if(users!=null){
				item.put("userId", users.getId());
				item.put("userName", users.getNickName());
				item.put("userTitleImg", users.getAvatar());	
			}else{
				Users user=userService.queryOne(10029);
				item.put("userId",user.getId());
				item.put("userName", user.getNickName());
				item.put("userTitleImg", user.getAvatar());
			}
			item.put("playMoneySum", ThreadLocalRandom.current().nextInt(3000, 25000));
			item.put("id", article.getId());
			item.put("title", article.getTitle());
			item.put("description", article.getDescription());
			item.put("imgUrl", article.getImgUrl());
			imgListObj = new ArrayList();
			String imglistStr = article.getImgList();
			if(!StringUtils.isBlank(imglistStr)){
				Map imgMap = (Map) JSONObject.parse(imglistStr);
				imgListObj = (List) imgMap.get("imgList");
			}
			item.put("imgList", imgListObj);
			item.put("type", article.getType());
//			item.put("readingCount", article.getReadingCount());
//			item.put("likingCount", article.getLikingCount());
//			item.put("commentCount", article.getCommentCount());
			if(article.getType()==1){
				item.put("readingCount", article.getReadingCount()*10);//暂定将阅读数和视频播放数扩大十倍
				item.put("likingCount", article.getLikingCount());
				item.put("sendingCount", article.getSendingCount());
				item.put("commentCount", article.getCommentCount());
				
			}else{
				item.put("readingCount", ThreadLocalRandom.current().nextInt(9000, 25000)*10);//暂定将阅读数和视频播放数扩大十倍
				item.put("likingCount", ThreadLocalRandom.current().nextInt(1000, 3000));
				item.put("sendingCount", ThreadLocalRandom.current().nextInt(1000, 3000));
				item.put("commentCount", ThreadLocalRandom.current().nextInt(100, 1000));
			}
			
			if(likesMap!=null&&likesMap.containsKey(article.getId())){
				likeType = 1;
			}
			item.put("likeType",likeType);
			
			
			datas.add(item);
		}
//		统计每日articleList点击量
		handleCountService.handleCountPlusOne("articleList");
		map.put("list", datas);
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}
	
	
	/**
	 * 根据Id获取文章内容
	 * @return
	 * @throws ParseException 
	 * 
	 */
	@RequestMapping(value="/getContent")
	@ResponseBody
	public Map<String,Object> getContent(HttpServletRequest request,Integer articleId,Integer userId) {
		Map<String,Object> map=new HashMap<String,Object>();
		if(articleId==null||"".equals(articleId)){
			map.put("msg", "articleId为空");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		//article 获取不到数据为Null
		Article article=articleService.findById(articleId);
		
		if(article==null){
			map.put("msg", "该文章不存在");
			map.put("code", StaticKey.ReturnServerNullError);
			return map;
		}
		ReadHistory readHistory=readHistoryService.findByUserId(articleId, userId,0);
		if(readHistory!=null){
			readHistory.setNum(readHistory.getNum()+1);
			readHistoryService.updateMoble(readHistory);
		}else{
			ReadHistory history=new ReadHistory(UUID.randomUUID().toString(),userId,articleId,0,new Date(),1);
			readHistoryService.savemobile(history);
		}
		
		/*//关键字提取
		//使用正则表达式清空文章内容里的标签
		String strs=article.getContent().replaceAll("</?[^>]+>", "");
		StringBuffer buffer=new StringBuffer(strs).append(article.getDescription()).append(article.getTitle());
		List<String> list=HanlpKeyWords.getMainIdea(buffer.toString());
		List<String> liStrings=HanlpKeyWords.getDuanYu(buffer.toString());
		list.addAll(liStrings);
		KeyWords keyWords=null;
		for (String string : list) {
			keyWords=keyWordsService.findById(userId, string);
			if(keyWords!=null){
				keyWords.setFreq(keyWords.getFreq()+1);
				keyWordsService.updateMoble(keyWords);
				continue;
			}else{
				keyWords =new KeyWords();
				keyWords.setName(string); 
				keyWords.setUserId(userId);
				keyWords.setAddDate(new Date());
				keyWords.setFreq(1);
				keyWords.setId(UUID.randomUUID().toString());
				keyWordsService.add(keyWords);
			}
		} */    
		//在内容中穿插广告(在第一个p标签后穿插)
		String cssStyle = "<style type=\"text/css\">img{width:100%;text-align:center;margin:10px 0px;}body{font-size:12px}p{margin:5px 5px;}</style>";
		String content = cssStyle;
		//获得广告
		String toOrder = advertService.toOrder();
		
		String articleContent = article.getContent();
		//遍历拼接在第一个位置加入广告
		if(toOrder!=null&&toOrder.length()>0){
			//以</p>切割
			String[] split = articleContent.split("</p>");
			StringBuffer sb = new StringBuffer();
			sb.append(split[0]);
			sb.append("</p>");
			sb.append("<p>");
			sb.append("<div>");
			//sb.append(toOrder);   //此处的位置有待确定,且添加待指定的广告
			sb.append("</div>");
			sb.append("</p>");
			for(int i=1;i<split.length;i++){
				sb.append(split[i]);
				sb.append("</p>");
			}
			content+=sb.toString();
		}else{
			content+=articleContent;
		}
		
		
		String addd = "?timestamp="+System.currentTimeMillis()/1000L+"&articleId="+articleId+"&random="+(int) (Math.random()*10000);
//		阅读文件获取收益时间
		int articleCoinTime = 99;
		String articleCoinTimeStr = redisService.getConfigValue("articleCoinTime");
		if(StringUtils.isBlank(articleCoinTimeStr)){
			System.out.println("请检查SystemConfig表数据");
		}else{
			articleCoinTime = Integer.valueOf(articleCoinTimeStr);
		}
		
		map.put("advertTop", "/share/top"+addd);
		map.put("advertTopFloat", "/share/topFloat"+addd);
		map.put("advertBottom", "/share/bottom"+addd);
		map.put("content", content);
//		RC4加密
		String idStr = RC4Kit.encry_RC4_string(String.valueOf(article.getId()), "liao");
		map.put("shareUrl", "/share/article/"+idStr);
		map.put("coinTime", articleCoinTime);
		
		Users luser = userService.findById(StaticKey.liaoliaoArticleId);
		Long luserFl = focusLogService.countNum(StaticKey.liaoliaoArticleId);
		luserFl = focusLogService.countNum(luser.getId());
		
		//随机出虚拟用户替换官方用户(User,关注人数)
		luser=userService.findInventUserByRand();
		if(luser!=null){
			luserFl = focusLogService.countNum(luser.getId());
		}
		
		
		
		int luserCount=0;
		if(luserFl!=null){
			luserCount= luserFl.intValue();
		}
		if(article.getType()==1){
			Users user = userService.findById(article.getSourceId());
			//user=userService.findById(luser.getId());//使用随机的虚拟用户替换官方用户
			//System.out.println("我是原创的 line 421 ContentAction.java /getContent");
			if(user==null){
				map.put("name", luser.getNickName());//料料头条
				map.put("userId",luser.getId());
				map.put("avatar",luser.getAvatar());
				map.put("focusCount", luserCount);
			}else{
				Long number = focusLogService.countNum(user.getId());
				map.put("focusCount", number!=null?number:0);
				
				
				//虚拟用户的关注数(非数据库中数据)开始
				if(user.getId()>=2000&&user.getId()<=2009){
					map.put("focusCount", 20356);
				}
				if(user.getId()>=2010 &&user.getId()<=2019 ){
					map.put("focusCount",10253 );
				}
				if(user.getId()>=2020 &&user.getId()<=2039 ){
					map.put("focusCount",5023 );
				}
				if(user.getId()>=2040 &&user.getId()<=2059 ){
					map.put("focusCount",2560 );
				}
				if(user.getId()>=2060 &&user.getId()<=2079 ){
					map.put("focusCount",1251 );
				}
				if(user.getId()>=2080 &&user.getId()<=2099 ){
					map.put("focusCount",625 );
				}
				if(user.getId()>=2100 &&user.getId()<=2119 ){
					map.put("focusCount",385 );
				}
				if(user.getId()>=2120 &&user.getId()<=2139 ){
					map.put("focusCount",154 );
				}
				if(user.getId()>=2140 &&user.getId()<=2159 ){
					map.put("focusCount",81 );
				}
				if(user.getId()>=2160 &&user.getId()<=2179 ){
					map.put("focusCount",47 );
				}
				if(user.getId()>=2180 &&user.getId()<=2199 ){
					map.put("focusCount",27 );
				}
				//虚拟用户的关注数结束
				
				map.put("name", user.getNickName());
				map.put("userId", user.getId());
				map.put("avatar", user.getAvatar());
			}
		}else{
			map.put("name", luser.getNickName());//料料头条
			map.put("userId",luser.getId());
			map.put("avatar",luser.getAvatar());
			map.put("focusCount", luserCount);
			
			//虚拟用户的关注数(非数据库中数据)开始
			if(luser.getId()>=2000&&luser.getId()<=2009){
				map.put("focusCount", 20138);
			}
			if(luser.getId()>=2010 &&luser.getId()<=2019 ){
				map.put("focusCount",10026 );
			}
			if(luser.getId()>=2020 &&luser.getId()<=2039 ){
				map.put("focusCount",5386 );
			}
			if(luser.getId()>=2040 &&luser.getId()<=2059 ){
				map.put("focusCount",2563 );
			}
			if(luser.getId()>=2060 &&luser.getId()<=2079 ){
				map.put("focusCount",1257 );
			}
			if(luser.getId()>=2080 &&luser.getId()<=2099 ){
				map.put("focusCount",625 );
			}
			if(luser.getId()>=2100 &&luser.getId()<=2119 ){
				map.put("focusCount",319 );
			}
			if(luser.getId()>=2120 &&luser.getId()<=2139 ){
				map.put("focusCount",151 );
			}
			if(luser.getId()>=2140 &&luser.getId()<=2159 ){
				map.put("focusCount",86 );
			}
			if(luser.getId()>=2160 &&luser.getId()<=2179 ){
				map.put("focusCount",43 );
			}
			if(luser.getId()>=2180 &&luser.getId()<=2199 ){
				map.put("focusCount",28 );
			}
			//虚拟用户的关注数结束
			
			
			
		}
		
		if(userId!=null&&redisService.getValidate(request,userId)){
			if(article.getType()==1){
				FocusLog fl = focusLogService.findByFocusId(userId, article.getSourceId());
			//	fl = focusLogService.findByFocusId(userId, luser.getId()); 如果是原创的视频或文章,保持真实数据
				if(fl!=null&&fl.getStatus()==1){
					map.put("focusStatus", StaticKey.FocusTrue);
				}else{
					map.put("focusStatus", StaticKey.FocusFlase);
				}
			}else{
				FocusLog fl = focusLogService.findByFocusId(userId, StaticKey.liaoliaoArticleId);
				fl = focusLogService.findByFocusId(userId, luser.getId());//如果文章不是原创的,使用虚拟用户替代官方用户
				if(fl!=null&&fl.getStatus()==1){
					map.put("focusStatus", StaticKey.FocusTrue);
				}else{
					map.put("focusStatus", StaticKey.FocusFlase);
				}
			}
		}else{
				map.put("focusStatus", StaticKey.FocusFlase);
		}
		map.put("msg", "success");
		map.put("code", StaticKey.ReturnServerTrue);
		
//		阅读数+1
		article.setReadingCount(article.getReadingCount()+1);
		articleService.updateArticle(article);
		
//		统计每日articleContent点击量
		handleCountService.handleCountPlusOne("articleContent");
		
		//获取用户点赞文章记录
		List<Likes> likes = null;
		if(userId!=null){
			likes = likesService.findLikesById(userId,0);//0:文章 
		}
		
		Map<Integer,Object> likesMap = new HashMap<Integer,Object>();
		Integer likeType = 0;
		if(likes!=null){
			for (Likes li : likes) {
				likesMap.put(li.getContentId(), 1);
			}
		}
		
		if(likesMap!=null&&likesMap.containsKey(article.getId())){
			likeType = 1;
		}
		map.put("likeType",likeType);
		
//		内容页大拇指上方显示本篇文章点赞数: 目前随机:100-1000
		RandomKit ran = new RandomKit();
		int countLike = ran.getRandomBetween(100, 1000);
		map.put("countLike", countLike);
		
		//写在阅读分润里面
	/*	if(article.getType()==1){//原创作品被阅读，作者获得1料币
			Users user = userService.findById(article.getSourceId());
			OriginalArticleInfo oai = originalArticleInfoService.findByArticleId(article.getId());
			if(user!=null&&oai!=null){
				int money = StaticKey.OriginalReadMoney;
				if(user.getVipStatus()==StaticKey.UserVipStatusTrue){
					money = money*2;
				}
				if(oai.getCountMoney()>=money){
				user.setTotalMoney(user.getTotalMoney()+money);
				user.setDayMoney(user.getDayMoney()+money);
				userService.updateUser(user);
				FenrunLog fl = new FenrunLog();
				fl.setAddTime(new Date());
				fl.setUser(user);
				fl.setMoney(money);
				fl.setType(StaticKey.FenrunArticle);
				fl.setContentId(article.getId());
				oai.setCountMoney(oai.getCountMoney()-money);
				oai.setReadCount(oai.getReadCount()+1);
				originalArticleInfoService.updateOAI(oai);
				fenrunLogService.saveFenrunLog(fl);
				handleCountService.handleCountTotalMoney("totalProfitMoney", money);
				}
			}
			
		}*/
		
		return map;
	}

	
	
	
	
	/**
	 * 用户点赞  
	 * @param request
	 * @param id   :  文章或者视频id
	 * @param type ：0：文章    1：视频
	 * @return
	 */
	@RequestMapping(value="/likeCount")
	@ResponseBody
	public Map<String,Object> likeCount(HttpServletRequest request,Integer userId,Integer contentId,Integer type){
		Map<String,Object> map=new HashMap<String,Object>();
		if(contentId==null||type==null || userId==null){
			map.put("msg", "id为空！");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		Users user = userService.findById(userId);
		if(user==null){
			map.put("msg", "查询用户未空!");
			map.put("code", StaticKey.ReturnServerNullError);
			return map;
		}
		//0:表示文章
		if(type==0){
			Article article=articleService.findById(contentId);
			if(article==null){
				map.put("msg", "id为空！");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			article.setLikingCount(article.getLikingCount()+1);
			articleService.updateArticle(article);
			Likes likes = new Likes();
			likes.setUser(user);
			likes.setContentId(contentId);
			likes.setType(type);
			likes.setAddTime(new Date());
			likesService.saveLikes(likes);
		}else
		//1:表示视频
		if(type==1){
			Video video=videoService.findById(contentId);
			if(video==null){
				map.put("msg", "id为空！");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			video.setLikingCount(video.getLikingCount()+1);
			videoService.updateVideo(video);
			Likes likes = new Likes();
			likes.setUser(user);
			likes.setContentId(contentId);
			likes.setType(type);
			likes.setAddTime(new Date());
			likesService.saveLikes(likes);
		}else{
			map.put("msg", "类型错误");
			map.put("code", StaticKey.ReturnServerNullError);
			return map;
		}
		map.put("msg", "success");
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}
	
	/**
	 * 进入评论列表
	 * @param request
	 * @param contentId
	 * @param type 0:文章   1：视频
	 * @return
	 */
	@RequestMapping("/getCommentList")
	@ResponseBody
	public Map<String,Object> getCommentList(HttpServletRequest request,Integer contentId, Integer type){
		Map<String,Object> map=new HashMap<String,Object>();
		if(contentId==null||type==null||(type!=0&&type!=1)){
			map.put("msg", "参数为空或有误！");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		List<Map<String,Object>> datas = new ArrayList<>();
		if(type==0){
			Article article=articleService.findById(contentId);
			if(article==null){
				map.put("msg", "文章不存在！");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			List<ArticleComment> articleCommentList=articleCommentService.findByArticleId(contentId);
			for(ArticleComment articleComment:articleCommentList){
				Map<String,Object> dataMap = new HashMap<String,Object>();
//				dataMap.put("articleId", articleComment.getArticle().getId());
				dataMap.put("content", articleComment.getContent());
//				if(articleComment.getUser()==null){
//					dataMap.put("userId", "");
//				}else{
//					dataMap.put("userId", articleComment.getUser().getId());
//				}
				if(articleComment.getUser()!=null){
					dataMap.put("avatarUrl", articleComment.getUser().getAvatar());
					dataMap.put("userName", articleComment.getUser().getNickName());
				}else{
					dataMap.put("avatarUrl", articleComment.getAvatarUrl());
					dataMap.put("userName", articleComment.getUserName());
				}
				dataMap.put("likingCount", articleComment.getLikingCount());
				dataMap.put("addTime", articleComment.getAddTime());
				datas.add(dataMap);
			}
		}else
		if(type==1){
			Video video=videoService.findById(contentId);
			if(video==null){
				map.put("msg", "视频不存在！");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			List<VideoComment> videoCommentList = videoCommentService.findByVideoId(contentId);
			for(VideoComment videoComment:videoCommentList){
				Map<String,Object> dataMap = new HashMap<String,Object>();
//				dataMap.put("videoId", videoComment.getVideo().getId());
				dataMap.put("content", videoComment.getContent());
//				if(videoComment.getUser()==null){
//					dataMap.put("userId", "");
//				}else{
//					dataMap.put("userId", videoComment.getUser().getId());
//				}
				dataMap.put("avatarUrl", videoComment.getAvatarUrl());
				dataMap.put("userName", videoComment.getUserName());
				dataMap.put("likingCount", videoComment.getLikingCount());
				dataMap.put("addTime", videoComment.getAddTime());
				datas.add(dataMap);
			}
		}
//		统计每日commentList点击量
		handleCountService.handleCountPlusOne("commentList");
		map.put("datas", datas);
		map.put("msg", "success");
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}
	
	
	
	/**
	 * 接收用户评论内容
	 * @param request
	 * @param articleId
	 * @return
	 */
	@RequestMapping(value="/userComment")
	@ResponseBody
	public Map<String,Object> userComment(HttpServletRequest request,Integer userId,String content,Integer contentId,Integer type){
		Map<String,Object> map=new HashMap<String,Object>();
		if(userId==null||StringUtils.isBlank(content)||contentId==null||type==null){
			map.put("msg", "有参数为空！");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		if(!redisService.getValidate(request,userId)){
			map.put("msg", "token失效或错误");
			map.put("code", StaticKey.ReturnClientTokenError);
			return map;
		}
		Users user=userService.findById(userId);
		if(type==0){
			Article article=articleService.findById(contentId);
			if(user==null||article==null){
				map.put("msg", "数据不存在！");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			ArticleComment articleComment=new ArticleComment();
			if(user.getNickName()==null){
				articleComment.setUserName(user.getMobile());
			}else{
				articleComment.setUserName(user.getNickName());
			}
			content = CommonUtil.emojiFilter(content);
			articleComment.setLikingCount(0);
			articleComment.setUser(user);
			articleComment.setContent(content);
			articleComment.setAvatarUrl(user.getAvatar());
			articleComment.setArticle(article);
			articleComment.setAddTime(new Date());
			articleCommentService.saveComment(articleComment);
//			评论数+1
			article.setCommentCount(article.getCommentCount()+1);
			articleService.updateArticle(article);
		}else if(type==1){
			Video video=videoService.findById(contentId);
			if(user==null||video==null){
				map.put("msg", "数据不存在！");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			VideoComment videoComment=new VideoComment();
			if(user.getNickName()==null){
				videoComment.setUserName(user.getMobile());
			}else{
				videoComment.setUserName(user.getNickName());
			}
			videoComment.setLikingCount(0);
			videoComment.setUser(user);
			videoComment.setContent(content);
			videoComment.setAvatarUrl(user.getAvatar());
			videoComment.setVideo(video);
			videoComment.setAddTime(new Date());
			videoCommentService.saveComment(videoComment);
//			评论数+1
			video.setCommentCount(video.getCommentCount()+1);
			videoService.updateVideo(video);
		}else{
			map.put("msg", "type值错误！");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		map.put("msg", "success！");
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}
	
	/**
	 * 转发文章或者视频
	 * @param request
	 * @param id
	 * @param type :0:文章   1：视频
	 * 
	 * @return
	 */
	@RequestMapping(value="/sendCount")
	@ResponseBody
	public Map<String,Object> sendCount(HttpServletRequest request,Integer contentId,Integer type,Integer userId,Integer getTask){
		Map<String,Object> map=new HashMap<String,Object>();
		
		//判断用户是否领取任务
		if(1!=getTask){
			map.put("msg", "该用户未领取任务");
			map.put("code", StaticKey.NotReceiveTask);
		}
		
		if(contentId==null||type==null){
			map.put("msg", "参数异常！");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		if(type==0){
			Article article=articleService.findById(contentId);
			if(article==null){
				map.put("msg", "数据不存在！");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			article.setSendingCount(article.getSendingCount()+1);
			articleService.updateArticle(article);
//			统计每日shareArticle成功分享量
			handleCountService.handleCountPlusOne("shareArticle");
			
			if(article.getType()==1){
				OriginalArticleInfo oai=originalArticleInfoService.findByArticleId(article.getId());
				if(oai==null){
					map.put("msg", "数据不存在！");
					map.put("code", StaticKey.ReturnServerNullError);
					return map;
				}
				if(oai.getCountMoney()-StaticKey.OriginalShareMoney>=0){
					Users originalUser = userService.findById(article.getSourceId());//获取原创作品作者
					if(originalUser!=null){
						int money =StaticKey.OriginalShareMoney;
						if(originalUser.getVipStatus()==StaticKey.UserVipStatusTrue){
							money=money*2; 
						}
						originalUser.setTotalMoney(originalUser.getTotalMoney()+money);
						originalUser.setDayMoney(originalUser.getDayMoney()+money);
						oai.setCountMoney(oai.getCountMoney()-money);
						oai.setShareCount(oai.getShareCount()+1);
						userService.updateUser(originalUser);
						originalArticleInfoService.updateOAI(oai);
						
						OriginalProfitLog opl = new OriginalProfitLog();
						opl.setAddTime(new Date());
						opl.setContentId(article.getId());
						opl.setType(0);
						opl.setMoney(money);
						opl.setUser(originalUser);
						originalProfitLogService.saveOriginalProfitLog(opl);
						
						/*FenrunLog fl = new FenrunLog();
						fl.setAddTime(new Date());
						fl.setContentId(article.getId());
						fl.setMoney(money);
						fl.setType(StaticKey.FenrunOriginalArticle);
						fl.setUser(originalUser);
						fenrunLogService.saveFenrunLog(fl);*/
						
						handleCountService.handleCountTotalMoney("totalProfitMoney", money);
					}
				}
			}	
		}else
		if(type==1){
			Video video=videoService.findById(contentId);
			if(video==null){
				map.put("msg", "数据不存在！");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			video.setSendingCount(video.getSendingCount()+1);
			videoService.updateVideo(video);
//			统计每日shareVideo成功分享量
			handleCountService.handleCountPlusOne("shareVideo");
			if(video.getType()==1){
				OriginalVideoInfo ovi=originalVideoInfoService.findByVideoId(video.getId());
				if(ovi==null){
					map.put("msg", "数据不存在！");
					map.put("code", StaticKey.ReturnServerNullError);
					return map;
				}
				if(ovi.getCountMoney()-StaticKey.OriginalShareMoney>=0){
					Users originalUser = userService.findById(video.getSourceId());//获取原创作品作者
					if(originalUser!=null){
						int money =StaticKey.OriginalShareMoney;
						if(originalUser.getVipStatus()==StaticKey.UserVipStatusTrue){
							money=money*2; 
						}
						originalUser.setTotalMoney(originalUser.getTotalMoney()+money);
						originalUser.setDayMoney(originalUser.getDayMoney()+money);
						ovi.setCountMoney(ovi.getCountMoney()-money);
						ovi.setShareCount(ovi.getShareCount()+1);
						userService.updateUser(originalUser);
						originalVideoInfoService.updateOVI(ovi);
						
						OriginalProfitLog opl = new OriginalProfitLog();
						opl.setAddTime(new Date());
						opl.setContentId(video.getId());
						opl.setType(0);
						opl.setMoney(money);
						opl.setUser(originalUser);
						originalProfitLogService.saveOriginalProfitLog(opl);
						
						/*FenrunLog fl = new FenrunLog();
						fl.setAddTime(new Date());
						fl.setContentId(video.getId());
						fl.setMoney(money);
						fl.setType(StaticKey.FenrunOriginalVideo);
						fl.setUser(originalUser);
						fenrunLogService.saveFenrunLog(fl);*/
						handleCountService.handleCountTotalMoney("totalProfitMoney", money);
					}
				}
			}
		}else{
			map.put("msg", "类型错误！");
			map.put("code", StaticKey.ReturnServerNullError);
			return map;
		}
		
		//如果已经登录，并且转发文章，并且已经领取任务，那么就是完成了任务
		if(userId!=null&&!("".equals(userId))&&getTask!=0&&getTask!=null){
			if(!redisService.getValidate(request,userId)){
				map.put("msg", "token失效或错误");
				map.put("code", StaticKey.ReturnClientTokenError);
				return map;
			}
			Users user = userService.findById(userId);
			//分享转发
			TaskLog taskLog = taskLogService.findDayExist(TimeKit.todayStart(), 3, userId);
			if(taskLog==null){
				taskLog = new TaskLog();
				taskLog.setFinishTime(new Date());
				taskLog.setStatus(2);
				taskLog.setUser(user);
				taskLog.setObtain(0);
				UserTask ut = userTaskService.findById(3);
				taskLog.setUserTask(ut);//查询出用户完成修改昵称这条记录
				taskLogService.savaTaskLog(taskLog);
			}else if(taskLog.getStatus()==1){
				taskLog.setStatus(2);
				taskLog.setFinishTime(new Date());
				taskLogService.updateTaskLog(taskLog);
			}
		}
		
		map.put("msg", "success！");
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}
	
	/**
	 * 播放视频
	 * @param request
	 * @param contentId
	 * @return
	 */
	@RequestMapping(value="/playCount")
	@ResponseBody
	public Map<String,Object> playCount(HttpServletRequest request,Integer contentId,Integer userId){
		Map<String,Object> map=new HashMap<String,Object>();
		if(contentId==null){
			map.put("msg", "参数异常！");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}

		Video video=videoService.findById(contentId);
		if(video==null){
			map.put("msg", "数据不存在！");
			map.put("code", StaticKey.ReturnServerNullError);
			return map;
		}
		video.setPlayingCount(video.getPlayingCount()+1);
		ReadHistory readHistory=readHistoryService.findByUserId(contentId, userId,1);
		if(readHistory!=null){
			if(readHistory.getNum()==null){
				readHistory.setNum(0);
			}
			readHistory.setNum(readHistory.getNum()+1);
			readHistoryService.updateMoble(readHistory);
		}else{
			ReadHistory history=new ReadHistory(UUID.randomUUID().toString(),userId,contentId,1,new Date(),1);
			readHistoryService.savemobile(history);
		}
		videoService.updateVideo(video);
//		统计每日videoContent点击量
		handleCountService.handleCountPlusOne("videoContent");
		
		//写在阅读分润里面
//		统计原创视频   
/*		if(video.getType()==1){
			OriginalVideoInfo ovi=originalVideoInfoService.findByVideoId(video.getId());
			if(ovi.getCountMoney()-StaticKey.OriginalReadMoney>=0){
				Users originalUser = userService.findById(video.getSourceId());//获取原创作品作者
				if(originalUser!=null){
					int money =StaticKey.OriginalReadMoney;
					if(originalUser.getVipStatus()==StaticKey.UserVipStatusTrue){
						money=money*2; 
					}
					originalUser.setTotalMoney(originalUser.getTotalMoney()+money);
					originalUser.setDayMoney(originalUser.getDayMoney()+money);
					ovi.setCountMoney(ovi.getCountMoney()-money);
					ovi.setReadCount(ovi.getReadCount()+1);
					userService.updateUser(originalUser);
					originalVideoInfoService.updateOVI(ovi);
					FenrunLog fl = new FenrunLog();
					fl.setAddTime(new Date());
					fl.setContentId(video.getId());
					fl.setMoney(money);
					fl.setType(StaticKey.FenrunOriginalVideo);
					fl.setUser(originalUser);
					fenrunLogService.saveFenrunLog(fl);
					handleCountService.handleCountTotalMoney("totalProfitMoney", money);
				}
			}
		}*/
		
		map.put("msg", "success！");
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}
	
	
	/**
	 * 原创视频作者信息
	 * @param request
	 * @param videoId
	 * @return
	 */
	@ResponseBody 
	@RequestMapping(value="/videoUserInfo")
	public Map<String,Object> videoUserInfo(HttpServletRequest request,Integer videoId){
		Map<String,Object> map=new HashMap<String,Object>();
		if(videoId==null||"".equals(videoId)){
			map.put("msg", "传值为空!");
			map.put("code", StaticKey.ReturnClientNullError);
			return map;
		}
		Video video = videoService.findById(videoId);
		if(video==null){
			map.put("msg", "数据不存在！");
			map.put("code", StaticKey.ReturnServerNullError);
			return map;
		}
		if(video.getType()==1){
			Users user = userService.findById(video.getSourceId());
			if(user==null){
				map.put("msg", "原创作者不存在!");
				map.put("code", StaticKey.ReturnServerNullError);
				return map;
			}
			Long number = focusLogService.countNum(user.getId());
			map.put("beConcernedFocus", number);
			map.put("name", user.getNickName());
			map.put("id", user.getId());
			map.put("avatar", user.getAvatar());
		}else{
			map.put("name", "");//料料头条
			map.put("id", "");
			map.put("avatar", "");
			map.put("beConcernedFocus", "");
		}
		map.put("msg", "success");
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}
	
	
	/**
	 * AD+文章+视频
	 * @return
	 */
	/*@RequestMapping(value="/getAdArticleVideo")
	@ResponseBody 
	public Map<String,Object> getAdArticleVideo(){
		Map<String,Object> map=new HashMap<String,Object>();
		
		Video video = null;
		Article article = null;
		
		List imgListObj = new ArrayList();
		// Map-->List-->Map 三层转换
		List<Map<String, Object>> datas = new ArrayList<>();
		Map<String, Object> item = null;
		for (int i = 0; i < 3; i++) {
			video = videoService.findByRand(1).get(0);
			article = articleService.findByRand(1,1).get(0);
			
			item = new LinkedHashMap<>();
//			video
			item.put("videoId", video.getId());
			item.put("videoTitle", video.getTitle());
			item.put("videoDescription", video.getDescription());
			item.put("videoImgUrl", video.getImgUrl());
			item.put("videoUrl", video.getVideoUrl());
			item.put("videoDuration", video.getDuration());
//			item.put("videoCommentCount", video.getCommentCount());
//			item.put("videoPlayCount", video.getPlayCount());
			item.put("videoCommentCount", ThreadLocalRandom.current().nextInt(100, 1000));
			item.put("videoLikingCount", ThreadLocalRandom.current().nextInt(1000, 3000));
			item.put("videoSendingCount", ThreadLocalRandom.current().nextInt(1000, 3000));
			item.put("videoPlayCount", ThreadLocalRandom.current().nextInt(9000, 25000));
			item.put("videoShareUrl", "/share/article/"+video.getId());
//			article
			item.put("articleId", article.getId());
			item.put("articleTitle", article.getTitle());
			item.put("articleDescription", article.getDescription());
			item.put("articleImgUrl", article.getImgUrl());
			String imglistStr = article.getImgList();
			if(!StringUtils.isBlank(imglistStr)){
				Map imgMap = (Map) JSONObject.parse(imglistStr);
				imgListObj = (List) imgMap.get("imgList");
			}
			item.put("articleImgList", imgListObj);
//			item.put("articleReadingCount", article.getReadingCount());
//			item.put("articleCommentCount", article.getCommentCount());
			item.put("articleReadingCount", ThreadLocalRandom.current().nextInt(9000, 25000));
			item.put("articleLikingCount", ThreadLocalRandom.current().nextInt(1000, 3000));
			item.put("articleSendingCount", ThreadLocalRandom.current().nextInt(1000, 3000));
			item.put("articleCommentCount", ThreadLocalRandom.current().nextInt(100, 1000));
//			Ad
			int key = i+1;
			String addd = "?timestamp="+System.currentTimeMillis()/1000L+"&articleId="+article.getId()+"&videoId="+video.getId()+"&random="+(int) (Math.random()*10000);
			item.put("advertUrl", "/share/more"+key+addd);
			datas.add(item);
		}
		map.put("list", datas);
		map.put("code", StaticKey.ReturnServerTrue);
		return map;
	}*/

	/**
	 * 下一篇 文章详情
	 * @return
	 */
	@RequestMapping(value="/getNextContent")
	@ResponseBody 
	public Map<String,Object> getNextContent(HttpServletRequest request,Integer userId){
		Map<String,Object> map=new HashMap<String,Object>();
//		随机获取Article
		Article article = articleService.findByRand(1,1).get(0);
		String cssStyle = "<style type=\"text/css\">img{width:100%;text-align:center;margin:10px 0px;}body{font-size:12px}p{margin:5px 5px;}</style>";
		String content = cssStyle + article.getContent();
		String addd = "?timestamp="+System.currentTimeMillis()/1000L+"&articleId="+article.getId()+"&random="+(int) (Math.random()*10000);
//		阅读文章获取收益时间
		int articleCoinTime = 99;
		String articleCoinTimeStr = redisService.getConfigValue("articleCoinTime");
		if(StringUtils.isBlank(articleCoinTimeStr)){
			System.out.println("请检查SystemConfig表数据");
		}else{
			articleCoinTime = Integer.valueOf(articleCoinTimeStr);
		}
		
		map.put("advertTop", "/share/top"+addd);
		map.put("advertTopFloat", "/share/topFloat"+addd);
		map.put("advertBottom", "/share/bottom"+addd);
		map.put("content", content);
//		RC4加密
		String idStr = RC4Kit.encry_RC4_string(String.valueOf(article.getId()), "liao");
		map.put("shareUrl", "/share/article/"+idStr);
		map.put("coinTime", articleCoinTime);
		
		map.put("id", article.getId());
		map.put("type", article.getType());
		map.put("title", article.getTitle());
		map.put("description", article.getDescription());
		map.put("imgUrl", article.getImgUrl());
		map.put("commentCount", ThreadLocalRandom.current().nextInt(100, 1000));
		
		Users luser = userService.findById(StaticKey.liaoliaoArticleId);
		Long luserFl = focusLogService.countNum(StaticKey.liaoliaoArticleId);
		
		//随机出虚拟用户替换官方用户(User,关注人数)
		luser=userService.findInventUserByRand();
		if(luser!=null){
			luserFl = focusLogService.countNum(luser.getId());
		}
		luserFl = focusLogService.countNum(luser.getId());
		
		int luserCount=0;
		if(luserFl!=null){
			luserCount= luserFl.intValue();
		}
		if(article.getType()==1){
			Users user = userService.findById(article.getSourceId());
			if(user==null){
				map.put("name", luser.getNickName());//料料头条
				map.put("userId",luser.getId());
				map.put("avatar",luser.getAvatar());
				map.put("focusCount", luserCount);
				
				//虚拟用户的关注数(非数据库中数据)开始
				if(luser.getId()>=2000&&luser.getId()<=2009){
					map.put("focusCount", 20138);
				}
				if(luser.getId()>=2010 &&luser.getId()<=2019 ){
					map.put("focusCount",10026 );
				}
				if(luser.getId()>=2020 &&luser.getId()<=2039 ){
					map.put("focusCount",5386 );
				}
				if(luser.getId()>=2040 &&luser.getId()<=2059 ){
					map.put("focusCount",2563 );
				}
				if(luser.getId()>=2060 &&luser.getId()<=2079 ){
					map.put("focusCount",1257 );
				}
				if(luser.getId()>=2080 &&luser.getId()<=2099 ){
					map.put("focusCount",625 );
				}
				if(luser.getId()>=2100 &&luser.getId()<=2119 ){
					map.put("focusCount",319 );
				}
				if(luser.getId()>=2120 &&luser.getId()<=2139 ){
					map.put("focusCount",151 );
				}
				if(luser.getId()>=2140 &&luser.getId()<=2159 ){
					map.put("focusCount",86 );
				}
				if(luser.getId()>=2160 &&luser.getId()<=2179 ){
					map.put("focusCount",43 );
				}
				if(luser.getId()>=2180 &&luser.getId()<=2199 ){
					map.put("focusCount",28 );
				}
				//虚拟用户的关注数结束
				
			}else{
				Long number = focusLogService.countNum(user.getId());
				map.put("focusCount", number!=null?number:0);
				map.put("name", user.getNickName());
				map.put("userId", user.getId());
				map.put("avatar", user.getAvatar());
			}
		}else{
			map.put("name", luser.getNickName());//料料头条
			map.put("userId",luser.getId());
			map.put("avatar",luser.getAvatar());
			map.put("focusCount", luserCount);
			
			//虚拟用户的关注数(非数据库中数据)开始
			if(luser.getId()>=2000&&luser.getId()<=2009){
				map.put("focusCount", 20138);
			}
			if(luser.getId()>=2010 &&luser.getId()<=2019 ){
				map.put("focusCount",10026 );
			}
			if(luser.getId()>=2020 &&luser.getId()<=2039 ){
				map.put("focusCount",5386 );
			}
			if(luser.getId()>=2040 &&luser.getId()<=2059 ){
				map.put("focusCount",2563 );
			}
			if(luser.getId()>=2060 &&luser.getId()<=2079 ){
				map.put("focusCount",1257 );
			}
			if(luser.getId()>=2080 &&luser.getId()<=2099 ){
				map.put("focusCount",625 );
			}
			if(luser.getId()>=2100 &&luser.getId()<=2119 ){
				map.put("focusCount",319 );
			}
			if(luser.getId()>=2120 &&luser.getId()<=2139 ){
				map.put("focusCount",151 );
			}
			if(luser.getId()>=2140 &&luser.getId()<=2159 ){
				map.put("focusCount",86 );
			}
			if(luser.getId()>=2160 &&luser.getId()<=2179 ){
				map.put("focusCount",43 );
			}
			if(luser.getId()>=2180 &&luser.getId()<=2199 ){
				map.put("focusCount",28 );
			}
			//虚拟用户的关注数结束
			
			
			
		}
		
		if(userId!=null&&redisService.getValidate(request,userId)){
			if(article.getType()==1){
				FocusLog fl = focusLogService.findByFocusId(userId, article.getSourceId());
				if(fl!=null&&fl.getStatus()==1){
					map.put("focusStatus", StaticKey.FocusTrue);
				}else{
					map.put("focusStatus", StaticKey.FocusFlase);
				}
			}else{
				FocusLog fl = focusLogService.findByFocusId(userId, StaticKey.liaoliaoArticleId);
				if(fl!=null&&fl.getStatus()==1){
					map.put("focusStatus", StaticKey.FocusTrue);
				}else{
					map.put("focusStatus", StaticKey.FocusFlase);
				}
			}
		}else{
				map.put("focusStatus", StaticKey.FocusFlase);
		}
		map.put("msg", "success");
		map.put("code", StaticKey.ReturnServerTrue);
		
//		阅读数+1
		article.setReadingCount(article.getReadingCount()+1);
		articleService.updateArticle(article);
		
//		统计每日nextArticle点击量
		handleCountService.handleCountPlusOne("articleNext");
		return map;
	}
	
	
	
	
}
