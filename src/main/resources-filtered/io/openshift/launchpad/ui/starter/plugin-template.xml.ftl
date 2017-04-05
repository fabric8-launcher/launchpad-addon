<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>
   <groupId>ignored</groupId>
   <artifactId>ignored</artifactId>
   <version>1</version>
   <build>
      <plugins>
         <!-- The plugin will be read and added to the model -->
         <plugin>
            <groupId>io.fabric8</groupId>
            <artifactId>fabric8-maven-plugin</artifactId>
            <version>${version.fabric8-maven-plugin}</version>
            <executions>
               <execution>
                  <goals>
                     <goal>resource</goal>
                     <goal>build</goal>
                  </goals>
               </execution>
            </executions>
            <configuration>
               <resources>
                  <labels>
                     <service>
                        <expose>true</expose>
                     </service>
                  </labels>
               </resources>
               <#if type == "vert.x">
               <generator>
                  <config>
                     <java-exec>
                        <mainClass>io.vertx.core.Launcher</mainClass>
                     </java-exec>
                  </config>
               </generator>
               </#if>
            </configuration>
         </plugin>
      </plugins>
   </build>
</project>