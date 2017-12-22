# ![catapult](media/catapult.png)
Create a distributed, fault tolerant **application as a service** by implementing **3 callback functions**. Catapult is a mesos framework SDK that abstracts the scheduler and executor interfaces, and handles persistent storage for application state. Catapult allows the developer to define with what level of detail they want to control their application.

# Build Requirements
- java
- maven

# Install
Catapult is not currently hosted as a maven artifact so you'll need to download the source and built it manually :(. During the install phase of the build the jar file will be installed into your local maven repo.
```
git clone https://github.com/timbouvier/catapult.git
cd catapult
make install
```

# Usage
The major components of each catapult application are outlined below. 
  - Scheduler
    - SchedulerNode
      - An object you will extend to implement any node level logic and event processing
    - SchedulerApp
      - An object you will extend to implement any app level logic and event processing
    - MesosAppListener
      - An interface you will implement to handle any framework level logic and event processing
  - Executor
    - ExecutableApp
      - An interface you will implement so the catapult library can start your application node
 
#### Create ExecutableApp
```java
public class MyExecutableApp implements ExecutableApp {

  @Override
  public void restart(){
    ...
  }
  
  @Override
  public int getExitStatus() {
    ...
  }
  
  @Override
  public void message(AppExecutor appExecutor, byte[] message){
    ...
  }
  
  @Override
  public void kill(){
    ...
  }
  
  @Override
  public void run(AppExecutor executor){
    ...
  }
}
```

#### Create Scheduler Node Listener
```java
import com.tbouvier.scheduler.Protos;

public class MyNode extends SchedulerNode {
   
   public MyNode(Protos.SchedulerNodeInfo nodeInfo){
      /*initialize parent obj with nodeInfo*/
      super(nodeInfo);
   }

   @Override
   public void failed(AppDriver appDriver){
      /*default implementation will relaunch node*/
   }
   
   @Override
   public void running(AppDriver appDriver){
     /*default implementation does nothing*/
   }
   
   @Override
   public void finished(AppDriver appDriver){
    /*default implementation does nothing*/
   }
   
   @Override
   public void killed(AppDriver appDriver){
     /*default implementation relaunches node*/
   }
   
   @Override 
   public void message(AppDriver appDriver, byte[] data){
     /*default implementation drops message*/
   }
}
```
The schedulerNode callbacks are events for pretty much what they look like they're events for. The message callback is a communication channel between the SchedulerNode and the ExecutableApp (what's running on the physical node).

#### Create Scheduler Application Listener
```java
public class MyApplication extends SchedulerApp {
  
   public MyApplication(){
      super("my-app-name");
   }
  
   @Override
   public void initialized(AppDriver appDriver, Protos.AppID appID){
   
      Protos.ContainerLiteInfo.Builder containerLiteInfo = Protos.ContainerLiteInfo.newBuilder();
   
     /*create nodeInfo*/
     Protos.SchedulerNodeInfo nodeInfo = Protos.SchedulerNodeInfo.newBuilder()
   
     /*Start launching nodes*/
     MyNode node = new MyNode();
     appDriver.launchNode(node);
   }
   
   @Override
   public void initFailed(){
    /*
    no driver supplied because it failed to create it
    Most common reason for this to happen is if zookeeper 
    or mesos environment variables are misconfigured
    */
   }
}
```
There are additional methods that can be overriden but the default implementations are almost always what you want. For instance, "message" can be overriden but its default implementation delivers the message to the schedulerNode object to which it belongs.

#### Create Main Framework Listener
```java
public class MyFrameworkListener implements MesosAppListener {
  
  public void disconnected(AppFramework appFramework){
    ...
  }
  
  public void connected(AppFramework appFramework){
    /*start deploying apps!*/
    MyApplication app = new MyApplication();
    appFramework.register(app);
  }
  
  public void applicationFailed(AppFramework appFramework){
    ...
  }
}
```
Once the connected callback fires you can safely assume the library is initialized an ready to receive API callins.

#### Putting it All Together

##### Scheduler Main
```java
import com.verizon.mesos.MesosAppFramework;
import com.tbouvier.mesos.scheduler.Protos;

public class SchedulerMain {

  public static void main(String[] args){
    Protos.SchedulerConfig schedulerConfig = Protos.SchedulerConfig.newBuilder()
             .setZooKeeperInfo(Protos.ZookeeperInfo.newBuilder()
                     .setAddress("zk://my-zookeeper-ip-list")
                     .setRootNode("framework-root-zk-node-name")
                      .build())
             .setMesosInfo(Protos.MesosInfo.newBuilder()
                     .setAddress("zk://my-zookeeper-ip-list/mesos/")
                     .build())
             .setFrameworkName("my-framework")
             .build();
    
    MyFrameworkListener myFramework = new MyFrameworkListener();
    new MesosAppFramework(schedulerConfig, myFramework).run();
  }
}
```

##### Executor Main
```java
public class ExecutorMain {

  public static void main(String[] args){
     new MesosApplicationFrameworkExecutor(new MyExecutableApp()).run();
  }
}
```




