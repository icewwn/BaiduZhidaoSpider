
package test.com.hitsz.util;


import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hitsz.model.QA;
import com.hitsz.spider.Baidu;
import com.hitsz.util.Log;
import com.hitsz.util.QADBUtil;
import com.hitsz.util.config.ConfigUtil;
import com.hitsz.util.config.Configuration;
import com.hitsz.util.config.PropNotConfigedException;

public class TestBaidu {
	static Baidu baidu = null;
	
	
	@BeforeClass
	public static void create(){
		baidu = new Baidu();
	}
	
	/**
	 * 第一步运行这个函数，将每个问句的搜索页面保存到本地
	 * 
	 * 测试下载每个问句的baidu搜索网页
	 */
	@Test
	public void testDownloadItems() {
		// 读取未下载过的问句列表
		baidu.getQuerysFromDB(0);
		List<String> keywords = baidu.getKeywords();
		
		// 下载所有的问句对应的搜索页面，最多下载10 页
		for(String keyword : keywords){
			Log.info("Downloading the pages about \"" + keywords + "\" now !\n");
			baidu.downLoadPages(keyword);
			
			QADBUtil.setQueryFinished(keyword);
			
		}
	
	}
	
	/**
	 * 第二步 分析第一步下载下来的文件，将搜索的每个item存入到数据库qapair_resultlist中 
	 * 
	 * 
	 * 分析每个问题的搜索网页，并将每个结果存入到数据库中
	 * 对每个问题解析完之后，需要对TermList清空
	 * 
	 */
	@Test
	public void testParseItemPages(){
		//获取所有已经下载过的问句列表
		baidu.getQuerysFromDB(1);
		
		List<String> keywords = baidu.getKeywords();
		//分析下载下来的对应的问句页面，将搜索页面对应的每个item，保存到数据库中
		for(String keyword : keywords){
			baidu.parsePage(keyword);	
			
			baidu.saveItemListIntoDB(keyword);
			
//			baidu.saveTermList(keyword);
			
			baidu.getBdu().getTermList().clear();
		}
	}
	
	/**
	 * 第三步 在第二步后，在从数据库中的qapair_resultlist中选出出那些还没有下载过的items，保存到一个list中
	 * 		对每个url的list遍历，逐一下载对应的问答网页，然后以问题的id命名文件，保存到本地
	 * 
	 *  从数据库qapair_resultlist中获取所有 还没有下载过
	 *  的item的URL，然后进行下载
	 */
	@Test
	public void testDownLoadQAPages(){
		//获取所有qapair_resultlist中还没有下载的urls，然后逐一下载，保存到本地，并以问句对应的id命名
		List<String> urls = QADBUtil.getUrlsFromQAPair(0);
		int count = 0;
		for(String url : urls){
			
			/**
			 *  如果某个网页由于频繁的访问baidu而被禁止，
			 *  就是线程休息5分钟在再访问
			 */
			int i = 1;
			int oneDelay = 1000;
			int tenDelay = 1000;
			int maxDelay = 1000;
			try {
				oneDelay = ConfigUtil.strToInt(ConfigUtil.getPropValue("oneDelay"))  * 1000;
				tenDelay = ConfigUtil.strToInt(ConfigUtil.getPropValue("tenDelay"))  * 1000;
				maxDelay = ConfigUtil.strToInt(ConfigUtil.getPropValue("maxDelay")) * 1000;
			} catch (PropNotConfigedException e1) {
				e1.printStackTrace();
			}
			/**
			 * 循环下载某个下载不成功的网页
			 * 延迟不能大于maxdelay
			 */
			 while( !baidu.downLoadItem(url)){
				 Log.info("Will sleep "+ i * oneDelay/1000 +" seconds and try download again !!!");
				 try {
					 if(i * oneDelay > maxDelay)
						 break;
					Thread.sleep(i * oneDelay);
					i++;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				 
			 }
			
			 /**
			  * 设置finished标记位
			  * 
			  */
			 QADBUtil.setQAPairResultFinished(url);
			 
			/**
			 *   每总共下载 10个网页就暂停 tenDelay秒，
			 *   每下载 1 个网页暂停 onedelay 秒
			 *   主要是防止 baidu 封锁 IP 或者禁止访问
			 */
			try {
				if(10 == count){
					Thread.sleep( tenDelay);
					count = 0;
				}
				else{
					Thread.sleep( oneDelay);
					count++;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * 第四步，将第三步中保存下来的问答对网页逐一分析，本将每个问答对网页结果存入到数据库qapair,baiduuser中
	 * 
	 * 分析每个问答对的网页
	 * 并保存到数据库中
	 */
	@Test
	public void testParseQAPage(){
		
		List<QA> qas = baidu.parserQAPages();
		
		QADBUtil.saveQAList(qas);
		
		
	}

}
