package com.gao.curator.watcher;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class CuratorWatcher1 {
	
	//zookeeper地址
//	static final String CONNECT_ADDR = "192.168.0.206:2181,192.168.0.207:2181,192.168.0.208:2181";
	static final String CONNECT_ADDR = "47.103.97.241:2181";
	//session超时时间
	static final int SESSION_OUTTIME = 50000;//ms
	
	public static void main(String[] args) throws Exception {
		
		//1 重试策略：初试时间为1s 重试10次
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
		//2 通过工厂创建连接
		CuratorFramework cf = CuratorFrameworkFactory.builder()
					.connectString(CONNECT_ADDR)
					.sessionTimeoutMs(SESSION_OUTTIME)
					.retryPolicy(retryPolicy)
					.build();
		
		//3 建立连接
		cf.start();
		
		//4 建立一个cache缓存
		final NodeCache cache = new NodeCache(cf, "/super", false);
		cache.start(true);
		cache.getListenable().addListener(new NodeCacheListener() {
			//触发事件为创建、更新和删除节点，在删除子节点的时候并不触发此操作
			@Override
			public void nodeChanged() throws Exception {
				ChildData data = cache.getCurrentData();
				if(data != null){
					System.out.println("路径为：" + cache.getCurrentData().getPath());
					System.out.println("数据为：" + new String(cache.getCurrentData().getData()));
					System.out.println("状态为：" + cache.getCurrentData().getStat());
					System.out.println("---------------------------------------");
				}else{
					System.out.println("节点被删除");
				}
			}
		});
		
		Thread.sleep(1000);
		cf.create().forPath("/super", "123".getBytes());
		
		Thread.sleep(1000);
		cf.setData().forPath("/super", "456".getBytes());
		
		Thread.sleep(1000);
		cf.delete().forPath("/super");
		Thread.sleep(Integer.MAX_VALUE);
	}
}
