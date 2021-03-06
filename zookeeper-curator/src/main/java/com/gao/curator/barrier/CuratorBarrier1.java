package com.gao.curator.barrier;

import java.util.Random;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.barriers.DistributedDoubleBarrier;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;

/**
 * 双栅栏Double Barrier
 */
public class CuratorBarrier1 {

	public static void main(String[] args) throws Exception {
        final TestingServer server = new TestingServer();
		for(int i = 0; i < 5; i++){
			new Thread(()->{
                try {
                    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
                    CuratorFramework cf = CuratorFrameworkFactory.builder()
                                .connectString(server.getConnectString())
                                .retryPolicy(retryPolicy)
                                .build();
                    cf.start();

                    DistributedDoubleBarrier barrier = new DistributedDoubleBarrier(cf, "/super", 5);
                    Thread.sleep(1000 * (new Random()).nextInt(3));
                    System.out.println(Thread.currentThread().getName() + "已经准备");
                    barrier.enter();//阻塞
                    System.out.println("同时开始运行...");
                    Thread.sleep(1000 * (new Random()).nextInt(3));
                    System.out.println(Thread.currentThread().getName() + "运行完毕");
                    barrier.leave();//阻塞
                    System.out.println("同时退出运行...");


                } catch (Exception e) {
                    e.printStackTrace();
                }
			},"t" + i).start();
		}
	}
}