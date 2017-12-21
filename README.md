# ![catapult](media/catapult.png)
Create a distributed, fault tolerant **application as a service** by implementing **3 callback functions**. Catapult is a mesos framework SDK that abstracts the scheduler and executor interfaces, and handles persistent storage for application state. Catapult allows the developer to define with what level of detail they want to control their application.

# Prerequisites
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

#### Create Main Application Listener
```java
public class MyAppListener extends MesosAppListener {
  
  public void disconnected(AppFramework app){
    ...
  }
  
  public void connected(AppFramework app){
    ...
  }
  
  public void applicationFailed(AppFramework app){
    ...
  }
}
```

