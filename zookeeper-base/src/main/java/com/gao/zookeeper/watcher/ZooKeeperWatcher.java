package com.gao.zookeeper.watcher;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.Stat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   名称: ZooKeeperWatcher.java <br>
 *   描述: zookeeper监控功能<br>
 *   类型: JAVA <br>
 *   最近修改时间:2017/9/6 14:32.<br>
 *   @version [版本号, V1.0]
 *   @since 2017/9/6 14:32.
 *   @author gaoshudian
 */
public class ZooKeeperWatcher implements Watcher{

    /** 定义原子变量 */
    AtomicInteger seq = new AtomicInteger();
    /** 定义session失效时间 */
    private static final int SESSION_TIMEOUT = 10000;
    /** zookeeper服务器地址 */
    private static final String CONNECTION_ADDR = "172.16.35.204:2181,172.16.35.204:2182,172.16.35.204:2183";
    /** zk父路径设置 */
    private static final String PARENT_PATH = "/testWatch";
    /** zk子路径设置 */
    private static final String CHILDREN_PATH = "/testWatch/children";
    /** 进入标识 */
    private static final String LOG_PREFIX_OF_MAIN = "【Main】";
    /** zk变量 */
    private ZooKeeper zk = null;
    /** 信号量设置，用于等待zookeeper连接建立之后 通知阻塞程序继续向下执行 */
    private CountDownLatch connectedSemaphore = new CountDownLatch(1);

    /**
     * 主要测试watch功能
     */
    public static void main(String[] args) throws Exception {
        //建立watcher
        ZooKeeperWatcher zkWatch = new ZooKeeperWatcher();
        //创建连接
        zkWatch.createConnection(CONNECTION_ADDR, SESSION_TIMEOUT);
        // 清理节点
        zkWatch.deleteAllTestPath();
        //创建父节点
        zkWatch.createPath(PARENT_PATH, System.currentTimeMillis() + "");
        Thread.sleep(1000);
        // 读取父节点数据
        zkWatch.readData(PARENT_PATH, true);
        // 读取子节点
        zkWatch.getChildren(PARENT_PATH, true);
        // 更新父节点数据
        zkWatch.writeData(PARENT_PATH, System.currentTimeMillis() + "");
        Thread.sleep(1000);
        // 创建子节点
        zkWatch.createPath(CHILDREN_PATH, System.currentTimeMillis() + "");
        Thread.sleep(1000);
        //修改子节点数据
        zkWatch.writeData(CHILDREN_PATH, System.currentTimeMillis() + "");
        Thread.sleep(1000);
        // 清理节点
        zkWatch.deleteAllTestPath();
        Thread.sleep(1000);
        zkWatch.releaseConnection();
    }

    /**
     * 收到来自Server的Watcher通知后的处理。
     */
    @Override
    public void process(WatchedEvent event) {
        System.out.println("进入 process 。。。。。event = " + event);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (event == null) {
            return;
        }
        // 连接状态
        Event.KeeperState keeperState = event.getState();
        // 事件类型
        Event.EventType eventType = event.getType();
        // 受影响的path
        String path = event.getPath();
        String logPrefix = "【Watcher-" + this.seq.incrementAndGet() + "】";
        System.out.println(logPrefix + "收到Watcher通知");
        System.out.println(logPrefix + "连接状态:\t" + keeperState.toString());
        System.out.println(logPrefix + "事件类型:\t" + eventType.toString());
        if (Event.KeeperState.SyncConnected == keeperState) {

            if (Event.EventType.None == eventType) {// 成功连接上ZK服务器
                System.out.println(logPrefix + "成功连接上ZK服务器");
                connectedSemaphore.countDown();
            }else if (Event.EventType.NodeCreated == eventType) {//创建节点
                System.out.println(logPrefix + "节点创建");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.exists(path, true);
            }else if (EventType.NodeDataChanged == eventType) {//更新节点
                System.out.println(logPrefix + "节点数据更新");
                System.out.println("我看看走不走这里........");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(logPrefix + "数据内容: " + this.readData(PARENT_PATH, true));
            }else if (EventType.NodeChildrenChanged == eventType) {//更新子节点
                System.out.println(logPrefix + "子节点变更");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(logPrefix + "子节点列表：" + this.getChildren(PARENT_PATH, true));
            }else if (EventType.NodeDeleted == eventType) {//删除节点
                System.out.println(logPrefix + "节点 " + path + " 被删除");
            }
        }else if (KeeperState.Disconnected == keeperState) {
            System.out.println(logPrefix + "与ZK服务器断开连接");
        }else if (KeeperState.AuthFailed == keeperState) {
            System.out.println(logPrefix + "权限检查失败");
        }else if (KeeperState.Expired == keeperState) {
            System.out.println(logPrefix + "会话失效");
        }
        System.out.println("--------------------------------------------");

    }

    //创建ZK连接
    public void createConnection(String connectAddr, int sessionTimeout) {
        this.releaseConnection();
        try {
            zk = new ZooKeeper(connectAddr, sessionTimeout, this);
            System.out.println(LOG_PREFIX_OF_MAIN + "开始连接ZK服务器");
            connectedSemaphore.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //关闭ZK连接
    public void releaseConnection() {
        if (this.zk != null) {
            try {
                this.zk.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //创建节点
    private boolean createPath(String path, String data) {
        try {
            //设置监控(由于zookeeper的监控都是一次性的所以 每次必须设置监控)
            this.zk.exists(path, true);
            System.out.println(LOG_PREFIX_OF_MAIN + "节点创建成功, Path: " + this.zk.create(path,data.getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT ) +", content: " + data);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //读取指定节点数据内容
    private String readData(String path, boolean needWatch) {
        try {
            return new String(this.zk.getData(path, needWatch, null));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //更新指定节点数据内容
    private boolean writeData(String path, String data) {
        try {
            System.out.println(LOG_PREFIX_OF_MAIN + "更新数据成功，path：" + path + ", stat: " +
                    this.zk.setData(path, data.getBytes(), -1));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //删除指定节点
    private void deleteNode(String path) {
        try {
            this.zk.delete(path, -1);
            System.out.println(LOG_PREFIX_OF_MAIN + "删除节点成功，path：" + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //判断指定节点是否存在
    private Stat exists(String path, boolean needWatch) {
        try {
            return this.zk.exists(path, needWatch);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //获取子节点
    private List<String> getChildren(String path, boolean needWatch) {
        try {
            return this.zk.getChildren(path, needWatch);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //删除所有节点
    private void deleteAllTestPath() {
        if(this.exists(CHILDREN_PATH, false) != null){
            this.deleteNode(CHILDREN_PATH);
        }
        if(this.exists(PARENT_PATH, false) != null){
            this.deleteNode(PARENT_PATH);
        }
    }
}